package com.edgegesture.evilgodxu.screens.gesture.component.dimension

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.edgegesture.evilgodxu.R
import com.edgegesture.evilgodxu.ui.component.SliderTrack
import kotlin.math.roundToInt

// 左右边缘的尺寸、长度与位置滑杆
@Composable
fun EdgeDimensionSliders(
    width: Int,
    heightPercent: Int,
    positionPercent: Int,
    onWidthChange: (Int) -> Unit,
    onHeightPercentChange: (Int) -> Unit,
    onPositionPercentChange: (Int) -> Unit,
    onWidthChangeFinished: () -> Unit,
    onHeightPercentChangeFinished: () -> Unit,
    onPositionPercentChangeFinished: () -> Unit,
) {
    SliderSetting(
        label = stringResource(R.string.edge_width),
        value = width,
        range = 10f..50f,
        steps = 39,
        suffix = "dp",
        onValueChange = onWidthChange,
        onValueChangeFinished = onWidthChangeFinished,
    )
    Spacer(modifier = Modifier.height(12.dp))
    SliderSetting(
        label = stringResource(R.string.edge_height_percent),
        value = heightPercent,
        range = 20f..100f,
        steps = 79,
        suffix = "%",
        onValueChange = onHeightPercentChange,
        onValueChangeFinished = onHeightPercentChangeFinished,
    )
    Spacer(modifier = Modifier.height(12.dp))
    SliderSetting(
        label = stringResource(R.string.edge_position_percent),
        value = positionPercent,
        range = 0f..100f,
        steps = 99,
        suffix = "%",
        onValueChange = onPositionPercentChange,
        onValueChangeFinished = onPositionPercentChangeFinished,
    )
}

// 底部边缘的厚度与宽度滑杆
@Composable
fun BottomDimensionSliders(
    height: Int,
    widthPercent: Int,
    onHeightChange: (Int) -> Unit,
    onWidthPercentChange: (Int) -> Unit,
    onHeightChangeFinished: () -> Unit,
    onWidthPercentChangeFinished: () -> Unit,
) {
    SliderSetting(
        label = stringResource(R.string.edge_height),
        value = height,
        range = 10f..50f,
        steps = 39,
        suffix = "dp",
        onValueChange = onHeightChange,
        onValueChangeFinished = onHeightChangeFinished,
    )
    Spacer(modifier = Modifier.height(12.dp))
    SliderSetting(
        label = stringResource(R.string.edge_width_percent),
        value = widthPercent,
        range = 20f..100f,
        steps = 79,
        suffix = "%",
        onValueChange = onWidthPercentChange,
        onValueChangeFinished = onWidthPercentChangeFinished,
    )
}

// 带标签与数值回显的整数滑杆
@Composable
private fun SliderSetting(
    label: String,
    value: Int,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    suffix: String,
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
                text = "$value$suffix",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        val fraction = ((value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)
        SliderTrack(
            fraction = fraction,
            onFractionChange = { ratio ->
                val raw = range.start + ratio * (range.endInclusive - range.start)
                val stepped = if (steps > 0) {
                    val stepSize = (range.endInclusive - range.start) / (steps + 1)
                    Math.round((raw - range.start) / stepSize) * stepSize + range.start
                } else {
                    raw
                }
                onValueChange(stepped.toInt().coerceIn(range.start.toInt(), range.endInclusive.toInt()))
            },
            onFractionChangeFinished = onValueChangeFinished,
        )
    }
}

// 分段数量滑杆：1..3 段
@Composable
fun SegmentCountSlider(
    segmentCount: Int,
    onSegmentCountChange: (Int) -> Unit,
    onSegmentCountChangeFinished: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.edge_segment_count),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "${segmentCount}${stringResource(R.string.edge_segment_unit)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        SliderTrack(
            fraction = (segmentCount - 1) / 2f,
            onFractionChange = { ratio ->
                onSegmentCountChange((1 + ratio * 2).roundToInt().coerceIn(1, 3))
            },
            onFractionChangeFinished = onSegmentCountChangeFinished,
        )
    }
}
