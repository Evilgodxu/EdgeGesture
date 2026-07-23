package com.edgegesture.evilgodxu.screens.edge_config.compact.trigger_area.size_sliders

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.edgegesture.evilgodxu.R
import com.edgegesture.evilgodxu.screens.edge_config.EdgeType
import com.edgegesture.evilgodxu.screens.edge_config.reuse.SliderSetting

// 尺寸滑块职责组件 — 根据边缘类型展示对应滑块组
@Composable
fun SizeSliders(
    edgeType: EdgeType,
    width: Int,
    heightPercent: Int,
    positionPercent: Int,
    onWidthChange: (Int) -> Unit,
    onHeightPercentChange: (Int) -> Unit,
    onPositionPercentChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.layout.Column(modifier = modifier.fillMaxWidth()) {
        if (edgeType == EdgeType.BOTTOM) {
            SliderSetting(
                label = stringResource(R.string.edge_height), value = width, range = 10f..50f, steps = 39, suffix = "dp",
                onValueChange = onWidthChange,
            )
            Spacer(modifier = Modifier.height(12.dp))
            SliderSetting(
                label = stringResource(R.string.edge_width_percent), value = heightPercent, range = 20f..100f, steps = 79, suffix = "%",
                onValueChange = onHeightPercentChange,
            )
        } else {
            SliderSetting(
                label = stringResource(R.string.edge_width), value = width, range = 10f..50f, steps = 39, suffix = "dp",
                onValueChange = onWidthChange,
            )
            Spacer(modifier = Modifier.height(12.dp))
            SliderSetting(
                label = stringResource(R.string.edge_height_percent), value = heightPercent, range = 20f..100f, steps = 79, suffix = "%",
                onValueChange = onHeightPercentChange,
            )
            Spacer(modifier = Modifier.height(12.dp))
            SliderSetting(
                label = stringResource(R.string.edge_position_percent), value = positionPercent, range = 0f..100f, steps = 99, suffix = "%",
                onValueChange = onPositionPercentChange,
            )
        }
    }
}
