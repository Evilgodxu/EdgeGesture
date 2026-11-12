package com.edgegesture.evilgodxu.screens.backtap.component.config

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edgegesture.evilgodxu.R
import com.edgegesture.evilgodxu.data.gesture.BackTapMode
import com.edgegesture.evilgodxu.data.gesture.GestureSettingsState
import com.edgegesture.evilgodxu.ui.component.SectionCard
import com.edgegesture.evilgodxu.ui.component.SliderTrack
import com.edgegesture.evilgodxu.ui.theme.AppSwitch
import kotlin.math.roundToInt

// 背面双击配置区块：启用开关、灵敏度/检测范围滑块、工作模式与充电暂停
@Composable
fun BackTapConfig(
    settings: GestureSettingsState?,
    onEnabledChange: (Boolean) -> Unit,
    onSensitivityChange: (Int) -> Unit,
    onRangeChange: (Int) -> Unit,
    onModeChange: (BackTapMode) -> Unit,
    onPauseOnChargingChange: (Boolean) -> Unit,
) {
    // 滑块本地状态，拖动预览用，松手时才持久化
    var localSensitivity by remember(settings?.backTapSensitivity) {
        mutableIntStateOf(settings?.backTapSensitivity ?: 5)
    }
    var localRange by remember(settings?.backTapRange) {
        mutableIntStateOf(settings?.backTapRange ?: 5)
    }

    SectionCard(
        title = stringResource(R.string.back_tap_config_title),
        titleTopPadding = 0.dp,
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            ToggleRow(
                title = stringResource(R.string.back_tap_enable_title),
                description = stringResource(R.string.back_tap_enable_desc),
                checked = settings?.backTapEnabled ?: false,
                onCheckedChange = onEnabledChange,
            )

            if (settings?.backTapEnabled == true) {
                Spacer(modifier = Modifier.height(16.dp))

                LevelSlider(
                    label = stringResource(R.string.back_tap_sensitivity),
                    value = localSensitivity,
                    onValueChange = { value -> localSensitivity = value },
                    onValueChangeFinished = { onSensitivityChange(localSensitivity) },
                )

                Spacer(modifier = Modifier.height(16.dp))

                LevelSlider(
                    label = stringResource(R.string.back_tap_range),
                    value = localRange,
                    onValueChange = { value -> localRange = value },
                    onValueChangeFinished = { onRangeChange(localRange) },
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = stringResource(R.string.back_tap_mode),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    BackTapMode.entries.forEach { mode ->
                        ModeButton(
                            mode = mode,
                            selected = settings.backTapMode == mode,
                            onClick = { onModeChange(mode) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                ToggleRow(
                    title = stringResource(R.string.back_tap_pause_on_charging_title),
                    description = stringResource(R.string.back_tap_pause_on_charging_desc),
                    checked = settings.backTapPauseOnCharging,
                    onCheckedChange = onPauseOnChargingChange,
                )
            }
        }
    }
}

// 标题 + 说明 + 开关的单行
@Composable
private fun ToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AppSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

// 1..10 级滑块：点击与拖动都即时预览，松手才持久化
@Composable
private fun LevelSlider(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    onValueChangeFinished: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "$value",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        SliderTrack(
            fraction = ((value - 1) / 9f).coerceIn(0f, 1f),
            onFractionChange = { ratio ->
                onValueChange((1 + ratio * 9).roundToInt().coerceIn(1, 10))
            },
            onFractionChangeFinished = onValueChangeFinished,
        )
    }
}

// 工作模式按钮：选中项使用主色
@Composable
private fun ModeButton(
    mode: BackTapMode,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = when (mode) {
        BackTapMode.ALWAYS -> stringResource(R.string.back_tap_mode_always)
        BackTapMode.SCREEN_OFF -> stringResource(R.string.back_tap_mode_screen_off)
        BackTapMode.SCREEN_ON -> stringResource(R.string.back_tap_mode_screen_on)
    }
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            contentColor = if (selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        ),
    ) {
        Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
