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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class AccessibilityActionExecutor(
    private val service: AccessibilityService
) : ExpandPanelPermissionCallback {
    private val appHistory = mutableListOf<String>()
    private var currentApp: String? = null
    private var justConfigChanged: Boolean = false

    private var flashlightOn = false
    private val cameraManager = service.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val freeformAppLauncher = FreeformAppLauncher(service)

    private var expandPanelViewManager: ExpandPanelViewManager? = null
    private var pendingExpandPanelShow = false
    private var musicPanelViewManager: MusicPanelViewManager? = null
    private var taskPanelViewManager: TaskPanelViewManager? = null
    private val permissionMonitor = PermissionMonitor(service)
    private val executorScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // 等待权限监控任务
    private var writeSettingsMonitorJob: kotlinx.coroutines.Job? = null

    // 缓存黑名单避免频繁读取 DataStore
    private var cachedBlacklist: Set<String>? = null
    private var lastBlacklistCacheTime: Long = 0
    private val blacklistCacheValidityMs = 5000L // 5秒缓存有效期

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
            GestureAction.TASK_PANEL -> showTaskPanel(getBlacklistSync())
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
                    PackageManager.PERMISSION_GRANTED -> showPanel()
            else -> {
                MusicPanelPermissionBridge.pendingShowAction = showPanel
                val intent = Intent(service, MusicPanelPermissionActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                service.startActivity(intent)
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
        taskPanelViewManager?.dismiss()
        taskPanelViewManager = null
    }

    private fun showTaskPanel(blacklist: Set<String>) {
        if (taskPanelViewManager != null) { dismissTaskPanel(); return }
        val currentPackage = currentApp ?: service.packageName
        // 从 appHistory 构建列表，最近使用的在前，当前应用置顶
        val packages = mutableListOf<String>()
        if (currentPackage != service.packageName && currentPackage !in blacklist) {
            packages.add(currentPackage)
        }
        packages.addAll(appHistory.reversed().filter {
            it != currentPackage && it != service.packageName && it !in blacklist
        })
        // 确保列表中没有重复项
        val distinctPackages = packages.distinct()
        packages.clear()
        packages.addAll(distinctPackages)
        val apps = packages.mapNotNull { pkg ->
            try {
                service.packageManager.getLaunchIntentForPackage(pkg) ?: return@mapNotNull null
                val info = service.packageManager.getApplicationInfo(pkg, 0)
                TaskPanelApp(pkg, service.packageManager.getApplicationLabel(info).toString(), service.packageManager.getApplicationIcon(info))
            } catch (_: Exception) { null }
        }.take(10).toList()
        taskPanelViewManager = TaskPanelViewManager(
            service, apps, currentPackage,
            { launchApp(it) },
            { freeformAppLauncher.launch(it, useFreeform = true) },
            { removeFromHistory(it) },
            { taskPanelViewManager = null }
        ).also { it.show() }
    }

    private fun removeFromHistory(packageName: String) {
        appHistory.removeAll { it == packageName }
    }

    // 清理资源
    fun cleanup() {
        dismissExpandPanel()
        dismissMusicPanel()
        dismissTaskPanel()
        executorScope.cancel()
    }

    // 监听窗口变化事件，记录应用切换历史，用于实现"切换到上一个应用"功能
    fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString()
        if (packageName == null || packageName == service.packageName) return

        val blacklist = getBlacklistSync()

        // 屏幕旋转等配置变化会导致Activity重建，此时不应记录为应用切换
        if (justConfigChanged) {
            justConfigChanged = false
            if (packageName == currentApp) {
                return
            }
        }

        if (currentApp == packageName) return

        // 将当前应用添加到历史记录，用于后续返回
        currentApp?.let { current ->
            if (current != packageName && current !in blacklist) {
                appHistory.remove(packageName)
                appHistory.remove(current)
                appHistory.add(current)
                // 限制历史记录大小，避免内存无限增长
                if (appHistory.size > 20) {
                    appHistory.removeAt(0)
                }
            }
        }
        currentApp = packageName
    }

    fun markConfigChanged() {
        justConfigChanged = true
    }

    private fun launchCurrentAppInFreeform() {
        val packageName = currentApp ?: return
        if (packageName == service.packageName) return
        freeformAppLauncher.launch(packageName, useFreeform = true)
    }

    private fun switchToLastApp() {
        try {
            val blacklist = getBlacklistSync()
            val target = appHistory.findLast { it != currentApp && it !in blacklist }
            if (target != null && target != service.packageName && target != currentApp) {
                val launched = launchApp(target)
                if (launched) {
                    currentApp?.let { current ->
                        if (current != target && current !in blacklist) {
                            appHistory.remove(target)
                            appHistory.add(current)
                            if (appHistory.size > 20) {
                                appHistory.removeAt(0)
                            }
                        }
                    }
                    currentApp = target
                } else {
                    service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
                }
            } else {
                service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
            }
        } catch (_: Exception) {
            service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
        }
    }

    private fun getBlacklistSync(): Set<String> {
        // 检查缓存是否有效
        val now = System.currentTimeMillis()
        cachedBlacklist?.let { cached ->
            if (now - lastBlacklistCacheTime < blacklistCacheValidityMs) {
                return cached
            }
        }

        // 缓存无效或不存在，从 DataStore 读取
        return try {
            val prefs = runBlocking { (service as Context).gestureDataStore.data.first() }
            val blacklist = prefs[GestureSettingsKeys.APP_SWITCH_BLACKLIST] ?: emptySet()
            // 更新缓存
            cachedBlacklist = blacklist
            lastBlacklistCacheTime = now
            blacklist
        } catch (_: Exception) {
            emptySet()
        }
    }

    // 清除黑名单缓存，在设置变更时调用
    fun invalidateBlacklistCache() {
        cachedBlacklist = null
        lastBlacklistCacheTime = 0
    }

    private fun launchApp(packageName: String): Boolean {
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
        } catch (_: Exception) {
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
        } catch (_: Exception) {
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
        } catch (_: CameraAccessException) {
        }
    }

    private fun launchVoiceAssistant() {
        try {
            val intent = Intent(Intent.ACTION_ASSIST).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                addCategory(Intent.CATEGORY_DEFAULT)
            }
            service.startActivity(intent)
        } catch (_: Exception) {
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
        } catch (_: Exception) {}

        // 备用 BaseCaptureUI（部分微信版本）
        try {
            val intent = Intent().apply {
                component = ComponentName("com.tencent.mm", "com.tencent.mm.plugin.scanner.ui.BaseCaptureUI")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            service.startActivity(intent)
            return
        } catch (_: Exception) {}

        // 兜底：打开微信
        try {
            val launchIntent = service.packageManager.getLaunchIntentForPackage("com.tencent.mm")
            if (launchIntent != null) {
                launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                service.startActivity(launchIntent)
            }
        } catch (_: Exception) {}
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
        } catch (_: Exception) {
            try {
                val launchIntent = service.packageManager.getLaunchIntentForPackage("com.eg.android.AlipayGphone")
                if (launchIntent != null) {
                    launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    service.startActivity(launchIntent)
                }
            } catch (_: Exception) {}
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
        } catch (_: Exception) {}
    }

    private fun cancelRemind(minutes: Int, pi: PendingIntent) {
        try {
            val alarmManager = service.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pi)
            pi.cancel()
        } catch (_: Exception) {}
        try {
            Toast.makeText(
                service,
                service.getString(R.string.gesture_remind_cancelled, minutes),
                Toast.LENGTH_SHORT
            ).show()
        } catch (_: Exception) {}
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
        } catch (_: SecurityException) {
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
            } catch (_: Exception) {}
        } catch (_: Exception) {}
    }

    companion object {
        private const val TAG = "ActionExecutor"
        private const val REMIND_REQUEST_CODE_BASE = 3000
    }
}
