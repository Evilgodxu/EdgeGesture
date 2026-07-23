package com.edgegesture.evilgodxu.screens.backtap.compact.config_area

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edgegesture.evilgodxu.R
import com.edgegesture.evilgodxu.data.gesture.BackTapMode
import com.edgegesture.evilgodxu.data.gesture.GestureSettingsState
import com.edgegesture.evilgodxu.screens.backtap.compact.config_area.tap_mode_selector.TapModeSelector
import com.edgegesture.evilgodxu.screens.backtap.reuse.SettingsSliderItem
import com.edgegesture.evilgodxu.screens.backtap.reuse.SettingsToggleRow

// 背面双击配置 Area — 仅负责组件排列
@Composable
fun ConfigArea(
    settings: GestureSettingsState?,
    onSaveBackTapEnabled: (Boolean) -> Unit,
    onSaveBackTapSensitivity: (Int) -> Unit,
    onSaveBackTapRange: (Int) -> Unit,
    onSaveBackTapMode: (BackTapMode) -> Unit,
    onSaveBackTapPauseOnCharging: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.back_tap_config_title),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                SettingsToggleRow(
                    title = stringResource(R.string.back_tap_enable_title),
                    description = stringResource(R.string.back_tap_enable_desc),
                    checked = settings?.backTapEnabled ?: false,
                    onCheckedChange = onSaveBackTapEnabled,
                )

                if (settings?.backTapEnabled == true) {
                    Spacer(modifier = Modifier.height(16.dp))

                    SettingsSliderItem(
                        label = stringResource(R.string.back_tap_sensitivity),
                        value = settings.backTapSensitivity, onValueChange = onSaveBackTapSensitivity,
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    SettingsSliderItem(
                        label = stringResource(R.string.back_tap_range),
                        value = settings.backTapRange, onValueChange = onSaveBackTapRange,
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = stringResource(R.string.back_tap_mode),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    TapModeSelector(
                        currentMode = settings.backTapMode,
                        onModeSelected = onSaveBackTapMode,
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    SettingsToggleRow(
                        title = stringResource(R.string.back_tap_pause_on_charging_title),
                        description = stringResource(R.string.back_tap_pause_on_charging_desc),
                        checked = settings.backTapPauseOnCharging,
                        onCheckedChange = onSaveBackTapPauseOnCharging,
                    )
                }
            }
        }
    }
}
