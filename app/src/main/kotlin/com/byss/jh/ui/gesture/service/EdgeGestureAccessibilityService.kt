package com.byss.jh.ui.gesture.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import com.byss.jh.util.Logger
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
    private var isKeyboardVisible = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        Logger.i(this, TAG, "无障碍服务已连接")
        weakInstance = java.lang.ref.WeakReference(this)

        actionExecutor = AccessibilityActionExecutor(this)
        gestureDetector = AccessibilityGestureDetector(this, this)
        edgeViewManager = AccessibilityEdgeViewManager(this, gestureDetector)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(settingsReceiver, IntentFilter("ACTION_UPDATE_SETTINGS"), Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(settingsReceiver, IntentFilter("ACTION_UPDATE_SETTINGS"))
        }

        startSettingsFlow()

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
                            Logger.i(this@EdgeGestureAccessibilityService, TAG, "边缘尺寸或位置设置变化，重新创建视图")
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

        // 检测输入法状态变化
        event?.let {
            if (it.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
                it.eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED) {
                checkKeyboardVisibility()
            }
        }
    }

    /**
     * 检测输入法是否弹出
     * 通过检查窗口列表中是否存在输入法窗口
     */
    private fun checkKeyboardVisibility() {
        val windowList = windows
        if (windowList.isEmpty()) {
            return
        }

        val hasKeyboardWindow = windowList.any { windowInfo ->
            windowInfo.type == android.view.accessibility.AccessibilityWindowInfo.TYPE_INPUT_METHOD
        }

        if (hasKeyboardWindow && !isKeyboardVisible) {
            isKeyboardVisible = true
            Logger.i(this, TAG, "检测到输入法弹出")
            edgeViewManager.hideEdgeViewsForKeyboard()
        } else if (!hasKeyboardWindow && isKeyboardVisible) {
            isKeyboardVisible = false
            Logger.i(this, TAG, "检测到输入法关闭")
            edgeViewManager.restoreEdgeViewsAfterKeyboard()
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
        Handler(Looper.getMainLooper()).postDelayed({
            if (settings.gestureEnabled && edgeViewManager.isViewAttached()) {
                edgeViewManager.updateEdgeViewsLayout(settings)
            }
        }, 100)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Logger.i(this, TAG, "无障碍服务断开")
        weakInstance = null
        unregisterReceiver(settingsReceiver)
        settingsFlowJob?.cancel()
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
                        Logger.i(this@EdgeGestureAccessibilityService, TAG, "边缘尺寸或位置改变，更新布局")
                        edgeViewManager.removeEdgeViews()
                        edgeViewManager.createEdgeViews(settings, settingsProvider)
                        edgeViewManager.showEdgeViews(settings)
                    }
                }
            }
        }
    }
}
