package com.edgegesture.evilgodxu.data.gesture

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

val Context.gestureDataStore: DataStore<Preferences> by preferencesDataStore(name = "gesture_settings")

object GestureSettingsKeys {
    val GESTURE_ENABLED = booleanPreferencesKey("gesture_enabled")
    val HIDE_OVERLAY = booleanPreferencesKey("hide_overlay")
    val HIDE_FROM_RECENTS = booleanPreferencesKey("hide_from_recents")
    val AVOID_KEYBOARD_OVERLAP = booleanPreferencesKey("avoid_keyboard_overlap")

    // 左侧边缘设置
    val LEFT_EDGE_WIDTH = intPreferencesKey("left_edge_width")
    val LEFT_EDGE_HEIGHT_PERCENT = intPreferencesKey("left_edge_height_percent")
    val LEFT_EDGE_POSITION_PERCENT = intPreferencesKey("left_edge_position_percent")
    val LEFT_SEGMENT_COUNT = intPreferencesKey("left_segment_count")

    // 右侧边缘设置
    val RIGHT_EDGE_WIDTH = intPreferencesKey("right_edge_width")
    val RIGHT_EDGE_HEIGHT_PERCENT = intPreferencesKey("right_edge_height_percent")
    val RIGHT_EDGE_POSITION_PERCENT = intPreferencesKey("right_edge_position_percent")
    val RIGHT_SEGMENT_COUNT = intPreferencesKey("right_segment_count")

    // 底部边缘设置
    val BOTTOM_EDGE_HEIGHT = intPreferencesKey("bottom_edge_height")
    val BOTTOM_EDGE_WIDTH_PERCENT = intPreferencesKey("bottom_edge_width_percent")
    val BOTTOM_SEGMENT_COUNT = intPreferencesKey("bottom_segment_count")

    // 手势操作设置（第1段）
    val LEFT_SWIPE_RIGHT = stringPreferencesKey("left_swipe_right")
    val LEFT_SWIPE_RIGHT_LONG = stringPreferencesKey("left_swipe_right_long")
    val LEFT_SWIPE_UP = stringPreferencesKey("left_swipe_up")
    val LEFT_SWIPE_UP_LONG = stringPreferencesKey("left_swipe_up_long")
    val LEFT_SWIPE_DOWN = stringPreferencesKey("left_swipe_down")
    val LEFT_SWIPE_DOWN_LONG = stringPreferencesKey("left_swipe_down_long")

    val RIGHT_SWIPE_LEFT = stringPreferencesKey("right_swipe_left")
    val RIGHT_SWIPE_LEFT_LONG = stringPreferencesKey("right_swipe_left_long")
    val RIGHT_SWIPE_UP = stringPreferencesKey("right_swipe_up")
    val RIGHT_SWIPE_UP_LONG = stringPreferencesKey("right_swipe_up_long")
    val RIGHT_SWIPE_DOWN = stringPreferencesKey("right_swipe_down")
    val RIGHT_SWIPE_DOWN_LONG = stringPreferencesKey("right_swipe_down_long")

    val BOTTOM_SWIPE_UP = stringPreferencesKey("bottom_swipe_up")
    val BOTTOM_SWIPE_UP_LONG = stringPreferencesKey("bottom_swipe_up_long")
    val BOTTOM_SWIPE_LEFT = stringPreferencesKey("bottom_swipe_left")
    val BOTTOM_SWIPE_LEFT_LONG = stringPreferencesKey("bottom_swipe_left_long")
    val BOTTOM_SWIPE_RIGHT = stringPreferencesKey("bottom_swipe_right")
    val BOTTOM_SWIPE_RIGHT_LONG = stringPreferencesKey("bottom_swipe_right_long")

    // 左侧手势操作设置（第2段）
    val LEFT_2_SWIPE_RIGHT = stringPreferencesKey("left_2_swipe_right")
    val LEFT_2_SWIPE_RIGHT_LONG = stringPreferencesKey("left_2_swipe_right_long")
    val LEFT_2_SWIPE_UP = stringPreferencesKey("left_2_swipe_up")
    val LEFT_2_SWIPE_UP_LONG = stringPreferencesKey("left_2_swipe_up_long")
    val LEFT_2_SWIPE_DOWN = stringPreferencesKey("left_2_swipe_down")
    val LEFT_2_SWIPE_DOWN_LONG = stringPreferencesKey("left_2_swipe_down_long")

    // 左侧手势操作设置（第3段）
    val LEFT_3_SWIPE_RIGHT = stringPreferencesKey("left_3_swipe_right")
    val LEFT_3_SWIPE_RIGHT_LONG = stringPreferencesKey("left_3_swipe_right_long")
    val LEFT_3_SWIPE_UP = stringPreferencesKey("left_3_swipe_up")
    val LEFT_3_SWIPE_UP_LONG = stringPreferencesKey("left_3_swipe_up_long")
    val LEFT_3_SWIPE_DOWN = stringPreferencesKey("left_3_swipe_down")
    val LEFT_3_SWIPE_DOWN_LONG = stringPreferencesKey("left_3_swipe_down_long")

    // 右侧手势操作设置（第2段）
    val RIGHT_2_SWIPE_LEFT = stringPreferencesKey("right_2_swipe_left")
    val RIGHT_2_SWIPE_LEFT_LONG = stringPreferencesKey("right_2_swipe_left_long")
    val RIGHT_2_SWIPE_UP = stringPreferencesKey("right_2_swipe_up")
    val RIGHT_2_SWIPE_UP_LONG = stringPreferencesKey("right_2_swipe_up_long")
    val RIGHT_2_SWIPE_DOWN = stringPreferencesKey("right_2_swipe_down")
    val RIGHT_2_SWIPE_DOWN_LONG = stringPreferencesKey("right_2_swipe_down_long")

    // 右侧手势操作设置（第3段）
    val RIGHT_3_SWIPE_LEFT = stringPreferencesKey("right_3_swipe_left")
    val RIGHT_3_SWIPE_LEFT_LONG = stringPreferencesKey("right_3_swipe_left_long")
    val RIGHT_3_SWIPE_UP = stringPreferencesKey("right_3_swipe_up")
    val RIGHT_3_SWIPE_UP_LONG = stringPreferencesKey("right_3_swipe_up_long")
    val RIGHT_3_SWIPE_DOWN = stringPreferencesKey("right_3_swipe_down")
    val RIGHT_3_SWIPE_DOWN_LONG = stringPreferencesKey("right_3_swipe_down_long")

    // 底部手势操作设置（第2段）
    val BOTTOM_2_SWIPE_UP = stringPreferencesKey("bottom_2_swipe_up")
    val BOTTOM_2_SWIPE_UP_LONG = stringPreferencesKey("bottom_2_swipe_up_long")
    val BOTTOM_2_SWIPE_LEFT = stringPreferencesKey("bottom_2_swipe_left")
    val BOTTOM_2_SWIPE_LEFT_LONG = stringPreferencesKey("bottom_2_swipe_left_long")
    val BOTTOM_2_SWIPE_RIGHT = stringPreferencesKey("bottom_2_swipe_right")
    val BOTTOM_2_SWIPE_RIGHT_LONG = stringPreferencesKey("bottom_2_swipe_right_long")

    // 底部手势操作设置（第3段）
    val BOTTOM_3_SWIPE_UP = stringPreferencesKey("bottom_3_swipe_up")
    val BOTTOM_3_SWIPE_UP_LONG = stringPreferencesKey("bottom_3_swipe_up_long")
    val BOTTOM_3_SWIPE_LEFT = stringPreferencesKey("bottom_3_swipe_left")
    val BOTTOM_3_SWIPE_LEFT_LONG = stringPreferencesKey("bottom_3_swipe_left_long")
    val BOTTOM_3_SWIPE_RIGHT = stringPreferencesKey("bottom_3_swipe_right")
    val BOTTOM_3_SWIPE_RIGHT_LONG = stringPreferencesKey("bottom_3_swipe_right_long")

    // 背面双击设置
    val BACK_TAP_ENABLED = booleanPreferencesKey("back_tap_enabled")
    val BACK_TAP_SENSITIVITY = intPreferencesKey("back_tap_sensitivity")
    val BACK_TAP_RANGE = intPreferencesKey("back_tap_range")
    val BACK_TAP_ACTION = stringPreferencesKey("back_tap_action")
    val BACK_TAP_MODE = stringPreferencesKey("back_tap_mode")
    val BACK_TAP_PAUSE_ON_CHARGING = booleanPreferencesKey("back_tap_pause_on_charging")

    val APP_SWITCH_BLACKLIST = stringSetPreferencesKey("app_switch_blacklist")
    val BLACKLIST_INITIALIZED = booleanPreferencesKey("blacklist_initialized")
    val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
}

// 手势动作枚举，显示名称通过 [getActionDisplayName] 函数从字符串资源获取，支持多语言
enum class GestureAction(val value: String) {
    NONE("none"),
    HOME("home"),
    RECENT("recent"),
    BACK("back"),
    LAST_APP("last_app"),
    PREVIOUS_TRACK("previous_track"),
    NEXT_TRACK("next_track"),
    FLASHLIGHT("flashlight"),
    VOICE_ASSISTANT("voice_assistant"),
    POWER_MENU("power_menu"),
    LOCK_SCREEN("lock_screen"),
    SCREENSHOT("screenshot"),
    EXPAND_PANEL("expand_panel"),
    // 扫一扫
    ALIPAY_SCAN("alipay_scan"),
    WECHAT_SCAN("wechat_scan"),
    // 延时提醒
    REMIND_1M("remind_1m"),
    REMIND_3M("remind_3m"),
    REMIND_5M("remind_5m"),
    REMIND_10M("remind_10m"),
    REMIND_15M("remind_15m");

    companion object {
        fun fromValue(value: String): GestureAction = entries.find { it.value == value } ?: NONE
    }
}

enum class EdgePosition {
    LEFT, RIGHT, BOTTOM
}

enum class BackTapMode(val value: String) {
    ALWAYS("always"),
    SCREEN_OFF("screen_off"),
    SCREEN_ON("screen_on");

    companion object {
        fun fromValue(value: String): BackTapMode = entries.find { it.value == value } ?: ALWAYS
    }
}

data class EdgeGestureConfig(
    val swipe: GestureAction = GestureAction.NONE,
    val swipeLong: GestureAction = GestureAction.NONE
)

data class LeftEdgeConfig(
    val swipeRight: GestureAction = GestureAction.BACK,
    val swipeRightLong: GestureAction = GestureAction.LAST_APP,
    val swipeUp: GestureAction = GestureAction.PREVIOUS_TRACK,
    val swipeUpLong: GestureAction = GestureAction.POWER_MENU,
    val swipeDown: GestureAction = GestureAction.SCREENSHOT,
    val swipeDownLong: GestureAction = GestureAction.LOCK_SCREEN
)

data class RightEdgeConfig(
    val swipeLeft: GestureAction = GestureAction.BACK,
    val swipeLeftLong: GestureAction = GestureAction.LAST_APP,
    val swipeUp: GestureAction = GestureAction.NEXT_TRACK,
    val swipeUpLong: GestureAction = GestureAction.FLASHLIGHT,
    val swipeDown: GestureAction = GestureAction.VOICE_ASSISTANT,
    val swipeDownLong: GestureAction = GestureAction.EXPAND_PANEL
)

data class BottomEdgeConfig(
    val swipeUp: GestureAction = GestureAction.HOME,
    val swipeUpLong: GestureAction = GestureAction.RECENT,
    val swipeLeft: GestureAction = GestureAction.LAST_APP,
    val swipeLeftLong: GestureAction = GestureAction.NONE,
    val swipeRight: GestureAction = GestureAction.LAST_APP,
    val swipeRightLong: GestureAction = GestureAction.NONE
)

data class GestureSettingsState(
    val gestureEnabled: Boolean = false,
    val hideOverlay: Boolean = false,
    val hideFromRecents: Boolean = false,
    val avoidKeyboardOverlap: Boolean = false,
    val vibrationEnabled: Boolean = false,
    // 背面双击设置
    val backTapEnabled: Boolean = false,
    val backTapSensitivity: Int = 5,
    val backTapRange: Int = 5,
    val backTapAction: GestureAction = GestureAction.HOME,
    val backTapMode: BackTapMode = BackTapMode.ALWAYS,
    val backTapPauseOnCharging: Boolean = false,
    // 左侧边缘尺寸设置
    val leftEdgeWidth: Int = 20,
    val leftEdgeHeightPercent: Int = 60,
    val leftEdgePositionPercent: Int = 90,
    val leftSegmentCount: Int = 1,
    // 右侧边缘尺寸设置
    val rightEdgeWidth: Int = 20,
    val rightEdgeHeightPercent: Int = 60,
    val rightEdgePositionPercent: Int = 90,
    val rightSegmentCount: Int = 1,
    // 底部边缘尺寸设置
    val bottomEdgeHeight: Int = 20,
    val bottomEdgeWidthPercent: Int = 80,
    val bottomSegmentCount: Int = 1,
    // 手势配置（第1段）
    val leftEdge: LeftEdgeConfig = LeftEdgeConfig(),
    val rightEdge: RightEdgeConfig = RightEdgeConfig(),
    val bottomEdge: BottomEdgeConfig = BottomEdgeConfig(),
    // 手势配置（第2段）
    val leftEdgeSegment2: LeftEdgeConfig = LeftEdgeConfig(
        swipeRight = GestureAction.NONE,
        swipeRightLong = GestureAction.NONE,
        swipeUp = GestureAction.NONE,
        swipeUpLong = GestureAction.NONE,
        swipeDown = GestureAction.NONE,
        swipeDownLong = GestureAction.NONE
    ),
    val rightEdgeSegment2: RightEdgeConfig = RightEdgeConfig(
        swipeLeft = GestureAction.NONE,
        swipeLeftLong = GestureAction.NONE,
        swipeUp = GestureAction.NONE,
        swipeUpLong = GestureAction.NONE,
        swipeDown = GestureAction.NONE,
        swipeDownLong = GestureAction.NONE
    ),
    val bottomEdgeSegment2: BottomEdgeConfig = BottomEdgeConfig(
        swipeUp = GestureAction.NONE,
        swipeUpLong = GestureAction.NONE,
        swipeLeft = GestureAction.NONE,
        swipeLeftLong = GestureAction.NONE,
        swipeRight = GestureAction.NONE,
        swipeRightLong = GestureAction.NONE
    ),
    // 手势配置（第3段）
    val leftEdgeSegment3: LeftEdgeConfig = LeftEdgeConfig(
        swipeRight = GestureAction.NONE,
        swipeRightLong = GestureAction.NONE,
        swipeUp = GestureAction.NONE,
        swipeUpLong = GestureAction.NONE,
        swipeDown = GestureAction.NONE,
        swipeDownLong = GestureAction.NONE
    ),
    val rightEdgeSegment3: RightEdgeConfig = RightEdgeConfig(
        swipeLeft = GestureAction.NONE,
        swipeLeftLong = GestureAction.NONE,
        swipeUp = GestureAction.NONE,
        swipeUpLong = GestureAction.NONE,
        swipeDown = GestureAction.NONE,
        swipeDownLong = GestureAction.NONE
    ),
    val bottomEdgeSegment3: BottomEdgeConfig = BottomEdgeConfig(
        swipeUp = GestureAction.NONE,
        swipeUpLong = GestureAction.NONE,
        swipeLeft = GestureAction.NONE,
        swipeLeftLong = GestureAction.NONE,
        swipeRight = GestureAction.NONE,
        swipeRightLong = GestureAction.NONE
    )
)

// 从 Preferences 构建 GestureSettingsState，统一默认值处理
fun Preferences.toGestureSettingsState(): GestureSettingsState {
    return GestureSettingsState(
        gestureEnabled = this[GestureSettingsKeys.GESTURE_ENABLED] ?: false,
        hideOverlay = this[GestureSettingsKeys.HIDE_OVERLAY] ?: false,
        hideFromRecents = this[GestureSettingsKeys.HIDE_FROM_RECENTS] ?: false,
        avoidKeyboardOverlap = this[GestureSettingsKeys.AVOID_KEYBOARD_OVERLAP] ?: false,
        vibrationEnabled = this[GestureSettingsKeys.VIBRATION_ENABLED] ?: false,
        // 背面双击
        backTapEnabled = this[GestureSettingsKeys.BACK_TAP_ENABLED] ?: false,
        backTapSensitivity = this[GestureSettingsKeys.BACK_TAP_SENSITIVITY] ?: 5,
        backTapRange = this[GestureSettingsKeys.BACK_TAP_RANGE] ?: 5,
        backTapAction = GestureAction.fromValue(this[GestureSettingsKeys.BACK_TAP_ACTION] ?: GestureAction.HOME.value),
        backTapMode = BackTapMode.fromValue(this[GestureSettingsKeys.BACK_TAP_MODE] ?: BackTapMode.ALWAYS.value),
        backTapPauseOnCharging = this[GestureSettingsKeys.BACK_TAP_PAUSE_ON_CHARGING] ?: false,
        // 左侧边缘尺寸
        leftEdgeWidth = this[GestureSettingsKeys.LEFT_EDGE_WIDTH] ?: 20,
        leftEdgeHeightPercent = this[GestureSettingsKeys.LEFT_EDGE_HEIGHT_PERCENT] ?: 60,
        leftEdgePositionPercent = this[GestureSettingsKeys.LEFT_EDGE_POSITION_PERCENT] ?: 90,
        leftSegmentCount = this[GestureSettingsKeys.LEFT_SEGMENT_COUNT] ?: 1,
        // 右侧边缘尺寸
        rightEdgeWidth = this[GestureSettingsKeys.RIGHT_EDGE_WIDTH] ?: 20,
        rightEdgeHeightPercent = this[GestureSettingsKeys.RIGHT_EDGE_HEIGHT_PERCENT] ?: 60,
        rightEdgePositionPercent = this[GestureSettingsKeys.RIGHT_EDGE_POSITION_PERCENT] ?: 90,
        rightSegmentCount = this[GestureSettingsKeys.RIGHT_SEGMENT_COUNT] ?: 1,
        // 底部边缘尺寸
        bottomEdgeHeight = this[GestureSettingsKeys.BOTTOM_EDGE_HEIGHT] ?: 20,
        bottomEdgeWidthPercent = this[GestureSettingsKeys.BOTTOM_EDGE_WIDTH_PERCENT] ?: 80,
        bottomSegmentCount = this[GestureSettingsKeys.BOTTOM_SEGMENT_COUNT] ?: 1,
        // 手势配置（第1段）
        leftEdge = LeftEdgeConfig(
            swipeRight = GestureAction.fromValue(this[GestureSettingsKeys.LEFT_SWIPE_RIGHT] ?: GestureAction.BACK.value),
            swipeRightLong = GestureAction.fromValue(this[GestureSettingsKeys.LEFT_SWIPE_RIGHT_LONG] ?: GestureAction.LAST_APP.value),
            swipeUp = GestureAction.fromValue(this[GestureSettingsKeys.LEFT_SWIPE_UP] ?: GestureAction.PREVIOUS_TRACK.value),
            swipeUpLong = GestureAction.fromValue(this[GestureSettingsKeys.LEFT_SWIPE_UP_LONG] ?: GestureAction.POWER_MENU.value),
            swipeDown = GestureAction.fromValue(this[GestureSettingsKeys.LEFT_SWIPE_DOWN] ?: GestureAction.SCREENSHOT.value),
            swipeDownLong = GestureAction.fromValue(this[GestureSettingsKeys.LEFT_SWIPE_DOWN_LONG] ?: GestureAction.LOCK_SCREEN.value)
        ),
        rightEdge = RightEdgeConfig(
            swipeLeft = GestureAction.fromValue(this[GestureSettingsKeys.RIGHT_SWIPE_LEFT] ?: GestureAction.BACK.value),
            swipeLeftLong = GestureAction.fromValue(this[GestureSettingsKeys.RIGHT_SWIPE_LEFT_LONG] ?: GestureAction.LAST_APP.value),
            swipeUp = GestureAction.fromValue(this[GestureSettingsKeys.RIGHT_SWIPE_UP] ?: GestureAction.NEXT_TRACK.value),
            swipeUpLong = GestureAction.fromValue(this[GestureSettingsKeys.RIGHT_SWIPE_UP_LONG] ?: GestureAction.FLASHLIGHT.value),
            swipeDown = GestureAction.fromValue(this[GestureSettingsKeys.RIGHT_SWIPE_DOWN] ?: GestureAction.VOICE_ASSISTANT.value),
            swipeDownLong = GestureAction.fromValue(this[GestureSettingsKeys.RIGHT_SWIPE_DOWN_LONG] ?: GestureAction.EXPAND_PANEL.value)
        ),
        bottomEdge = BottomEdgeConfig(
            swipeUp = GestureAction.fromValue(this[GestureSettingsKeys.BOTTOM_SWIPE_UP] ?: GestureAction.HOME.value),
            swipeUpLong = GestureAction.fromValue(this[GestureSettingsKeys.BOTTOM_SWIPE_UP_LONG] ?: GestureAction.RECENT.value),
            swipeLeft = GestureAction.fromValue(this[GestureSettingsKeys.BOTTOM_SWIPE_LEFT] ?: GestureAction.LAST_APP.value),
            swipeLeftLong = GestureAction.fromValue(this[GestureSettingsKeys.BOTTOM_SWIPE_LEFT_LONG] ?: GestureAction.NONE.value),
            swipeRight = GestureAction.fromValue(this[GestureSettingsKeys.BOTTOM_SWIPE_RIGHT] ?: GestureAction.LAST_APP.value),
            swipeRightLong = GestureAction.fromValue(this[GestureSettingsKeys.BOTTOM_SWIPE_RIGHT_LONG] ?: GestureAction.NONE.value)
        ),
        // 手势配置（第2段）
        leftEdgeSegment2 = LeftEdgeConfig(
            swipeRight = GestureAction.fromValue(this[GestureSettingsKeys.LEFT_2_SWIPE_RIGHT] ?: GestureAction.NONE.value),
            swipeRightLong = GestureAction.fromValue(this[GestureSettingsKeys.LEFT_2_SWIPE_RIGHT_LONG] ?: GestureAction.NONE.value),
            swipeUp = GestureAction.fromValue(this[GestureSettingsKeys.LEFT_2_SWIPE_UP] ?: GestureAction.NONE.value),
            swipeUpLong = GestureAction.fromValue(this[GestureSettingsKeys.LEFT_2_SWIPE_UP_LONG] ?: GestureAction.NONE.value),
            swipeDown = GestureAction.fromValue(this[GestureSettingsKeys.LEFT_2_SWIPE_DOWN] ?: GestureAction.NONE.value),
            swipeDownLong = GestureAction.fromValue(this[GestureSettingsKeys.LEFT_2_SWIPE_DOWN_LONG] ?: GestureAction.NONE.value)
        ),
        rightEdgeSegment2 = RightEdgeConfig(
            swipeLeft = GestureAction.fromValue(this[GestureSettingsKeys.RIGHT_2_SWIPE_LEFT] ?: GestureAction.NONE.value),
            swipeLeftLong = GestureAction.fromValue(this[GestureSettingsKeys.RIGHT_2_SWIPE_LEFT_LONG] ?: GestureAction.NONE.value),
            swipeUp = GestureAction.fromValue(this[GestureSettingsKeys.RIGHT_2_SWIPE_UP] ?: GestureAction.NONE.value),
            swipeUpLong = GestureAction.fromValue(this[GestureSettingsKeys.RIGHT_2_SWIPE_UP_LONG] ?: GestureAction.NONE.value),
            swipeDown = GestureAction.fromValue(this[GestureSettingsKeys.RIGHT_2_SWIPE_DOWN] ?: GestureAction.NONE.value),
            swipeDownLong = GestureAction.fromValue(this[GestureSettingsKeys.RIGHT_2_SWIPE_DOWN_LONG] ?: GestureAction.NONE.value)
        ),
        bottomEdgeSegment2 = BottomEdgeConfig(
            swipeUp = GestureAction.fromValue(this[GestureSettingsKeys.BOTTOM_2_SWIPE_UP] ?: GestureAction.NONE.value),
            swipeUpLong = GestureAction.fromValue(this[GestureSettingsKeys.BOTTOM_2_SWIPE_UP_LONG] ?: GestureAction.NONE.value),
            swipeLeft = GestureAction.fromValue(this[GestureSettingsKeys.BOTTOM_2_SWIPE_LEFT] ?: GestureAction.NONE.value),
            swipeLeftLong = GestureAction.fromValue(this[GestureSettingsKeys.BOTTOM_2_SWIPE_LEFT_LONG] ?: GestureAction.NONE.value),
            swipeRight = GestureAction.fromValue(this[GestureSettingsKeys.BOTTOM_2_SWIPE_RIGHT] ?: GestureAction.NONE.value),
            swipeRightLong = GestureAction.fromValue(this[GestureSettingsKeys.BOTTOM_2_SWIPE_RIGHT_LONG] ?: GestureAction.NONE.value)
        ),
        // 手势配置（第3段）
        leftEdgeSegment3 = LeftEdgeConfig(
            swipeRight = GestureAction.fromValue(this[GestureSettingsKeys.LEFT_3_SWIPE_RIGHT] ?: GestureAction.NONE.value),
            swipeRightLong = GestureAction.fromValue(this[GestureSettingsKeys.LEFT_3_SWIPE_RIGHT_LONG] ?: GestureAction.NONE.value),
            swipeUp = GestureAction.fromValue(this[GestureSettingsKeys.LEFT_3_SWIPE_UP] ?: GestureAction.NONE.value),
            swipeUpLong = GestureAction.fromValue(this[GestureSettingsKeys.LEFT_3_SWIPE_UP_LONG] ?: GestureAction.NONE.value),
            swipeDown = GestureAction.fromValue(this[GestureSettingsKeys.LEFT_3_SWIPE_DOWN] ?: GestureAction.NONE.value),
            swipeDownLong = GestureAction.fromValue(this[GestureSettingsKeys.LEFT_3_SWIPE_DOWN_LONG] ?: GestureAction.NONE.value)
        ),
        rightEdgeSegment3 = RightEdgeConfig(
            swipeLeft = GestureAction.fromValue(this[GestureSettingsKeys.RIGHT_3_SWIPE_LEFT] ?: GestureAction.NONE.value),
            swipeLeftLong = GestureAction.fromValue(this[GestureSettingsKeys.RIGHT_3_SWIPE_LEFT_LONG] ?: GestureAction.NONE.value),
            swipeUp = GestureAction.fromValue(this[GestureSettingsKeys.RIGHT_3_SWIPE_UP] ?: GestureAction.NONE.value),
            swipeUpLong = GestureAction.fromValue(this[GestureSettingsKeys.RIGHT_3_SWIPE_UP_LONG] ?: GestureAction.NONE.value),
            swipeDown = GestureAction.fromValue(this[GestureSettingsKeys.RIGHT_3_SWIPE_DOWN] ?: GestureAction.NONE.value),
            swipeDownLong = GestureAction.fromValue(this[GestureSettingsKeys.RIGHT_3_SWIPE_DOWN_LONG] ?: GestureAction.NONE.value)
        ),
        bottomEdgeSegment3 = BottomEdgeConfig(
            swipeUp = GestureAction.fromValue(this[GestureSettingsKeys.BOTTOM_3_SWIPE_UP] ?: GestureAction.NONE.value),
            swipeUpLong = GestureAction.fromValue(this[GestureSettingsKeys.BOTTOM_3_SWIPE_UP_LONG] ?: GestureAction.NONE.value),
            swipeLeft = GestureAction.fromValue(this[GestureSettingsKeys.BOTTOM_3_SWIPE_LEFT] ?: GestureAction.NONE.value),
            swipeLeftLong = GestureAction.fromValue(this[GestureSettingsKeys.BOTTOM_3_SWIPE_LEFT_LONG] ?: GestureAction.NONE.value),
            swipeRight = GestureAction.fromValue(this[GestureSettingsKeys.BOTTOM_3_SWIPE_RIGHT] ?: GestureAction.NONE.value),
            swipeRightLong = GestureAction.fromValue(this[GestureSettingsKeys.BOTTOM_3_SWIPE_RIGHT_LONG] ?: GestureAction.NONE.value)
        )
    )
}

fun Context.gestureSettingsFlow(): Flow<GestureSettingsState> = gestureDataStore.data.map { prefs ->
    prefs.toGestureSettingsState()
}

suspend fun Context.saveGestureEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
    gestureDataStore.edit { prefs ->
        prefs[GestureSettingsKeys.GESTURE_ENABLED] = enabled
    }
}

suspend fun Context.saveHideOverlay(hide: Boolean) = withContext(Dispatchers.IO) {
    gestureDataStore.edit { prefs ->
        prefs[GestureSettingsKeys.HIDE_OVERLAY] = hide
    }
}

suspend fun Context.saveHideFromRecents(hide: Boolean) = withContext(Dispatchers.IO) {
    gestureDataStore.edit { prefs ->
        prefs[GestureSettingsKeys.HIDE_FROM_RECENTS] = hide
    }
}

suspend fun Context.saveAvoidKeyboardOverlap(enabled: Boolean) = withContext(Dispatchers.IO) {
    gestureDataStore.edit { prefs ->
        prefs[GestureSettingsKeys.AVOID_KEYBOARD_OVERLAP] = enabled
    }
}

// 左侧边缘尺寸设置
suspend fun Context.saveLeftEdgeWidth(width: Int) = withContext(Dispatchers.IO) {
    gestureDataStore.edit { prefs ->
        prefs[GestureSettingsKeys.LEFT_EDGE_WIDTH] = width.coerceIn(10, 50)
    }
}

suspend fun Context.saveLeftEdgeHeightPercent(percent: Int) = withContext(Dispatchers.IO) {
    gestureDataStore.edit { prefs ->
        prefs[GestureSettingsKeys.LEFT_EDGE_HEIGHT_PERCENT] = percent.coerceIn(20, 100)
    }
}

suspend fun Context.saveLeftEdgePositionPercent(percent: Int) = withContext(Dispatchers.IO) {
    gestureDataStore.edit { prefs ->
        prefs[GestureSettingsKeys.LEFT_EDGE_POSITION_PERCENT] = percent.coerceIn(0, 100)
    }
}

suspend fun Context.saveLeftSegmentCount(count: Int) = withContext(Dispatchers.IO) {
    gestureDataStore.edit { prefs ->
        prefs[GestureSettingsKeys.LEFT_SEGMENT_COUNT] = count.coerceIn(1, 3)
    }
}

// 右侧边缘尺寸设置
suspend fun Context.saveRightEdgeWidth(width: Int) = withContext(Dispatchers.IO) {
    gestureDataStore.edit { prefs ->
        prefs[GestureSettingsKeys.RIGHT_EDGE_WIDTH] = width.coerceIn(10, 50)
    }
}

suspend fun Context.saveRightEdgeHeightPercent(percent: Int) = withContext(Dispatchers.IO) {
    gestureDataStore.edit { prefs ->
        prefs[GestureSettingsKeys.RIGHT_EDGE_HEIGHT_PERCENT] = percent.coerceIn(20, 100)
    }
}

suspend fun Context.saveRightEdgePositionPercent(percent: Int) = withContext(Dispatchers.IO) {
    gestureDataStore.edit { prefs ->
        prefs[GestureSettingsKeys.RIGHT_EDGE_POSITION_PERCENT] = percent.coerceIn(0, 100)
    }
}

suspend fun Context.saveRightSegmentCount(count: Int) = withContext(Dispatchers.IO) {
    gestureDataStore.edit { prefs ->
        prefs[GestureSettingsKeys.RIGHT_SEGMENT_COUNT] = count.coerceIn(1, 3)
    }
}

// 底部边缘尺寸设置
suspend fun Context.saveBottomEdgeHeight(height: Int) = withContext(Dispatchers.IO) {
    gestureDataStore.edit { prefs ->
        prefs[GestureSettingsKeys.BOTTOM_EDGE_HEIGHT] = height.coerceIn(10, 50)
    }
}

suspend fun Context.saveBottomEdgeWidthPercent(percent: Int) = withContext(Dispatchers.IO) {
    gestureDataStore.edit { prefs ->
        prefs[GestureSettingsKeys.BOTTOM_EDGE_WIDTH_PERCENT] = percent.coerceIn(20, 100)
    }
}

suspend fun Context.saveBottomSegmentCount(count: Int) = withContext(Dispatchers.IO) {
    gestureDataStore.edit { prefs ->
        prefs[GestureSettingsKeys.BOTTOM_SEGMENT_COUNT] = count.coerceIn(1, 3)
    }
}

// 手势动作设置
suspend fun Context.saveLeftEdgeGesture(key: Preferences.Key<String>, action: GestureAction) = withContext(Dispatchers.IO) {
    gestureDataStore.edit { prefs ->
        prefs[key] = action.value
    }
}

suspend fun Context.saveRightEdgeGesture(key: Preferences.Key<String>, action: GestureAction) = withContext(Dispatchers.IO) {
    gestureDataStore.edit { prefs ->
        prefs[key] = action.value
    }
}

suspend fun Context.saveBottomEdgeGesture(key: Preferences.Key<String>, action: GestureAction) = withContext(Dispatchers.IO) {
    gestureDataStore.edit { prefs ->
        prefs[key] = action.value
    }
}

fun Context.appSwitchBlacklistFlow(): Flow<Set<String>> = gestureDataStore.data.map { prefs ->
    prefs[GestureSettingsKeys.APP_SWITCH_BLACKLIST] ?: emptySet()
}

suspend fun Context.saveAppSwitchBlacklist(blacklist: Set<String>) = withContext(Dispatchers.IO) {
    gestureDataStore.edit { prefs ->
        prefs[GestureSettingsKeys.APP_SWITCH_BLACKLIST] = blacklist
    }
}

suspend fun Context.addToAppSwitchBlacklist(packageNames: Set<String>) = withContext(Dispatchers.IO) {
    gestureDataStore.edit { prefs ->
        val current = prefs[GestureSettingsKeys.APP_SWITCH_BLACKLIST] ?: emptySet()
        prefs[GestureSettingsKeys.APP_SWITCH_BLACKLIST] = current + packageNames
    }
}

suspend fun Context.removeFromAppSwitchBlacklist(packageNames: Set<String>) = withContext(Dispatchers.IO) {
    gestureDataStore.edit { prefs ->
        val current = prefs[GestureSettingsKeys.APP_SWITCH_BLACKLIST] ?: emptySet()
        prefs[GestureSettingsKeys.APP_SWITCH_BLACKLIST] = current - packageNames
    }
}

// 初始化黑名单，将所有系统应用（包括无入口的）和本应用加入黑名单
// 有 QUERY_ALL_PACKAGES 权限时通过 getSystemAppPackages() 获取全部系统应用；
// 无权限或调用失败时使用已扫描的可启动系统应用兜底。
suspend fun Context.initBlacklistIfNeeded(launcherApps: Set<String>? = null) = withContext(Dispatchers.IO) {
    gestureDataStore.edit { prefs ->
        if (prefs[GestureSettingsKeys.BLACKLIST_INITIALIZED] != true) {
            // 先尝试获取所有系统应用，失败再使用兜底集合
            val allSystemApps = runCatching { getSystemAppPackages() }.getOrNull()
                ?: launcherApps
                ?: emptySet()
            val apps = allSystemApps.toMutableSet()
            // 将本应用加入黑名单
            apps.add(packageName)
            prefs[GestureSettingsKeys.APP_SWITCH_BLACKLIST] = apps
            prefs[GestureSettingsKeys.BLACKLIST_INITIALIZED] = true
        }
    }
}

// 重置黑名单初始化标志，用于用户重新授予 QUERY_ALL_PACKAGES 后重新初始化
suspend fun Context.resetBlacklistInitialized() = withContext(Dispatchers.IO) {
    gestureDataStore.edit { prefs ->
        prefs.remove(GestureSettingsKeys.BLACKLIST_INITIALIZED)
    }
}

suspend fun Context.saveVibrationEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
    gestureDataStore.edit { prefs ->
        prefs[GestureSettingsKeys.VIBRATION_ENABLED] = enabled
    }
}

// 背面双击设置
suspend fun Context.saveBackTapEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
    gestureDataStore.edit { prefs ->
        prefs[GestureSettingsKeys.BACK_TAP_ENABLED] = enabled
    }
}

suspend fun Context.saveBackTapSensitivity(sensitivity: Int) = withContext(Dispatchers.IO) {
    gestureDataStore.edit { prefs ->
        prefs[GestureSettingsKeys.BACK_TAP_SENSITIVITY] = sensitivity.coerceIn(1, 10)
    }
}

suspend fun Context.saveBackTapRange(range: Int) = withContext(Dispatchers.IO) {
    gestureDataStore.edit { prefs ->
        prefs[GestureSettingsKeys.BACK_TAP_RANGE] = range.coerceIn(1, 10)
    }
}

suspend fun Context.saveBackTapAction(action: GestureAction) = withContext(Dispatchers.IO) {
    gestureDataStore.edit { prefs ->
        prefs[GestureSettingsKeys.BACK_TAP_ACTION] = action.value
    }
}

suspend fun Context.saveBackTapMode(mode: BackTapMode) = withContext(Dispatchers.IO) {
    gestureDataStore.edit { prefs ->
        prefs[GestureSettingsKeys.BACK_TAP_MODE] = mode.value
    }
}

suspend fun Context.saveBackTapPauseOnCharging(pause: Boolean) = withContext(Dispatchers.IO) {
    gestureDataStore.edit { prefs ->
        prefs[GestureSettingsKeys.BACK_TAP_PAUSE_ON_CHARGING] = pause
    }
}

private fun Context.getSystemAppPackages(): Set<String> {
    val pm = packageManager
    val systemApps = mutableSetOf<String>()
    val installedApps = pm.getInstalledApplications(0)
    for (app in installedApps) {
        if (app.flags and ApplicationInfo.FLAG_SYSTEM != 0 ||
            app.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0) {
            systemApps.add(app.packageName)
        }
    }
    return systemApps
}
