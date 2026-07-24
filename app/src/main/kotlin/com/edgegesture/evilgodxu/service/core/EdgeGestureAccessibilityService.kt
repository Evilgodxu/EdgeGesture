package com.edgegesture.evilgodxu.service.core

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityWindowInfo
import com.edgegesture.evilgodxu.data.gesture.GestureAction
import com.edgegesture.evilgodxu.data.gesture.GestureSettingsState
import com.edgegesture.evilgodxu.data.gesture.GestureStatsManager
import com.edgegesture.evilgodxu.data.gesture.gestureDataStore
import com.edgegesture.evilgodxu.data.gesture.gestureSettingsFlow
import com.edgegesture.evilgodxu.data.app.AppRepository
import com.edgegesture.evilgodxu.data.gesture.toGestureSettingsState
import com.edgegesture.evilgodxu.data.launchblock.launchBlockFlow
import com.edgegesture.evilgodxu.data.shizuku.ShizukuManager
import com.edgegesture.evilgodxu.service.core.LaunchBlockInterceptor
import com.edgegesture.evilgodxu.service.remind.RemindAlarmReceiver
import com.edgegesture.evilgodxu.service.remind.RemindAlarmService
import com.edgegesture.evilgodxu.service.signal.BackTapDetector
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
                            // 视图创建/恢复后同步检查键盘状态
                            checkKeyboardVisibility()
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
    private val settingsProvider: () -> GestureSettingsState = {
        val s = settings
        if (s.doubleSwipeEnabled && !isLandscapeMode()) {
            s.copy(doubleSwipeEnabled = false)
        } else {
            s
        }
    }

    private lateinit var edgeViewManager: AccessibilityEdgeViewManager
    private lateinit var actionExecutor: AccessibilityActionExecutor
    private lateinit var gestureDetector: AccessibilityGestureDetector
    private var backTapDetector: BackTapDetector? = null

    // 检测当前是否为横屏模式（二次滑动仅需在横屏下生效）
    private fun isLandscapeMode(): Boolean {
        return resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

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
    private lateinit var launchBlockInterceptor: LaunchBlockInterceptor
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

    override fun onServiceConnected() {
        super.onServiceConnected()
        weakInstance = java.lang.ref.WeakReference(this)

        // 重置连续失败计数，每次服务重新连接都从头开始
        consecutiveAttachFailures = 0

        GestureStatsManager.init(this)

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
            // 黑名单初始化由 initializeWithScan() 中的 initBlacklistFromApps() 完成，
            // 它会根据实际权限状态正确处理（有权限时扫描全部系统应用，无权限时使用已扫描的可启动应用兜底）
            loadSettings()
            // 注意：边缘视图的创建由 startSettingsFlow() 的初始发射值处理，
            // 背面双击检测器的启动也由 updateBackTapDetector() 管理，
            // 这里只负责加载设置到内存，不重复创建视图/检测器。
        }

        // 初始化完成后延迟检查键盘状态，确保在 startSettingsFlow()
        // 完成边缘视图创建后同步键盘可见性。
        Handler(Looper.getMainLooper()).postDelayed({
            checkKeyboardVisibility()
        }, 1000)

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
                launchBlockInterceptor.launchBlockState = state
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
                            // 启用手势后同步检查键盘状态
                            checkKeyboardVisibility()
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
                            // 视图重建后同步检查键盘状态
                            checkKeyboardVisibility()
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
                    launchBlockInterceptor.checkAppLaunch(it)
                }
                AccessibilityEvent.TYPE_VIEW_FOCUSED -> checkInputFieldFocused(it)
            }
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
                        checkKeyboardVisibility()
                    }
                } else if (settings.gestureEnabled) {
                    if (oldSettings.hideOverlay != settings.hideOverlay) {
                        edgeViewManager.updateEdgeViewsAlpha(settings)
                    }
                    if (hasSizeOrPositionChanged(oldSettings, settings)) {
                        edgeViewManager.removeEdgeViews()
                        edgeViewManager.createEdgeViews(settings, settingsProvider)
                        edgeViewManager.showEdgeViews(settings)
                        checkKeyboardVisibility()
                    }
                }
                // 即使设置未变化，也可能只切换了 avoidKeyboardOverlap 开关，
                // 每次 refresh 都重新同步键盘状态确保一致性
                if (settings.gestureEnabled) {
                    checkKeyboardVisibility()
                }
            }
        }
    }
}
