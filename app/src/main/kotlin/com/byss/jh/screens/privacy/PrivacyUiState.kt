package com.byss.jh.screens.privacy

// 隐私协议页面 UI 状态
data class PrivacyUiState(
    val isLoading: Boolean = true,
    val overlayGranted: Boolean = false,
    val notificationGranted: Boolean = false,
    val batteryOptimized: Boolean = false,
    val usageStatsGranted: Boolean = false,
    val queryAllPackagesGranted: Boolean = false,
) {
    // 检查所有权限是否已授予
    val allPermissionsGranted: Boolean
        get() = overlayGranted && notificationGranted && batteryOptimized &&
                usageStatsGranted && queryAllPackagesGranted
}
