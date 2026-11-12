package com.edgegesture.evilgodxu.screens.gesture.component.config

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edgegesture.evilgodxu.R
import com.edgegesture.evilgodxu.ui.component.SwitchRow

// 触发区设置区块：悬浮层显示、后台隐藏、键盘避让、震动与二次滑动开关
@Composable
fun TriggerAreaSection(
    hideOverlay: Boolean,
    hideFromRecents: Boolean,
    avoidKeyboardOverlap: Boolean,
    vibrationEnabled: Boolean,
    doubleSwipeEnabled: Boolean,
    onHideOverlayChange: (Boolean) -> Unit,
    onHideFromRecentsChange: (Boolean) -> Unit,
    onAvoidKeyboardOverlapChange: (Boolean) -> Unit,
    onVibrationEnabledChange: (Boolean) -> Unit,
    onDoubleSwipeEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.trigger_area_settings_title),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, top = 20.dp, bottom = 10.dp),
        )

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(0.95f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SwitchRow(
                        title = stringResource(R.string.gesture_hide_overlay_title),
                        subtitle = stringResource(R.string.gesture_hide_overlay_desc),
                        checked = hideOverlay,
                        onCheckedChange = onHideOverlayChange,
                    )
                    SwitchRow(
                        title = stringResource(R.string.gesture_hide_recents_title),
                        subtitle = stringResource(R.string.gesture_hide_recents_desc),
                        checked = hideFromRecents,
                        onCheckedChange = onHideFromRecentsChange,
                    )
                    SwitchRow(
                        title = stringResource(R.string.gesture_avoid_keyboard_overlap_title),
                        subtitle = stringResource(R.string.gesture_avoid_keyboard_overlap_desc),
                        checked = avoidKeyboardOverlap,
                        onCheckedChange = onAvoidKeyboardOverlapChange,
                    )
                    SwitchRow(
                        title = stringResource(R.string.settings_vibration_title),
                        subtitle = stringResource(R.string.settings_vibration_desc),
                        checked = vibrationEnabled,
                        onCheckedChange = onVibrationEnabledChange,
                    )
                    SwitchRow(
                        title = stringResource(R.string.gesture_double_swipe_title),
                        subtitle = stringResource(R.string.gesture_double_swipe_desc),
                        checked = doubleSwipeEnabled,
                        onCheckedChange = onDoubleSwipeEnabledChange,
                    )
                }
            }
        }
    }
}
