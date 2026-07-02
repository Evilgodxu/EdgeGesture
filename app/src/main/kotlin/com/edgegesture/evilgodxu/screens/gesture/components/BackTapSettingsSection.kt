package com.edgegesture.evilgodxu.screens.gesture.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.edgegesture.evilgodxu.R
import com.edgegesture.evilgodxu.data.gesture.BackTapMode
import com.edgegesture.evilgodxu.data.gesture.GestureAction

// 背面双击设置区域组件
@Composable
fun BackTapSettingsSection(
    enabled: Boolean,
    sensitivity: Int,
    range: Int,
    action: GestureAction,
    mode: BackTapMode,
    pauseOnCharging: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onSensitivityChange: (Int) -> Unit,
    onRangeChange: (Int) -> Unit,
    onActionClick: () -> Unit,
    onModeChange: (BackTapMode) -> Unit,
    onPauseOnChargingChange: (Boolean) -> Unit,
    getActionDisplayName: @Composable (GestureAction) -> String
) {
    var expanded by remember { mutableStateOf(false) }
    var modeMenuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 标题行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.back_tap_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    // 开关
                    GestureSettingsSwitchItem(
                        title = stringResource(R.string.back_tap_enable_title),
                        subtitle = stringResource(R.string.back_tap_enable_desc),
                        checked = enabled,
                        onCheckedChange = onEnabledChange
                    )

                    if (enabled) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        // 灵敏度滑块
                        SettingSliderItem(
                            label = stringResource(R.string.back_tap_sensitivity),
                            value = sensitivity,
                            range = 1f..10f,
                            steps = 8,
                            valueFormat = { "${it.toInt()}" },
                            onValueChange = { onSensitivityChange(it.toInt()) }
                        )

                        // 检测范围滑块
                        SettingSliderItem(
                            label = stringResource(R.string.back_tap_range),
                            value = range,
                            range = 1f..10f,
                            steps = 8,
                            valueFormat = { "${it.toInt()}" },
                            onValueChange = { onRangeChange(it.toInt()) }
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        // 操作选择
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onActionClick() }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.back_tap_action),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = getActionDisplayName(action),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // 工作模式选择
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { modeMenuExpanded = true }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.back_tap_mode),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Box {
                                Text(
                                    text = stringResource(modeLabelRes(mode)),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                DropdownMenu(
                                    expanded = modeMenuExpanded,
                                    onDismissRequest = { modeMenuExpanded = false }
                                ) {
                                    BackTapMode.entries.forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(stringResource(modeLabelRes(option))) },
                                            onClick = {
                                                onModeChange(option)
                                                modeMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        // 充电时暂停
                        GestureSettingsSwitchItem(
                            title = stringResource(R.string.back_tap_pause_on_charging_title),
                            subtitle = stringResource(R.string.back_tap_pause_on_charging_desc),
                            checked = pauseOnCharging,
                            onCheckedChange = onPauseOnChargingChange
                        )

                    }
                }
            }
        }
    }
}

private fun modeLabelRes(mode: BackTapMode) = when (mode) {
    BackTapMode.ALWAYS -> R.string.back_tap_mode_always
    BackTapMode.SCREEN_OFF -> R.string.back_tap_mode_screen_off
    BackTapMode.SCREEN_ON -> R.string.back_tap_mode_screen_on
}
