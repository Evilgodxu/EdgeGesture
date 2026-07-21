package com.edgegesture.evilgodxu.screens.gesture

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.BatteryFull
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PictureInPicture
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass
import com.edgegesture.evilgodxu.R
import com.edgegesture.evilgodxu.data.gesture.GestureAction
import com.edgegesture.evilgodxu.data.gesture.GestureSettingsState
import com.edgegesture.evilgodxu.data.gesture.GestureSettingsKeys
import com.edgegesture.evilgodxu.data.permission.PermissionType
import com.edgegesture.evilgodxu.screens.gesture.service.EdgeGestureAccessibilityService
import com.edgegesture.evilgodxu.ui.adaptive.rememberWindowSizeClass
import com.edgegesture.evilgodxu.screens.gesture.components.ActionSelectionDialog
import com.edgegesture.evilgodxu.screens.gesture.components.BackTapSettingsSection
import com.edgegesture.evilgodxu.screens.gesture.components.BottomEdgeSettingsSection
import com.edgegesture.evilgodxu.screens.gesture.components.EdgeGestureSection
import com.edgegesture.evilgodxu.screens.gesture.components.EdgeSettingsSection
import com.edgegesture.evilgodxu.screens.gesture.components.GesturePreview
import com.edgegesture.evilgodxu.screens.gesture.components.GestureSettingsSwitchItem
import com.edgegesture.evilgodxu.screens.gesture.components.PermissionCard
import com.edgegesture.evilgodxu.screens.gesture.components.PermissionGroupCard
import com.edgegesture.evilgodxu.screens.gesture.components.getActionDisplayName
import org.koin.androidx.compose.koinViewModel

@Composable
fun GestureSettingsScreen(
    onNavigateToSettings: () -> Unit,
    viewModel: GestureSettingsViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings = uiState.settings
    val lifecycleOwner = LocalLifecycleOwner.current
    val windowSizeClass = rememberWindowSizeClass()

    // 加载状态处理
    if (uiState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    var showActionDialog by remember { mutableStateOf(false) }
    var currentActionKey by remember { mutableStateOf<androidx.datastore.preferences.core.Preferences.Key<String>?>(null) }
    var currentActionValue by remember { mutableStateOf(GestureAction.NONE) }
    var showBackTapActionDialog by remember { mutableStateOf(false) }

    var waitingForSystemSetting by remember { mutableStateOf(false) }

    // rememberUpdatedState 确保在 DisposableEffect 中也能获取到最新的 waitingForSystemSetting 值
    val currentWaitingState by rememberUpdatedState { waitingForSystemSetting }
    val setWaitingState = { value: Boolean ->
        waitingForSystemSetting = value
    }

    // 通知权限申请 launcher
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.setNotificationGranted(isGranted)
    }

    // 监听生命周期，用户从系统设置返回时刷新权限状态
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // 用户可能刚从系统设置页返回，需要刷新权限状态
                viewModel.refreshAccessibilityState()
                viewModel.refreshPermissions()
                if (currentWaitingState()) {
                    setWaitingState(false)
                }
                // 页面返回时停止权限监控
                viewModel.stopPermissionMonitor()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            // 页面销毁时停止权限监控
            viewModel.stopPermissionMonitor()
        }
    }

    Scaffold(
        topBar = {
            EdgeGestureHeader(onNavigateToSettings = onNavigateToSettings)
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        GestureSettingsContent(
            modifier = Modifier
                .fillMaxSize()
                .consumeWindowInsets(innerPadding)
                .padding(innerPadding),
            uiState = uiState,
            settings = settings,
            viewModel = viewModel,
            windowSizeClass = windowSizeClass,
            activity = activity,
            notificationPermissionLauncher = notificationPermissionLauncher,
            onNavigateToSettings = onNavigateToSettings,
            onShowActionDialog = { key, action ->
                currentActionKey = key
                currentActionValue = action
                showActionDialog = true
            },
            onShowBackTapActionDialog = { showBackTapActionDialog = true }
        )
    }

    // 手势动作选择对话框
    if (showActionDialog && currentActionKey != null) {
        ActionSelectionDialog(
            currentAction = currentActionValue,
            onDismiss = { showActionDialog = false },
            onActionSelected = { action ->
                val key = currentActionKey!!
                when (key) {
                    GestureSettingsKeys.LEFT_SWIPE_RIGHT,
                    GestureSettingsKeys.LEFT_SWIPE_RIGHT_LONG,
                    GestureSettingsKeys.LEFT_SWIPE_UP,
                    GestureSettingsKeys.LEFT_SWIPE_UP_LONG,
                    GestureSettingsKeys.LEFT_SWIPE_DOWN,
                    GestureSettingsKeys.LEFT_SWIPE_DOWN_LONG -> viewModel.setLeftEdgeGesture(key, action)
                    GestureSettingsKeys.RIGHT_SWIPE_LEFT,
                    GestureSettingsKeys.RIGHT_SWIPE_LEFT_LONG,
                    GestureSettingsKeys.RIGHT_SWIPE_UP,
                    GestureSettingsKeys.RIGHT_SWIPE_UP_LONG,
                    GestureSettingsKeys.RIGHT_SWIPE_DOWN,
                    GestureSettingsKeys.RIGHT_SWIPE_DOWN_LONG -> viewModel.setRightEdgeGesture(key, action)
                    GestureSettingsKeys.BOTTOM_SWIPE_UP,
                    GestureSettingsKeys.BOTTOM_SWIPE_UP_LONG,
                    GestureSettingsKeys.BOTTOM_SWIPE_LEFT,
                    GestureSettingsKeys.BOTTOM_SWIPE_LEFT_LONG,
                    GestureSettingsKeys.BOTTOM_SWIPE_RIGHT,
                    GestureSettingsKeys.BOTTOM_SWIPE_RIGHT_LONG -> viewModel.setBottomEdgeGesture(key, action)
                    // 左侧第2段
                    GestureSettingsKeys.LEFT_2_SWIPE_RIGHT,
                    GestureSettingsKeys.LEFT_2_SWIPE_RIGHT_LONG,
                    GestureSettingsKeys.LEFT_2_SWIPE_UP,
                    GestureSettingsKeys.LEFT_2_SWIPE_UP_LONG,
                    GestureSettingsKeys.LEFT_2_SWIPE_DOWN,
                    GestureSettingsKeys.LEFT_2_SWIPE_DOWN_LONG -> viewModel.setLeftEdgeGesture(key, action)
                    // 左侧第3段
                    GestureSettingsKeys.LEFT_3_SWIPE_RIGHT,
                    GestureSettingsKeys.LEFT_3_SWIPE_RIGHT_LONG,
                    GestureSettingsKeys.LEFT_3_SWIPE_UP,
                    GestureSettingsKeys.LEFT_3_SWIPE_UP_LONG,
                    GestureSettingsKeys.LEFT_3_SWIPE_DOWN,
                    GestureSettingsKeys.LEFT_3_SWIPE_DOWN_LONG -> viewModel.setLeftEdgeGesture(key, action)
                    // 右侧第2段
                    GestureSettingsKeys.RIGHT_2_SWIPE_LEFT,
                    GestureSettingsKeys.RIGHT_2_SWIPE_LEFT_LONG,
                    GestureSettingsKeys.RIGHT_2_SWIPE_UP,
                    GestureSettingsKeys.RIGHT_2_SWIPE_UP_LONG,
                    GestureSettingsKeys.RIGHT_2_SWIPE_DOWN,
                    GestureSettingsKeys.RIGHT_2_SWIPE_DOWN_LONG -> viewModel.setRightEdgeGesture(key, action)
                    // 右侧第3段
                    GestureSettingsKeys.RIGHT_3_SWIPE_LEFT,
                    GestureSettingsKeys.RIGHT_3_SWIPE_LEFT_LONG,
                    GestureSettingsKeys.RIGHT_3_SWIPE_UP,
                    GestureSettingsKeys.RIGHT_3_SWIPE_UP_LONG,
                    GestureSettingsKeys.RIGHT_3_SWIPE_DOWN,
                    GestureSettingsKeys.RIGHT_3_SWIPE_DOWN_LONG -> viewModel.setRightEdgeGesture(key, action)
                    // 底部第2段
                    GestureSettingsKeys.BOTTOM_2_SWIPE_UP,
                    GestureSettingsKeys.BOTTOM_2_SWIPE_UP_LONG,
                    GestureSettingsKeys.BOTTOM_2_SWIPE_LEFT,
                    GestureSettingsKeys.BOTTOM_2_SWIPE_LEFT_LONG,
                    GestureSettingsKeys.BOTTOM_2_SWIPE_RIGHT,
                    GestureSettingsKeys.BOTTOM_2_SWIPE_RIGHT_LONG -> viewModel.setBottomEdgeGesture(key, action)
                    // 底部第3段
                    GestureSettingsKeys.BOTTOM_3_SWIPE_UP,
                    GestureSettingsKeys.BOTTOM_3_SWIPE_UP_LONG,
                    GestureSettingsKeys.BOTTOM_3_SWIPE_LEFT,
                    GestureSettingsKeys.BOTTOM_3_SWIPE_LEFT_LONG,
                    GestureSettingsKeys.BOTTOM_3_SWIPE_RIGHT,
                    GestureSettingsKeys.BOTTOM_3_SWIPE_RIGHT_LONG -> viewModel.setBottomEdgeGesture(key, action)
                }
                showActionDialog = false
            },
            getActionDisplayName = { getActionDisplayName(it) }
        )
    }

    // 背面双击操作选择对话框
    if (showBackTapActionDialog) {
        ActionSelectionDialog(
            currentAction = settings.backTapAction,
            onDismiss = { showBackTapActionDialog = false },
            onActionSelected = { action ->
                viewModel.setBackTapAction(action)
                showBackTapActionDialog = false
            },
            getActionDisplayName = { getActionDisplayName(it) }
        )
    }

}

@Composable
private fun GestureSettingsContent(
    modifier: Modifier = Modifier,
    uiState: GestureSettingsUiState,
    settings: GestureSettingsState,
    viewModel: GestureSettingsViewModel,
    windowSizeClass: WindowSizeClass,
    activity: Activity?,
    notificationPermissionLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    onNavigateToSettings: () -> Unit,
    onShowActionDialog: (androidx.datastore.preferences.core.Preferences.Key<String>, GestureAction) -> Unit,
    onShowBackTapActionDialog: () -> Unit
) {
    val context = LocalContext.current
    // 判断是否使用双列布局：横屏或大屏幕设备
    val useTwoPaneLayout = windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)

    if (useTwoPaneLayout) {
        // 双列布局：左侧开关，右侧手势设置
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // 左侧列：警告卡片、权限卡片、开关组件
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                GestureSettingsSwitchesColumn(
                    uiState = uiState,
                    settings = settings,
                    viewModel = viewModel,
                    activity = activity,
                    notificationPermissionLauncher = notificationPermissionLauncher
                )

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
                    bottomSegmentCount = settings.bottomSegmentCount
                )
            }

            // 右侧列：手势设置折叠组件
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                GestureSettingsExpandableColumn(
                    settings = settings,
                    viewModel = viewModel,
                    onShowActionDialog = onShowActionDialog,
                    onShowBackTapActionDialog = onShowBackTapActionDialog
                )

                TriggerAreaSettingsCard(
                    settings = settings,
                    viewModel = viewModel
                )

                MoreGridCard(
                    onSettings = onNavigateToSettings
                )
            }
        }
    } else {
        // 单列布局：原始垂直布局
        Column(
            modifier = modifier.verticalScroll(rememberScrollState())
        ) {
            GestureSettingsSwitchesColumn(
                uiState = uiState,
                settings = settings,
                viewModel = viewModel,
                activity = activity,
                notificationPermissionLauncher = notificationPermissionLauncher
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
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            AnimatedVisibility(visible = settings.gestureEnabled) {
                GestureSettingsExpandableColumn(
                    settings = settings,
                    viewModel = viewModel,
                    onShowActionDialog = onShowActionDialog,
                    onShowBackTapActionDialog = onShowBackTapActionDialog
                )
            }

            AnimatedVisibility(visible = settings.gestureEnabled) {
                TriggerAreaSettingsCard(
                    settings = settings,
                    viewModel = viewModel
                )
            }

            AnimatedVisibility(visible = settings.gestureEnabled) {
                MoreGridCard(
                    onSettings = onNavigateToSettings
                )
            }
        }
    }
}

@Composable
private fun GestureSettingsSwitchesColumn(
    uiState: GestureSettingsUiState,
    settings: GestureSettingsState,
    viewModel: GestureSettingsViewModel,
    activity: Activity?,
    notificationPermissionLauncher: androidx.activity.result.ActivityResultLauncher<String>
) {
    val context = LocalContext.current

    // 启动服务状态卡片
    ServiceStatusCard(
        enabled = settings.gestureEnabled,
        isAccessibilityEnabled = uiState.isAccessibilityEnabled,
        totalSegments = settings.leftSegmentCount + settings.rightSegmentCount + settings.bottomSegmentCount,
        totalGestures = countNonNoneGestures(settings),
        onToggleService = { enable ->
            if (enable && !uiState.isAccessibilityEnabled) {
                if (activity != null) {
                    viewModel.startPermissionMonitor(PermissionType.ACCESSIBILITY, activity)
                }
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                context.startActivity(intent)
                return@ServiceStatusCard
            }
            viewModel.setGestureEnabled(enable)
            if (enable) {
                EdgeGestureAccessibilityService.startGesture(context)
            } else {
                EdgeGestureAccessibilityService.stopGesture(context)
            }
        }
    )

    // 权限状态分组卡片（全部授权后自动隐藏）
    AnimatedVisibility(
        visible = !uiState.isAccessibilityEnabled || !uiState.allPermissionsGranted,
        enter = expandVertically(),
        exit = shrinkVertically()
    ) {
        PermissionGroupCard(
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
        // 无障碍权限
        PermissionCard(
            title = stringResource(R.string.permission_accessibility_title),
            description = stringResource(R.string.permission_accessibility_desc),
            granted = uiState.isAccessibilityEnabled,
            onRequest = {
                if (activity != null) {
                    viewModel.startPermissionMonitor(PermissionType.ACCESSIBILITY, activity)
                }
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                context.startActivity(intent)
            },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Security,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        )
        PermissionCard(
            title = stringResource(R.string.permission_overlay_title),
            description = stringResource(R.string.permission_overlay_desc),
            granted = uiState.overlayGranted,
            onRequest = {
                if (activity != null) {
                    viewModel.startPermissionMonitor(PermissionType.OVERLAY, activity)
                }
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    "package:${context.packageName}".toUri()
                )
                activity?.startActivity(intent)
            },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.PictureInPicture,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        )
        PermissionCard(
            title = stringResource(R.string.permission_notification_title),
            description = stringResource(R.string.permission_notification_desc),
            granted = uiState.notificationGranted,
            onRequest = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        )
        PermissionCard(
            title = stringResource(R.string.permission_battery_title),
            description = stringResource(R.string.permission_battery_desc),
            granted = uiState.batteryOptimized,
            onRequest = {
                if (activity != null) {
                    viewModel.startPermissionMonitor(PermissionType.BATTERY_OPTIMIZATION, activity)
                }
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = "package:${context.packageName}".toUri()
                }
                activity?.startActivity(intent)
            },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.BatteryFull,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        )
        PermissionCard(
            title = stringResource(R.string.permission_usage_stats_title),
            description = stringResource(R.string.permission_usage_stats_desc),
            granted = uiState.usageStatsGranted,
            onRequest = {
                if (activity != null) {
                    viewModel.startPermissionMonitor(PermissionType.USAGE_STATS, activity)
                }
                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                    data = "package:${context.packageName}".toUri()
                }
                activity?.startActivity(intent)
            },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.BarChart,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        )
        PermissionCard(
            title = stringResource(R.string.permission_query_packages_title),
            description = stringResource(R.string.permission_query_packages_desc),
            granted = uiState.queryAllPackagesGranted,
            onRequest = {
                if (activity != null) {
                    viewModel.startPermissionMonitor(PermissionType.QUERY_ALL_PACKAGES, activity)
                }
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = "package:${context.packageName}".toUri()
                }
                activity?.startActivity(intent)
            },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Android,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        )
    }
    }

    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun GestureSettingsExpandableColumn(
    settings: GestureSettingsState,
    viewModel: GestureSettingsViewModel,
    onShowActionDialog: (androidx.datastore.preferences.core.Preferences.Key<String>, GestureAction) -> Unit,
    onShowBackTapActionDialog: () -> Unit
) {
    if (!settings.gestureEnabled) return

    Column {
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // 左侧边缘触发区域尺寸配置
        EdgeSettingsSection(
            title = stringResource(R.string.gesture_left_edge_title),
            width = settings.leftEdgeWidth,
            heightPercent = settings.leftEdgeHeightPercent,
            positionPercent = settings.leftEdgePositionPercent,
            segmentCount = settings.leftSegmentCount,
            onWidthChange = { viewModel.setLeftEdgeWidth(it) },
            onHeightPercentChange = { viewModel.setLeftEdgeHeightPercent(it) },
            onPositionPercentChange = { viewModel.setLeftEdgePositionPercent(it) },
            onSegmentCountChange = { viewModel.setLeftSegmentCount(it) }
        )

        // 左侧边缘第1段手势动作配置
        EdgeGestureSection(
            title = stringResource(R.string.gesture_left_edge_actions),
            gestures = listOf(
                Triple(stringResource(R.string.gesture_swipe_right), settings.leftEdge.swipeRight, GestureSettingsKeys.LEFT_SWIPE_RIGHT),
                Triple(stringResource(R.string.gesture_swipe_right_long), settings.leftEdge.swipeRightLong, GestureSettingsKeys.LEFT_SWIPE_RIGHT_LONG),
                Triple(stringResource(R.string.gesture_swipe_up), settings.leftEdge.swipeUp, GestureSettingsKeys.LEFT_SWIPE_UP),
                Triple(stringResource(R.string.gesture_swipe_up_long), settings.leftEdge.swipeUpLong, GestureSettingsKeys.LEFT_SWIPE_UP_LONG),
                Triple(stringResource(R.string.gesture_swipe_down), settings.leftEdge.swipeDown, GestureSettingsKeys.LEFT_SWIPE_DOWN),
                Triple(stringResource(R.string.gesture_swipe_down_long), settings.leftEdge.swipeDownLong, GestureSettingsKeys.LEFT_SWIPE_DOWN_LONG)
            ),
            disabledGestures = setOf(stringResource(R.string.gesture_swipe_left), stringResource(R.string.gesture_swipe_left_long)),
            onGestureClick = { _, action, key -> onShowActionDialog(key, action) },
            getActionDisplayName = { getActionDisplayName(it) }
        )

        // 左侧边缘第2段手势动作配置（当段数>=2时显示）
        if (settings.leftSegmentCount >= 2) {
            EdgeGestureSection(
                title = stringResource(R.string.gesture_left_edge_actions_2),
                gestures = listOf(
                    Triple(stringResource(R.string.gesture_swipe_right), settings.leftEdgeSegment2.swipeRight, GestureSettingsKeys.LEFT_2_SWIPE_RIGHT),
                    Triple(stringResource(R.string.gesture_swipe_right_long), settings.leftEdgeSegment2.swipeRightLong, GestureSettingsKeys.LEFT_2_SWIPE_RIGHT_LONG),
                    Triple(stringResource(R.string.gesture_swipe_up), settings.leftEdgeSegment2.swipeUp, GestureSettingsKeys.LEFT_2_SWIPE_UP),
                    Triple(stringResource(R.string.gesture_swipe_up_long), settings.leftEdgeSegment2.swipeUpLong, GestureSettingsKeys.LEFT_2_SWIPE_UP_LONG),
                    Triple(stringResource(R.string.gesture_swipe_down), settings.leftEdgeSegment2.swipeDown, GestureSettingsKeys.LEFT_2_SWIPE_DOWN),
                    Triple(stringResource(R.string.gesture_swipe_down_long), settings.leftEdgeSegment2.swipeDownLong, GestureSettingsKeys.LEFT_2_SWIPE_DOWN_LONG)
                ),
                disabledGestures = setOf(stringResource(R.string.gesture_swipe_left), stringResource(R.string.gesture_swipe_left_long)),
                onGestureClick = { _, action, key -> onShowActionDialog(key, action) },
                getActionDisplayName = { getActionDisplayName(it) }
            )
        }

        // 左侧边缘第3段手势动作配置（当段数>=3时显示）
        if (settings.leftSegmentCount >= 3) {
            EdgeGestureSection(
                title = stringResource(R.string.gesture_left_edge_actions_3),
                gestures = listOf(
                    Triple(stringResource(R.string.gesture_swipe_right), settings.leftEdgeSegment3.swipeRight, GestureSettingsKeys.LEFT_3_SWIPE_RIGHT),
                    Triple(stringResource(R.string.gesture_swipe_right_long), settings.leftEdgeSegment3.swipeRightLong, GestureSettingsKeys.LEFT_3_SWIPE_RIGHT_LONG),
                    Triple(stringResource(R.string.gesture_swipe_up), settings.leftEdgeSegment3.swipeUp, GestureSettingsKeys.LEFT_3_SWIPE_UP),
                    Triple(stringResource(R.string.gesture_swipe_up_long), settings.leftEdgeSegment3.swipeUpLong, GestureSettingsKeys.LEFT_3_SWIPE_UP_LONG),
                    Triple(stringResource(R.string.gesture_swipe_down), settings.leftEdgeSegment3.swipeDown, GestureSettingsKeys.LEFT_3_SWIPE_DOWN),
                    Triple(stringResource(R.string.gesture_swipe_down_long), settings.leftEdgeSegment3.swipeDownLong, GestureSettingsKeys.LEFT_3_SWIPE_DOWN_LONG)
                ),
                disabledGestures = setOf(stringResource(R.string.gesture_swipe_left), stringResource(R.string.gesture_swipe_left_long)),
                onGestureClick = { _, action, key -> onShowActionDialog(key, action) },
                getActionDisplayName = { getActionDisplayName(it) }
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // 右侧边缘触发区域尺寸配置
        EdgeSettingsSection(
            title = stringResource(R.string.gesture_right_edge_title),
            width = settings.rightEdgeWidth,
            heightPercent = settings.rightEdgeHeightPercent,
            positionPercent = settings.rightEdgePositionPercent,
            segmentCount = settings.rightSegmentCount,
            onWidthChange = { viewModel.setRightEdgeWidth(it) },
            onHeightPercentChange = { viewModel.setRightEdgeHeightPercent(it) },
            onPositionPercentChange = { viewModel.setRightEdgePositionPercent(it) },
            onSegmentCountChange = { viewModel.setRightSegmentCount(it) }
        )

        // 右侧边缘第1段手势动作配置
        EdgeGestureSection(
            title = stringResource(R.string.gesture_right_edge_actions),
            gestures = listOf(
                Triple(stringResource(R.string.gesture_swipe_left), settings.rightEdge.swipeLeft, GestureSettingsKeys.RIGHT_SWIPE_LEFT),
                Triple(stringResource(R.string.gesture_swipe_left_long), settings.rightEdge.swipeLeftLong, GestureSettingsKeys.RIGHT_SWIPE_LEFT_LONG),
                Triple(stringResource(R.string.gesture_swipe_up), settings.rightEdge.swipeUp, GestureSettingsKeys.RIGHT_SWIPE_UP),
                Triple(stringResource(R.string.gesture_swipe_up_long), settings.rightEdge.swipeUpLong, GestureSettingsKeys.RIGHT_SWIPE_UP_LONG),
                Triple(stringResource(R.string.gesture_swipe_down), settings.rightEdge.swipeDown, GestureSettingsKeys.RIGHT_SWIPE_DOWN),
                Triple(stringResource(R.string.gesture_swipe_down_long), settings.rightEdge.swipeDownLong, GestureSettingsKeys.RIGHT_SWIPE_DOWN_LONG)
            ),
            disabledGestures = setOf(stringResource(R.string.gesture_swipe_right), stringResource(R.string.gesture_swipe_right_long)),
            onGestureClick = { _, action, key -> onShowActionDialog(key, action) },
            getActionDisplayName = { getActionDisplayName(it) }
        )

        // 右侧边缘第2段手势动作配置（当段数>=2时显示）
        if (settings.rightSegmentCount >= 2) {
            EdgeGestureSection(
                title = stringResource(R.string.gesture_right_edge_actions_2),
                gestures = listOf(
                    Triple(stringResource(R.string.gesture_swipe_left), settings.rightEdgeSegment2.swipeLeft, GestureSettingsKeys.RIGHT_2_SWIPE_LEFT),
                    Triple(stringResource(R.string.gesture_swipe_left_long), settings.rightEdgeSegment2.swipeLeftLong, GestureSettingsKeys.RIGHT_2_SWIPE_LEFT_LONG),
                    Triple(stringResource(R.string.gesture_swipe_up), settings.rightEdgeSegment2.swipeUp, GestureSettingsKeys.RIGHT_2_SWIPE_UP),
                    Triple(stringResource(R.string.gesture_swipe_up_long), settings.rightEdgeSegment2.swipeUpLong, GestureSettingsKeys.RIGHT_2_SWIPE_UP_LONG),
                    Triple(stringResource(R.string.gesture_swipe_down), settings.rightEdgeSegment2.swipeDown, GestureSettingsKeys.RIGHT_2_SWIPE_DOWN),
                    Triple(stringResource(R.string.gesture_swipe_down_long), settings.rightEdgeSegment2.swipeDownLong, GestureSettingsKeys.RIGHT_2_SWIPE_DOWN_LONG)
                ),
                disabledGestures = setOf(stringResource(R.string.gesture_swipe_right), stringResource(R.string.gesture_swipe_right_long)),
                onGestureClick = { _, action, key -> onShowActionDialog(key, action) },
                getActionDisplayName = { getActionDisplayName(it) }
            )
        }

        // 右侧边缘第3段手势动作配置（当段数>=3时显示）
        if (settings.rightSegmentCount >= 3) {
            EdgeGestureSection(
                title = stringResource(R.string.gesture_right_edge_actions_3),
                gestures = listOf(
                    Triple(stringResource(R.string.gesture_swipe_left), settings.rightEdgeSegment3.swipeLeft, GestureSettingsKeys.RIGHT_3_SWIPE_LEFT),
                    Triple(stringResource(R.string.gesture_swipe_left_long), settings.rightEdgeSegment3.swipeLeftLong, GestureSettingsKeys.RIGHT_3_SWIPE_LEFT_LONG),
                    Triple(stringResource(R.string.gesture_swipe_up), settings.rightEdgeSegment3.swipeUp, GestureSettingsKeys.RIGHT_3_SWIPE_UP),
                    Triple(stringResource(R.string.gesture_swipe_up_long), settings.rightEdgeSegment3.swipeUpLong, GestureSettingsKeys.RIGHT_3_SWIPE_UP_LONG),
                    Triple(stringResource(R.string.gesture_swipe_down), settings.rightEdgeSegment3.swipeDown, GestureSettingsKeys.RIGHT_3_SWIPE_DOWN),
                    Triple(stringResource(R.string.gesture_swipe_down_long), settings.rightEdgeSegment3.swipeDownLong, GestureSettingsKeys.RIGHT_3_SWIPE_DOWN_LONG)
                ),
                disabledGestures = setOf(stringResource(R.string.gesture_swipe_right), stringResource(R.string.gesture_swipe_right_long)),
                onGestureClick = { _, action, key -> onShowActionDialog(key, action) },
                getActionDisplayName = { getActionDisplayName(it) }
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // 底部边缘触发区域尺寸配置
        BottomEdgeSettingsSection(
            title = stringResource(R.string.gesture_bottom_edge_title),
            height = settings.bottomEdgeHeight,
            widthPercent = settings.bottomEdgeWidthPercent,
            segmentCount = settings.bottomSegmentCount,
            onHeightChange = { viewModel.setBottomEdgeHeight(it) },
            onWidthPercentChange = { viewModel.setBottomEdgeWidthPercent(it) },
            onSegmentCountChange = { viewModel.setBottomSegmentCount(it) }
        )

        // 底部边缘第1段手势动作配置
        EdgeGestureSection(
            title = stringResource(R.string.gesture_bottom_edge_actions),
            gestures = listOf(
                Triple(stringResource(R.string.gesture_swipe_up), settings.bottomEdge.swipeUp, GestureSettingsKeys.BOTTOM_SWIPE_UP),
                Triple(stringResource(R.string.gesture_swipe_up_long), settings.bottomEdge.swipeUpLong, GestureSettingsKeys.BOTTOM_SWIPE_UP_LONG),
                Triple(stringResource(R.string.gesture_swipe_left), settings.bottomEdge.swipeLeft, GestureSettingsKeys.BOTTOM_SWIPE_LEFT),
                Triple(stringResource(R.string.gesture_swipe_left_long), settings.bottomEdge.swipeLeftLong, GestureSettingsKeys.BOTTOM_SWIPE_LEFT_LONG),
                Triple(stringResource(R.string.gesture_swipe_right), settings.bottomEdge.swipeRight, GestureSettingsKeys.BOTTOM_SWIPE_RIGHT),
                Triple(stringResource(R.string.gesture_swipe_right_long), settings.bottomEdge.swipeRightLong, GestureSettingsKeys.BOTTOM_SWIPE_RIGHT_LONG)
            ),
            disabledGestures = setOf(stringResource(R.string.gesture_swipe_down), stringResource(R.string.gesture_swipe_down_long)),
            onGestureClick = { _, action, key -> onShowActionDialog(key, action) },
            getActionDisplayName = { getActionDisplayName(it) }
        )

        // 底部边缘第2段手势动作配置（当段数>=2时显示）
        if (settings.bottomSegmentCount >= 2) {
            EdgeGestureSection(
                title = stringResource(R.string.gesture_bottom_edge_actions_2),
                gestures = listOf(
                    Triple(stringResource(R.string.gesture_swipe_up), settings.bottomEdgeSegment2.swipeUp, GestureSettingsKeys.BOTTOM_2_SWIPE_UP),
                    Triple(stringResource(R.string.gesture_swipe_up_long), settings.bottomEdgeSegment2.swipeUpLong, GestureSettingsKeys.BOTTOM_2_SWIPE_UP_LONG),
                    Triple(stringResource(R.string.gesture_swipe_left), settings.bottomEdgeSegment2.swipeLeft, GestureSettingsKeys.BOTTOM_2_SWIPE_LEFT),
                    Triple(stringResource(R.string.gesture_swipe_left_long), settings.bottomEdgeSegment2.swipeLeftLong, GestureSettingsKeys.BOTTOM_2_SWIPE_LEFT_LONG),
                    Triple(stringResource(R.string.gesture_swipe_right), settings.bottomEdgeSegment2.swipeRight, GestureSettingsKeys.BOTTOM_2_SWIPE_RIGHT),
                    Triple(stringResource(R.string.gesture_swipe_right_long), settings.bottomEdgeSegment2.swipeRightLong, GestureSettingsKeys.BOTTOM_2_SWIPE_RIGHT_LONG)
                ),
                disabledGestures = setOf(stringResource(R.string.gesture_swipe_down), stringResource(R.string.gesture_swipe_down_long)),
                onGestureClick = { _, action, key -> onShowActionDialog(key, action) },
                getActionDisplayName = { getActionDisplayName(it) }
            )
        }

        // 底部边缘第3段手势动作配置（当段数>=3时显示）
        if (settings.bottomSegmentCount >= 3) {
            EdgeGestureSection(
                title = stringResource(R.string.gesture_bottom_edge_actions_3),
                gestures = listOf(
                    Triple(stringResource(R.string.gesture_swipe_up), settings.bottomEdgeSegment3.swipeUp, GestureSettingsKeys.BOTTOM_3_SWIPE_UP),
                    Triple(stringResource(R.string.gesture_swipe_up_long), settings.bottomEdgeSegment3.swipeUpLong, GestureSettingsKeys.BOTTOM_3_SWIPE_UP_LONG),
                    Triple(stringResource(R.string.gesture_swipe_left), settings.bottomEdgeSegment3.swipeLeft, GestureSettingsKeys.BOTTOM_3_SWIPE_LEFT),
                    Triple(stringResource(R.string.gesture_swipe_left_long), settings.bottomEdgeSegment3.swipeLeftLong, GestureSettingsKeys.BOTTOM_3_SWIPE_LEFT_LONG),
                    Triple(stringResource(R.string.gesture_swipe_right), settings.bottomEdgeSegment3.swipeRight, GestureSettingsKeys.BOTTOM_3_SWIPE_RIGHT),
                    Triple(stringResource(R.string.gesture_swipe_right_long), settings.bottomEdgeSegment3.swipeRightLong, GestureSettingsKeys.BOTTOM_3_SWIPE_RIGHT_LONG)
                ),
                disabledGestures = setOf(stringResource(R.string.gesture_swipe_down), stringResource(R.string.gesture_swipe_down_long)),
                onGestureClick = { _, action, key -> onShowActionDialog(key, action) },
                getActionDisplayName = { getActionDisplayName(it) }
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // 背面双击设置
        BackTapSettingsSection(
            enabled = settings.backTapEnabled,
            sensitivity = settings.backTapSensitivity,
            range = settings.backTapRange,
            action = settings.backTapAction,
            mode = settings.backTapMode,
            pauseOnCharging = settings.backTapPauseOnCharging,
            onEnabledChange = { viewModel.setBackTapEnabled(it) },
            onSensitivityChange = { viewModel.setBackTapSensitivity(it) },
            onRangeChange = { viewModel.setBackTapRange(it) },
            onActionClick = onShowBackTapActionDialog,
            onModeChange = { viewModel.setBackTapMode(it) },
            onPauseOnChargingChange = { viewModel.setBackTapPauseOnCharging(it) },
            getActionDisplayName = { getActionDisplayName(it) }
        )
    }
}

@Composable
private fun EdgeGestureHeader(
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标容器(匹配设计: 32x32, border-radius 8, icon-bg 背景)
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = "EdgeGesture",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        IconButton(onClick = onNavigateToSettings) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = stringResource(R.string.settings_title),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 统计所有非 NONE 的手势动作总数
 */
private fun countNonNoneGestures(settings: GestureSettingsState): Int {
    var count = 0
    // 左侧第1段
    with(settings.leftEdge) {
        if (swipeRight != GestureAction.NONE) count++
        if (swipeRightLong != GestureAction.NONE) count++
        if (swipeUp != GestureAction.NONE) count++
        if (swipeUpLong != GestureAction.NONE) count++
        if (swipeDown != GestureAction.NONE) count++
        if (swipeDownLong != GestureAction.NONE) count++
    }
    // 左侧第2段
    if (settings.leftSegmentCount >= 2) {
        with(settings.leftEdgeSegment2) {
            if (swipeRight != GestureAction.NONE) count++
            if (swipeRightLong != GestureAction.NONE) count++
            if (swipeUp != GestureAction.NONE) count++
            if (swipeUpLong != GestureAction.NONE) count++
            if (swipeDown != GestureAction.NONE) count++
            if (swipeDownLong != GestureAction.NONE) count++
        }
    }
    // 左侧第3段
    if (settings.leftSegmentCount >= 3) {
        with(settings.leftEdgeSegment3) {
            if (swipeRight != GestureAction.NONE) count++
            if (swipeRightLong != GestureAction.NONE) count++
            if (swipeUp != GestureAction.NONE) count++
            if (swipeUpLong != GestureAction.NONE) count++
            if (swipeDown != GestureAction.NONE) count++
            if (swipeDownLong != GestureAction.NONE) count++
        }
    }
    // 右侧第1段
    with(settings.rightEdge) {
        if (swipeLeft != GestureAction.NONE) count++
        if (swipeLeftLong != GestureAction.NONE) count++
        if (swipeUp != GestureAction.NONE) count++
        if (swipeUpLong != GestureAction.NONE) count++
        if (swipeDown != GestureAction.NONE) count++
        if (swipeDownLong != GestureAction.NONE) count++
    }
    // 右侧第2段
    if (settings.rightSegmentCount >= 2) {
        with(settings.rightEdgeSegment2) {
            if (swipeLeft != GestureAction.NONE) count++
            if (swipeLeftLong != GestureAction.NONE) count++
            if (swipeUp != GestureAction.NONE) count++
            if (swipeUpLong != GestureAction.NONE) count++
            if (swipeDown != GestureAction.NONE) count++
            if (swipeDownLong != GestureAction.NONE) count++
        }
    }
    // 右侧第3段
    if (settings.rightSegmentCount >= 3) {
        with(settings.rightEdgeSegment3) {
            if (swipeLeft != GestureAction.NONE) count++
            if (swipeLeftLong != GestureAction.NONE) count++
            if (swipeUp != GestureAction.NONE) count++
            if (swipeUpLong != GestureAction.NONE) count++
            if (swipeDown != GestureAction.NONE) count++
            if (swipeDownLong != GestureAction.NONE) count++
        }
    }
    // 底部第1段
    with(settings.bottomEdge) {
        if (swipeUp != GestureAction.NONE) count++
        if (swipeUpLong != GestureAction.NONE) count++
        if (swipeLeft != GestureAction.NONE) count++
        if (swipeLeftLong != GestureAction.NONE) count++
        if (swipeRight != GestureAction.NONE) count++
        if (swipeRightLong != GestureAction.NONE) count++
    }
    // 底部第2段
    if (settings.bottomSegmentCount >= 2) {
        with(settings.bottomEdgeSegment2) {
            if (swipeUp != GestureAction.NONE) count++
            if (swipeUpLong != GestureAction.NONE) count++
            if (swipeLeft != GestureAction.NONE) count++
            if (swipeLeftLong != GestureAction.NONE) count++
            if (swipeRight != GestureAction.NONE) count++
            if (swipeRightLong != GestureAction.NONE) count++
        }
    }
    // 底部第3段
    if (settings.bottomSegmentCount >= 3) {
        with(settings.bottomEdgeSegment3) {
            if (swipeUp != GestureAction.NONE) count++
            if (swipeUpLong != GestureAction.NONE) count++
            if (swipeLeft != GestureAction.NONE) count++
            if (swipeLeftLong != GestureAction.NONE) count++
            if (swipeRight != GestureAction.NONE) count++
            if (swipeRightLong != GestureAction.NONE) count++
        }
    }
    return count
}

/**
 * 启动服务状态卡片
 * 匹配新版 UI 设计：状态标题 + 圆点指示器 + 按钮 + 统计信息
 */
@Composable
private fun ServiceStatusCard(
    enabled: Boolean,
    isAccessibilityEnabled: Boolean,
    totalSegments: Int,
    totalGestures: Int,
    onToggleService: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant
            )
        ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // 状态标题 + 圆点指示器
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (enabled) stringResource(R.string.gesture_service_running)
                           else stringResource(R.string.gesture_service_stopped),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Light,
                        letterSpacing = 2.sp
                    ),
                    color = if (enabled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
                // 状态圆点
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            color = if (enabled) Color(0xFF3FB950)
                                    else MaterialTheme.colorScheme.error,
                            shape = RoundedCornerShape(50)
                        )
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 副标题
            Text(
                text = stringResource(R.string.gesture_service_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 启动/停止 按钮
            Button(
                onClick = { onToggleService(!enabled) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (enabled) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.primary,
                    contentColor = if (enabled) MaterialTheme.colorScheme.onError
                                   else MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = if (enabled) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (enabled) stringResource(R.string.gesture_service_stop)
                           else stringResource(R.string.gesture_service_start),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 统计信息网格
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // 段数统计
                StatItem(
                    icon = {
                        // 手机轮廓示意
                        Box(
                            modifier = Modifier
                                .size(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp, 20.dp)
                                    .border(
                                        1.5.dp,
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                        RoundedCornerShape(3.dp)
                                    )
                            ) {
                                // 左边缘指示
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .size(2.dp, 10.dp)
                                        .background(
                                            MaterialTheme.colorScheme.primary,
                                            RoundedCornerShape(1.dp)
                                        )
                                )
                                // 右边缘指示
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .size(2.dp, 10.dp)
                                        .background(
                                            MaterialTheme.colorScheme.primary,
                                            RoundedCornerShape(1.dp)
                                        )
                                )
                                // 底边缘指示
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .size(8.dp, 2.dp)
                                        .background(
                                            MaterialTheme.colorScheme.primary,
                                            RoundedCornerShape(1.dp)
                                        )
                                )
                            }
                        }
                    },
                    value = totalSegments.toString(),
                    label = stringResource(R.string.edge_segment_count)
                )

                // 手势数统计
                StatItem(
                    icon = {
                        GestureWaveIcon(
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    value = totalGestures.toString(),
                    label = stringResource(R.string.gesture_count_label)
                )
            }
        }
        }
    }
}

/**
 * 手势波浪图标 - 匹配新版 UI 设计
 * SVG: circle(cx=4,cy=12,r=2) + cubic bezier wave
 */
@Composable
private fun GestureWaveIcon(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Canvas(modifier = modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        val strokeWidth = 1.8.dp.toPx()
        val halfH = h / 2f

        // 左侧圆点
        drawCircle(
            color = tint,
            radius = 2.dp.toPx(),
            center = Offset(5.dp.toPx(), halfH)
        )

        // 波浪路径 (cubic bezier)
        val path = Path().apply {
            val startX = 9.dp.toPx()
            val cp1x = 9.dp.toPx() + 2.dp.toPx()
            val cp2x = 9.dp.toPx() + 6.dp.toPx()
            val endX = 9.dp.toPx() + 8.dp.toPx()
            val amplitude = 6.dp.toPx()

            moveTo(startX, halfH)
            cubicTo(
                cp1x, halfH - amplitude,
                cp2x, halfH - amplitude,
                endX, halfH
            )
            // 第二段波浪 (smooth cubic)
            val cp3x = endX + 6.dp.toPx()
            val endX2 = endX + 8.dp.toPx()
            cubicTo(
                cp3x, halfH + amplitude,
                cp3x, halfH + amplitude,
                endX2, halfH
            )
        }

        drawPath(
            path = path,
            color = tint,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round
            )
        )
    }
}

@Composable
private fun StatItem(
    icon: @Composable () -> Unit,
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        icon()
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TriggerAreaSettingsCard(
    settings: GestureSettingsState,
    viewModel: GestureSettingsViewModel,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.95f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 卡片头部: 闪电图标 + "触发区设置"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "\u26A1",
                            fontSize = 16.sp
                        )
                    }
                    Text(
                        text = stringResource(R.string.trigger_area_settings_title),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // 隐藏显示
                GestureSettingsSwitchItem(
                    title = stringResource(R.string.gesture_hide_overlay_title),
                    subtitle = stringResource(R.string.gesture_hide_overlay_desc),
                    checked = settings.hideOverlay,
                    onCheckedChange = { hide ->
                        viewModel.setHideOverlay(hide)
                    }
                )

                // 隐藏后台
                GestureSettingsSwitchItem(
                    title = stringResource(R.string.gesture_hide_recents_title),
                    subtitle = stringResource(R.string.gesture_hide_recents_desc),
                    checked = settings.hideFromRecents,
                    onCheckedChange = { hide ->
                        viewModel.setHideFromRecents(hide)
                    }
                )

                // 避免遮挡
                GestureSettingsSwitchItem(
                    title = stringResource(R.string.gesture_avoid_keyboard_overlap_title),
                    subtitle = stringResource(R.string.gesture_avoid_keyboard_overlap_desc),
                    checked = settings.avoidKeyboardOverlap,
                    onCheckedChange = { enabled ->
                        viewModel.setAvoidKeyboardOverlap(enabled)
                    }
                )
            }
        }
    }
}

@Composable
private fun MoreGridCard(
    modifier: Modifier = Modifier,
    onExpandPanel: (() -> Unit)? = null,
    onBlacklist: (() -> Unit)? = null,
    onLaunchBlock: (() -> Unit)? = null,
    onSettings: (() -> Unit)? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.more_section_title),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, top = 20.dp, bottom = 10.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MoreGridItem(
                icon = {
                    Icon(
                        imageVector = Icons.Filled.GridView,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                label = stringResource(R.string.more_expand_panel),
                onClick = onExpandPanel,
                modifier = Modifier.weight(1f)
            )

            MoreGridItem(
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Block,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                label = stringResource(R.string.more_blacklist),
                onClick = onBlacklist,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MoreGridItem(
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Security,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                label = stringResource(R.string.more_launch_block),
                onClick = onLaunchBlock,
                modifier = Modifier.weight(1f)
            )

            MoreGridItem(
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                label = stringResource(R.string.more_settings),
                onClick = onSettings,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun MoreGridItem(
    icon: @Composable () -> Unit,
    label: String,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(16.dp)
            )
            .then(
                if (onClick != null) Modifier.clickable { onClick() }
                else Modifier
            )
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        icon()
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

const val REQUEST_OVERLAY = 1001
