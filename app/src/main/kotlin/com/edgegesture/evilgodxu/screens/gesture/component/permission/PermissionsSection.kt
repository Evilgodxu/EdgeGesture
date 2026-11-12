package com.edgegesture.evilgodxu.screens.gesture.component.permission

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.BatteryFull
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PictureInPicture
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edgegesture.evilgodxu.R
import com.edgegesture.evilgodxu.screens.gesture.GestureSettingsUiState
import com.edgegesture.evilgodxu.screens.gesture.component.permission.PermissionCard
import com.edgegesture.evilgodxu.screens.gesture.component.permission.PermissionGroupCard

// 权限状态区块：全部授权后自动隐藏，各条目点击交给宿主申请系统权限
@Composable
fun PermissionsSection(
    uiState: GestureSettingsUiState,
    onRequestAccessibility: () -> Unit,
    onRequestOverlay: () -> Unit,
    onRequestNotification: () -> Unit,
    onRequestBattery: () -> Unit,
    onRequestQueryPackages: () -> Unit,
) {
    AnimatedVisibility(
        visible = !uiState.isAccessibilityEnabled || !uiState.allPermissionsGranted,
        enter = expandVertically(),
        exit = shrinkVertically(),
    ) {
        Column {
            Text(
                text = stringResource(R.string.permission_status_title),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.2.sp,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 20.dp, bottom = 10.dp),
            )

            PermissionGroupCard(modifier = Modifier.padding(vertical = 4.dp)) {
                PermissionCard(
                    title = stringResource(R.string.permission_accessibility_title),
                    description = stringResource(R.string.permission_accessibility_desc),
                    granted = uiState.isAccessibilityEnabled,
                    onRequest = onRequestAccessibility,
                    icon = { PermissionIcon(Icons.Outlined.Security) },
                )
                PermissionCard(
                    title = stringResource(R.string.permission_overlay_title),
                    description = stringResource(R.string.permission_overlay_desc),
                    granted = uiState.overlayGranted,
                    onRequest = onRequestOverlay,
                    icon = { PermissionIcon(Icons.Outlined.PictureInPicture) },
                )
                PermissionCard(
                    title = stringResource(R.string.permission_notification_title),
                    description = stringResource(R.string.permission_notification_desc),
                    granted = uiState.notificationGranted,
                    onRequest = onRequestNotification,
                    icon = { PermissionIcon(Icons.Outlined.Notifications) },
                )
                PermissionCard(
                    title = stringResource(R.string.permission_battery_title),
                    description = stringResource(R.string.permission_battery_desc),
                    granted = uiState.batteryOptimized,
                    onRequest = onRequestBattery,
                    icon = { PermissionIcon(Icons.Outlined.BatteryFull) },
                )
                PermissionCard(
                    title = stringResource(R.string.permission_query_packages_title),
                    description = stringResource(R.string.permission_query_packages_desc),
                    granted = uiState.queryAllPackagesGranted,
                    onRequest = onRequestQueryPackages,
                    icon = { PermissionIcon(Icons.Outlined.Android) },
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// 权限条目统一图标样式
@Composable
private fun PermissionIcon(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(18.dp),
        tint = MaterialTheme.colorScheme.primary,
    )
}
