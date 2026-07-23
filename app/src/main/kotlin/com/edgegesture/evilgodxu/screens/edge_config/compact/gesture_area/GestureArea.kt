package com.edgegesture.evilgodxu.screens.edge_config.compact.gesture_area

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.edgegesture.evilgodxu.data.gesture.GestureAction
import com.edgegesture.evilgodxu.screens.edge_config.compact.gesture_area.gesture_action_row.GestureActionRow
import com.edgegesture.evilgodxu.screens.edge_config.compact.gesture_area.segment_selector.SegmentSelector
import com.edgegesture.evilgodxu.screens.gesture.components.getActionDisplayName

// 手势操作 Area — 仅做组件排列，不含 UI 实现
@Composable
fun GestureArea(
    segmentCount: Int,
    selectedSegment: Int,
    gestureActions: List<Pair<GestureAction, *>>,
    gestureLabels: List<String>,
    onSegmentSelected: (Int) -> Unit,
    onGestureClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.gesture_operation_section_title),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
        )

        SegmentSelector(
            segmentCount = segmentCount,
            selectedSegment = selectedSegment,
            onSegmentSelected = onSegmentSelected,
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                gestureActions.forEachIndexed { index, (action, _) ->
                    val rot = when {
                        gestureLabels[index].contains(stringResource(R.string.gesture_swipe_right)) -> 0f
                        gestureLabels[index].contains(stringResource(R.string.gesture_swipe_left)) -> 180f
                        gestureLabels[index].contains(stringResource(R.string.gesture_swipe_up)) -> -90f
                        gestureLabels[index].contains(stringResource(R.string.gesture_swipe_down)) -> 90f
                        else -> 0f
                    }
                    GestureActionRow(
                        label = gestureLabels[index],
                        actionName = getActionDisplayName(action),
                        isLongPress = index % 2 == 1,
                        iconRotation = rot,
                        onClick = { onGestureClick(index) }
                    )
                    if (index < gestureActions.lastIndex) {
                        Spacer(modifier = Modifier.size(8.dp))
                    }
                }
            }
        }
    }
}
