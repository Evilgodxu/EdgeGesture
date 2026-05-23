package com.byss.jh.ui.gesture

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass
import com.byss.jh.R
import com.byss.jh.data.gesture.GestureAction
import com.byss.jh.data.gesture.GestureSettingsState
import com.byss.jh.data.gesture.GestureSettingsKeys
import com.byss.jh.ui.gesture.service.EdgeGestureAccessibilityService
import com.byss.jh.ui.adaptive.rememberWindowSizeClass
import com.byss.jh.ui.gesture.components.ActionSelectionDialog
import com.byss.jh.ui.gesture.components.BottomEdgeSettingsSection
import com.byss.jh.ui.gesture.components.EdgeGestureSection
import com.byss.jh.ui.gesture.components.EdgeSettingsSection
import com.byss.jh.ui.gesture.components.GestureSettingsSwitchItem
import com.byss.jh.ui.gesture.components.getActionDisplayName
import com.byss.jh.util.Logger
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestureSettingsScreen(
    onNavigateToSettings: () -> Unit,
    viewModel: GestureSettingsViewModel = koinViewModel(),
) {
    val context = LocalContext.current
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

    var showAccessibilityDialog by remember { mutableStateOf(false) }
    var waitingForSystemSetting by remember { mutableStateOf(false) }

    // rememberUpdatedState 确保在 DisposableEffect 中也能获取到最新的 waitingForSystemSetting 值
    val currentWaitingState by rememberUpdatedState { waitingForSystemSetting }
    val setWaitingState = { value: Boolean ->
        waitingForSystemSetting = value
    }

    // 监听生命周期，用户从系统设置返回时刷新无障碍权限状态
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // 用户可能刚从无障碍设置页返回，需要刷新权限状态
                viewModel.refreshAccessibilityState()
                if (currentWaitingState()) {
                    setWaitingState(false)
                    Logger.i(context, "GestureSettings", "从系统设置返回，刷新无障碍权限状态")
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val topBarInsets = if (!windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)) {
        WindowInsets.statusBars
    } else {
        WindowInsets(0, 0, 0, 0)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.gesture_settings_title)) },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings_title),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                windowInsets = topBarInsets,
            )
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
            onShowAccessibilityDialog = { showAccessibilityDialog = true },
            onShowActionDialog = { key, action ->
                currentActionKey = key
                currentActionValue = action
                showActionDialog = true
            }
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
                Logger.i(context, "GestureSettings", "设置手势: $key -> ${action.displayName}")
                showActionDialog = false
            },
            getActionDisplayName = { getActionDisplayName(it) }
        )
    }

    // 无障碍权限对话框
    if (showAccessibilityDialog) {
        AlertDialog(
            onDismissRequest = { showAccessibilityDialog = false },
            title = {
                Text(
                    stringResource(R.string.accessibility_dialog_title),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    stringResource(R.string.accessibility_dialog_message),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        waitingForSystemSetting = true
                        Logger.i(context, "GestureSettings", "打开无障碍设置，标记等待状态")
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        context.startActivity(intent)
                        showAccessibilityDialog = false
                    }
                ) {
                    Text(stringResource(R.string.accessibility_go_settings))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAccessibilityDialog = false }
                ) {
                    Text(stringResource(R.string.accessibility_cancel))
                }
            }
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
    onShowAccessibilityDialog: () -> Unit,
    onShowActionDialog: (androidx.datastore.preferences.core.Preferences.Key<String>, GestureAction) -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = modifier.verticalScroll(rememberScrollState())
    ) {
        // 无障碍权限未开启时显示警告卡片
        if (!uiState.isAccessibilityEnabled) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clickable { onShowAccessibilityDialog() },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.accessibility_permission_required),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.accessibility_permission_desc),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // 边缘手势功能总开关，依赖无障碍权限
        GestureSettingsSwitchItem(
            title = stringResource(R.string.gesture_enable_title),
            subtitle = stringResource(R.string.gesture_enable_desc),
            checked = settings.gestureEnabled,
            onCheckedChange = { enabled ->
                if (enabled && !uiState.isAccessibilityEnabled) {
                    onShowAccessibilityDialog()
                    return@GestureSettingsSwitchItem
                }
                viewModel.setGestureEnabled(enabled)
                Logger.i(context, "GestureSettings", "边缘手势: $enabled")
                if (enabled) {
                    EdgeGestureAccessibilityService.startService(context)
                } else {
                    EdgeGestureAccessibilityService.stopService(context)
                }
            }
        )

        AnimatedVisibility(visible = settings.gestureEnabled) {
            Column {
                // 隐藏边缘触摸区域可视化反馈
                GestureSettingsSwitchItem(
                    title = stringResource(R.string.gesture_hide_overlay_title),
                    subtitle = stringResource(R.string.gesture_hide_overlay_desc),
                    checked = settings.hideOverlay,
                    onCheckedChange = { hide ->
                        viewModel.setHideOverlay(hide)
                        Logger.i(context, "GestureSettings", "隐藏显示: $hide")
                    }
                )

                // 从最近任务列表中隐藏本应用
                GestureSettingsSwitchItem(
                    title = stringResource(R.string.gesture_hide_recents_title),
                    subtitle = stringResource(R.string.gesture_hide_recents_desc),
                    checked = settings.hideFromRecents,
                    onCheckedChange = { hide ->
                        viewModel.setHideFromRecents(hide)
                        Logger.i(context, "GestureSettings", "隐藏后台: $hide")
                    }
                )

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
            }
        }
    }
}
