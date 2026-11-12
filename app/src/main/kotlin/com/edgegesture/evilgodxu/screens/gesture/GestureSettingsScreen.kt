package com.edgegesture.evilgodxu.screens.gesture

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.edgegesture.evilgodxu.data.permission.PermissionType
import com.edgegesture.evilgodxu.screens.gesture.assembly.GestureSettingsAssembly
import com.edgegesture.evilgodxu.screens.gesture.component.header.GestureSettingsHeader
import com.edgegesture.evilgodxu.screens.gesture.service.EdgeGestureAccessibilityService
import com.edgegesture.evilgodxu.ui.adaptive.currentWindowSizeClass

// 页面入口：只做状态连接、权限判定与副作用，布局交给组装器
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
    viewModel: GestureSettingsViewModel = viewModel(),
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings = uiState.settings
    val lifecycleOwner = LocalLifecycleOwner.current
    val windowSizeClass = currentWindowSizeClass()

    if (uiState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    // 通知权限申请 launcher
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        viewModel.setNotificationGranted(isGranted)
    }

    // 监听生命周期，用户从系统设置返回时刷新权限状态
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshAccessibilityState()
                viewModel.refreshPermissions()
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
        topBar = {
            GestureSettingsHeader(onNavigateToSettings = onNavigateToSettings)
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        GestureSettingsAssembly(
            uiState = uiState,
            settings = settings,
            windowSizeClass = windowSizeClass,
            onToggleService = { enable ->
                if (enable && !uiState.isAccessibilityEnabled) {
                    // 未授权无障碍：跳转系统设置并开始监控授权结果
                    activity?.let { viewModel.startPermissionMonitor(PermissionType.ACCESSIBILITY, it) }
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                } else {
                    viewModel.setGestureEnabled(enable)
                    if (enable) {
                        EdgeGestureAccessibilityService.startGesture(context)
                    } else {
                        EdgeGestureAccessibilityService.stopGesture(context)
                    }
                }
            },
            onRequestAccessibility = {
                activity?.let { viewModel.startPermissionMonitor(PermissionType.ACCESSIBILITY, it) }
                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            },
            onRequestOverlay = {
                activity?.let {
                    viewModel.startPermissionMonitor(PermissionType.OVERLAY, it)
                    it.startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            "package:${context.packageName}".toUri(),
                        ),
                    )
                }
            },
            onRequestNotification = {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            },
            onRequestBattery = {
                activity?.let {
                    viewModel.startPermissionMonitor(PermissionType.BATTERY_OPTIMIZATION, it)
                    it.startActivity(
                        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = "package:${context.packageName}".toUri()
                        },
                    )
                }
            },
            onRequestQueryPackages = {
                activity?.let {
                    viewModel.startPermissionMonitor(PermissionType.QUERY_ALL_PACKAGES, it)
                    it.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = "package:${context.packageName}".toUri()
                        },
                    )
                }
            },
            onNavigateToSettings = onNavigateToSettings,
            onNavigateToBlacklist = onNavigateToBlacklist,
            onNavigateToLaunchBlock = onNavigateToLaunchBlock,
            onNavigateToBackTap = onNavigateToBackTap,
            onNavigateToLeftEdge = onNavigateToLeftEdge,
            onNavigateToRightEdge = onNavigateToRightEdge,
            onNavigateToBottomEdge = onNavigateToBottomEdge,
            onNavigateToExpandPanel = onNavigateToExpandPanel,
            onHideOverlayChange = viewModel::setHideOverlay,
            onHideFromRecentsChange = viewModel::setHideFromRecents,
            onAvoidKeyboardOverlapChange = viewModel::setAvoidKeyboardOverlap,
            onVibrationEnabledChange = viewModel::setVibrationEnabled,
            onDoubleSwipeEnabledChange = viewModel::setDoubleSwipeEnabled,
            modifier = Modifier
                .fillMaxSize()
                .consumeWindowInsets(innerPadding)
                .padding(innerPadding),
        )
    }
}
