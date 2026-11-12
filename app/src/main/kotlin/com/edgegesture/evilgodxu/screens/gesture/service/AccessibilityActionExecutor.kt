package com.edgegesture.evilgodxu.screens.gesture.service

import android.accessibilityservice.AccessibilityService
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.VibratorManager
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import com.edgegesture.evilgodxu.App
import androidx.core.net.toUri
import com.edgegesture.evilgodxu.R
import com.edgegesture.evilgodxu.data.gesture.GestureAction
import com.edgegesture.evilgodxu.data.gesture.GestureSettingsKeys
import com.edgegesture.evilgodxu.data.gesture.GestureSettingsState
import com.edgegesture.evilgodxu.data.gesture.expandPanelShortcutsFlow
import com.edgegesture.evilgodxu.data.gesture.gestureDataStore
import com.edgegesture.evilgodxu.data.gesture.saveExpandPanelShortcut
import com.edgegesture.evilgodxu.data.gesture.saveExpandPanelShortcutFreeform
import com.edgegesture.evilgodxu.data.permission.PermissionMonitor
import com.edgegesture.evilgodxu.data.permission.PermissionType
import com.edgegesture.evilgodxu.data.settings.ThemeMode
import com.edgegesture.evilgodxu.log.CrashLogManager
import com.edgegesture.evilgodxu.screens.gesture.service.expandpanel.ExpandPanelPermissionCallback
import com.edgegesture.evilgodxu.screens.gesture.service.expandpanel.ExpandPanelViewManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class AccessibilityActionExecutor(
    private val service: AccessibilityService
) : ExpandPanelPermissionCallback {

    // 主题来源是应用级仓库，服务只读装配器需要的流
    private fun themeModeFlow(): Flow<ThemeMode> =
        (service.application as App).container.settingsRepository.settings.map { it.themeMode }
    private var currentApp: String? = null
    private var previousApp: String? = null

    private val appContainer = (service.application as App).container

    private var flashlightOn = false
    private val cameraManager = service.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val freeformAppLauncher = FreeformAppLauncher(service)

    private var expandPanelViewManager: ExpandPanelViewManager? = null
    private var pendingExpandPanelShow = false
    private val permissionMonitor = PermissionMonitor(service)
    private val executorScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // 等待权限监控任务
    private var writeSettingsMonitorJob: kotlinx.coroutines.Job? = null

    @Volatile private var blacklistSnapshot: Set<String> = emptySet()

    init {
        executorScope.launch {
            (service as Context).gestureDataStore.data
                .map { prefs -> prefs[GestureSettingsKeys.APP_SWITCH_BLACKLIST] ?: emptySet() }
                .collect { blacklistSnapshot = it }
        }
    }

    fun performAction(
        action: GestureAction,
        settings: GestureSettingsState,
        launchAppTarget: String? = null,
        useFreeform: Boolean = false,
        remindMinutes: Int? = null
    ) {
        if (action == GestureAction.NONE) return
        vibrate(settings)
        executorScope.launch { appContainer.gestureStatsRepository.incrementGestureCount() }

        when (action) {
            GestureAction.HOME -> {
                service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
            }
            GestureAction.RECENT -> service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
            GestureAction.BACK -> service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
            // 无可切换目标或启动失败时回到最近任务，避免手势没有任何响应
            GestureAction.LAST_APP -> if (!switchToLastApp()) {
                service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
            }
            GestureAction.PREVIOUS_TRACK -> sendMediaKeyEvent(android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS)
            GestureAction.NEXT_TRACK -> sendMediaKeyEvent(android.view.KeyEvent.KEYCODE_MEDIA_NEXT)
            GestureAction.FLASHLIGHT -> toggleFlashlight()
            GestureAction.VOICE_ASSISTANT -> launchVoiceAssistant()
            GestureAction.POWER_MENU -> service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_POWER_DIALOG)
            GestureAction.LOCK_SCREEN -> service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
            GestureAction.SCREENSHOT -> service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT)
            GestureAction.FREEFORM_MODE -> launchCurrentAppInFreeform()
            GestureAction.EXPAND_PANEL -> showExpandPanel()
            GestureAction.ALIPAY_SCAN -> launchScanAlipay()
            GestureAction.WECHAT_SCAN -> launchScanWechat()
            GestureAction.REMIND -> scheduleReminder(remindMinutes ?: GestureSettingsKeys.DEFAULT_REMIND_MINUTES)
            GestureAction.LAUNCH_APP -> launchAppTarget?.let { launchApp(it, useFreeform) }
            GestureAction.NONE -> {}
        }
    }

    private fun showExpandPanel() {
        if (expandPanelViewManager != null) {
            expandPanelViewManager?.dismiss()
            return
        }
        val manager = ExpandPanelViewManager(
            context = service,
            shortcutsFlow = service.expandPanelShortcutsFlow(),
            themeModeFlow = themeModeFlow(),
            onShortcutSet = { index, packageName ->
                kotlinx.coroutines.runBlocking {
                    service.saveExpandPanelShortcut(index, packageName)
                }
            },
            onFreeformToggle = { index, enabled ->
                kotlinx.coroutines.runBlocking {
                    service.saveExpandPanelShortcutFreeform(index, enabled)
                }
            },
            onDismiss = {
                expandPanelViewManager = null
            },
            permissionCallback = this
        )
        expandPanelViewManager = manager
        val shown = manager.show()
        if (!shown) {
            // 权限未授予，等待用户授权后自动显示
            expandPanelViewManager = null
        }
    }

    // 实现 ExpandPanelPermissionCallback 接口
    override fun onRequestWriteSettings(): Boolean {
        // 启动权限监控，授权后自动显示扩展面板
        startWriteSettingsMonitor()
        // 跳转到系统设置页
        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
            data = "package:${service.packageName}".toUri()
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        service.startActivity(intent)
        return true
    }

    // 启动修改系统设置权限监控，授权后自动返回上一个应用并显示扩展面板
    private fun startWriteSettingsMonitor() {
        // 取消之前的监控
        writeSettingsMonitorJob?.cancel()
        pendingExpandPanelShow = true

        writeSettingsMonitorJob = executorScope.launch {
            permissionMonitor.monitorPermission(PermissionType.WRITE_SETTINGS, intervalMs = 500)
                .collect { granted ->
                    if (granted && pendingExpandPanelShow) {
                        pendingExpandPanelShow = false
                        // 权限已授予，在主线程先返回上一个应用，再显示扩展面板
                        kotlinx.coroutines.withContext(Dispatchers.Main) {
                            returnToPreviousAppAndShowExpandPanel()
                        }
                        writeSettingsMonitorJob?.cancel()
                    }
                }
        }
    }

    // 返回上一个应用并显示扩展面板
    private fun returnToPreviousAppAndShowExpandPanel() {
        // 先切换到上一个应用
        switchToLastApp()
        // 延迟后显示扩展面板，确保应用切换完成
        Handler(Looper.getMainLooper()).postDelayed({
            showExpandPanelAfterPermissionGranted()
        }, 300)
    }

    // 权限授予后显示扩展面板
    private fun showExpandPanelAfterPermissionGranted() {
        if (expandPanelViewManager != null) return
        val manager = ExpandPanelViewManager(
            context = service,
            shortcutsFlow = service.expandPanelShortcutsFlow(),
            themeModeFlow = themeModeFlow(),
            onShortcutSet = { index, packageName ->
                kotlinx.coroutines.runBlocking {
                    service.saveExpandPanelShortcut(index, packageName)
                }
            },
            onFreeformToggle = { index, enabled ->
                kotlinx.coroutines.runBlocking {
                    service.saveExpandPanelShortcutFreeform(index, enabled)
                }
            },
            onDismiss = {
                expandPanelViewManager = null
            },
            permissionCallback = this
        )
        expandPanelViewManager = manager
        val shown = manager.show()
        if (!shown) {
            expandPanelViewManager = null
        }
    }

    fun dismissExpandPanel() {
        expandPanelViewManager?.dismiss()
        expandPanelViewManager = null
        pendingExpandPanelShow = false
        writeSettingsMonitorJob?.cancel()
    }

    // 清理资源
    fun cleanup() {
        dismissExpandPanel()
        writeSettingsMonitorJob?.cancel()
        writeSettingsMonitorJob = null
        executorScope.cancel()
    }

    // 监听窗口变化事件，维护当前应用和上一个应用
    fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event?.eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED
        ) return

        val packageName = event.packageName?.toString() ?: return
        if (packageName == service.packageName) return
        if (packageName == currentApp) return

        val blacklist = blacklistSnapshot
        if (packageName in blacklist) return

        // 只记录可启动的应用：输入法、系统面板等临时窗口无法作为切换目标，
        // 记入后会让「上一个应用」锁死在一个启动不起来的包上
        if (service.packageManager.getLaunchIntentForPackage(packageName) == null) return

        previousApp = currentApp
        currentApp = packageName
    }

    private fun launchCurrentAppInFreeform() {
        val packageName = currentApp ?: return
        if (packageName == service.packageName) return
        freeformAppLauncher.launch(packageName, useFreeform = true)
    }

    // 切回上一个应用；显式目标命中应用切换黑名单时不作为切换目标，
    // 改用黑名单过滤后的上一个应用，避免落到桌面、系统弹窗等临时窗口。
    // 返回值表示是否成功切换，供调用方决定是否需要兜底
    fun switchToLastApp(target: String? = null): Boolean {
        val preferred = target?.takeIf { it.isNotBlank() && it !in blacklistSnapshot }
        // 与当前应用相同的记录不是有效的切换目标，交由调用方兜底
        val pkg = preferred
            ?: previousApp?.takeIf { it != currentApp && it !in blacklistSnapshot }
            ?: return false
        if (!launchApp(pkg)) return false
        previousApp = currentApp
        currentApp = pkg
        return true
    }

    // 切回指定应用（用于启动拦截后返回来源应用）：只启动该应用，不回退到其他应用。
    // 返回值表示是否成功切换，供调用方决定是否需要兜底
    fun switchToApp(packageName: String): Boolean {
        if (packageName.isBlank() || !launchApp(packageName)) return false
        previousApp = currentApp
        currentApp = packageName
        return true
    }

    fun invalidateBlacklistCache() {
    }

    fun launchApp(packageName: String, useFreeform: Boolean = false): Boolean {
        if (useFreeform) return freeformAppLauncher.launch(packageName, useFreeform = true)
        return try {
            val launchIntent = service.packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                service.startActivity(launchIntent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            CrashLogManager.logException("AccessibilityActionExecutor", "启动应用失败", e)
            false
        }
    }

    private fun sendMediaKeyEvent(keyCode: Int) {
        try {
            val audioManager = service.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            audioManager.dispatchMediaKeyEvent(android.view.KeyEvent(
                android.view.KeyEvent.ACTION_DOWN, keyCode
            ))
            audioManager.dispatchMediaKeyEvent(android.view.KeyEvent(
                android.view.KeyEvent.ACTION_UP, keyCode
            ))
        } catch (e: Exception) {
            CrashLogManager.logException("AccessibilityActionExecutor", "发送媒体按键失败", e)
        }
    }

    private fun toggleFlashlight() {
        try {
            val cameraId = cameraManager.cameraIdList.find { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
            cameraId?.let { id ->
                flashlightOn = !flashlightOn
                cameraManager.setTorchMode(id, flashlightOn)
            }
        } catch (e: CameraAccessException) {
            CrashLogManager.logException("AccessibilityActionExecutor", "切换手电筒失败", e)
        }
    }

    private fun launchVoiceAssistant() {
        try {
            val intent = Intent(Intent.ACTION_ASSIST).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                addCategory(Intent.CATEGORY_DEFAULT)
            }
            service.startActivity(intent)
        } catch (e: Exception) {
            CrashLogManager.logException("AccessibilityActionExecutor", "启动语音助手失败", e)
        }
    }

    private fun vibrate(settings: GestureSettingsState) {
        if (!settings.vibrationEnabled) return

        val vibratorManager = service.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        val vibrator = vibratorManager.defaultVibrator
        vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    // ============================================================
    // 扫一扫
    // ============================================================

    // 微信扫一扫：LauncherUI + From.Scaner.Shortcut extra
    private fun launchScanWechat() {
        try {
            val intent = Intent().apply {
                component = ComponentName("com.tencent.mm", "com.tencent.mm.ui.LauncherUI")
                putExtra("LauncherUI.From.Scaner.Shortcut", true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            service.startActivity(intent)
            return
        } catch (e: Exception) {
            CrashLogManager.logException("AccessibilityActionExecutor", "打开微信扫一扫失败", e)
        }

        // 备用 BaseCaptureUI（部分微信版本）
        try {
            val intent = Intent().apply {
                component = ComponentName("com.tencent.mm", "com.tencent.mm.plugin.scanner.ui.BaseCaptureUI")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            service.startActivity(intent)
            return
        } catch (e: Exception) {
            CrashLogManager.logException("AccessibilityActionExecutor", "打开微信扫一扫备用页面失败", e)
        }

        // 兜底：打开微信
        try {
            val launchIntent = service.packageManager.getLaunchIntentForPackage("com.tencent.mm")
            if (launchIntent != null) {
                launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                service.startActivity(launchIntent)
            }
        } catch (e: Exception) {
            CrashLogManager.logException("AccessibilityActionExecutor", "打开微信失败", e)
        }
    }

    // 支付宝扫一扫
    private fun launchScanAlipay() {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = "alipays://platformapi/startapp?saId=10000007".toUri()
                setPackage("com.eg.android.AlipayGphone")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            service.startActivity(intent)
        } catch (e: Exception) {
            CrashLogManager.logException("AccessibilityActionExecutor", "打开支付宝扫一扫失败", e)
            try {
                val launchIntent = service.packageManager.getLaunchIntentForPackage("com.eg.android.AlipayGphone")
                if (launchIntent != null) {
                    launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    service.startActivity(launchIntent)
                }
            } catch (e2: Exception) {
                CrashLogManager.logException("AccessibilityActionExecutor", "打开支付宝失败", e2)
            }
        }
    }

    // ============================================================
    // 延时提醒
    // ============================================================

    private fun scheduleReminder(minutes: Int) {
        // 开关模式：检查是否已有同一时长的待触发闹钟
        val alarmIntent = Intent(service, RemindAlarmReceiver::class.java).apply {
            action = RemindAlarmReceiver.ACTION_REMIND
            putExtra(RemindAlarmReceiver.EXTRA_MINUTES, minutes)
        }
        val existingPI = PendingIntent.getBroadcast(
            service,
            REMIND_REQUEST_CODE_BASE + minutes,
            alarmIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (existingPI != null) {
            // 已有闹钟 → 注销
            cancelRemind(minutes, existingPI)
            return
        }

        // 没有闹钟 → 创建
        showRemindFeedback(minutes)
        scheduleOwnAlarm(minutes)
    }

    private fun showRemindFeedback(minutes: Int) {
        try {
            Toast.makeText(
                service,
                service.resources.getQuantityString(R.plurals.gesture_remind_feedback, minutes, minutes),
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            CrashLogManager.logException("AccessibilityActionExecutor", "显示提醒反馈失败", e)
        }
    }

    private fun cancelRemind(minutes: Int, pi: PendingIntent) {
        try {
            val alarmManager = service.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pi)
            pi.cancel()
        } catch (e: Exception) {
            CrashLogManager.logException("AccessibilityActionExecutor", "取消提醒闹钟失败", e)
        }
        try {
            Toast.makeText(
                service,
                service.resources.getQuantityString(R.plurals.gesture_remind_cancelled, minutes, minutes),
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            CrashLogManager.logException("AccessibilityActionExecutor", "显示取消提醒反馈失败", e)
        }
    }

    // minSdk 34 下 USE_EXACT_ALARM 已覆盖精确闹钟授权，此告警为误报
    @android.annotation.SuppressLint("MissingPermission")
    private fun scheduleOwnAlarm(minutes: Int) {
        try {
            val alarmManager = service.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val triggerTime = System.currentTimeMillis() + minutes * 60 * 1000L

            val intent = Intent(service, RemindAlarmReceiver::class.java).apply {
                action = RemindAlarmReceiver.ACTION_REMIND
                putExtra(RemindAlarmReceiver.EXTRA_MINUTES, minutes)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                service,
                REMIND_REQUEST_CODE_BASE + minutes,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        } catch (e: SecurityException) {
            CrashLogManager.logException("AccessibilityActionExecutor", "设置精确闹钟失败（无精确闹钟权限）", e)
            // 没有 USE_EXACT_ALARM 权限时，使用非精确闹钟
            try {
                val alarmManager = service.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val triggerTime = System.currentTimeMillis() + minutes * 60 * 1000L
                val intent = Intent(service, RemindAlarmReceiver::class.java).apply {
                    action = RemindAlarmReceiver.ACTION_REMIND
                    putExtra(RemindAlarmReceiver.EXTRA_MINUTES, minutes)
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    service,
                    REMIND_REQUEST_CODE_BASE + minutes,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } catch (e2: Exception) {
                CrashLogManager.logException("AccessibilityActionExecutor", "设置普通闹钟失败", e2)
            }
        } catch (e: Exception) {
            CrashLogManager.logException("AccessibilityActionExecutor", "设置提醒闹钟失败", e)
        }
    }

    companion object {
        private const val TAG = "ActionExecutor"
        private const val REMIND_REQUEST_CODE_BASE = 3000
    }
}
