package com.edgegesture.evilgodxu.screens.edge_config

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.edgegesture.evilgodxu.R
import androidx.datastore.preferences.core.Preferences
import com.edgegesture.evilgodxu.data.gesture.GestureAction
import com.edgegesture.evilgodxu.data.gesture.GestureSettingsKeys
import com.edgegesture.evilgodxu.data.gesture.gestureSettingsFlow
import com.edgegesture.evilgodxu.data.gesture.saveBottomEdgeGesture
import com.edgegesture.evilgodxu.data.gesture.saveBottomEdgeHeight
import com.edgegesture.evilgodxu.data.gesture.saveBottomEdgeWidthPercent
import com.edgegesture.evilgodxu.data.gesture.saveBottomSegmentCount
import com.edgegesture.evilgodxu.data.gesture.saveLeftEdgeGesture
import com.edgegesture.evilgodxu.data.gesture.saveLeftEdgeHeightPercent
import com.edgegesture.evilgodxu.data.gesture.saveLeftEdgePositionPercent
import com.edgegesture.evilgodxu.data.gesture.saveLeftEdgeWidth
import com.edgegesture.evilgodxu.data.gesture.saveLeftSegmentCount
import com.edgegesture.evilgodxu.data.gesture.saveRightEdgeGesture
import com.edgegesture.evilgodxu.data.gesture.saveRightEdgeHeightPercent
import com.edgegesture.evilgodxu.data.gesture.saveRightEdgePositionPercent
import com.edgegesture.evilgodxu.data.gesture.saveRightEdgeWidth
import com.edgegesture.evilgodxu.data.gesture.saveRightSegmentCount
import com.edgegesture.evilgodxu.screens.edge_config.compact.CompactAssembly
import com.edgegesture.evilgodxu.ui.action.ActionSelectionDialog
import com.edgegesture.evilgodxu.ui.action.getActionDisplayName
import kotlinx.coroutines.launch

// 边缘手势配置页 — 页面入口 Composable
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EdgeGestureConfigScreen(
    edgeType: EdgeType,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by context.gestureSettingsFlow().collectAsState(initial = null)

    var selectedSegment by remember { mutableIntStateOf(1) }
    var showActionDialog by remember { mutableStateOf(false) }
    var currentGestureIndex by remember { mutableIntStateOf(-1) }

    val title = when (edgeType) {
        EdgeType.LEFT -> stringResource(R.string.gesture_config_left)
        EdgeType.RIGHT -> stringResource(R.string.gesture_config_right)
        EdgeType.BOTTOM -> stringResource(R.string.gesture_config_bottom)
    }

    val currentSettings = settings

    val initW = when (edgeType) {
        EdgeType.LEFT -> currentSettings?.leftEdgeWidth ?: 20
        EdgeType.RIGHT -> currentSettings?.rightEdgeWidth ?: 20
        EdgeType.BOTTOM -> currentSettings?.bottomEdgeHeight ?: 20
    }
    val initH = when (edgeType) {
        EdgeType.LEFT -> currentSettings?.leftEdgeHeightPercent ?: 60
        EdgeType.RIGHT -> currentSettings?.rightEdgeHeightPercent ?: 60
        EdgeType.BOTTOM -> currentSettings?.bottomEdgeWidthPercent ?: 80
    }
    val initP = when (edgeType) {
        EdgeType.LEFT -> currentSettings?.leftEdgePositionPercent ?: 90
        EdgeType.RIGHT -> currentSettings?.rightEdgePositionPercent ?: 90
        else -> 0
    }
    var localWidth by remember(currentSettings, edgeType) { mutableIntStateOf(initW) }
    var localHeightPercent by remember(currentSettings, edgeType) { mutableIntStateOf(initH) }
    var localPositionPercent by remember(currentSettings, edgeType) { mutableIntStateOf(initP) }

    val maxSegments = when (edgeType) {
        EdgeType.LEFT -> currentSettings?.leftSegmentCount ?: 1
        EdgeType.RIGHT -> currentSettings?.rightSegmentCount ?: 1
        EdgeType.BOTTOM -> currentSettings?.bottomSegmentCount ?: 1
    }

    val gestureActions = getGestureActions(edgeType, selectedSegment, currentSettings)
    val gestureLabels = getGestureLabels(edgeType)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = title, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.nav_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        CompactAssembly(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            edgeType = edgeType,
            width = localWidth,
            heightPercent = localHeightPercent,
            positionPercent = localPositionPercent,
            segmentCount = maxSegments,
            selectedSegment = selectedSegment,
            gestureActions = gestureActions,
            gestureLabels = gestureLabels,
            onWidthChange = { v ->
                localWidth = v
                scope.launch {
                    when (edgeType) { EdgeType.LEFT -> context.saveLeftEdgeWidth(v); EdgeType.RIGHT -> context.saveRightEdgeWidth(v); EdgeType.BOTTOM -> context.saveBottomEdgeHeight(v) }
                }
            },
            onHeightPercentChange = { v ->
                localHeightPercent = v
                scope.launch {
                    when (edgeType) { EdgeType.LEFT -> context.saveLeftEdgeHeightPercent(v); EdgeType.RIGHT -> context.saveRightEdgeHeightPercent(v); EdgeType.BOTTOM -> context.saveBottomEdgeWidthPercent(v) }
                }
            },
            onPositionPercentChange = { v ->
                localPositionPercent = v
                scope.launch {
                    when (edgeType) { EdgeType.LEFT -> context.saveLeftEdgePositionPercent(v); EdgeType.RIGHT -> context.saveRightEdgePositionPercent(v); else -> {} }
                }
            },
            onSegmentCountChange = { count ->
                scope.launch {
                    when (edgeType) { EdgeType.LEFT -> context.saveLeftSegmentCount(count); EdgeType.RIGHT -> context.saveRightSegmentCount(count); EdgeType.BOTTOM -> context.saveBottomSegmentCount(count) }
                }
                if (selectedSegment > count) selectedSegment = count
            },
            onSegmentSelected = { selectedSegment = it },
            onGestureClick = { index ->
                currentGestureIndex = index
                showActionDialog = true
            },
        )
    }

    // 动作选择对话框
    if (showActionDialog && currentGestureIndex >= 0 && currentSettings != null) {
        val (currentAction, key) = gestureActions.getOrNull(currentGestureIndex) ?: return
        ActionSelectionDialog(
            currentAction = currentAction,
            onDismiss = { showActionDialog = false; currentGestureIndex = -1 },
            onActionSelected = { action ->
                scope.launch {
                    when (edgeType) {
                        EdgeType.LEFT -> context.saveLeftEdgeGesture(key, action)
                        EdgeType.RIGHT -> context.saveRightEdgeGesture(key, action)
                        EdgeType.BOTTOM -> context.saveBottomEdgeGesture(key, action)
                    }
                }
                showActionDialog = false
                currentGestureIndex = -1
            },
            getActionDisplayName = { getActionDisplayName(it) }
        )
    }
}

// 获取指定边缘和段的动作列表
private fun getGestureActions(
    edgeType: EdgeType,
    segment: Int,
    settings: com.edgegesture.evilgodxu.data.gesture.GestureSettingsState?
): List<Pair<GestureAction, Preferences.Key<String>>> {
    if (settings == null) return emptyList()

    return when (edgeType) {
        EdgeType.LEFT -> {
            val edge = when (segment) { 2 -> settings.leftEdgeSegment2; 3 -> settings.leftEdgeSegment3; else -> settings.leftEdge }
            val keys = when (segment) {
                2 -> listOf(GestureSettingsKeys.LEFT_2_SWIPE_RIGHT, GestureSettingsKeys.LEFT_2_SWIPE_RIGHT_LONG, GestureSettingsKeys.LEFT_2_SWIPE_UP, GestureSettingsKeys.LEFT_2_SWIPE_UP_LONG, GestureSettingsKeys.LEFT_2_SWIPE_DOWN, GestureSettingsKeys.LEFT_2_SWIPE_DOWN_LONG)
                3 -> listOf(GestureSettingsKeys.LEFT_3_SWIPE_RIGHT, GestureSettingsKeys.LEFT_3_SWIPE_RIGHT_LONG, GestureSettingsKeys.LEFT_3_SWIPE_UP, GestureSettingsKeys.LEFT_3_SWIPE_UP_LONG, GestureSettingsKeys.LEFT_3_SWIPE_DOWN, GestureSettingsKeys.LEFT_3_SWIPE_DOWN_LONG)
                else -> listOf(GestureSettingsKeys.LEFT_SWIPE_RIGHT, GestureSettingsKeys.LEFT_SWIPE_RIGHT_LONG, GestureSettingsKeys.LEFT_SWIPE_UP, GestureSettingsKeys.LEFT_SWIPE_UP_LONG, GestureSettingsKeys.LEFT_SWIPE_DOWN, GestureSettingsKeys.LEFT_SWIPE_DOWN_LONG)
            }
            listOf(edge.swipeRight to keys[0], edge.swipeRightLong to keys[1], edge.swipeUp to keys[2], edge.swipeUpLong to keys[3], edge.swipeDown to keys[4], edge.swipeDownLong to keys[5])
        }
        EdgeType.RIGHT -> {
            val edge = when (segment) { 2 -> settings.rightEdgeSegment2; 3 -> settings.rightEdgeSegment3; else -> settings.rightEdge }
            val keys = when (segment) {
                2 -> listOf(GestureSettingsKeys.RIGHT_2_SWIPE_LEFT, GestureSettingsKeys.RIGHT_2_SWIPE_LEFT_LONG, GestureSettingsKeys.RIGHT_2_SWIPE_UP, GestureSettingsKeys.RIGHT_2_SWIPE_UP_LONG, GestureSettingsKeys.RIGHT_2_SWIPE_DOWN, GestureSettingsKeys.RIGHT_2_SWIPE_DOWN_LONG)
                3 -> listOf(GestureSettingsKeys.RIGHT_3_SWIPE_LEFT, GestureSettingsKeys.RIGHT_3_SWIPE_LEFT_LONG, GestureSettingsKeys.RIGHT_3_SWIPE_UP, GestureSettingsKeys.RIGHT_3_SWIPE_UP_LONG, GestureSettingsKeys.RIGHT_3_SWIPE_DOWN, GestureSettingsKeys.RIGHT_3_SWIPE_DOWN_LONG)
                else -> listOf(GestureSettingsKeys.RIGHT_SWIPE_LEFT, GestureSettingsKeys.RIGHT_SWIPE_LEFT_LONG, GestureSettingsKeys.RIGHT_SWIPE_UP, GestureSettingsKeys.RIGHT_SWIPE_UP_LONG, GestureSettingsKeys.RIGHT_SWIPE_DOWN, GestureSettingsKeys.RIGHT_SWIPE_DOWN_LONG)
            }
            listOf(edge.swipeLeft to keys[0], edge.swipeLeftLong to keys[1], edge.swipeUp to keys[2], edge.swipeUpLong to keys[3], edge.swipeDown to keys[4], edge.swipeDownLong to keys[5])
        }
        EdgeType.BOTTOM -> {
            val edge = when (segment) { 2 -> settings.bottomEdgeSegment2; 3 -> settings.bottomEdgeSegment3; else -> settings.bottomEdge }
            val keys = when (segment) {
                2 -> listOf(GestureSettingsKeys.BOTTOM_2_SWIPE_UP, GestureSettingsKeys.BOTTOM_2_SWIPE_UP_LONG, GestureSettingsKeys.BOTTOM_2_SWIPE_LEFT, GestureSettingsKeys.BOTTOM_2_SWIPE_LEFT_LONG, GestureSettingsKeys.BOTTOM_2_SWIPE_RIGHT, GestureSettingsKeys.BOTTOM_2_SWIPE_RIGHT_LONG)
                3 -> listOf(GestureSettingsKeys.BOTTOM_3_SWIPE_UP, GestureSettingsKeys.BOTTOM_3_SWIPE_UP_LONG, GestureSettingsKeys.BOTTOM_3_SWIPE_LEFT, GestureSettingsKeys.BOTTOM_3_SWIPE_LEFT_LONG, GestureSettingsKeys.BOTTOM_3_SWIPE_RIGHT, GestureSettingsKeys.BOTTOM_3_SWIPE_RIGHT_LONG)
                else -> listOf(GestureSettingsKeys.BOTTOM_SWIPE_UP, GestureSettingsKeys.BOTTOM_SWIPE_UP_LONG, GestureSettingsKeys.BOTTOM_SWIPE_LEFT, GestureSettingsKeys.BOTTOM_SWIPE_LEFT_LONG, GestureSettingsKeys.BOTTOM_SWIPE_RIGHT, GestureSettingsKeys.BOTTOM_SWIPE_RIGHT_LONG)
            }
            listOf(edge.swipeUp to keys[0], edge.swipeUpLong to keys[1], edge.swipeLeft to keys[2], edge.swipeLeftLong to keys[3], edge.swipeRight to keys[4], edge.swipeRightLong to keys[5])
        }
    }
}

@Composable
private fun getGestureLabels(edgeType: EdgeType): List<String> {
    return when (edgeType) {
        EdgeType.LEFT -> listOf(
            stringResource(R.string.gesture_swipe_right), stringResource(R.string.gesture_swipe_right_long),
            stringResource(R.string.gesture_swipe_up), stringResource(R.string.gesture_swipe_up_long),
            stringResource(R.string.gesture_swipe_down), stringResource(R.string.gesture_swipe_down_long)
        )
        EdgeType.RIGHT -> listOf(
            stringResource(R.string.gesture_swipe_left), stringResource(R.string.gesture_swipe_left_long),
            stringResource(R.string.gesture_swipe_up), stringResource(R.string.gesture_swipe_up_long),
            stringResource(R.string.gesture_swipe_down), stringResource(R.string.gesture_swipe_down_long)
        )
        EdgeType.BOTTOM -> listOf(
            stringResource(R.string.gesture_swipe_up), stringResource(R.string.gesture_swipe_up_long),
            stringResource(R.string.gesture_swipe_left), stringResource(R.string.gesture_swipe_left_long),
            stringResource(R.string.gesture_swipe_right), stringResource(R.string.gesture_swipe_right_long)
        )
    }
}
