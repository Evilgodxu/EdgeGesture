package com.edgegesture.evilgodxu.ui.action

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.edgegesture.evilgodxu.R
import com.edgegesture.evilgodxu.data.gesture.GestureAction

// 获取手势动作的本地化显示名称
@Composable
fun getActionDisplayName(action: GestureAction): String {
    return when (action) {
        GestureAction.NONE -> stringResource(R.string.gesture_action_none)
        GestureAction.HOME -> stringResource(R.string.gesture_action_home)
        GestureAction.RECENT -> stringResource(R.string.gesture_action_recent)
        GestureAction.BACK -> stringResource(R.string.gesture_action_back)
        GestureAction.LAST_APP -> stringResource(R.string.gesture_action_last_app)
        GestureAction.PREVIOUS_TRACK -> stringResource(R.string.gesture_action_previous_track)
        GestureAction.NEXT_TRACK -> stringResource(R.string.gesture_action_next_track)
        GestureAction.FLASHLIGHT -> stringResource(R.string.gesture_action_flashlight)
        GestureAction.VOICE_ASSISTANT -> stringResource(R.string.gesture_action_voice_assistant)
        GestureAction.POWER_MENU -> stringResource(R.string.gesture_action_power_menu)
        GestureAction.LOCK_SCREEN -> stringResource(R.string.gesture_action_lock_screen)
        GestureAction.SCREENSHOT -> stringResource(R.string.gesture_action_screenshot)
        GestureAction.EXPAND_PANEL -> stringResource(R.string.gesture_action_expand_panel)
        GestureAction.ALIPAY_SCAN -> stringResource(R.string.gesture_action_alipay_scan)
        GestureAction.WECHAT_SCAN -> stringResource(R.string.gesture_action_wechat_scan)
        GestureAction.REMIND_1M -> stringResource(R.string.gesture_action_remind_1m)
        GestureAction.REMIND_3M -> stringResource(R.string.gesture_action_remind_3m)
        GestureAction.REMIND_5M -> stringResource(R.string.gesture_action_remind_5m)
        GestureAction.REMIND_10M -> stringResource(R.string.gesture_action_remind_10m)
        GestureAction.REMIND_15M -> stringResource(R.string.gesture_action_remind_15m)
    }
}
