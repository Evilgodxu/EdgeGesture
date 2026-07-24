package com.edgegesture.evilgodxu.screens.gesture.reuse

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.edgegesture.evilgodxu.R
import com.edgegesture.evilgodxu.data.gesture.GestureSettingsState
import com.edgegesture.evilgodxu.ui.action.getActionDisplayName

// 构建背面双击摘要描述文字
@Composable
fun buildBackTapDescription(settings: GestureSettingsState): String {
    if (!settings.backTapEnabled) {
        return stringResource(R.string.back_tap_summary_no_action, settings.backTapSensitivity, settings.backTapRange)
    }
    val actionName = getActionDisplayName(settings.backTapAction)
    return stringResource(R.string.back_tap_summary_with_action, settings.backTapSensitivity, settings.backTapRange, actionName)
}
