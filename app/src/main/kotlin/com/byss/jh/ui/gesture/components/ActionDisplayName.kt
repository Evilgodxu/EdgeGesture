package com.byss.jh.ui.gesture.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.byss.jh.R
import com.byss.jh.data.gesture.GestureAction

// 获取动作显示名称
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
    }
}
