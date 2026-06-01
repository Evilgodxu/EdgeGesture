package com.byss.jh.ui.privacy

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
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
import com.byss.jh.R
import com.byss.jh.ui.adaptive.rememberWindowSizeClass
import com.byss.jh.ui.privacy.components.PermissionStatusCard
import com.byss.jh.ui.privacy.components.PrivacySection
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyScreen(
    onAgree: () -> Unit,
    viewModel: PrivacyViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val windowSizeClass = rememberWindowSizeClass()

    // 等待权限检查完成时显示加载指示器
    if (uiState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.setNotificationGranted(isGranted)
    }

    // 监听生命周期，用户从系统权限设置返回时刷新权限状态
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissions()
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
                title = { Text(stringResource(R.string.privacy_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                windowInsets = topBarInsets,
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .consumeWindowInsets(innerPadding)
            .padding(innerPadding)
            .padding(16.dp)

        if (windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)) {
            // 宽屏设备使用双列布局，左侧显示隐私政策，右侧显示权限状态
            PrivacyScreenExpanded(
                modifier = contentModifier,
                uiState = uiState,
                onAgree = onAgree,
                onRequestOverlay = {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        "package:${context.packageName}".toUri()
                    )
                    activity?.startActivityForResult(intent, REQUEST_OVERLAY)
                },
                onRequestNotification = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
                onRequestBatteryOptimization = {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = "package:${context.packageName}".toUri()
                    }
                    activity?.startActivity(intent)
                },
                onRequestUsageStats = {
                    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                        data = "package:${context.packageName}".toUri()
                    }
                    activity?.startActivity(intent)
                },
                onRequestQueryAllPackages = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = "package:${context.packageName}".toUri()
                    }
                    activity?.startActivity(intent)
                }
            )
        } else {
            // 窄屏设备使用单列布局，隐私政策和权限状态上下排列
            PrivacyScreenCompact(
                modifier = contentModifier,
                uiState = uiState,
                onAgree = onAgree,
                onRequestOverlay = {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        "package:${context.packageName}".toUri()
                    )
                    activity?.startActivityForResult(intent, REQUEST_OVERLAY)
                },
                onRequestNotification = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
                onRequestBatteryOptimization = {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = "package:${context.packageName}".toUri()
                    }
                    activity?.startActivity(intent)
                },
                onRequestUsageStats = {
                    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                        data = "package:${context.packageName}".toUri()
                    }
                    activity?.startActivity(intent)
                },
                onRequestQueryAllPackages = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = "package:${context.packageName}".toUri()
                    }
                    activity?.startActivity(intent)
                }
            )
        }
    }
}

@Composable
private fun PrivacyScreenCompact(
    modifier: Modifier = Modifier,
    uiState: PrivacyUiState,
    onAgree: () -> Unit,
    onRequestOverlay: () -> Unit,
    onRequestNotification: () -> Unit,
    onRequestBatteryOptimization: () -> Unit,
    onRequestUsageStats: () -> Unit,
    onRequestQueryAllPackages: () -> Unit,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState())
    ) {
        Text(
            text = stringResource(R.string.privacy_statement_title),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(16.dp))

        PrivacySection(
            title = stringResource(R.string.privacy_section_1_title),
            content = stringResource(R.string.privacy_section_1_content)
        )

        PrivacySection(
            title = stringResource(R.string.privacy_section_2_title),
            content = stringResource(R.string.privacy_section_2_content)
        )

        PrivacySection(
            title = stringResource(R.string.privacy_section_3_title),
            content = stringResource(R.string.privacy_section_3_content)
        )

        PrivacySection(
            title = stringResource(R.string.privacy_section_4_title),
            content = stringResource(R.string.privacy_section_4_content)
        )

        PrivacySection(
            title = stringResource(R.string.privacy_section_5_title),
            content = stringResource(R.string.privacy_section_5_content)
        )

        PrivacySection(
            title = stringResource(R.string.privacy_section_6_title),
            content = stringResource(R.string.privacy_section_6_content)
        )

        Spacer(modifier = Modifier.height(24.dp))

        PermissionStatusCard(
            overlayGranted = uiState.overlayGranted,
            notificationGranted = uiState.notificationGranted,
            batteryOptimized = uiState.batteryOptimized,
            usageStatsGranted = uiState.usageStatsGranted,
            queryAllPackagesGranted = uiState.queryAllPackagesGranted,
            onRequestOverlay = onRequestOverlay,
            onRequestNotification = onRequestNotification,
            onRequestBatteryOptimization = onRequestBatteryOptimization,
            onRequestUsageStats = onRequestUsageStats,
            onRequestQueryAllPackages = onRequestQueryAllPackages
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onAgree,
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState.allPermissionsGranted
        ) {
            Text(stringResource(R.string.privacy_agree_button))
        }
    }
}

@Composable
private fun PrivacyScreenExpanded(
    modifier: Modifier = Modifier,
    uiState: PrivacyUiState,
    onAgree: () -> Unit,
    onRequestOverlay: () -> Unit,
    onRequestNotification: () -> Unit,
    onRequestBatteryOptimization: () -> Unit,
    onRequestUsageStats: () -> Unit,
    onRequestQueryAllPackages: () -> Unit,
) {
    Row(
        modifier = modifier.verticalScroll(rememberScrollState())
    ) {
        // 左侧列：隐私政策内容
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 24.dp)
        ) {
            Text(
                text = stringResource(R.string.privacy_statement_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(16.dp))

            PrivacySection(
                title = stringResource(R.string.privacy_section_1_title),
                content = stringResource(R.string.privacy_section_1_content)
            )

            PrivacySection(
                title = stringResource(R.string.privacy_section_2_title),
                content = stringResource(R.string.privacy_section_2_content)
            )

            PrivacySection(
                title = stringResource(R.string.privacy_section_3_title),
                content = stringResource(R.string.privacy_section_3_content)
            )

            PrivacySection(
                title = stringResource(R.string.privacy_section_4_title),
                content = stringResource(R.string.privacy_section_4_content)
            )

            PrivacySection(
                title = stringResource(R.string.privacy_section_5_title),
                content = stringResource(R.string.privacy_section_5_content)
            )

            PrivacySection(
                title = stringResource(R.string.privacy_section_6_title),
                content = stringResource(R.string.privacy_section_6_content)
            )
        }

        // 右侧列：权限状态和同意按钮
        Column(
            modifier = Modifier.weight(1f)
        ) {
            PermissionStatusCard(
                overlayGranted = uiState.overlayGranted,
                notificationGranted = uiState.notificationGranted,
                batteryOptimized = uiState.batteryOptimized,
                usageStatsGranted = uiState.usageStatsGranted,
                queryAllPackagesGranted = uiState.queryAllPackagesGranted,
                onRequestOverlay = onRequestOverlay,
                onRequestNotification = onRequestNotification,
                onRequestBatteryOptimization = onRequestBatteryOptimization,
                onRequestUsageStats = onRequestUsageStats,
                onRequestQueryAllPackages = onRequestQueryAllPackages
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onAgree,
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.allPermissionsGranted
            ) {
                Text(stringResource(R.string.privacy_agree_button))
            }
        }
    }
}

const val REQUEST_OVERLAY = 1001
