package com.byss.jh.data.gesture

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

    val APP_SWITCH_BLACKLIST = stringSetPreferencesKey("app_switch_blacklist")
    val BLACKLIST_INITIALIZED = booleanPreferencesKey("blacklist_initialized")
    val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
}

enum class GestureAction(val value: String, val displayName: String) {
    NONE("none", "无"),
    HOME("home", "主页键"),
    RECENT("recent", "任务键"),
    BACK("back", "返回键"),
    LAST_APP("last_app", "上一个应用"),
    PREVIOUS_TRACK("previous_track", "上一曲"),
    NEXT_TRACK("next_track", "下一曲"),
    FLASHLIGHT("flashlight", "手电筒"),
    VOICE_ASSISTANT("voice_assistant", "语音助手"),
    POWER_MENU("power_menu", "电源菜单"),
    LOCK_SCREEN("lock_screen", "锁屏"),
    SCREENSHOT("screenshot", "截屏"),
    EXPAND_PANEL("expand_panel", "扩展面板");

    companion object {
        fun fromValue(value: String): GestureAction = entries.find { it.value == value } ?: NONE
    }
}

enum class EdgePosition {
    LEFT, RIGHT, BOTTOM
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
    val swipeDownLong: GestureAction = GestureAction.LOCK_SCREEN
)

data class BottomEdgeConfig(
    val swipeUp: GestureAction = GestureAction.HOME,
    val swipeUpLong: GestureAction = GestureAction.RECENT,
    val swipeLeft: GestureAction = GestureAction.NONE,
    val swipeLeftLong: GestureAction = GestureAction.NONE,
    val swipeRight: GestureAction = GestureAction.NONE,
    val swipeRightLong: GestureAction = GestureAction.NONE
)

data class GestureSettingsState(
    val gestureEnabled: Boolean = false,
    val hideOverlay: Boolean = false,
    val hideFromRecents: Boolean = false,
    val vibrationEnabled: Boolean = false,
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

fun Context.gestureSettingsFlow(): Flow<GestureSettingsState> = gestureDataStore.data.map { prefs ->
    GestureSettingsState(
        gestureEnabled = prefs[GestureSettingsKeys.GESTURE_ENABLED] ?: false,
        hideOverlay = prefs[GestureSettingsKeys.HIDE_OVERLAY] ?: false,
        hideFromRecents = prefs[GestureSettingsKeys.HIDE_FROM_RECENTS] ?: false,
        vibrationEnabled = prefs[GestureSettingsKeys.VIBRATION_ENABLED] ?: false,
        // 左侧边缘尺寸
        leftEdgeWidth = prefs[GestureSettingsKeys.LEFT_EDGE_WIDTH] ?: 20,
        leftEdgeHeightPercent = prefs[GestureSettingsKeys.LEFT_EDGE_HEIGHT_PERCENT] ?: 60,
        leftEdgePositionPercent = prefs[GestureSettingsKeys.LEFT_EDGE_POSITION_PERCENT] ?: 90,
        leftSegmentCount = prefs[GestureSettingsKeys.LEFT_SEGMENT_COUNT] ?: 1,
        // 右侧边缘尺寸
        rightEdgeWidth = prefs[GestureSettingsKeys.RIGHT_EDGE_WIDTH] ?: 20,
        rightEdgeHeightPercent = prefs[GestureSettingsKeys.RIGHT_EDGE_HEIGHT_PERCENT] ?: 60,
        rightEdgePositionPercent = prefs[GestureSettingsKeys.RIGHT_EDGE_POSITION_PERCENT] ?: 90,
        rightSegmentCount = prefs[GestureSettingsKeys.RIGHT_SEGMENT_COUNT] ?: 1,
        // 底部边缘尺寸
        bottomEdgeHeight = prefs[GestureSettingsKeys.BOTTOM_EDGE_HEIGHT] ?: 20,
        bottomEdgeWidthPercent = prefs[GestureSettingsKeys.BOTTOM_EDGE_WIDTH_PERCENT] ?: 80,
        bottomSegmentCount = prefs[GestureSettingsKeys.BOTTOM_SEGMENT_COUNT] ?: 1,
        // 手势配置（第1段）
        leftEdge = LeftEdgeConfig(
            swipeRight = GestureAction.fromValue(prefs[GestureSettingsKeys.LEFT_SWIPE_RIGHT] ?: GestureAction.BACK.value),
            swipeRightLong = GestureAction.fromValue(prefs[GestureSettingsKeys.LEFT_SWIPE_RIGHT_LONG] ?: GestureAction.LAST_APP.value),
            swipeUp = GestureAction.fromValue(prefs[GestureSettingsKeys.LEFT_SWIPE_UP] ?: GestureAction.PREVIOUS_TRACK.value),
            swipeUpLong = GestureAction.fromValue(prefs[GestureSettingsKeys.LEFT_SWIPE_UP_LONG] ?: GestureAction.POWER_MENU.value),
            swipeDown = GestureAction.fromValue(prefs[GestureSettingsKeys.LEFT_SWIPE_DOWN] ?: GestureAction.SCREENSHOT.value),
            swipeDownLong = GestureAction.fromValue(prefs[GestureSettingsKeys.LEFT_SWIPE_DOWN_LONG] ?: GestureAction.LOCK_SCREEN.value)
        ),
        rightEdge = RightEdgeConfig(
            swipeLeft = GestureAction.fromValue(prefs[GestureSettingsKeys.RIGHT_SWIPE_LEFT] ?: GestureAction.BACK.value),
            swipeLeftLong = GestureAction.fromValue(prefs[GestureSettingsKeys.RIGHT_SWIPE_LEFT_LONG] ?: GestureAction.LAST_APP.value),
            swipeUp = GestureAction.fromValue(prefs[GestureSettingsKeys.RIGHT_SWIPE_UP] ?: GestureAction.NEXT_TRACK.value),
            swipeUpLong = GestureAction.fromValue(prefs[GestureSettingsKeys.RIGHT_SWIPE_UP_LONG] ?: GestureAction.FLASHLIGHT.value),
            swipeDown = GestureAction.fromValue(prefs[GestureSettingsKeys.RIGHT_SWIPE_DOWN] ?: GestureAction.VOICE_ASSISTANT.value),
            swipeDownLong = GestureAction.fromValue(prefs[GestureSettingsKeys.RIGHT_SWIPE_DOWN_LONG] ?: GestureAction.LOCK_SCREEN.value)
        ),
        bottomEdge = BottomEdgeConfig(
            swipeUp = GestureAction.fromValue(prefs[GestureSettingsKeys.BOTTOM_SWIPE_UP] ?: GestureAction.HOME.value),
            swipeUpLong = GestureAction.fromValue(prefs[GestureSettingsKeys.BOTTOM_SWIPE_UP_LONG] ?: GestureAction.RECENT.value),
            swipeLeft = GestureAction.fromValue(prefs[GestureSettingsKeys.BOTTOM_SWIPE_LEFT] ?: GestureAction.NONE.value),
            swipeLeftLong = GestureAction.fromValue(prefs[GestureSettingsKeys.BOTTOM_SWIPE_LEFT_LONG] ?: GestureAction.NONE.value),
            swipeRight = GestureAction.fromValue(prefs[GestureSettingsKeys.BOTTOM_SWIPE_RIGHT] ?: GestureAction.NONE.value),
            swipeRightLong = GestureAction.fromValue(prefs[GestureSettingsKeys.BOTTOM_SWIPE_RIGHT_LONG] ?: GestureAction.NONE.value)
        ),
        // 手势配置（第2段）
        leftEdgeSegment2 = LeftEdgeConfig(
            swipeRight = GestureAction.fromValue(prefs[GestureSettingsKeys.LEFT_2_SWIPE_RIGHT] ?: GestureAction.NONE.value),
            swipeRightLong = GestureAction.fromValue(prefs[GestureSettingsKeys.LEFT_2_SWIPE_RIGHT_LONG] ?: GestureAction.NONE.value),
            swipeUp = GestureAction.fromValue(prefs[GestureSettingsKeys.LEFT_2_SWIPE_UP] ?: GestureAction.NONE.value),
            swipeUpLong = GestureAction.fromValue(prefs[GestureSettingsKeys.LEFT_2_SWIPE_UP_LONG] ?: GestureAction.NONE.value),
            swipeDown = GestureAction.fromValue(prefs[GestureSettingsKeys.LEFT_2_SWIPE_DOWN] ?: GestureAction.NONE.value),
            swipeDownLong = GestureAction.fromValue(prefs[GestureSettingsKeys.LEFT_2_SWIPE_DOWN_LONG] ?: GestureAction.NONE.value)
        ),
        rightEdgeSegment2 = RightEdgeConfig(
            swipeLeft = GestureAction.fromValue(prefs[GestureSettingsKeys.RIGHT_2_SWIPE_LEFT] ?: GestureAction.NONE.value),
            swipeLeftLong = GestureAction.fromValue(prefs[GestureSettingsKeys.RIGHT_2_SWIPE_LEFT_LONG] ?: GestureAction.NONE.value),
            swipeUp = GestureAction.fromValue(prefs[GestureSettingsKeys.RIGHT_2_SWIPE_UP] ?: GestureAction.NONE.value),
            swipeUpLong = GestureAction.fromValue(prefs[GestureSettingsKeys.RIGHT_2_SWIPE_UP_LONG] ?: GestureAction.NONE.value),
            swipeDown = GestureAction.fromValue(prefs[GestureSettingsKeys.RIGHT_2_SWIPE_DOWN] ?: GestureAction.NONE.value),
            swipeDownLong = GestureAction.fromValue(prefs[GestureSettingsKeys.RIGHT_2_SWIPE_DOWN_LONG] ?: GestureAction.NONE.value)
        ),
        bottomEdgeSegment2 = BottomEdgeConfig(
            swipeUp = GestureAction.fromValue(prefs[GestureSettingsKeys.BOTTOM_2_SWIPE_UP] ?: GestureAction.NONE.value),
            swipeUpLong = GestureAction.fromValue(prefs[GestureSettingsKeys.BOTTOM_2_SWIPE_UP_LONG] ?: GestureAction.NONE.value),
            swipeLeft = GestureAction.fromValue(prefs[GestureSettingsKeys.BOTTOM_2_SWIPE_LEFT] ?: GestureAction.NONE.value),
            swipeLeftLong = GestureAction.fromValue(prefs[GestureSettingsKeys.BOTTOM_2_SWIPE_LEFT_LONG] ?: GestureAction.NONE.value),
            swipeRight = GestureAction.fromValue(prefs[GestureSettingsKeys.BOTTOM_2_SWIPE_RIGHT] ?: GestureAction.NONE.value),
            swipeRightLong = GestureAction.fromValue(prefs[GestureSettingsKeys.BOTTOM_2_SWIPE_RIGHT_LONG] ?: GestureAction.NONE.value)
        ),
        // 手势配置（第3段）
        leftEdgeSegment3 = LeftEdgeConfig(
            swipeRight = GestureAction.fromValue(prefs[GestureSettingsKeys.LEFT_3_SWIPE_RIGHT] ?: GestureAction.NONE.value),
            swipeRightLong = GestureAction.fromValue(prefs[GestureSettingsKeys.LEFT_3_SWIPE_RIGHT_LONG] ?: GestureAction.NONE.value),
            swipeUp = GestureAction.fromValue(prefs[GestureSettingsKeys.LEFT_3_SWIPE_UP] ?: GestureAction.NONE.value),
            swipeUpLong = GestureAction.fromValue(prefs[GestureSettingsKeys.LEFT_3_SWIPE_UP_LONG] ?: GestureAction.NONE.value),
            swipeDown = GestureAction.fromValue(prefs[GestureSettingsKeys.LEFT_3_SWIPE_DOWN] ?: GestureAction.NONE.value),
            swipeDownLong = GestureAction.fromValue(prefs[GestureSettingsKeys.LEFT_3_SWIPE_DOWN_LONG] ?: GestureAction.NONE.value)
        ),
        rightEdgeSegment3 = RightEdgeConfig(
            swipeLeft = GestureAction.fromValue(prefs[GestureSettingsKeys.RIGHT_3_SWIPE_LEFT] ?: GestureAction.NONE.value),
            swipeLeftLong = GestureAction.fromValue(prefs[GestureSettingsKeys.RIGHT_3_SWIPE_LEFT_LONG] ?: GestureAction.NONE.value),
            swipeUp = GestureAction.fromValue(prefs[GestureSettingsKeys.RIGHT_3_SWIPE_UP] ?: GestureAction.NONE.value),
            swipeUpLong = GestureAction.fromValue(prefs[GestureSettingsKeys.RIGHT_3_SWIPE_UP_LONG] ?: GestureAction.NONE.value),
            swipeDown = GestureAction.fromValue(prefs[GestureSettingsKeys.RIGHT_3_SWIPE_DOWN] ?: GestureAction.NONE.value),
            swipeDownLong = GestureAction.fromValue(prefs[GestureSettingsKeys.RIGHT_3_SWIPE_DOWN_LONG] ?: GestureAction.NONE.value)
        ),
        bottomEdgeSegment3 = BottomEdgeConfig(
            swipeUp = GestureAction.fromValue(prefs[GestureSettingsKeys.BOTTOM_3_SWIPE_UP] ?: GestureAction.NONE.value),
            swipeUpLong = GestureAction.fromValue(prefs[GestureSettingsKeys.BOTTOM_3_SWIPE_UP_LONG] ?: GestureAction.NONE.value),
            swipeLeft = GestureAction.fromValue(prefs[GestureSettingsKeys.BOTTOM_3_SWIPE_LEFT] ?: GestureAction.NONE.value),
            swipeLeftLong = GestureAction.fromValue(prefs[GestureSettingsKeys.BOTTOM_3_SWIPE_LEFT_LONG] ?: GestureAction.NONE.value),
            swipeRight = GestureAction.fromValue(prefs[GestureSettingsKeys.BOTTOM_3_SWIPE_RIGHT] ?: GestureAction.NONE.value),
            swipeRightLong = GestureAction.fromValue(prefs[GestureSettingsKeys.BOTTOM_3_SWIPE_RIGHT_LONG] ?: GestureAction.NONE.value)
        )
    )
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

/**
 * 底部边缘尺寸设置
 */
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

/**
 * 手势动作设置
 */
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

suspend fun Context.initBlacklistIfNeeded() = withContext(Dispatchers.IO) {
    gestureDataStore.edit { prefs ->
        if (prefs[GestureSettingsKeys.BLACKLIST_INITIALIZED] != true) {
            val systemApps = getSystemAppPackages()
            prefs[GestureSettingsKeys.APP_SWITCH_BLACKLIST] = systemApps
            prefs[GestureSettingsKeys.BLACKLIST_INITIALIZED] = true
        }
    }
}

suspend fun Context.saveVibrationEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
    gestureDataStore.edit { prefs ->
        prefs[GestureSettingsKeys.VIBRATION_ENABLED] = enabled
    }
}

private fun Context.getSystemAppPackages(): Set<String> {
    val pm = packageManager
    val systemApps = mutableSetOf<String>()
    val installedApps = pm.getInstalledApplications(0)
    for (app in installedApps) {
        if (app.flags and ApplicationInfo.FLAG_SYSTEM != 0 ||
            app.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0) {
            if (app.packageName != this.packageName) {
                systemApps.add(app.packageName)
            }
        }
    }
    return systemApps
}
