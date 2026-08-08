package com.edgegesture.evilgodxu.screens.gesture.service

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.VibratorManager
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.edgegesture.evilgodxu.R
import com.edgegesture.evilgodxu.data.gesture.GestureAction
import com.edgegesture.evilgodxu.data.gesture.GestureSettingsKeys
import com.edgegesture.evilgodxu.data.gesture.GestureSettingsState
import com.edgegesture.evilgodxu.data.gesture.GestureStatsManager
import com.edgegesture.evilgodxu.data.gesture.expandPanelShortcutsFlow
import com.edgegesture.evilgodxu.data.gesture.gestureDataStore
import com.edgegesture.evilgodxu.data.gesture.saveExpandPanelShortcut
import com.edgegesture.evilgodxu.data.gesture.saveExpandPanelShortcutFreeform
import com.edgegesture.evilgodxu.data.permission.PermissionMonitor
import com.edgegesture.evilgodxu.data.permission.PermissionType
import com.edgegesture.evilgodxu.log.CrashLogManager
import com.edgegesture.evilgodxu.screens.gesture.service.expandpanel.ExpandPanelPermissionCallback
import com.edgegesture.evilgodxu.screens.gesture.service.expandpanel.ExpandPanelViewManager
import com.edgegesture.evilgodxu.screens.gesture.service.musicpanel.MusicPanelPermissionActivity
import com.edgegesture.evilgodxu.screens.gesture.service.musicpanel.MusicPanelPermissionBridge
import com.edgegesture.evilgodxu.screens.gesture.service.musicpanel.MusicPanelViewManager
import com.edgegesture.evilgodxu.screens.gesture.service.taskpanel.TaskPanelApp
import com.edgegesture.evilgodxu.screens.gesture.service.taskpanel.TaskPanelViewManager
import com.edgegesture.evilgodxu.screens.settings.themeModeFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class AccessibilityActionExecutor(
    private val service: AccessibilityService
) : ExpandPanelPermissionCallback {
    private val taskPanelHistory = mutableListOf<String>()
    private var currentApp: String? = null
    private var previousApp: String? = null

    private var flashlightOn = false
    private val cameraManager = service.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val freeformAppLauncher = FreeformAppLauncher(service)

    private var expandPanelViewManager: ExpandPanelViewManager? = null
    private var pendingExpandPanelShow = false
    private var musicPanelViewManager: MusicPanelViewManager? = null
    private var taskPanelViewManager: TaskPanelViewManager? = null
    private var pendingTaskPanelShow = false
    private var taskPanelLoadJob: Job? = null
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

    fun performAction(action: GestureAction, settings: GestureSettingsState) {
        if (action == GestureAction.NONE) return
        vibrate(settings)
        GestureStatsManager.incrementGestureCount(service)

        when (action) {
            GestureAction.HOME -> {
                dismissTaskPanel()
                service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
            }
            GestureAction.RECENT -> service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
            GestureAction.BACK -> service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
            GestureAction.LAST_APP -> switchToLastApp()
            GestureAction.PREVIOUS_TRACK -> sendMediaKeyEvent(android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS)
            GestureAction.NEXT_TRACK -> sendMediaKeyEvent(android.view.KeyEvent.KEYCODE_MEDIA_NEXT)
            GestureAction.FLASHLIGHT -> toggleFlashlight()
            GestureAction.VOICE_ASSISTANT -> launchVoiceAssistant()
            GestureAction.POWER_MENU -> service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_POWER_DIALOG)
            GestureAction.LOCK_SCREEN -> service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
            GestureAction.SCREENSHOT -> service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT)
            GestureAction.FREEFORM_MODE -> launchCurrentAppInFreeform()
            GestureAction.EXPAND_PANEL -> showExpandPanel()
            GestureAction.MUSIC_PANEL -> showMusicPanel()
            GestureAction.TASK_PANEL -> showTaskPanel()
            GestureAction.ALIPAY_SCAN -> launchScanAlipay()
            GestureAction.WECHAT_SCAN -> launchScanWechat()
            GestureAction.REMIND_1M -> scheduleReminder(1)
            GestureAction.REMIND_3M -> scheduleReminder(3)
            GestureAction.REMIND_5M -> scheduleReminder(5)
            GestureAction.REMIND_10M -> scheduleReminder(10)
            GestureAction.REMIND_15M -> scheduleReminder(15)
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
            themeModeFlow = service.themeModeFlow(),
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
            data = android.net.Uri.parse("package:${service.packageName}")
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
            themeModeFlow = service.themeModeFlow(),
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

    fun handleExternalAudioIntent(uri: android.net.Uri): Boolean {
        val manager = musicPanelViewManager ?: MusicPanelViewManager(
            context = service,
            onDismiss = { musicPanelViewManager = null }
        ).also {
            musicPanelViewManager = it
        }
        manager.playExternalUri(uri)
        return true
    }

    private fun showMusicPanel() {
        if (musicPanelViewManager != null) {
            musicPanelViewManager?.dismiss()
            return
        }

        val showPanel = {
            musicPanelViewManager = MusicPanelViewManager(
                context = service,
                onDismiss = { musicPanelViewManager = null }
            ).apply { show() }
        }

        when {
            ContextCompat.checkSelfPermission(service, Manifest.permission.READ_MEDIA_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED &&
                    Environment.isExternalStorageManager() -> showPanel()
            else -> {
                // 每次启动面板都检查音频访问与全部文件访问权限并自动申请
                MusicPanelPermissionBridge.pendingShowAction = showPanel
                val intent = Intent(service, MusicPanelPermissionActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                try {
                    service.startActivity(intent)
                } catch (e: Exception) {
                    MusicPanelPermissionBridge.clearPendingShowAction()
                    CrashLogManager.logException("AccessibilityActionExecutor", "启动音乐面板权限页面失败", e)
                }
            }
        }
    }

    fun dismissExpandPanel() {
        expandPanelViewManager?.dismiss()
        expandPanelViewManager = null
        pendingExpandPanelShow = false
        writeSettingsMonitorJob?.cancel()
    }

    fun dismissMusicPanel() {
        musicPanelViewManager?.dismiss()
        musicPanelViewManager = null
    }

    fun dismissTaskPanel() {
        taskPanelLoadJob?.cancel()
        taskPanelLoadJob = null
        taskPanelViewManager?.dismiss()
        taskPanelViewManager = null
        pendingTaskPanelShow = false
    }

    private fun showTaskPanel() {
        if (taskPanelViewManager != null) { dismissTaskPanel(); return }
        if (pendingTaskPanelShow) {
            dismissTaskPanel()
            return
        }
        pendingTaskPanelShow = true
        val currentPackage = service.rootInActiveWindow?.packageName?.toString()
            ?.takeIf { it != service.packageName }
            ?: currentApp
            ?: service.packageName
        taskPanelLoadJob = executorScope.launch {
            val blacklist = blacklistSnapshot
            val packages = taskPanelHistory
                .asReversed()
                .filter { it != service.packageName }
                .distinct()
                .filter { it !in blacklist }
            val apps = packages.mapNotNull { pkg ->
                try {
                    service.packageManager.getLaunchIntentForPackage(pkg) ?: return@mapNotNull null
                    val info = service.packageManager.getApplicationInfo(pkg, 0)
                    TaskPanelApp(pkg, service.packageManager.getApplicationLabel(info).toString(), service.packageManager.getApplicationIcon(info))
                } catch (e: Exception) {
                    CrashLogManager.logException("AccessibilityActionExecutor", "获取任务面板应用信息失败", e)
                    null
                }
            }.take(10).toList()
            withContext(Dispatchers.Main) {
                // 异步加载期间可能已被关闭，不再显示
                if (!pendingTaskPanelShow) return@withContext
                pendingTaskPanelShow = false
                taskPanelViewManager = TaskPanelViewManager(
                    service, apps, currentPackage, currentPackage,
                    { launchApp(it) },
                    { freeformAppLauncher.launch(it, useFreeform = true) },
                    { packageName -> taskPanelHistory.removeAll { it == packageName } },
                    { taskPanelViewManager = null; pendingTaskPanelShow = false }
                ).also {
                    taskPanelLoadJob = null
                    if (!it.show()) {
                        taskPanelViewManager = null
                    }
                }
            }
        }
    }

    // 清理资源
    fun cleanup() {
        dismissExpandPanel()
        dismissMusicPanel()
        dismissTaskPanel()
        writeSettingsMonitorJob?.cancel()
        writeSettingsMonitorJob = null
        MusicPanelPermissionBridge.clearPendingShowAction()
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

        taskPanelHistory.remove(packageName)
        taskPanelHistory.add(packageName)
        if (taskPanelHistory.size > 10) taskPanelHistory.removeAt(0)

        val blacklist = blacklistSnapshot
        if (packageName in blacklist) return

        previousApp = currentApp
        currentApp = packageName
    }

    private fun launchCurrentAppInFreeform() {
        val packageName = currentApp ?: return
        if (packageName == service.packageName) return
        freeformAppLauncher.launch(packageName, useFreeform = true)
    }

    private fun switchToLastApp() {
        val target = previousApp ?: return
        if (launchApp(target)) {
            previousApp = currentApp
            currentApp = target
        }
    }

    fun invalidateBlacklistCache() {
    }

    fun launchApp(packageName: String): Boolean {
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
                data = Uri.parse("alipays://platformapi/startapp?saId=10000007")
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
                service.getString(R.string.gesture_remind_feedback, minutes),
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
                service.getString(R.string.gesture_remind_cancelled, minutes),
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            CrashLogManager.logException("AccessibilityActionExecutor", "显示取消提醒反馈失败", e)
        }
    }

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
