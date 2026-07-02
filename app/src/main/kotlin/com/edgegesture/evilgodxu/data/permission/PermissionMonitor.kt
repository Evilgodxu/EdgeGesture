package com.edgegesture.evilgodxu.data.permission

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

// 需要监控的权限类型
enum class PermissionType {
    OVERLAY,
    NOTIFICATION,
    BATTERY_OPTIMIZATION,
    USAGE_STATS,
    QUERY_ALL_PACKAGES,
    ACCESSIBILITY,
    WRITE_SETTINGS
}

// 权限监控管理器
class PermissionMonitor(private val context: Context) {

    // 检查悬浮窗权限
    fun isOverlayGranted(): Boolean = Settings.canDrawOverlays(context)

    // 检查通知权限
    fun isNotificationGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    // 检查电池优化权限
    fun isBatteryOptimizationIgnored(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    // 检查使用情况统计权限
    fun isUsageStatsGranted(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    // 检查查询所有应用权限
    fun isQueryAllPackagesGranted(): Boolean {
        return try {
            val pm = context.packageManager
            val apps = pm.getInstalledApplications(0)
            apps.isNotEmpty() && apps.any { it.packageName != context.packageName }
        } catch (e: Exception) {
            false
        }
    }

    // 检查无障碍服务权限
    fun isAccessibilityGranted(): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val packageName = context.packageName
        return enabledServices.contains(packageName)
    }

    // 检查修改系统设置权限
    fun isWriteSettingsGranted(): Boolean = Settings.System.canWrite(context)

    // 检查指定权限是否已授权
    fun isGranted(permissionType: PermissionType): Boolean {
        return when (permissionType) {
            PermissionType.OVERLAY -> isOverlayGranted()
            PermissionType.NOTIFICATION -> isNotificationGranted()
            PermissionType.BATTERY_OPTIMIZATION -> isBatteryOptimizationIgnored()
            PermissionType.USAGE_STATS -> isUsageStatsGranted()
            PermissionType.QUERY_ALL_PACKAGES -> isQueryAllPackagesGranted()
            PermissionType.ACCESSIBILITY -> isAccessibilityGranted()
            PermissionType.WRITE_SETTINGS -> isWriteSettingsGranted()
        }
    }

    // 持续监控指定权限，直到授权后返回true
    // intervalMs: 检测间隔，默认500ms
    fun monitorPermission(permissionType: PermissionType, intervalMs: Long = 500): Flow<Boolean> = flow {
        while (true) {
            val granted = isGranted(permissionType)
            emit(granted)
            if (granted) break
            delay(intervalMs)
        }
    }

    // 同时监控多个权限
    fun monitorPermissions(permissionTypes: List<PermissionType>, intervalMs: Long = 500): Flow<Map<PermissionType, Boolean>> = flow {
        while (true) {
            val result = permissionTypes.associateWith { isGranted(it) }
            emit(result)
            if (result.all { it.value }) break
            delay(intervalMs)
        }
    }
}
