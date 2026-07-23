package com.edgegesture.evilgodxu.screens.edge_config.compact

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.edgegesture.evilgodxu.data.gesture.GestureAction
import com.edgegesture.evilgodxu.screens.edge_config.EdgeType
import com.edgegesture.evilgodxu.screens.edge_config.compact.gesture_area.GestureArea
import com.edgegesture.evilgodxu.screens.edge_config.compact.trigger_area.TriggerArea

// 紧凑视图物理空间组装器
@Composable
fun CompactAssembly(
    edgeType: EdgeType,
    width: Int,
    heightPercent: Int,
    positionPercent: Int,
    segmentCount: Int,
    selectedSegment: Int,
    gestureActions: List<Pair<GestureAction, *>>,
    gestureLabels: List<String>,
    onWidthChange: (Int) -> Unit,
    onHeightPercentChange: (Int) -> Unit,
    onPositionPercentChange: (Int) -> Unit,
    onSegmentCountChange: (Int) -> Unit,
    onSegmentSelected: (Int) -> Unit,
    onGestureClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        TriggerArea(
            edgeType = edgeType,
            width = width,
            heightPercent = heightPercent,
            positionPercent = positionPercent,
            segmentCount = segmentCount,
            selectedSegment = selectedSegment,
            onWidthChange = onWidthChange,
            onHeightPercentChange = onHeightPercentChange,
            onPositionPercentChange = onPositionPercentChange,
            onSegmentCountChange = onSegmentCountChange,
        )

        Spacer(modifier = Modifier.height(20.dp))

        GestureArea(
            segmentCount = segmentCount,
            selectedSegment = selectedSegment,
            gestureActions = gestureActions,
            gestureLabels = gestureLabels,
            onSegmentSelected = onSegmentSelected,
            onGestureClick = onGestureClick,
        )

        Spacer(modifier = Modifier.height(80.dp))
    }
}
