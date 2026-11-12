package com.edgegesture.evilgodxu.data.gesture

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
    FREEFORM_MODE("freeform_mode"),
    EXPAND_PANEL("expand_panel"),
    // 扫一扫
    ALIPAY_SCAN("alipay_scan"),
    WECHAT_SCAN("wechat_scan"),
    // 定时提醒，具体分钟数单独存储
    REMIND("remind"),
    // 启动指定应用，目标包名单独存储
    LAUNCH_APP("launch_app");

    companion object {
        fun fromValue(value: String): GestureAction = entries.find { it.value == value } ?: NONE
    }
}
