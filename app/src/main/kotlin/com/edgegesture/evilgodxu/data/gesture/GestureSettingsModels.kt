package com.edgegesture.evilgodxu.data.gesture

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

// 边缘分段的点击类手势槽位：单击、双击、长按（不滑动），供手势解析与配置页共用
interface EdgeTapGestureConfig {
    val tap: GestureAction
    val doubleTap: GestureAction
    val longPress: GestureAction
}

data class LeftEdgeConfig(
    val swipeRight: GestureAction = GestureAction.BACK,
    val swipeRightLong: GestureAction = GestureAction.LAST_APP,
    val swipeUp: GestureAction = GestureAction.PREVIOUS_TRACK,
    val swipeUpLong: GestureAction = GestureAction.POWER_MENU,
    val swipeDown: GestureAction = GestureAction.SCREENSHOT,
    val swipeDownLong: GestureAction = GestureAction.LOCK_SCREEN,
    override val tap: GestureAction = GestureAction.NONE,
    override val doubleTap: GestureAction = GestureAction.NONE,
    override val longPress: GestureAction = GestureAction.NONE
) : EdgeTapGestureConfig

data class RightEdgeConfig(
    val swipeLeft: GestureAction = GestureAction.BACK,
    val swipeLeftLong: GestureAction = GestureAction.LAST_APP,
    val swipeUp: GestureAction = GestureAction.NEXT_TRACK,
    val swipeUpLong: GestureAction = GestureAction.FLASHLIGHT,
    val swipeDown: GestureAction = GestureAction.VOICE_ASSISTANT,
    val swipeDownLong: GestureAction = GestureAction.EXPAND_PANEL,
    override val tap: GestureAction = GestureAction.NONE,
    override val doubleTap: GestureAction = GestureAction.NONE,
    override val longPress: GestureAction = GestureAction.NONE
) : EdgeTapGestureConfig

data class BottomEdgeConfig(
    val swipeUp: GestureAction = GestureAction.HOME,
    val swipeUpLong: GestureAction = GestureAction.RECENT,
    val swipeLeft: GestureAction = GestureAction.LAST_APP,
    val swipeLeftLong: GestureAction = GestureAction.NONE,
    val swipeRight: GestureAction = GestureAction.LAST_APP,
    val swipeRightLong: GestureAction = GestureAction.NONE,
    override val tap: GestureAction = GestureAction.NONE,
    override val doubleTap: GestureAction = GestureAction.NONE,
    override val longPress: GestureAction = GestureAction.NONE
) : EdgeTapGestureConfig

data class GestureSettingsState(
    val gestureEnabled: Boolean = false,
    val hideOverlay: Boolean = false,
    val hideFromRecents: Boolean = false,
    val avoidKeyboardOverlap: Boolean = false,
    val vibrationEnabled: Boolean = false,
    val doubleSwipeEnabled: Boolean = false,
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
    ),
    // 启动应用动作绑定的目标包名，key 为动作存储键名
    val launchAppTargets: Map<String, String> = emptyMap(),
    // 启动应用动作是否以小窗模式启动，key 为动作存储键名，仅记录开启项
    val launchAppFreeform: Map<String, Boolean> = emptyMap(),
    // 定时提醒动作的分钟数，key 为动作存储键名，缺省时使用 [GestureSettingsKeys.DEFAULT_REMIND_MINUTES]
    val remindMinutes: Map<String, Int> = emptyMap()
)
