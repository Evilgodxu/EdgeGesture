package com.byss.jh.ui.gesture.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import com.byss.jh.data.gesture.GestureAction
import com.byss.jh.data.gesture.GestureSettingsKeys
import com.byss.jh.data.gesture.GestureSettingsState
import com.byss.jh.data.gesture.gestureDataStore
import com.byss.jh.data.gesture.gestureSettingsFlow
import com.byss.jh.data.gesture.initBlacklistIfNeeded
import com.byss.jh.data.launchblock.LaunchBlockRule
import com.byss.jh.data.launchblock.LaunchBlockState
import com.byss.jh.data.launchblock.launchBlockFlow
import com.byss.jh.data.launchblock.updateLaunchBlockRule
import com.byss.jh.data.shizuku.ShizukuManager
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

        private var weakInstance: java.lang.ref.WeakReference<EdgeGestureAccessibilityService>? = null

        fun getInstance(): EdgeGestureAccessibilityService? = weakInstance?.get()

        fun isAvailable(): Boolean = weakInstance?.get() != null

        fun startService(context: Context) {
            val intent = Intent(context, EdgeGestureAccessibilityService::class.java).apply {
                action = "ACTION_START"
            }
            context.startService(intent)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, EdgeGestureAccessibilityService::class.java).apply {
                action = "ACTION_STOP"
            }
            context.startService(intent)
        }

        fun updateSettings(context: Context) {
            val intent = Intent(context, EdgeGestureAccessibilityService::class.java).apply {
                action = "ACTION_UPDATE_SETTINGS"
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var settings: GestureSettingsState = GestureSettingsState()
    private val settingsProvider: () -> GestureSettingsState = { settings }

    private lateinit var edgeViewManager: AccessibilityEdgeViewManager
    private lateinit var actionExecutor: AccessibilityActionExecutor
    private lateinit var gestureDetector: AccessibilityGestureDetector

    private val settingsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "ACTION_UPDATE_SETTINGS" -> refreshSettings()
            }
        }
    }

    private var settingsFlowJob: kotlinx.coroutines.Job? = null
    private var launchBlockFlowJob: kotlinx.coroutines.Job? = null
    private var isKeyboardVisible = false

    // 启动拦截相关
    private var launchBlockState: LaunchBlockState = LaunchBlockState()
    private var currentPackage: String? = null
    private val launchHistory = mutableMapOf<String, MutableList<Long>>() // 包名 -> 启动时间列表

    override fun onServiceConnected() {
        super.onServiceConnected()
        weakInstance = java.lang.ref.WeakReference(this)

        actionExecutor = AccessibilityActionExecutor(this)
        gestureDetector = AccessibilityGestureDetector(this, this)
        edgeViewManager = AccessibilityEdgeViewManager(this, gestureDetector)

        // 初始化 Shizuku
        ShizukuManager.init(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(settingsReceiver, IntentFilter("ACTION_UPDATE_SETTINGS"), Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(settingsReceiver, IntentFilter("ACTION_UPDATE_SETTINGS"))
        }

        startSettingsFlow()
        startLaunchBlockFlow()

        serviceScope.launch {
            initBlacklistIfNeeded()
            loadSettings()
            if (settings.gestureEnabled) {
                withContext(Dispatchers.Main) {
                    edgeViewManager.createEdgeViews(settings, settingsProvider)
                    edgeViewManager.showEdgeViews(settings)
                }
            }
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

        // 执行拦截：切换应用或返回桌面
        if (isLauncherSystemApp && launcherPackage != null) {
            // 系统应用启动，返回桌面并提示
            performGlobalAction(GLOBAL_ACTION_HOME)
            showSystemAppWarning(launcherPackage)
        } else if (launcherPackage != null && launcherPackage != packageName) {
            // 非系统应用，切换到上一个应用
            actionExecutor.performAction(GestureAction.LAST_APP, settings)
        } else {
            // 没有上一个应用，返回桌面
            performGlobalAction(GLOBAL_ACTION_HOME)
        }

        // 终止被启动者（如果启用）
        if (rule.enableKillTarget) {
            // 系统应用需要额外检查是否允许终止
            if (!isTargetSystemApp || rule.allowKillSystemApp) {
                killAppProcess(targetPackage)
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

    // 显示系统应用警告提示
    private fun showSystemAppWarning(packageName: String) {
        val appName = try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            packageName
        }

        Handler(Looper.getMainLooper()).post {
            android.widget.Toast.makeText(
                this,
                "警告：非法程序为系统应用 ($appName)",
                android.widget.Toast.LENGTH_LONG
            ).show()
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
        if (visible && !isKeyboardVisible) {
            isKeyboardVisible = true
            edgeViewManager.disableEdgeViewsTouch()
        } else if (!visible && isKeyboardVisible) {
            isKeyboardVisible = false
            edgeViewManager.enableEdgeViewsTouch()
        }
    }

    override fun onInterrupt() {}

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "ACTION_UPDATE_SETTINGS" -> refreshSettings()
            "ACTION_START" -> {
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
            "ACTION_STOP" -> edgeViewManager.removeEdgeViews()
        }
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
        unregisterReceiver(settingsReceiver)
        settingsFlowJob?.cancel()
        launchBlockFlowJob?.cancel()
        edgeViewManager.removeEdgeViews()
        actionExecutor.dismissExpandPanel()
        serviceScope.cancel()
        return super.onUnbind(intent)
    }

    override fun onSwipeAction(action: GestureAction) {
        actionExecutor.performAction(action, settings)
    }

    private suspend fun loadSettings() {
        val prefs = gestureDataStore.data.first()
        settings = GestureSettingsState(
            gestureEnabled = prefs[GestureSettingsKeys.GESTURE_ENABLED] ?: false,
            hideOverlay = prefs[GestureSettingsKeys.HIDE_OVERLAY] ?: false,
            hideFromRecents = prefs[GestureSettingsKeys.HIDE_FROM_RECENTS] ?: false,
            vibrationEnabled = prefs[GestureSettingsKeys.VIBRATION_ENABLED] ?: false,
            leftEdgeWidth = prefs[GestureSettingsKeys.LEFT_EDGE_WIDTH] ?: 20,
            leftEdgeHeightPercent = prefs[GestureSettingsKeys.LEFT_EDGE_HEIGHT_PERCENT] ?: 60,
            leftEdgePositionPercent = prefs[GestureSettingsKeys.LEFT_EDGE_POSITION_PERCENT] ?: 90,
            leftSegmentCount = prefs[GestureSettingsKeys.LEFT_SEGMENT_COUNT] ?: 1,
            rightEdgeWidth = prefs[GestureSettingsKeys.RIGHT_EDGE_WIDTH] ?: 20,
            rightEdgeHeightPercent = prefs[GestureSettingsKeys.RIGHT_EDGE_HEIGHT_PERCENT] ?: 60,
            rightEdgePositionPercent = prefs[GestureSettingsKeys.RIGHT_EDGE_POSITION_PERCENT] ?: 90,
            rightSegmentCount = prefs[GestureSettingsKeys.RIGHT_SEGMENT_COUNT] ?: 1,
            bottomEdgeHeight = prefs[GestureSettingsKeys.BOTTOM_EDGE_HEIGHT] ?: 20,
            bottomEdgeWidthPercent = prefs[GestureSettingsKeys.BOTTOM_EDGE_WIDTH_PERCENT] ?: 80,
            bottomSegmentCount = prefs[GestureSettingsKeys.BOTTOM_SEGMENT_COUNT] ?: 1,
            leftEdge = com.byss.jh.data.gesture.LeftEdgeConfig(
                swipeRight = GestureAction.fromValue(prefs[GestureSettingsKeys.LEFT_SWIPE_RIGHT] ?: GestureAction.BACK.value),
                swipeRightLong = GestureAction.fromValue(prefs[GestureSettingsKeys.LEFT_SWIPE_RIGHT_LONG] ?: GestureAction.LAST_APP.value),
                swipeUp = GestureAction.fromValue(prefs[GestureSettingsKeys.LEFT_SWIPE_UP] ?: GestureAction.PREVIOUS_TRACK.value),
                swipeUpLong = GestureAction.fromValue(prefs[GestureSettingsKeys.LEFT_SWIPE_UP_LONG] ?: GestureAction.POWER_MENU.value),
                swipeDown = GestureAction.fromValue(prefs[GestureSettingsKeys.LEFT_SWIPE_DOWN] ?: GestureAction.SCREENSHOT.value),
                swipeDownLong = GestureAction.fromValue(prefs[GestureSettingsKeys.LEFT_SWIPE_DOWN_LONG] ?: GestureAction.LOCK_SCREEN.value)
            ),
            rightEdge = com.byss.jh.data.gesture.RightEdgeConfig(
                swipeLeft = GestureAction.fromValue(prefs[GestureSettingsKeys.RIGHT_SWIPE_LEFT] ?: GestureAction.BACK.value),
                swipeLeftLong = GestureAction.fromValue(prefs[GestureSettingsKeys.RIGHT_SWIPE_LEFT_LONG] ?: GestureAction.LAST_APP.value),
                swipeUp = GestureAction.fromValue(prefs[GestureSettingsKeys.RIGHT_SWIPE_UP] ?: GestureAction.NEXT_TRACK.value),
                swipeUpLong = GestureAction.fromValue(prefs[GestureSettingsKeys.RIGHT_SWIPE_UP_LONG] ?: GestureAction.FLASHLIGHT.value),
                swipeDown = GestureAction.fromValue(prefs[GestureSettingsKeys.RIGHT_SWIPE_DOWN] ?: GestureAction.VOICE_ASSISTANT.value),
                swipeDownLong = GestureAction.fromValue(prefs[GestureSettingsKeys.RIGHT_SWIPE_DOWN_LONG] ?: GestureAction.LOCK_SCREEN.value)
            ),
            bottomEdge = com.byss.jh.data.gesture.BottomEdgeConfig(
                swipeUp = GestureAction.fromValue(prefs[GestureSettingsKeys.BOTTOM_SWIPE_UP] ?: GestureAction.HOME.value),
                swipeUpLong = GestureAction.fromValue(prefs[GestureSettingsKeys.BOTTOM_SWIPE_UP_LONG] ?: GestureAction.RECENT.value),
                swipeLeft = GestureAction.fromValue(prefs[GestureSettingsKeys.BOTTOM_SWIPE_LEFT] ?: GestureAction.NONE.value),
                swipeLeftLong = GestureAction.fromValue(prefs[GestureSettingsKeys.BOTTOM_SWIPE_LEFT_LONG] ?: GestureAction.NONE.value),
                swipeRight = GestureAction.fromValue(prefs[GestureSettingsKeys.BOTTOM_SWIPE_RIGHT] ?: GestureAction.NONE.value),
                swipeRightLong = GestureAction.fromValue(prefs[GestureSettingsKeys.BOTTOM_SWIPE_RIGHT_LONG] ?: GestureAction.NONE.value)
            ),
            leftEdgeSegment2 = com.byss.jh.data.gesture.LeftEdgeConfig(
                swipeRight = GestureAction.fromValue(prefs[GestureSettingsKeys.LEFT_2_SWIPE_RIGHT] ?: GestureAction.NONE.value),
                swipeRightLong = GestureAction.fromValue(prefs[GestureSettingsKeys.LEFT_2_SWIPE_RIGHT_LONG] ?: GestureAction.NONE.value),
                swipeUp = GestureAction.fromValue(prefs[GestureSettingsKeys.LEFT_2_SWIPE_UP] ?: GestureAction.NONE.value),
                swipeUpLong = GestureAction.fromValue(prefs[GestureSettingsKeys.LEFT_2_SWIPE_UP_LONG] ?: GestureAction.NONE.value),
                swipeDown = GestureAction.fromValue(prefs[GestureSettingsKeys.LEFT_2_SWIPE_DOWN] ?: GestureAction.NONE.value),
                swipeDownLong = GestureAction.fromValue(prefs[GestureSettingsKeys.LEFT_2_SWIPE_DOWN_LONG] ?: GestureAction.NONE.value)
            ),
            rightEdgeSegment2 = com.byss.jh.data.gesture.RightEdgeConfig(
                swipeLeft = GestureAction.fromValue(prefs[GestureSettingsKeys.RIGHT_2_SWIPE_LEFT] ?: GestureAction.NONE.value),
                swipeLeftLong = GestureAction.fromValue(prefs[GestureSettingsKeys.RIGHT_2_SWIPE_LEFT_LONG] ?: GestureAction.NONE.value),
                swipeUp = GestureAction.fromValue(prefs[GestureSettingsKeys.RIGHT_2_SWIPE_UP] ?: GestureAction.NONE.value),
                swipeUpLong = GestureAction.fromValue(prefs[GestureSettingsKeys.RIGHT_2_SWIPE_UP_LONG] ?: GestureAction.NONE.value),
                swipeDown = GestureAction.fromValue(prefs[GestureSettingsKeys.RIGHT_2_SWIPE_DOWN] ?: GestureAction.NONE.value),
                swipeDownLong = GestureAction.fromValue(prefs[GestureSettingsKeys.RIGHT_2_SWIPE_DOWN_LONG] ?: GestureAction.NONE.value)
            ),
            bottomEdgeSegment2 = com.byss.jh.data.gesture.BottomEdgeConfig(
                swipeUp = GestureAction.fromValue(prefs[GestureSettingsKeys.BOTTOM_2_SWIPE_UP] ?: GestureAction.NONE.value),
                swipeUpLong = GestureAction.fromValue(prefs[GestureSettingsKeys.BOTTOM_2_SWIPE_UP_LONG] ?: GestureAction.NONE.value),
                swipeLeft = GestureAction.fromValue(prefs[GestureSettingsKeys.BOTTOM_2_SWIPE_LEFT] ?: GestureAction.NONE.value),
                swipeLeftLong = GestureAction.fromValue(prefs[GestureSettingsKeys.BOTTOM_2_SWIPE_LEFT_LONG] ?: GestureAction.NONE.value),
                swipeRight = GestureAction.fromValue(prefs[GestureSettingsKeys.BOTTOM_2_SWIPE_RIGHT] ?: GestureAction.NONE.value),
                swipeRightLong = GestureAction.fromValue(prefs[GestureSettingsKeys.BOTTOM_2_SWIPE_RIGHT_LONG] ?: GestureAction.NONE.value)
            ),
            leftEdgeSegment3 = com.byss.jh.data.gesture.LeftEdgeConfig(
                swipeRight = GestureAction.fromValue(prefs[GestureSettingsKeys.LEFT_3_SWIPE_RIGHT] ?: GestureAction.NONE.value),
                swipeRightLong = GestureAction.fromValue(prefs[GestureSettingsKeys.LEFT_3_SWIPE_RIGHT_LONG] ?: GestureAction.NONE.value),
                swipeUp = GestureAction.fromValue(prefs[GestureSettingsKeys.LEFT_3_SWIPE_UP] ?: GestureAction.NONE.value),
                swipeUpLong = GestureAction.fromValue(prefs[GestureSettingsKeys.LEFT_3_SWIPE_UP_LONG] ?: GestureAction.NONE.value),
                swipeDown = GestureAction.fromValue(prefs[GestureSettingsKeys.LEFT_3_SWIPE_DOWN] ?: GestureAction.NONE.value),
                swipeDownLong = GestureAction.fromValue(prefs[GestureSettingsKeys.LEFT_3_SWIPE_DOWN_LONG] ?: GestureAction.NONE.value)
            ),
            rightEdgeSegment3 = com.byss.jh.data.gesture.RightEdgeConfig(
                swipeLeft = GestureAction.fromValue(prefs[GestureSettingsKeys.RIGHT_3_SWIPE_LEFT] ?: GestureAction.NONE.value),
                swipeLeftLong = GestureAction.fromValue(prefs[GestureSettingsKeys.RIGHT_3_SWIPE_LEFT_LONG] ?: GestureAction.NONE.value),
                swipeUp = GestureAction.fromValue(prefs[GestureSettingsKeys.RIGHT_3_SWIPE_UP] ?: GestureAction.NONE.value),
                swipeUpLong = GestureAction.fromValue(prefs[GestureSettingsKeys.RIGHT_3_SWIPE_UP_LONG] ?: GestureAction.NONE.value),
                swipeDown = GestureAction.fromValue(prefs[GestureSettingsKeys.RIGHT_3_SWIPE_DOWN] ?: GestureAction.NONE.value),
                swipeDownLong = GestureAction.fromValue(prefs[GestureSettingsKeys.RIGHT_3_SWIPE_DOWN_LONG] ?: GestureAction.NONE.value)
            ),
            bottomEdgeSegment3 = com.byss.jh.data.gesture.BottomEdgeConfig(
                swipeUp = GestureAction.fromValue(prefs[GestureSettingsKeys.BOTTOM_3_SWIPE_UP] ?: GestureAction.NONE.value),
                swipeUpLong = GestureAction.fromValue(prefs[GestureSettingsKeys.BOTTOM_3_SWIPE_UP_LONG] ?: GestureAction.NONE.value),
                swipeLeft = GestureAction.fromValue(prefs[GestureSettingsKeys.BOTTOM_3_SWIPE_LEFT] ?: GestureAction.NONE.value),
                swipeLeftLong = GestureAction.fromValue(prefs[GestureSettingsKeys.BOTTOM_3_SWIPE_LEFT_LONG] ?: GestureAction.NONE.value),
                swipeRight = GestureAction.fromValue(prefs[GestureSettingsKeys.BOTTOM_3_SWIPE_RIGHT] ?: GestureAction.NONE.value),
                swipeRightLong = GestureAction.fromValue(prefs[GestureSettingsKeys.BOTTOM_3_SWIPE_RIGHT_LONG] ?: GestureAction.NONE.value)
            )
        )
    }

    private fun refreshSettings() {
        serviceScope.launch {
            val oldSettings = settings
            loadSettings()
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
