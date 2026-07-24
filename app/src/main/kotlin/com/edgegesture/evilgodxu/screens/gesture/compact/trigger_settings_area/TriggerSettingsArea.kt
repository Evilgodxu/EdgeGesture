package com.edgegesture.evilgodxu.screens.gesture.compact.trigger_settings_area

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
import com.edgegesture.evilgodxu.data.gesture.GestureSettingsState

// 触发区域设置 Area — 开关项列表
@Composable
fun TriggerSettingsArea(
    settings: GestureSettingsState,
    onHideOverlayChange: (Boolean) -> Unit,
    onHideFromRecentsChange: (Boolean) -> Unit,
    onAvoidKeyboardOverlapChange: (Boolean) -> Unit,
    onVibrationChange: (Boolean) -> Unit,
    onDoubleSwipeChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.trigger_area_settings_title),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, top = 20.dp, bottom = 10.dp)
        )

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Card(
                modifier = Modifier.fillMaxWidth(0.95f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    GestureSettingsSwitchItem(title = stringResource(R.string.gesture_hide_overlay_title), subtitle = stringResource(R.string.gesture_hide_overlay_desc), checked = settings.hideOverlay, onCheckedChange = onHideOverlayChange)
                    GestureSettingsSwitchItem(title = stringResource(R.string.gesture_hide_recents_title), subtitle = stringResource(R.string.gesture_hide_recents_desc), checked = settings.hideFromRecents, onCheckedChange = onHideFromRecentsChange)
                    GestureSettingsSwitchItem(title = stringResource(R.string.gesture_avoid_keyboard_overlap_title), subtitle = stringResource(R.string.gesture_avoid_keyboard_overlap_desc), checked = settings.avoidKeyboardOverlap, onCheckedChange = onAvoidKeyboardOverlapChange)
                    GestureSettingsSwitchItem(title = stringResource(R.string.settings_vibration_title), subtitle = stringResource(R.string.settings_vibration_desc), checked = settings.vibrationEnabled, onCheckedChange = onVibrationChange)
                    GestureSettingsSwitchItem(title = stringResource(R.string.gesture_double_swipe_title), subtitle = stringResource(R.string.gesture_double_swipe_desc), checked = settings.doubleSwipeEnabled, onCheckedChange = onDoubleSwipeChange)
                }
            }
        }
    }
}
