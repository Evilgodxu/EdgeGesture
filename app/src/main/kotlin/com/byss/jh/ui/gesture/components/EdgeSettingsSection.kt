package com.byss.jh.ui.gesture.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.byss.jh.R

// 边缘设置区域组件（左/右边）
@Composable
fun EdgeSettingsSection(
    title: String,
    width: Int,
    heightPercent: Int,
    positionPercent: Int,
    segmentCount: Int,
    onWidthChange: (Int) -> Unit,
    onHeightPercentChange: (Int) -> Unit,
    onPositionPercentChange: (Int) -> Unit,
    onSegmentCountChange: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
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
                Column(
                    modifier = Modifier.padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 宽度调节
                    SettingSliderItem(
                        label = stringResource(R.string.edge_width),
                        value = width,
                        range = 10f..50f,
                        valueFormat = { "${it.toInt()}dp" },
                        onValueChange = { onWidthChange(it.toInt()) }
                    )

                    // 高度百分比调节
                    SettingSliderItem(
                        label = stringResource(R.string.edge_height_percent),
                        value = heightPercent,
                        range = 20f..100f,
                        valueFormat = { "${it.toInt()}%" },
                        onValueChange = { onHeightPercentChange(it.toInt()) }
                    )

                    // 位置百分比调节
                    SettingSliderItem(
                        label = stringResource(R.string.edge_position_percent),
                        value = positionPercent,
                        range = 0f..100f,
                        valueFormat = { "${it.toInt()}%" },
                        onValueChange = { onPositionPercentChange(it.toInt()) }
                    )

                    // 段数选择
                    val segmentUnit = stringResource(R.string.edge_segment_unit)
                    SettingSliderItem(
                        label = stringResource(R.string.edge_segment_count),
                        value = segmentCount,
                        range = 1f..3f,
                        steps = 1,
                        valueFormat = { "${it.toInt()}$segmentUnit" },
                        onValueChange = { onSegmentCountChange(it.toInt()) }
                    )
                }
            }
        }
    }
}
