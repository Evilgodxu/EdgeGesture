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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.BatteryFull
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PictureInPicture
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.edgegesture.evilgodxu.data.gesture.GestureStatsManager
import com.edgegesture.evilgodxu.data.gesture.StatsPeriod
import com.edgegesture.evilgodxu.data.permission.PermissionType
import com.edgegesture.evilgodxu.screens.gesture.compact.CompactAssembly
import com.edgegesture.evilgodxu.screens.gesture.compact.status_area.service_status_card.ServiceStatusCard
import com.edgegesture.evilgodxu.screens.gesture.expanded.ExpandedAssembly
import com.edgegesture.evilgodxu.screens.gesture.compact.top_area.TopArea
import com.edgegesture.evilgodxu.screens.gesture.overlay.StatsPeriodSelectionDialog
import com.edgegesture.evilgodxu.screens.gesture.compact.permission_area.PermissionGroupCard
import com.edgegesture.evilgodxu.screens.gesture.compact.permission_area.PermissionCard
import com.edgegesture.evilgodxu.screens.gesture.reuse.countNonNoneGestures
import com.edgegesture.evilgodxu.service.core.EdgeGestureAccessibilityService
import com.edgegesture.evilgodxu.ui.adaptive.rememberWindowSizeClass
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

// 手势设置主页 — 页面入口
@Composable
fun GestureSettingsScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToBlacklist: () -> Unit = {},
    onNavigateToLaunchBlock: () -> Unit = {},
    onNavigateToBackTap: () -> Unit = {},
    onNavigateToLeftEdge: () -> Unit = {},
    onNavigateToRightEdge: () -> Unit = {},
    onNavigateToBottomEdge: () -> Unit = {},
    onNavigateToExpandPanel: () -> Unit = {},
    viewModel: GestureSettingsViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings = uiState.settings
    val lifecycleOwner = LocalLifecycleOwner.current
    val windowSizeClass = rememberWindowSizeClass()

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    var waitingForSystemSetting by remember { mutableStateOf(false) }
    val currentWaitingState by rememberUpdatedState { waitingForSystemSetting }
    val setWaitingState = { value: Boolean -> waitingForSystemSetting = value }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted -> viewModel.setNotificationGranted(isGranted) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshAccessibilityState()
                viewModel.refreshPermissions()
                if (currentWaitingState()) setWaitingState(false)
                viewModel.stopPermissionMonitor()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.stopPermissionMonitor()
        }
    }

    Scaffold(
        topBar = { TopArea(onNavigateToSettings = onNavigateToSettings) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        val useTwoPaneLayout = windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)
        val modifier = Modifier.fillMaxSize().consumeWindowInsets(innerPadding).padding(innerPadding)

        val statusAreaContent: @Composable (Modifier) -> Unit = { areaModifier ->
            GestureSettingsStatusColumn(
                modifier = areaModifier,
                uiState = uiState,
                settings = settings,
                viewModel = viewModel,
                activity = activity,
                notificationPermissionLauncher = notificationPermissionLauncher,
            )
        }

        if (useTwoPaneLayout) {
            ExpandedAssembly(
                modifier = modifier,
                settings = settings,
                onNavigateToLeftEdge = onNavigateToLeftEdge,
                onNavigateToRightEdge = onNavigateToRightEdge,
                onNavigateToBottomEdge = onNavigateToBottomEdge,
                onNavigateToBackTap = onNavigateToBackTap,
                onNavigateToSettings = onNavigateToSettings,
                onNavigateToBlacklist = onNavigateToBlacklist,
                onNavigateToLaunchBlock = onNavigateToLaunchBlock,
                onNavigateToExpandPanel = onNavigateToExpandPanel,
                onHideOverlayChange = { viewModel.setHideOverlay(it) },
                onHideFromRecentsChange = { viewModel.setHideFromRecents(it) },
                onAvoidKeyboardOverlapChange = { viewModel.setAvoidKeyboardOverlap(it) },
                onVibrationChange = { viewModel.setVibrationEnabled(it) },
                onDoubleSwipeChange = { viewModel.setDoubleSwipeEnabled(it) },
                statusArea = statusAreaContent,
            )
        } else {
            CompactAssembly(
                modifier = modifier,
                settings = settings,
                onNavigateToLeftEdge = onNavigateToLeftEdge,
                onNavigateToRightEdge = onNavigateToRightEdge,
                onNavigateToBottomEdge = onNavigateToBottomEdge,
                onNavigateToBackTap = onNavigateToBackTap,
                onNavigateToSettings = onNavigateToSettings,
                onNavigateToBlacklist = onNavigateToBlacklist,
                onNavigateToLaunchBlock = onNavigateToLaunchBlock,
                onNavigateToExpandPanel = onNavigateToExpandPanel,
                onHideOverlayChange = { viewModel.setHideOverlay(it) },
                onHideFromRecentsChange = { viewModel.setHideFromRecents(it) },
                onAvoidKeyboardOverlapChange = { viewModel.setAvoidKeyboardOverlap(it) },
                onVibrationChange = { viewModel.setVibrationEnabled(it) },
                onDoubleSwipeChange = { viewModel.setDoubleSwipeEnabled(it) },
                statusArea = statusAreaContent,
            )
        }
    }
}

// 状态与权限列 — 服务状态卡片 + 权限列表
@Composable
private fun GestureSettingsStatusColumn(
    modifier: Modifier = Modifier,
    uiState: GestureSettingsUiState,
    settings: com.edgegesture.evilgodxu.data.gesture.GestureSettingsState,
    viewModel: GestureSettingsViewModel,
    activity: Activity?,
    notificationPermissionLauncher: androidx.activity.result.ActivityResultLauncher<String>,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val stats by GestureStatsManager.stats.collectAsState()
    val statsPeriod by GestureStatsManager.period.collectAsState()
    var showStatsPeriodDialog by remember { mutableStateOf(false) }

    if (showStatsPeriodDialog) {
        StatsPeriodSelectionDialog(
            currentPeriod = statsPeriod,
            onDismiss = { showStatsPeriodDialog = false },
            onPeriodSelected = { period ->
                scope.launch { GestureStatsManager.setPeriod(context, period) }
                showStatsPeriodDialog = false
            }
        )
    }

    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.gesture_service_title),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 10.dp)
        )
        ServiceStatusCard(
            enabled = settings.gestureEnabled,
            isAccessibilityEnabled = uiState.isAccessibilityEnabled,
            totalSegments = settings.leftSegmentCount + settings.rightSegmentCount + settings.bottomSegmentCount,
            totalGestures = countNonNoneGestures(settings),
            stats = stats,
            statsPeriod = statsPeriod,
            onToggleService = { enable ->
                if (enable && !uiState.isAccessibilityEnabled) {
                    if (activity != null) viewModel.startPermissionMonitor(PermissionType.ACCESSIBILITY, activity)
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    return@ServiceStatusCard
                }
                viewModel.setGestureEnabled(enable)
                if (enable) EdgeGestureAccessibilityService.startGesture(context)
                else EdgeGestureAccessibilityService.stopGesture(context)
            },
            onLongPressStats = { showStatsPeriodDialog = true }
        )

        AnimatedVisibility(
            visible = !uiState.isAccessibilityEnabled || !uiState.allPermissionsGranted,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column {
                Text(
                    text = stringResource(R.string.permission_status_title),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, top = 20.dp, bottom = 10.dp)
                )

                PermissionGroupCard(modifier = Modifier.padding(vertical = 4.dp)) {
                    PermissionCard(
                        title = stringResource(R.string.permission_accessibility_title),
                        description = stringResource(R.string.permission_accessibility_desc),
                        granted = uiState.isAccessibilityEnabled,
                        onRequest = {
                            if (activity != null) viewModel.startPermissionMonitor(PermissionType.ACCESSIBILITY, activity)
                            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        },
                        icon = { Icon(imageVector = Icons.Outlined.Security, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary) }
                    )
                    PermissionCard(
                        title = stringResource(R.string.permission_overlay_title),
                        description = stringResource(R.string.permission_overlay_desc),
                        granted = uiState.overlayGranted,
                        onRequest = {
                            if (activity != null) viewModel.startPermissionMonitor(PermissionType.OVERLAY, activity)
                            activity?.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:${context.packageName}".toUri()))
                        },
                        icon = { Icon(imageVector = Icons.Outlined.PictureInPicture, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary) }
                    )
                    PermissionCard(
                        title = stringResource(R.string.permission_notification_title),
                        description = stringResource(R.string.permission_notification_desc),
                        granted = uiState.notificationGranted,
                        onRequest = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        },
                        icon = { Icon(imageVector = Icons.Outlined.Notifications, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary) }
                    )
                    PermissionCard(
                        title = stringResource(R.string.permission_battery_title),
                        description = stringResource(R.string.permission_battery_desc),
                        granted = uiState.batteryOptimized,
                        onRequest = {
                            if (activity != null) viewModel.startPermissionMonitor(PermissionType.BATTERY_OPTIMIZATION, activity)
                            activity?.startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply { data = "package:${context.packageName}".toUri() })
                        },
                        icon = { Icon(imageVector = Icons.Outlined.BatteryFull, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary) }
                    )
                    PermissionCard(
                        title = stringResource(R.string.permission_usage_stats_title),
                        description = stringResource(R.string.permission_usage_stats_desc),
                        granted = uiState.usageStatsGranted,
                        onRequest = {
                            if (activity != null) viewModel.startPermissionMonitor(PermissionType.USAGE_STATS, activity)
                            activity?.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply { data = "package:${context.packageName}".toUri() })
                        },
                        icon = { Icon(imageVector = Icons.Outlined.BarChart, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary) }
                    )
                    PermissionCard(
                        title = stringResource(R.string.permission_query_packages_title),
                        description = stringResource(R.string.permission_query_packages_desc),
                        granted = uiState.queryAllPackagesGranted,
                        onRequest = {
                            if (activity != null) viewModel.startPermissionMonitor(PermissionType.QUERY_ALL_PACKAGES, activity)
                            activity?.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply { data = "package:${context.packageName}".toUri() })
                        },
                        icon = { Icon(imageVector = Icons.Outlined.Android, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary) }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
