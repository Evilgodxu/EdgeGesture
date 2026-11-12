package com.edgegesture.evilgodxu.data.gesture

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

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

    // 点击类手势设置（第1段）：单击、双击、长按（不滑动）
    val LEFT_TAP = stringPreferencesKey("left_tap")
    val LEFT_DOUBLE_TAP = stringPreferencesKey("left_double_tap")
    val LEFT_LONG_PRESS = stringPreferencesKey("left_long_press")
    val RIGHT_TAP = stringPreferencesKey("right_tap")
    val RIGHT_DOUBLE_TAP = stringPreferencesKey("right_double_tap")
    val RIGHT_LONG_PRESS = stringPreferencesKey("right_long_press")
    val BOTTOM_TAP = stringPreferencesKey("bottom_tap")
    val BOTTOM_DOUBLE_TAP = stringPreferencesKey("bottom_double_tap")
    val BOTTOM_LONG_PRESS = stringPreferencesKey("bottom_long_press")

    // 点击类手势设置（第2段）
    val LEFT_2_TAP = stringPreferencesKey("left_2_tap")
    val LEFT_2_DOUBLE_TAP = stringPreferencesKey("left_2_double_tap")
    val LEFT_2_LONG_PRESS = stringPreferencesKey("left_2_long_press")
    val RIGHT_2_TAP = stringPreferencesKey("right_2_tap")
    val RIGHT_2_DOUBLE_TAP = stringPreferencesKey("right_2_double_tap")
    val RIGHT_2_LONG_PRESS = stringPreferencesKey("right_2_long_press")
    val BOTTOM_2_TAP = stringPreferencesKey("bottom_2_tap")
    val BOTTOM_2_DOUBLE_TAP = stringPreferencesKey("bottom_2_double_tap")
    val BOTTOM_2_LONG_PRESS = stringPreferencesKey("bottom_2_long_press")

    // 点击类手势设置（第3段）
    val LEFT_3_TAP = stringPreferencesKey("left_3_tap")
    val LEFT_3_DOUBLE_TAP = stringPreferencesKey("left_3_double_tap")
    val LEFT_3_LONG_PRESS = stringPreferencesKey("left_3_long_press")
    val RIGHT_3_TAP = stringPreferencesKey("right_3_tap")
    val RIGHT_3_DOUBLE_TAP = stringPreferencesKey("right_3_double_tap")
    val RIGHT_3_LONG_PRESS = stringPreferencesKey("right_3_long_press")
    val BOTTOM_3_TAP = stringPreferencesKey("bottom_3_tap")
    val BOTTOM_3_DOUBLE_TAP = stringPreferencesKey("bottom_3_double_tap")
    val BOTTOM_3_LONG_PRESS = stringPreferencesKey("bottom_3_long_press")

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
    val DOUBLE_SWIPE_ENABLED = booleanPreferencesKey("double_swipe_enabled")

    // 启动应用动作的目标包名存储键后缀，实际键名为「动作键名 + 后缀」
    const val LAUNCH_APP_TARGET_SUFFIX = "__launch_app_target"

    // 启动应用动作的小窗启动标记存储键后缀，实际键名为「动作键名 + 后缀」
    const val LAUNCH_APP_FREEFORM_SUFFIX = "__launch_app_freeform"

    // 定时提醒动作的分钟数存储键后缀，实际键名为「动作键名 + 后缀」
    const val REMIND_MINUTES_SUFFIX = "__remind_minutes"

    // 定时提醒的默认分钟数
    const val DEFAULT_REMIND_MINUTES = 5

    // 手势动作存储键的唯一来源：按「边缘 + 分段(0..2) + 槽位(0..8)」解析
    // 槽位顺序与配置页列表一致：左/右边缘为 主方向短滑/长按、次方向短滑/长按、第三方向短滑/长按，末尾为单击/双击/长按；底部边缘为上滑、左滑、右滑各短滑/长按，末尾同上
    private val EDGE_KEYS: Map<EdgePosition, List<List<Preferences.Key<String>>>> = mapOf(
        EdgePosition.LEFT to listOf(
            listOf(LEFT_SWIPE_RIGHT, LEFT_SWIPE_RIGHT_LONG, LEFT_SWIPE_UP, LEFT_SWIPE_UP_LONG, LEFT_SWIPE_DOWN, LEFT_SWIPE_DOWN_LONG, LEFT_TAP, LEFT_DOUBLE_TAP, LEFT_LONG_PRESS),
            listOf(LEFT_2_SWIPE_RIGHT, LEFT_2_SWIPE_RIGHT_LONG, LEFT_2_SWIPE_UP, LEFT_2_SWIPE_UP_LONG, LEFT_2_SWIPE_DOWN, LEFT_2_SWIPE_DOWN_LONG, LEFT_2_TAP, LEFT_2_DOUBLE_TAP, LEFT_2_LONG_PRESS),
            listOf(LEFT_3_SWIPE_RIGHT, LEFT_3_SWIPE_RIGHT_LONG, LEFT_3_SWIPE_UP, LEFT_3_SWIPE_UP_LONG, LEFT_3_SWIPE_DOWN, LEFT_3_SWIPE_DOWN_LONG, LEFT_3_TAP, LEFT_3_DOUBLE_TAP, LEFT_3_LONG_PRESS)
        ),
        EdgePosition.RIGHT to listOf(
            listOf(RIGHT_SWIPE_LEFT, RIGHT_SWIPE_LEFT_LONG, RIGHT_SWIPE_UP, RIGHT_SWIPE_UP_LONG, RIGHT_SWIPE_DOWN, RIGHT_SWIPE_DOWN_LONG, RIGHT_TAP, RIGHT_DOUBLE_TAP, RIGHT_LONG_PRESS),
            listOf(RIGHT_2_SWIPE_LEFT, RIGHT_2_SWIPE_LEFT_LONG, RIGHT_2_SWIPE_UP, RIGHT_2_SWIPE_UP_LONG, RIGHT_2_SWIPE_DOWN, RIGHT_2_SWIPE_DOWN_LONG, RIGHT_2_TAP, RIGHT_2_DOUBLE_TAP, RIGHT_2_LONG_PRESS),
            listOf(RIGHT_3_SWIPE_LEFT, RIGHT_3_SWIPE_LEFT_LONG, RIGHT_3_SWIPE_UP, RIGHT_3_SWIPE_UP_LONG, RIGHT_3_SWIPE_DOWN, RIGHT_3_SWIPE_DOWN_LONG, RIGHT_3_TAP, RIGHT_3_DOUBLE_TAP, RIGHT_3_LONG_PRESS)
        ),
        EdgePosition.BOTTOM to listOf(
            listOf(BOTTOM_SWIPE_UP, BOTTOM_SWIPE_UP_LONG, BOTTOM_SWIPE_LEFT, BOTTOM_SWIPE_LEFT_LONG, BOTTOM_SWIPE_RIGHT, BOTTOM_SWIPE_RIGHT_LONG, BOTTOM_TAP, BOTTOM_DOUBLE_TAP, BOTTOM_LONG_PRESS),
            listOf(BOTTOM_2_SWIPE_UP, BOTTOM_2_SWIPE_UP_LONG, BOTTOM_2_SWIPE_LEFT, BOTTOM_2_SWIPE_LEFT_LONG, BOTTOM_2_SWIPE_RIGHT, BOTTOM_2_SWIPE_RIGHT_LONG, BOTTOM_2_TAP, BOTTOM_2_DOUBLE_TAP, BOTTOM_2_LONG_PRESS),
            listOf(BOTTOM_3_SWIPE_UP, BOTTOM_3_SWIPE_UP_LONG, BOTTOM_3_SWIPE_LEFT, BOTTOM_3_SWIPE_LEFT_LONG, BOTTOM_3_SWIPE_RIGHT, BOTTOM_3_SWIPE_RIGHT_LONG, BOTTOM_3_TAP, BOTTOM_3_DOUBLE_TAP, BOTTOM_3_LONG_PRESS)
        )
    )

    fun keyFor(position: EdgePosition, segmentIndex: Int, slot: Int): Preferences.Key<String> =
        EDGE_KEYS.getValue(position)[segmentIndex.coerceIn(0, 2)][slot.coerceIn(0, 8)]
}
