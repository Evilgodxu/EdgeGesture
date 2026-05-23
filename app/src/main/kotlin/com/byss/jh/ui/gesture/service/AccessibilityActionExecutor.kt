package com.byss.jh.ui.gesture.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.VibrationEffect
import android.os.VibratorManager
import android.view.accessibility.AccessibilityEvent
import com.byss.jh.data.gesture.GestureAction
import com.byss.jh.data.gesture.GestureSettingsKeys
import com.byss.jh.data.gesture.GestureSettingsState
import com.byss.jh.data.gesture.expandPanelShortcutsFlow
import com.byss.jh.data.gesture.gestureDataStore
import com.byss.jh.data.gesture.saveExpandPanelShortcut
import com.byss.jh.ui.settings.themeModeFlow
import com.byss.jh.util.Logger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class AccessibilityActionExecutor(
    private val service: AccessibilityService
) {
    private val appHistory = mutableListOf<String>()
    private var currentApp: String? = null
    private var justConfigChanged: Boolean = false

    private var flashlightOn = false
    private val cameraManager = service.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    private var expandPanelViewManager: ExpandPanelViewManager? = null

    fun performAction(action: GestureAction, settings: GestureSettingsState) {
        if (action == GestureAction.NONE) return
        vibrate(settings)
        Logger.i(service, TAG, "执行操作: ${action.displayName}")

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
            }
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
    }

    fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString()
        if (packageName == null || packageName == service.packageName) return

        val blacklist = getBlacklistSync()

        if (justConfigChanged) {
            justConfigChanged = false
            if (packageName == currentApp) {
                Logger.i(service, TAG, "应用重建: current=$packageName, history=$appHistory")
                return
            }
        }

        if (currentApp == packageName) return

        currentApp?.let { current ->
            if (current != packageName && current !in blacklist) {
                appHistory.remove(packageName)
                appHistory.add(current)
                if (appHistory.size > 20) {
                    appHistory.removeAt(0)
                }
            }
        }
        currentApp = packageName
        Logger.i(service, TAG, "应用切换: current=$packageName, history=$appHistory")
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
                    Logger.i(service, TAG, "切换到上一个应用: $target, history=$appHistory")
                } else {
                    Logger.w(service, TAG, "无法启动应用: $target，尝试任务切换")
                    service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
                }
            } else {
                Logger.w(service, TAG, "没有找到上一个应用，尝试任务切换")
                service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
            }
        } catch (e: Exception) {
            Logger.e(service, TAG, "切换到上一个应用失败: ${e.message}")
            service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
        }
    }

    private fun getBlacklistSync(): Set<String> {
        return try {
            val prefs = runBlocking { (service as Context).gestureDataStore.data.first() }
            prefs[GestureSettingsKeys.APP_SWITCH_BLACKLIST] ?: emptySet()
        } catch (e: Exception) {
            Logger.e(service, TAG, "读取黑名单失败: ${e.message}")
            emptySet()
        }
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
        } catch (e: Exception) {
            Logger.e(service, TAG, "启动应用失败: ${e.message}")
            false
        }
    }

    private fun getLastAppFromUsageStats(blacklist: Set<String>): String? {
        return try {
            val usageStatsManager = service.getSystemService(Context.USAGE_STATS_SERVICE) as? android.app.usage.UsageStatsManager
                ?: return null
            val endTime = System.currentTimeMillis()
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
        } catch (e: Exception) {
            Logger.e(service, TAG, "获取最近应用失败: ${e.message}")
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
            Logger.i(service, TAG, "发送媒体按键: $keyCode")
        } catch (e: Exception) {
            Logger.e(service, TAG, "发送媒体按键失败: ${e.message}")
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
            Logger.e(service, TAG, "手电筒操作失败: ${e.message}")
        }
    }

    private fun launchVoiceAssistant() {
        try {
            val intent = Intent(Intent.ACTION_ASSIST).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                addCategory(Intent.CATEGORY_DEFAULT)
            }
            service.startActivity(intent)
            Logger.i(service, TAG, "触发语音助手")
        } catch (e: Exception) {
            Logger.e(service, TAG, "触发语音助手失败: ${e.message}")
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
