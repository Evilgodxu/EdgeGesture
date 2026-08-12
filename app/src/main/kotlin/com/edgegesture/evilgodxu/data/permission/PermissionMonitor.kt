package com.edgegesture.evilgodxu.data.permission

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.edgegesture.evilgodxu.log.CrashLogManager
import com.edgegesture.evilgodxu.screens.gesture.service.EdgeGestureAccessibilityService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

// 需要监控的权限类型
enum class PermissionType {
    OVERLAY,
    NOTIFICATION,
    BATTERY_OPTIMIZATION,
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
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    // 检查电池优化权限
    fun isBatteryOptimizationIgnored(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    // 检查查询所有应用权限
    fun isQueryAllPackagesGranted(): Boolean {
        return try {
            val pm = context.packageManager
            val apps = pm.getInstalledApplications(0)
            apps.isNotEmpty() && apps.any { it.packageName != context.packageName }
        } catch (e: Exception) {
            CrashLogManager.logException("PermissionMonitor", "获取已安装应用列表失败", e)
            false
        }
    }

    // 检查无障碍服务权限
    fun isAccessibilityGranted(): Boolean {
        // 服务实例存活说明无障碍服务正在运行，这是权限已生效的直接证据；
        // 系统设置串在个别机型上可能读取不到或滞后，不能作为唯一判断依据
        if (EdgeGestureAccessibilityService.isAvailable()) return true
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        // 按组件全名精确匹配，避免包名前缀相同的服务被误判
        val expected = ComponentName(
            context,
            EdgeGestureAccessibilityService::class.java
        ).flattenToString()
        return enabledServices.split(':').any { it.equals(expected, ignoreCase = true) }
    }

    // 检查修改系统设置权限
    fun isWriteSettingsGranted(): Boolean = Settings.System.canWrite(context)

    // 检查指定权限是否已授权
    fun isGranted(permissionType: PermissionType): Boolean {
        return when (permissionType) {
            PermissionType.OVERLAY -> isOverlayGranted()
            PermissionType.NOTIFICATION -> isNotificationGranted()
            PermissionType.BATTERY_OPTIMIZATION -> isBatteryOptimizationIgnored()
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
    }.flowOn(Dispatchers.IO)

    // 同时监控多个权限
    fun monitorPermissions(permissionTypes: List<PermissionType>, intervalMs: Long = 500): Flow<Map<PermissionType, Boolean>> = flow {
        while (true) {
            val result = permissionTypes.associateWith { isGranted(it) }
            emit(result)
            if (result.all { it.value }) break
            delay(intervalMs)
        }
    }.flowOn(Dispatchers.IO)
}
