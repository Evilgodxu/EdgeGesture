package com.byss.jh.screens.gesture.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.byss.jh.R

// 权限状态卡片组件
@Composable
fun PermissionStatusCard(
    modifier: Modifier = Modifier,
    overlayGranted: Boolean,
    notificationGranted: Boolean,
    batteryOptimized: Boolean,
    usageStatsGranted: Boolean,
    queryAllPackagesGranted: Boolean,
    onRequestOverlay: () -> Unit,
    onRequestNotification: () -> Unit,
    onRequestBatteryOptimization: () -> Unit,
    onRequestUsageStats: () -> Unit,
    onRequestQueryAllPackages: () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.permission_status_title),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            PermissionItem(
                name = stringResource(R.string.permission_overlay_title),
                desc = stringResource(R.string.permission_overlay_desc),
                granted = overlayGranted,
                onClick = onRequestOverlay
            )
            PermissionItem(
                name = stringResource(R.string.permission_notification_title),
                desc = stringResource(R.string.permission_notification_desc),
                granted = notificationGranted,
                onClick = onRequestNotification
            )
            PermissionItem(
                name = stringResource(R.string.permission_battery_title),
                desc = stringResource(R.string.permission_battery_desc),
                granted = batteryOptimized,
                onClick = onRequestBatteryOptimization
            )
            PermissionItem(
                name = stringResource(R.string.permission_usage_stats_title),
                desc = stringResource(R.string.permission_usage_stats_desc),
                granted = usageStatsGranted,
                onClick = onRequestUsageStats
            )
            PermissionItem(
                name = stringResource(R.string.permission_query_packages_title),
                desc = stringResource(R.string.permission_query_packages_desc),
                granted = queryAllPackagesGranted,
                onClick = onRequestQueryAllPackages
            )
        }
    }
}
