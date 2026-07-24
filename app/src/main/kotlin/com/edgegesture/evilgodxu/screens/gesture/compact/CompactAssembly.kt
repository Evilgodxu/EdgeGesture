package com.edgegesture.evilgodxu.screens.gesture.compact

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
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

// 紧凑视图物理空间组装器
@Composable
fun CompactAssembly(
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
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        statusArea(Modifier)

        AnimatedVisibility(visible = settings.gestureEnabled) {
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
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }

        AnimatedVisibility(visible = settings.gestureEnabled) {
            EdgeConfigArea(
                settings = settings,
                onLeftClick = onNavigateToLeftEdge,
                onRightClick = onNavigateToRightEdge,
                onBottomClick = onNavigateToBottomEdge,
            )
        }

        AnimatedVisibility(visible = settings.gestureEnabled) {
            AdvancedGestureArea(settings = settings, onClick = onNavigateToBackTap)
        }

        AnimatedVisibility(visible = settings.gestureEnabled) {
            TriggerSettingsArea(
                settings = settings,
                onHideOverlayChange = onHideOverlayChange,
                onHideFromRecentsChange = onHideFromRecentsChange,
                onAvoidKeyboardOverlapChange = onAvoidKeyboardOverlapChange,
                onVibrationChange = onVibrationChange,
                onDoubleSwipeChange = onDoubleSwipeChange,
            )
        }

        AnimatedVisibility(visible = settings.gestureEnabled) {
            MoreGridArea(
                onSettings = onNavigateToSettings,
                onBlacklist = onNavigateToBlacklist,
                onLaunchBlock = onNavigateToLaunchBlock,
                onExpandPanel = onNavigateToExpandPanel,
            )
        }
    }
}
