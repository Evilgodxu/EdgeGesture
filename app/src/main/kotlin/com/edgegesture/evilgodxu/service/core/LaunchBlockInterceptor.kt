package com.edgegesture.evilgodxu.service.core

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import com.edgegesture.evilgodxu.data.gesture.GestureAction
import com.edgegesture.evilgodxu.data.gesture.GestureSettingsState
import com.edgegesture.evilgodxu.data.gesture.GestureStatsManager
import com.edgegesture.evilgodxu.data.launchblock.LaunchBlockRule
import com.edgegesture.evilgodxu.data.launchblock.LaunchBlockState
import com.edgegesture.evilgodxu.data.launchblock.updateLaunchBlockRule
import com.edgegesture.evilgodxu.data.shizuku.ShizukuManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// 启动拦截职责组件 — 从 EdgeGestureAccessibilityService 提取
class LaunchBlockInterceptor(
    private val service: AccessibilityService,
    private val actionExecutor: AccessibilityActionExecutor,
    private val serviceScope: CoroutineScope,
) {
    private var currentPackage: String? = null
    private val launcherKillCount = mutableMapOf<String, Int>()
    private val launcherCooldownUntil = mutableMapOf<String, Long>()

    var launchBlockState: LaunchBlockState = LaunchBlockState()
    var settings: GestureSettingsState? = null

    // 检测应用启动并执行拦截
    fun checkAppLaunch(event: AccessibilityEvent) {
        if (!launchBlockState.enabled || launchBlockState.rules.isEmpty()) return

        val newPackage = event.packageName?.toString() ?: return
        if (newPackage == currentPackage) return
        if (newPackage == service.packageName) return

        val previousPackage = currentPackage
        currentPackage = newPackage

        if (previousPackage == service.packageName) return

        val matchedRule = launchBlockState.rules.find { rule ->
            val targetMatch = newPackage.contains(rule.targetApp, ignoreCase = true) ||
                rule.targetApp.contains(newPackage, ignoreCase = true)
            if (!targetMatch) return@find false
            if (rule.launcherApp.isNotBlank() && previousPackage != null) {
                previousPackage.contains(rule.launcherApp, ignoreCase = true) ||
                    rule.launcherApp.contains(previousPackage, ignoreCase = true)
            } else {
                true
            }
        }

        if (matchedRule != null) {
            blockLaunch(matchedRule, previousPackage, newPackage)
        }
    }

    private fun blockLaunch(rule: LaunchBlockRule, launcherPackage: String?, targetPackage: String) {
        GestureStatsManager.incrementBlockCount(service)
        val pm = service.packageManager

        val isLauncherSystemApp = launcherPackage?.let { pkg ->
            try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
            } catch (_: Exception) { false }
        } ?: false

        val isTargetSystemApp = targetPackage.let { pkg ->
            try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
            } catch (_: Exception) { false }
        }

        if (rule.enabled) {
            val blockAction = Runnable {
                if (isLauncherSystemApp) {
                    service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
                } else if (launcherPackage != null && launcherPackage != service.packageName) {
                    settings?.let { actionExecutor.performAction(GestureAction.LAST_APP, it) }
                } else {
                    service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
                }
            }
            if (rule.blockDelay > 0) {
                Handler(Looper.getMainLooper()).postDelayed(blockAction, rule.blockDelay.toLong())
            } else {
                blockAction.run()
            }
        }

        if (rule.enableKillTarget) {
            val killAction = Runnable {
                if (!isTargetSystemApp || rule.allowKillSystemApp) {
                    killAppProcess(targetPackage)
                }
            }
            if (rule.blockDelay > 0) {
                Handler(Looper.getMainLooper()).postDelayed(killAction, rule.blockDelay.toLong())
            } else {
                killAction.run()
            }
        }

        if (rule.enableKillOnFrequentLaunch && launcherPackage != null) {
            killLauncher(launcherPackage, rule)
        }

        serviceScope.launch {
            val updatedRule = rule.copy(
                launchCount = rule.launchCount + 1,
                lastLaunchTime = System.currentTimeMillis()
            )
            service.updateLaunchBlockRule(updatedRule)
        }
    }

    private fun killLauncher(launcherPackage: String, rule: LaunchBlockRule) {
        val now = System.currentTimeMillis()
        val cooldownEnd = launcherCooldownUntil[launcherPackage] ?: 0L
        if (now < cooldownEnd) return

        val count = launcherKillCount[launcherPackage] ?: 0
        if (count >= 5) {
            launcherCooldownUntil[launcherPackage] = now + 15000L
            launcherKillCount[launcherPackage] = 0
            return
        }

        val isSystemApp = try {
            val appInfo = service.packageManager.getApplicationInfo(launcherPackage, 0)
            (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
        } catch (_: Exception) { false }

        if (isSystemApp && !rule.allowKillSystemApp) return

        killAppProcess(launcherPackage)
        launcherKillCount[launcherPackage] = count + 1
    }

    private fun killAppProcess(packageName: String) {
        if (ShizukuManager.isAvailable()) {
            ShizukuManager.forceStopPackage(packageName)
            return
        }
        try {
            val am = service.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            am.killBackgroundProcesses(packageName)
        } catch (_: Exception) { }
    }
}
