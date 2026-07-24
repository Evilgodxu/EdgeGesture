package com.edgegesture.evilgodxu.screens.gesture.expanded

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.edgegesture.evilgodxu.data.gesture.GestureSettingsState
import com.edgegesture.evilgodxu.screens.gesture.compact.advanced_gesture_area.AdvancedGestureArea
import com.edgegesture.evilgodxu.screens.gesture.compact.edge_config_area.EdgeConfigArea
import com.edgegesture.evilgodxu.screens.gesture.compact.trigger_settings_area.TriggerSettingsArea
import com.edgegesture.evilgodxu.screens.gesture.compact.more_grid_area.MoreGridArea

// 展开视图物理空间组装器（双列）
@Composable
fun ExpandedAssembly(
    settings: GestureSettingsState,
    onNavigateToLeftEdge: () -> Unit,
    onNavigateToRightEdge: () -> Unit,
    onNavigateToBottomEdge: () -> Unit,
    onNavigateToBackTap: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToBlacklist: () -> Unit,
    onNavigateToLaunchBlock: () -> Unit,
    onNavigateToExpandPanel: () -> Unit,
    onHideOverlayChange: (Boolean) -> Unit,
    onHideFromRecentsChange: (Boolean) -> Unit,
    onAvoidKeyboardOverlapChange: (Boolean) -> Unit,
    onVibrationChange: (Boolean) -> Unit,
    onDoubleSwipeChange: (Boolean) -> Unit,
    statusArea: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // 左列：开关 + 预览
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
        ) {
            statusArea(Modifier)

            com.edgegesture.evilgodxu.screens.gesture.compact.preview.GesturePreview(
                leftEdgeWidth = settings.leftEdgeWidth,
                leftEdgeHeightPercent = settings.leftEdgeHeightPercent,
                leftEdgePositionPercent = settings.leftEdgePositionPercent,
                leftSegmentCount = settings.leftSegmentCount,
                rightEdgeWidth = settings.rightEdgeWidth,
                rightEdgeHeightPercent = settings.rightEdgeHeightPercent,
                rightEdgePositionPercent = settings.rightEdgePositionPercent,
                rightSegmentCount = settings.rightSegmentCount,
                bottomEdgeHeight = settings.bottomEdgeHeight,
                bottomEdgeWidthPercent = settings.bottomEdgeWidthPercent,
                bottomSegmentCount = settings.bottomSegmentCount,
            )
        }

        // 右列：配置卡片
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
        ) {
            EdgeConfigArea(
                settings = settings,
                onLeftClick = onNavigateToLeftEdge,
                onRightClick = onNavigateToRightEdge,
                onBottomClick = onNavigateToBottomEdge,
            )

            AdvancedGestureArea(settings = settings, onClick = onNavigateToBackTap)

            TriggerSettingsArea(
                settings = settings,
                onHideOverlayChange = onHideOverlayChange,
                onHideFromRecentsChange = onHideFromRecentsChange,
                onAvoidKeyboardOverlapChange = onAvoidKeyboardOverlapChange,
                onVibrationChange = onVibrationChange,
                onDoubleSwipeChange = onDoubleSwipeChange,
            )

            MoreGridArea(
                onSettings = onNavigateToSettings,
                onBlacklist = onNavigateToBlacklist,
                onLaunchBlock = onNavigateToLaunchBlock,
                onExpandPanel = onNavigateToExpandPanel,
            )
        }
    }
}
