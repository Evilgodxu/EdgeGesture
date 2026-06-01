package com.byss.jh.feature.privacy

import android.Manifest
import android.app.Application
import android.app.AppOpsManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// 隐私协议页面 ViewModel，管理权限检查和请求状态
class PrivacyViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val context get() = getApplication<Application>().applicationContext

    private val _uiState = MutableStateFlow(PrivacyUiState(isLoading = true))
    val uiState: StateFlow<PrivacyUiState> = _uiState.asStateFlow()

    init {
        refreshPermissions()
    }

    // 刷新所有权限状态
    fun refreshPermissions() {
        val overlay = Settings.canDrawOverlays(context)
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        val battery = isIgnoringBatteryOptimizations(context)
        val usageStats = hasUsageStatsPermission(context)
        val queryAllPackages = hasQueryAllPackagesPermission(context)

        _uiState.value = PrivacyUiState(
            isLoading = false,
            overlayGranted = overlay,
            notificationGranted = notification,
            batteryOptimized = battery,
            usageStatsGranted = usageStats,
            queryAllPackagesGranted = queryAllPackages,
        )
    }

    // 设置通知权限状态
    fun setNotificationGranted(granted: Boolean) {
        _uiState.value = _uiState.value.copy(notificationGranted = granted)
    }

    // 检查是否忽略电池优化
    private fun isIgnoringBatteryOptimizations(ctx: android.content.Context): Boolean {
        val powerManager = ctx.getSystemService(android.content.Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(ctx.packageName)
    }

    // 检查是否有使用情况统计权限
    private fun hasUsageStatsPermission(ctx: android.content.Context): Boolean {
        val appOps = ctx.getSystemService(android.content.Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            ctx.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    // 检查是否有查询所有应用权限
    private fun hasQueryAllPackagesPermission(ctx: android.content.Context): Boolean {
        return try {
            val pm = ctx.packageManager
            val apps = pm.getInstalledApplications(0)
            apps.isNotEmpty() && apps.any { it.packageName != ctx.packageName }
        } catch (e: Exception) {
            false
        }
    }
}
