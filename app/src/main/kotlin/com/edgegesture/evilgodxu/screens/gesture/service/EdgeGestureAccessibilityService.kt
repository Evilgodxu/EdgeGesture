package com.edgegesture.evilgodxu.screens.gesture.service

import android.accessibilityservice.AccessibilityService
import com.edgegesture.evilgodxu.R
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import com.edgegesture.evilgodxu.data.gesture.GestureAction
import com.edgegesture.evilgodxu.data.gesture.GestureSettingsState
import com.edgegesture.evilgodxu.data.gesture.gestureDataStore
import com.edgegesture.evilgodxu.data.gesture.gestureSettingsFlow
import com.edgegesture.evilgodxu.data.app.AppRepository
import com.edgegesture.evilgodxu.data.gesture.initBlacklistIfNeeded
import com.edgegesture.evilgodxu.data.gesture.toGestureSettingsState
import com.edgegesture.evilgodxu.data.launchblock.LaunchBlockRule
import com.edgegesture.evilgodxu.data.launchblock.LaunchBlockState
import com.edgegesture.evilgodxu.data.launchblock.launchBlockFlow
import com.edgegesture.evilgodxu.data.launchblock.updateLaunchBlockRule
import com.edgegesture.evilgodxu.data.shizuku.ShizukuManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

class EdgeGestureAccessibilityService : AccessibilityService(), AccessibilityGestureDetector.GestureCallback {

    companion object {
        const val TAG = "EdgeGestureService"

        private const val MAX_ATTACH_FAILURES = 3
        private const val RESTART_DELAY_MS = 1500L

        private var weakInstance: java.lang.ref.WeakReference<EdgeGestureAccessibilityService>? = null

        fun getInstance(): EdgeGestureAccessibilityService? = weakInstance?.get()

        fun isAvailable(): Boolean = weakInstance?.get() != null

        fun startGesture(context: Context) {
            getInstance()?.apply {
                serviceScope.launch {
                    loadSettings()
                    if (settings.gestureEnabled) {
                        withContext(Dispatchers.Main) {
                            if (!edgeViewManager.isViewAttached()) {
                                edgeViewManager.createEdgeViews(settings, settingsProvider)
                                edgeViewManager.showEdgeViews(settings)
                            }
                        }
                    }
                }
            }
        }

        fun stopGesture(context: Context) {
            getInstance()?.edgeViewManager?.removeEdgeViews()
        }

        fun updateSettings(context: Context) {
            getInstance()?.refreshSettings()
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var settings: GestureSettingsState = GestureSettingsState()
    private val settingsProvider: () -> GestureSettingsState = { settings }

    private lateinit var edgeViewManager: AccessibilityEdgeViewManager
    private lateinit var actionExecutor: AccessibilityActionExecutor
    private lateinit var gestureDetector: AccessibilityGestureDetector
    private var backTapDetector: BackTapDetector? = null

    private val settingsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "ACTION_UPDATE_SETTINGS" -> refreshSettings()
            }
        }
    }

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON,
                Intent.ACTION_USER_PRESENT -> backTapDetector?.setScreenOn(true)
                Intent.ACTION_SCREEN_OFF -> backTapDetector?.setScreenOn(false)
            }
        }
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != Intent.ACTION_BATTERY_CHANGED) return
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
            val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL ||
                    plugged != 0
            updateBackTapPauseState()
        }
    }

    private var settingsFlowJob: kotlinx.coroutines.Job? = null
    private var launchBlockFlowJob: kotlinx.coroutines.Job? = null
    private var isKeyboardVisible = false

    // 边缘视图添加失败后的重启保护
    private var consecutiveAttachFailures = 0
    private val restartHandler = Handler(Looper.getMainLooper())
    private val restartRunnable = Runnable {
        if (!isAvailable()) return@Runnable
        consecutiveAttachFailures = 0
        startGesture(this@EdgeGestureAccessibilityService)
    }

    // 背面双击自动暂停状态
    @Volatile private var isCharging = false

    // 启动拦截相关
    private var launchBlockState: LaunchBlockState = LaunchBlockState()
    private var currentPackage: String? = null
    private val launchHistory = mutableMapOf<String, MutableList<Long>>() // 包名 -> 启动时间列表

    override fun onServiceConnected() {
        super.onServiceConnected()
        weakInstance = java.lang.ref.WeakReference(this)

        actionExecutor = AccessibilityActionExecutor(this)
        gestureDetector = AccessibilityGestureDetector(this, this)
        edgeViewManager = AccessibilityEdgeViewManager(this, gestureDetector) { onEdgeViewAttachFailed() }

        // 初始化 Shizuku
        ShizukuManager.init(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(settingsReceiver, IntentFilter("ACTION_UPDATE_SETTINGS"), Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(settingsReceiver, IntentFilter("ACTION_UPDATE_SETTINGS"))
        }

        val screenFilter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenStateReceiver, screenFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(screenStateReceiver, screenFilter)
        }

        val batteryFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(batteryReceiver, batteryFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(batteryReceiver, batteryFilter)
        }

        startSettingsFlow()
        startLaunchBlockFlow()

        serviceScope.launch {
            // 黑名单初始化不应阻塞服务启动，失败时仅打印日志
            runCatching { initBlacklistIfNeeded() }.onFailure { it.printStackTrace() }
            loadSettings()
            if (settings.gestureEnabled) {
                withContext(Dispatchers.Main) {
                    edgeViewManager.createEdgeViews(settings, settingsProvider)
                    edgeViewManager.showEdgeViews(settings)
                }
            }
            if (settings.backTapEnabled) {
                startBackTapDetector(settings)
                updateBackTapPauseState()
            }
        }

        // 预加载应用列表到缓存，确保扩展面板的应用选择器能快速显示
        // 在后台线程执行，避免阻塞服务启动
        serviceScope.launch {
            val repository = AppRepository.getInstance(this@EdgeGestureAccessibilityService)
            repository.initializeWithScan()
        }
    }

    private fun startLaunchBlockFlow() {
        launchBlockFlowJob = launchBlockFlow()
            .flowOn(Dispatchers.IO)
            .onEach { state ->
                launchBlockState = state
            }
            .launchIn(serviceScope)
    }

    private fun startSettingsFlow() {
        settingsFlowJob = gestureSettingsFlow()
            .flowOn(Dispatchers.IO)
            .onEach { newSettings ->
                val oldSettings = settings
                settings = newSettings

                // 背面双击检测器管理
                updateBackTapDetector(oldSettings, newSettings)

                // 检测器启动后或自动暂停设置变化时，立即同步暂停状态
                if (newSettings.backTapEnabled) {
                    updateBackTapPauseState()
                }

                withContext(Dispatchers.Main) {
                    if (oldSettings.gestureEnabled != newSettings.gestureEnabled) {
                        if (newSettings.gestureEnabled) {
                            edgeViewManager.createEdgeViews(settings, settingsProvider)
                            edgeViewManager.showEdgeViews(settings)
                        } else {
                            edgeViewManager.removeEdgeViews()
                        }
                    } else if (newSettings.gestureEnabled) {
                        if (oldSettings.hideOverlay != newSettings.hideOverlay) {
                            edgeViewManager.updateEdgeViewsAlpha(settings)
                        }
                        if (hasSizeOrPositionChanged(oldSettings, newSettings)) {
                            edgeViewManager.removeEdgeViews()
                            edgeViewManager.createEdgeViews(settings, settingsProvider)
                            edgeViewManager.showEdgeViews(settings)
                        }
                    }
                }
            }
            .launchIn(serviceScope)
    }

    private fun hasSizeOrPositionChanged(old: GestureSettingsState, new: GestureSettingsState): Boolean {
        return old.leftEdgeWidth != new.leftEdgeWidth ||
            old.leftEdgeHeightPercent != new.leftEdgeHeightPercent ||
            old.leftEdgePositionPercent != new.leftEdgePositionPercent ||
            old.leftSegmentCount != new.leftSegmentCount ||
            old.rightEdgeWidth != new.rightEdgeWidth ||
            old.rightEdgeHeightPercent != new.rightEdgeHeightPercent ||
            old.rightEdgePositionPercent != new.rightEdgePositionPercent ||
            old.rightSegmentCount != new.rightSegmentCount ||
            old.bottomEdgeHeight != new.bottomEdgeHeight ||
            old.bottomEdgeWidthPercent != new.bottomEdgeWidthPercent ||
            old.bottomSegmentCount != new.bottomSegmentCount
    }

    private fun updateBackTapDetector(old: GestureSettingsState, new: GestureSettingsState) {
        val enabledChanged = old.backTapEnabled != new.backTapEnabled
        val paramsChanged = old.backTapSensitivity != new.backTapSensitivity ||
            old.backTapRange != new.backTapRange
        val actionChanged = old.backTapAction != new.backTapAction
        val modeChanged = old.backTapMode != new.backTapMode

        if (enabledChanged) {
            if (new.backTapEnabled) {
                startBackTapDetector(new)
            } else {
                stopBackTapDetector()
            }
        } else if ((paramsChanged || actionChanged) && new.backTapEnabled) {
            backTapDetector?.stop()
            startBackTapDetector(new)
        } else if (modeChanged && new.backTapEnabled) {
            backTapDetector?.setMode(new.backTapMode)
        }
    }

    private fun startBackTapDetector(s: GestureSettingsState) {
        backTapDetector?.stop()
        backTapDetector = BackTapDetector(this) {
            actionExecutor.performAction(s.backTapAction, settings)
        }.also {
            it.setMode(s.backTapMode)
            it.setScreenOn(isScreenOn())
            it.start(s.backTapSensitivity, s.backTapRange)
        }
    }

    private fun stopBackTapDetector() {
        backTapDetector?.stop()
        backTapDetector = null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        actionExecutor.onAccessibilityEvent(event)

        event?.let {
            when (it.eventType) {
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
                AccessibilityEvent.TYPE_WINDOWS_CHANGED -> {
                    checkKeyboardVisibility()
                    checkAppLaunch(it)
                }
                AccessibilityEvent.TYPE_VIEW_FOCUSED -> checkInputFieldFocused(it)
            }
        }
    }

    // 检测应用启动并执行拦截
    private fun checkAppLaunch(event: AccessibilityEvent) {
        if (!launchBlockState.enabled || launchBlockState.rules.isEmpty()) return

        val newPackage = event.packageName?.toString() ?: return
        if (newPackage == currentPackage) return

        // 排除本应用自身
        if (newPackage == packageName) return

        val previousPackage = currentPackage
        currentPackage = newPackage

        // 排除本应用作为启动者的情况
        if (previousPackage == packageName) return

        // 检查是否需要拦截
        val matchedRule = launchBlockState.rules.find { rule ->
            // 检查目标应用是否匹配
            val targetMatch = newPackage.contains(rule.targetApp, ignoreCase = true) ||
                rule.targetApp.contains(newPackage, ignoreCase = true)

            if (!targetMatch) return@find false

            // 如果指定了启动者，检查启动者是否匹配
            if (rule.launcherApp.isNotBlank() && previousPackage != null) {
                previousPackage.contains(rule.launcherApp, ignoreCase = true) ||
                    rule.launcherApp.contains(previousPackage, ignoreCase = true)
            } else {
                true // 未指定启动者，只匹配目标
            }
        }

        if (matchedRule != null) {
            blockLaunch(matchedRule, previousPackage, newPackage)
        }
    }

    private fun blockLaunch(rule: LaunchBlockRule, launcherPackage: String?, targetPackage: String) {
        // 检查启动者是否为系统应用
        val isLauncherSystemApp = launcherPackage?.let { pkg ->
            try {
                val appInfo = packageManager.getApplicationInfo(pkg, 0)
                (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
            } catch (_: Exception) {
                false
            }
        } ?: false

        // 检查被启动者是否为系统应用
        val isTargetSystemApp = targetPackage.let { pkg ->
            try {
                val appInfo = packageManager.getApplicationInfo(pkg, 0)
                (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
            } catch (_: Exception) {
                false
            }
        }

        // 仅在规则启用时执行拦截
        if (rule.enabled) {
            val blockAction = Runnable {
                // 执行拦截：切换应用或返回桌面
                if (isLauncherSystemApp && launcherPackage != null) {
                    performGlobalAction(GLOBAL_ACTION_HOME)
                } else if (launcherPackage != null && launcherPackage != packageName) {
                    actionExecutor.performAction(GestureAction.LAST_APP, settings)
                } else {
                    performGlobalAction(GLOBAL_ACTION_HOME)
                }
            }
            if (rule.blockDelay > 0) {
                Handler(Looper.getMainLooper()).postDelayed(blockAction, rule.blockDelay.toLong())
            } else {
                blockAction.run()
            }
        }

        // 终止被启动者进程（由 enableKillTarget 开关控制）
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

        // 检查是否需要高频检测
        val shouldKillLauncher = rule.enableKillOnFrequentLaunch && launcherPackage != null
        val isLauncherKillAllowed = !isLauncherSystemApp || rule.allowKillSystemApp
        if (shouldKillLauncher && isLauncherKillAllowed) {
            checkFrequentLaunchAndKill(launcherPackage, rule)
        }

        // 更新规则统计
        serviceScope.launch {
            val updatedRule = rule.copy(
                launchCount = rule.launchCount + 1,
                lastLaunchTime = System.currentTimeMillis()
            )
            updateLaunchBlockRule(updatedRule)
        }
    }

    // 检查是否高频启动，如果是则终止启动者进程
    private fun checkFrequentLaunchAndKill(launcherPackage: String, rule: LaunchBlockRule) {
        val now = System.currentTimeMillis()
        val history = launchHistory.getOrPut(launcherPackage) { mutableListOf() }

        // 清理超过1分钟的历史记录
        history.removeAll { now - it > 60000 }
        history.add(now)

        // 1分钟内启动超过3次视为高频
        if (history.size >= 3) {
            // 检查是否为系统应用
            val isSystemApp = try {
                val appInfo = packageManager.getApplicationInfo(launcherPackage, 0)
                (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
            } catch (_: Exception) {
                false
            }

            // 非系统应用才终止
            if (!isSystemApp) {
                killAppProcess(launcherPackage)
                history.clear() // 清除历史避免重复终止
            }
        }
    }

    private fun killAppProcess(packageName: String) {
        // 优先使用 Shizuku 执行 force-stop
        if (ShizukuManager.isAvailable()) {
            ShizukuManager.forceStopPackage(packageName)
            return
        }

        // 降级方案：使用 ActivityManager
        try {
            val am = getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            am.killBackgroundProcesses(packageName)
        } catch (_: Exception) {
            // 终止失败，忽略错误
        }
    }

    // 检测输入法是否弹出，通过检查窗口列表中是否存在输入法窗口
    private fun checkKeyboardVisibility() {
        val windowList = windows
        if (windowList.isEmpty()) return

        val hasKeyboardWindow = windowList.any { windowInfo ->
            windowInfo.type == android.view.accessibility.AccessibilityWindowInfo.TYPE_INPUT_METHOD
        }

        updateKeyboardState(hasKeyboardWindow)
    }

    // 检测输入框是否获得焦点，作为输入法显示的辅助判断
    private fun checkInputFieldFocused(event: AccessibilityEvent) {
        val className = event.className?.toString() ?: return
        val isInputField = isInputFieldClass(className)

        if (isInputField && !isKeyboardVisible) {
            // 输入框获得焦点，延迟检查输入法窗口（输入法弹出可能有动画延迟）
            Handler(Looper.getMainLooper()).postDelayed({
                checkKeyboardVisibility()
            }, 200)
        }
    }

    // 判断类名是否为输入框类型
    private fun isInputFieldClass(className: String): Boolean {
        return className.contains("EditText", ignoreCase = true) ||
            className.contains("TextInput", ignoreCase = true) ||
            className.contains("SearchView", ignoreCase = true) ||
            className.contains("AutoCompleteTextView", ignoreCase = true) ||
            className.contains("ComposeUiNode", ignoreCase = true) // Jetpack Compose 输入框
    }

    private fun updateKeyboardState(visible: Boolean) {
        // 只有在开启避免遮挡设置时才处理输入法状态
        if (!settings.avoidKeyboardOverlap) return

        if (visible && !isKeyboardVisible) {
            isKeyboardVisible = true
            edgeViewManager.disableEdgeViewsTouch()
        } else if (!visible && isKeyboardVisible) {
            isKeyboardVisible = false
            edgeViewManager.enableEdgeViewsTouch()
        }
    }

    // 根据充电状态决定暂停或恢复背面双击
    private fun updateBackTapPauseState() {
        val shouldPause = settings.backTapPauseOnCharging && isCharging
        backTapDetector?.let {
            if (shouldPause) it.pause() else it.resume()
        }
    }

    // 边缘视图 addView 抛出 BadTokenException 时的重启保护
    private fun onEdgeViewAttachFailed() {
        consecutiveAttachFailures++
        Log.w(TAG, "Edge view attach failed, failureCount=$consecutiveAttachFailures")

        if (consecutiveAttachFailures >= MAX_ATTACH_FAILURES) {
            Log.e(TAG, "Edge view attach failed too many times, disabling service to avoid crash loop")
            disableSelf()
            return
        }

        restartHandler.removeCallbacks(restartRunnable)
        edgeViewManager.removeEdgeViews()
        restartHandler.postDelayed(restartRunnable, RESTART_DELAY_MS)
    }

    override fun onInterrupt() {}

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        actionExecutor.markConfigChanged()
        // 延迟更新布局，等待系统完成配置切换
        Handler(Looper.getMainLooper()).postDelayed({
            if (settings.gestureEnabled && edgeViewManager.isViewAttached()) {
                edgeViewManager.updateEdgeViewsLayout(settings)
            }
        }, 100)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        weakInstance = null
        restartHandler.removeCallbacks(restartRunnable)
        unregisterReceiver(settingsReceiver)
        unregisterReceiver(screenStateReceiver)
        unregisterReceiver(batteryReceiver)
        settingsFlowJob?.cancel()
        launchBlockFlowJob?.cancel()
        stopBackTapDetector()
        edgeViewManager.removeEdgeViews()
        actionExecutor.cleanup()
        serviceScope.cancel()
        return super.onUnbind(intent)
    }

    private fun isScreenOn(): Boolean {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isInteractive
    }

    override fun onSwipeAction(action: GestureAction) {
        actionExecutor.performAction(action, settings)
    }

    private suspend fun loadSettings() {
        val prefs = gestureDataStore.data.first()
        settings = prefs.toGestureSettingsState()
    }

    fun refreshSettings() {
        serviceScope.launch {
            val oldSettings = settings
            loadSettings()
            updateBackTapDetector(oldSettings, settings)
            withContext(Dispatchers.Main) {
                if (oldSettings.gestureEnabled != settings.gestureEnabled) {
                    edgeViewManager.removeEdgeViews()
                    if (settings.gestureEnabled) {
                        edgeViewManager.createEdgeViews(settings, settingsProvider)
                        edgeViewManager.showEdgeViews(settings)
                    }
                } else if (settings.gestureEnabled) {
                    if (oldSettings.hideOverlay != settings.hideOverlay) {
                        edgeViewManager.updateEdgeViewsAlpha(settings)
                    }
                    if (hasSizeOrPositionChanged(oldSettings, settings)) {
                        edgeViewManager.removeEdgeViews()
                        edgeViewManager.createEdgeViews(settings, settingsProvider)
                        edgeViewManager.showEdgeViews(settings)
                    }
                }
            }
        }
    }
}
