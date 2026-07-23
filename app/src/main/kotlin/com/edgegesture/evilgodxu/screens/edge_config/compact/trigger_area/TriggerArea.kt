package com.edgegesture.evilgodxu.screens.edge_config.compact.trigger_area

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
import com.edgegesture.evilgodxu.screens.edge_config.EdgeType
import com.edgegesture.evilgodxu.screens.edge_config.compact.trigger_area.edge_preview.EdgePreview
import com.edgegesture.evilgodxu.screens.edge_config.compact.trigger_area.segment_count_slider.SegmentCountSlider
import com.edgegesture.evilgodxu.screens.edge_config.compact.trigger_area.size_sliders.SizeSliders

// 触发区域 Area — 仅做组件排列，不含 UI 实现
@Composable
fun TriggerArea(
    edgeType: EdgeType,
    width: Int,
    heightPercent: Int,
    positionPercent: Int,
    segmentCount: Int,
    selectedSegment: Int,
    onWidthChange: (Int) -> Unit,
    onHeightPercentChange: (Int) -> Unit,
    onPositionPercentChange: (Int) -> Unit,
    onSegmentCountChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.gesture_config_section_title),
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
                EdgePreview(
                    edgeType = edgeType,
                    width = width,
                    heightPercent = heightPercent,
                    positionPercent = positionPercent,
                    segmentCount = segmentCount,
                    selectedSegment = selectedSegment,
                    modifier = Modifier.fillMaxWidth().height(120.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                SizeSliders(
                    edgeType = edgeType,
                    width = width,
                    heightPercent = heightPercent,
                    positionPercent = positionPercent,
                    onWidthChange = onWidthChange,
                    onHeightPercentChange = onHeightPercentChange,
                    onPositionPercentChange = onPositionPercentChange,
                )

                Spacer(modifier = Modifier.height(16.dp))

                SegmentCountSlider(
                    segmentCount = segmentCount,
                    onSegmentCountChange = onSegmentCountChange,
                )
            }
        }
    }
}
