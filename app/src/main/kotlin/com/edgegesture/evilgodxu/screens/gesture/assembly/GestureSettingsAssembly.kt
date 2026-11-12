package com.edgegesture.evilgodxu.screens.gesture.assembly

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import com.edgegesture.evilgodxu.data.gesture.GestureSettingsState
import com.edgegesture.evilgodxu.screens.gesture.GestureSettingsUiState
import com.edgegesture.evilgodxu.screens.gesture.component.preview.GesturePreview
import com.edgegesture.evilgodxu.screens.gesture.component.config.AdvancedGestureSection
import com.edgegesture.evilgodxu.screens.gesture.component.config.GestureSummarySection
import com.edgegesture.evilgodxu.screens.gesture.component.config.TriggerAreaSection
import com.edgegesture.evilgodxu.screens.gesture.component.config.buildBackTapDescription
import com.edgegesture.evilgodxu.screens.gesture.component.more.MoreGridSection
import com.edgegesture.evilgodxu.screens.gesture.component.permission.PermissionsSection
import com.edgegesture.evilgodxu.screens.gesture.component.status.ServiceStatusSection

// 手势首页组装器：宽窗按「状态与入口 | 手势配置」两栏排布，窄窗单栏顺序堆叠，
// 两种形态消费同一份状态与同一套事件
@Composable
fun GestureSettingsAssembly(
    uiState: GestureSettingsUiState,
    settings: GestureSettingsState,
    windowSizeClass: WindowSizeClass,
    onToggleService: (Boolean) -> Unit,
    onRequestAccessibility: () -> Unit,
    onRequestOverlay: () -> Unit,
    onRequestNotification: () -> Unit,
    onRequestBattery: () -> Unit,
    onRequestQueryPackages: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToBlacklist: () -> Unit,
    onNavigateToLaunchBlock: () -> Unit,
    onNavigateToBackTap: () -> Unit,
    onNavigateToLeftEdge: () -> Unit,
    onNavigateToRightEdge: () -> Unit,
    onNavigateToBottomEdge: () -> Unit,
    onNavigateToExpandPanel: () -> Unit,
    onHideOverlayChange: (Boolean) -> Unit,
    onHideFromRecentsChange: (Boolean) -> Unit,
    onAvoidKeyboardOverlapChange: (Boolean) -> Unit,
    onVibrationEnabledChange: (Boolean) -> Unit,
    onDoubleSwipeEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    // 判断是否使用双列布局：横屏或大屏幕设备
    val useTwoPaneLayout = windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)

    if (useTwoPaneLayout) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            // 左侧列：服务状态、权限卡片、触发区预览
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
            ) {
                ServiceEntrySections(
                    uiState = uiState,
                    settings = settings,
                    onToggleService = onToggleService,
                    onRequestAccessibility = onRequestAccessibility,
                    onRequestOverlay = onRequestOverlay,
                    onRequestNotification = onRequestNotification,
                    onRequestBattery = onRequestBattery,
                    onRequestQueryPackages = onRequestQueryPackages,
                )

                AnimatedVisibility(visible = settings.gestureEnabled) {
                    GesturePreview(
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
            }

            // 右侧列：手势配置与更多入口
            AnimatedVisibility(
                visible = settings.gestureEnabled,
                modifier = Modifier.weight(1f),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                ) {
                    GestureConfigSections(
                        settings = settings,
                        onNavigateToBackTap = onNavigateToBackTap,
                        onNavigateToLeftEdge = onNavigateToLeftEdge,
                        onNavigateToRightEdge = onNavigateToRightEdge,
                        onNavigateToBottomEdge = onNavigateToBottomEdge,
                        onNavigateToSettings = onNavigateToSettings,
                        onNavigateToBlacklist = onNavigateToBlacklist,
                        onNavigateToLaunchBlock = onNavigateToLaunchBlock,
                        onNavigateToExpandPanel = onNavigateToExpandPanel,
                        onHideOverlayChange = onHideOverlayChange,
                        onHideFromRecentsChange = onHideFromRecentsChange,
                        onAvoidKeyboardOverlapChange = onAvoidKeyboardOverlapChange,
                        onVibrationEnabledChange = onVibrationEnabledChange,
                        onDoubleSwipeEnabledChange = onDoubleSwipeEnabledChange,
                    )
                }
            }
        }
    } else {
        Column(modifier = modifier.verticalScroll(rememberScrollState())) {
            ServiceEntrySections(
                uiState = uiState,
                settings = settings,
                onToggleService = onToggleService,
                onRequestAccessibility = onRequestAccessibility,
                onRequestOverlay = onRequestOverlay,
                onRequestNotification = onRequestNotification,
                onRequestBattery = onRequestBattery,
                onRequestQueryPackages = onRequestQueryPackages,
            )

            AnimatedVisibility(visible = settings.gestureEnabled) {
                GesturePreview(
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
                GestureSummarySection(
                    onLeftClick = onNavigateToLeftEdge,
                    onRightClick = onNavigateToRightEdge,
                    onBottomClick = onNavigateToBottomEdge,
                )
            }

            AnimatedVisibility(visible = settings.gestureEnabled) {
                AdvancedGestureSection(
                    backTapEnabled = settings.backTapEnabled,
                    backTapDescription = buildBackTapDescription(settings),
                    onClick = onNavigateToBackTap,
                )
            }

            AnimatedVisibility(visible = settings.gestureEnabled) {
                TriggerAreaSection(
                    hideOverlay = settings.hideOverlay,
                    hideFromRecents = settings.hideFromRecents,
                    avoidKeyboardOverlap = settings.avoidKeyboardOverlap,
                    vibrationEnabled = settings.vibrationEnabled,
                    doubleSwipeEnabled = settings.doubleSwipeEnabled,
                    onHideOverlayChange = onHideOverlayChange,
                    onHideFromRecentsChange = onHideFromRecentsChange,
                    onAvoidKeyboardOverlapChange = onAvoidKeyboardOverlapChange,
                    onVibrationEnabledChange = onVibrationEnabledChange,
                    onDoubleSwipeEnabledChange = onDoubleSwipeEnabledChange,
                )
            }

            AnimatedVisibility(visible = settings.gestureEnabled) {
                MoreGridSection(
                    onExpandPanel = onNavigateToExpandPanel,
                    onBlacklist = onNavigateToBlacklist,
                    onLaunchBlock = onNavigateToLaunchBlock,
                    onSettings = onNavigateToSettings,
                )
            }
        }
    }
}

// 服务状态与权限：两种形态共用同一段内容
@Composable
private fun ServiceEntrySections(
    uiState: GestureSettingsUiState,
    settings: GestureSettingsState,
    onToggleService: (Boolean) -> Unit,
    onRequestAccessibility: () -> Unit,
    onRequestOverlay: () -> Unit,
    onRequestNotification: () -> Unit,
    onRequestBattery: () -> Unit,
    onRequestQueryPackages: () -> Unit,
) {
    ServiceStatusSection(settings = settings, onToggleService = onToggleService)
    PermissionsSection(
        uiState = uiState,
        onRequestAccessibility = onRequestAccessibility,
        onRequestOverlay = onRequestOverlay,
        onRequestNotification = onRequestNotification,
        onRequestBattery = onRequestBattery,
        onRequestQueryPackages = onRequestQueryPackages,
    )
}

// 手势配置与更多入口：宽窗在右栏整体展示，窄窗由骨架逐段控制显隐
@Composable
private fun GestureConfigSections(
    settings: GestureSettingsState,
    onNavigateToBackTap: () -> Unit,
    onNavigateToLeftEdge: () -> Unit,
    onNavigateToRightEdge: () -> Unit,
    onNavigateToBottomEdge: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToBlacklist: () -> Unit,
    onNavigateToLaunchBlock: () -> Unit,
    onNavigateToExpandPanel: () -> Unit,
    onHideOverlayChange: (Boolean) -> Unit,
    onHideFromRecentsChange: (Boolean) -> Unit,
    onAvoidKeyboardOverlapChange: (Boolean) -> Unit,
    onVibrationEnabledChange: (Boolean) -> Unit,
    onDoubleSwipeEnabledChange: (Boolean) -> Unit,
) {
    GestureSummarySection(
        onLeftClick = onNavigateToLeftEdge,
        onRightClick = onNavigateToRightEdge,
        onBottomClick = onNavigateToBottomEdge,
    )
    AdvancedGestureSection(
        backTapEnabled = settings.backTapEnabled,
        backTapDescription = buildBackTapDescription(settings),
        onClick = onNavigateToBackTap,
    )
    TriggerAreaSection(
        hideOverlay = settings.hideOverlay,
        hideFromRecents = settings.hideFromRecents,
        avoidKeyboardOverlap = settings.avoidKeyboardOverlap,
        vibrationEnabled = settings.vibrationEnabled,
        doubleSwipeEnabled = settings.doubleSwipeEnabled,
        onHideOverlayChange = onHideOverlayChange,
        onHideFromRecentsChange = onHideFromRecentsChange,
        onAvoidKeyboardOverlapChange = onAvoidKeyboardOverlapChange,
        onVibrationEnabledChange = onVibrationEnabledChange,
        onDoubleSwipeEnabledChange = onDoubleSwipeEnabledChange,
    )
    MoreGridSection(
        onExpandPanel = onNavigateToExpandPanel,
        onBlacklist = onNavigateToBlacklist,
        onLaunchBlock = onNavigateToLaunchBlock,
        onSettings = onNavigateToSettings,
    )
}
