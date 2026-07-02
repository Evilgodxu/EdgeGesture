package com.edgegesture.evilgodxu.screens.gesture

import com.edgegesture.evilgodxu.data.gesture.GestureSettingsState
import com.edgegesture.evilgodxu.data.permission.PermissionType

// 手势设置页面 UI 状态
data class GestureSettingsUiState(
    val isLoading: Boolean = true,
    val settings: GestureSettingsState = GestureSettingsState(),
    val isAccessibilityEnabled: Boolean = false,
    // 权限状态
    val overlayGranted: Boolean = false,
    val notificationGranted: Boolean = false,
    val batteryOptimized: Boolean = false,
    val usageStatsGranted: Boolean = false,
    val queryAllPackagesGranted: Boolean = false,
    // 正在等待的权限类型，用于自动返回检测
    val waitingPermission: PermissionType? = null,
) {
    // 检查所有权限是否已授予
    val allPermissionsGranted: Boolean
        get() = overlayGranted && notificationGranted && batteryOptimized &&
                usageStatsGranted && queryAllPackagesGranted
}
