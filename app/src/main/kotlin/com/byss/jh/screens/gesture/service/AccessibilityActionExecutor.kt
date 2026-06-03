package com.byss.jh.screens.gesture.service

import android.accessibilityservice.AccessibilityService
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
import com.byss.jh.data.gesture.GestureAction
import com.byss.jh.data.gesture.GestureSettingsKeys
import com.byss.jh.data.gesture.GestureSettingsState
import com.byss.jh.data.gesture.expandPanelShortcutsFlow
import com.byss.jh.data.gesture.gestureDataStore
import com.byss.jh.data.gesture.saveExpandPanelShortcut
import com.byss.jh.data.permission.PermissionMonitor
import com.byss.jh.data.permission.PermissionType
import com.byss.jh.screens.gesture.service.expandpanel.ExpandPanelPermissionCallback
import com.byss.jh.screens.gesture.service.expandpanel.ExpandPanelViewManager
import com.byss.jh.screens.gesture.service.expandpanel.sendMediaKeyEvent
import com.byss.jh.screens.settings.themeModeFlow
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

    private var expandPanelViewManager: ExpandPanelViewManager? = null
    private var pendingExpandPanelShow = false
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

        when (action) {
            GestureAction.HOME -> service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
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
            GestureAction.EXPAND_PANEL -> showExpandPanel()
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

    private fun switchToLastApp() {
        try {
            val blacklist = getBlacklistSync()
            val target = appHistory.findLast { it != currentApp && it !in blacklist }
                ?: getLastAppFromUsageStats(blacklist)
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

    // 从 UsageStatsManager 获取最近使用的应用，作为应用历史记录的备用方案
    private fun getLastAppFromUsageStats(blacklist: Set<String>): String? {
        return try {
            val usageStatsManager = service.getSystemService(Context.USAGE_STATS_SERVICE) as? android.app.usage.UsageStatsManager
                ?: return null
            val endTime = System.currentTimeMillis()
            // 查询最近5分钟的使用统计
            val startTime = endTime - 300000

            val stats = usageStatsManager.queryUsageStats(
                android.app.usage.UsageStatsManager.INTERVAL_DAILY,
                startTime,
                endTime
            )

            if (stats != null && stats.isNotEmpty()) {
                val sortedStats = stats
                    .filter { it.packageName != service.packageName && it.packageName !in blacklist }
                    .sortedByDescending { it.lastTimeUsed }
                sortedStats.getOrNull(0)?.packageName
            } else {
                null
            }
        } catch (_: Exception) {
            null
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

    companion object {
        private const val TAG = "ActionExecutor"
    }
}
