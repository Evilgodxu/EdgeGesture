package com.edgegesture.evilgodxu.screens.gesture

import android.Manifest
import android.app.Activity
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
import com.edgegesture.evilgodxu.data.gesture.BackTapMode
import com.edgegesture.evilgodxu.data.gesture.GestureAction
import com.edgegesture.evilgodxu.data.gesture.GestureSettingsState
import com.edgegesture.evilgodxu.data.gesture.gestureSettingsFlow
import com.edgegesture.evilgodxu.data.gesture.saveBottomEdgeGesture
import com.edgegesture.evilgodxu.data.gesture.saveBottomEdgeHeight
import com.edgegesture.evilgodxu.data.gesture.saveBottomEdgeWidthPercent
import com.edgegesture.evilgodxu.data.gesture.saveBottomSegmentCount
import com.edgegesture.evilgodxu.data.gesture.saveDoubleSwipeEnabled
import com.edgegesture.evilgodxu.data.gesture.saveGestureEnabled
import com.edgegesture.evilgodxu.data.gesture.saveAvoidKeyboardOverlap
import com.edgegesture.evilgodxu.data.gesture.saveBackTapAction
import com.edgegesture.evilgodxu.data.gesture.saveBackTapEnabled
import com.edgegesture.evilgodxu.data.gesture.saveBackTapMode
import com.edgegesture.evilgodxu.data.gesture.saveBackTapPauseOnCharging
import com.edgegesture.evilgodxu.data.gesture.saveBackTapRange
import com.edgegesture.evilgodxu.data.gesture.saveBackTapSensitivity
import com.edgegesture.evilgodxu.data.gesture.saveHideFromRecents
import com.edgegesture.evilgodxu.data.gesture.saveHideOverlay
import com.edgegesture.evilgodxu.data.gesture.saveLeftEdgeGesture
import com.edgegesture.evilgodxu.data.gesture.saveLeftEdgeHeightPercent
import com.edgegesture.evilgodxu.data.gesture.saveLeftEdgePositionPercent
import com.edgegesture.evilgodxu.data.gesture.saveLeftEdgeWidth
import com.edgegesture.evilgodxu.data.gesture.saveLeftSegmentCount
import com.edgegesture.evilgodxu.data.gesture.saveRightEdgeGesture
import com.edgegesture.evilgodxu.data.gesture.saveRightEdgeHeightPercent
import com.edgegesture.evilgodxu.data.gesture.saveRightEdgePositionPercent
import com.edgegesture.evilgodxu.data.gesture.saveRightEdgeWidth
import com.edgegesture.evilgodxu.data.gesture.saveRightSegmentCount
import com.edgegesture.evilgodxu.data.gesture.saveVibrationEnabled
import com.edgegesture.evilgodxu.data.app.AppRepository
import com.edgegesture.evilgodxu.data.permission.PermissionMonitor
import com.edgegesture.evilgodxu.data.permission.PermissionType
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// 手势设置页面 ViewModel，管理手势开关、边缘尺寸、手势动作等设置状态
class GestureSettingsViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val context get() = getApplication<Application>().applicationContext
    private val permissionMonitor = PermissionMonitor(context)

    // 独立的无障碍权限状态流，用于实时刷新
    private val _accessibilityEnabledFlow = MutableStateFlow(isAccessibilityServiceEnabled(context))

    // 权限状态流
    private val _permissionsFlow = MutableStateFlow(PermissionsState())

    // 正在等待的权限类型
    private val _waitingPermissionFlow = MutableStateFlow<PermissionType?>(null)

    // 权限监控任务
    private var permissionMonitorJob: Job? = null

    data class PermissionsState(
        val overlayGranted: Boolean = false,
        val notificationGranted: Boolean = false,
        val batteryOptimized: Boolean = false,
        val usageStatsGranted: Boolean = false,
        val queryAllPackagesGranted: Boolean = false,
    )

    val uiState: StateFlow<GestureSettingsUiState> = combine(
        context.gestureSettingsFlow(),
        _accessibilityEnabledFlow,
        _permissionsFlow,
        _waitingPermissionFlow
    ) { settings, isAccessibilityEnabled, permissions, waitingPermission ->
        // 当无障碍权限未启用时，强制手势开关为关闭状态
        // 避免重复安装或权限被撤销后开关状态不一致的问题
        val effectiveGestureEnabled = settings.gestureEnabled && isAccessibilityEnabled
        GestureSettingsUiState(
            isLoading = false,
            settings = settings.copy(gestureEnabled = effectiveGestureEnabled),
            isAccessibilityEnabled = isAccessibilityEnabled,
            overlayGranted = permissions.overlayGranted,
            notificationGranted = permissions.notificationGranted,
            batteryOptimized = permissions.batteryOptimized,
            usageStatsGranted = permissions.usageStatsGranted,
            queryAllPackagesGranted = permissions.queryAllPackagesGranted,
            waitingPermission = waitingPermission,
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = GestureSettingsUiState(isLoading = true),
        )

    init {
        refreshPermissions()
    }

    // 开始监控指定权限，授权后自动返回应用
    fun startPermissionMonitor(permissionType: PermissionType, activity: Activity) {
        // 取消之前的监控
        permissionMonitorJob?.cancel()
        _waitingPermissionFlow.value = permissionType

        permissionMonitorJob = viewModelScope.launch {
            permissionMonitor.monitorPermission(permissionType, intervalMs = 500)
                .collect { granted ->
                    if (granted) {
                        // 权限已授权，刷新状态并返回应用
                        refreshPermissions()
                        _waitingPermissionFlow.value = null
                        // 将应用带回前台
                        bringAppToFront(activity)
                        // 如果是查询应用权限，重置黑名单初始化标志并重新扫描
                        // 以便用完整权限重新初始化黑名单（包含所有系统应用）
                        if (permissionType == PermissionType.QUERY_ALL_PACKAGES) {
                            AppRepository.getInstance(context).onQueryPermissionGranted()
                        }
                        permissionMonitorJob?.cancel()
                    }
                }
        }
    }

    // 将应用带回前台
    private fun bringAppToFront(activity: Activity) {
        val intent = activity.packageManager.getLaunchIntentForPackage(activity.packageName)
        intent?.let {
            it.flags = android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    android.content.Intent.FLAG_ACTIVITY_NEW_TASK
            activity.startActivity(it)
        }
    }

    // 停止权限监控
    fun stopPermissionMonitor() {
        permissionMonitorJob?.cancel()
        permissionMonitorJob = null
        _waitingPermissionFlow.value = null
    }

    // 刷新无障碍服务状态，在从系统设置返回时调用
    fun refreshAccessibilityState() {
        _accessibilityEnabledFlow.value = isAccessibilityServiceEnabled(context)
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

        _permissionsFlow.value = PermissionsState(
            overlayGranted = overlay,
            notificationGranted = notification,
            batteryOptimized = battery,
            usageStatsGranted = usageStats,
            queryAllPackagesGranted = queryAllPackages,
        )
    }

    // 设置通知权限状态
    fun setNotificationGranted(granted: Boolean) {
        _permissionsFlow.value = _permissionsFlow.value.copy(notificationGranted = granted)
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

    // 检查无障碍服务是否已启用
    fun checkAccessibilityEnabled(): Boolean {
        return isAccessibilityServiceEnabled(context)
    }

    // 设置手势总开关
    fun setGestureEnabled(enabled: Boolean) {
        viewModelScope.launch {
            context.saveGestureEnabled(enabled)
        }
    }

    // 设置隐藏悬浮窗
    fun setHideOverlay(hide: Boolean) {
        viewModelScope.launch {
            context.saveHideOverlay(hide)
        }
    }

    // 设置隐藏最近任务
    fun setHideFromRecents(hide: Boolean) {
        viewModelScope.launch {
            context.saveHideFromRecents(hide)
        }
    }

    // 设置避免输入法遮挡
    fun setAvoidKeyboardOverlap(enabled: Boolean) {
        viewModelScope.launch {
            context.saveAvoidKeyboardOverlap(enabled)
        }
    }

    // 设置左侧边缘宽度
    fun setLeftEdgeWidth(width: Int) {
        viewModelScope.launch {
            context.saveLeftEdgeWidth(width)
        }
    }

    // 设置左侧边缘高度百分比
    fun setLeftEdgeHeightPercent(percent: Int) {
        viewModelScope.launch {
            context.saveLeftEdgeHeightPercent(percent)
        }
    }

    // 设置左侧边缘位置百分比
    fun setLeftEdgePositionPercent(percent: Int) {
        viewModelScope.launch {
            context.saveLeftEdgePositionPercent(percent)
        }
    }

    // 设置左侧边缘段数
    fun setLeftSegmentCount(count: Int) {
        viewModelScope.launch {
            context.saveLeftSegmentCount(count)
        }
    }

    // 设置右侧边缘宽度
    fun setRightEdgeWidth(width: Int) {
        viewModelScope.launch {
            context.saveRightEdgeWidth(width)
        }
    }

    // 设置右侧边缘高度百分比
    fun setRightEdgeHeightPercent(percent: Int) {
        viewModelScope.launch {
            context.saveRightEdgeHeightPercent(percent)
        }
    }

    // 设置右侧边缘位置百分比
    fun setRightEdgePositionPercent(percent: Int) {
        viewModelScope.launch {
            context.saveRightEdgePositionPercent(percent)
        }
    }

    // 设置右侧边缘段数
    fun setRightSegmentCount(count: Int) {
        viewModelScope.launch {
            context.saveRightSegmentCount(count)
        }
    }

    // 设置底部边缘高度
    fun setBottomEdgeHeight(height: Int) {
        viewModelScope.launch {
            context.saveBottomEdgeHeight(height)
        }
    }

    // 设置底部边缘宽度百分比
    fun setBottomEdgeWidthPercent(percent: Int) {
        viewModelScope.launch {
            context.saveBottomEdgeWidthPercent(percent)
        }
    }

    // 设置底部边缘段数
    fun setBottomSegmentCount(count: Int) {
        viewModelScope.launch {
            context.saveBottomSegmentCount(count)
        }
    }

    // 设置左侧边缘手势动作
    fun setLeftEdgeGesture(key: androidx.datastore.preferences.core.Preferences.Key<String>, action: GestureAction) {
        viewModelScope.launch {
            context.saveLeftEdgeGesture(key, action)
        }
    }

    // 设置右侧边缘手势动作
    fun setRightEdgeGesture(key: androidx.datastore.preferences.core.Preferences.Key<String>, action: GestureAction) {
        viewModelScope.launch {
            context.saveRightEdgeGesture(key, action)
        }
    }

    // 设置底部边缘手势动作
    fun setBottomEdgeGesture(key: androidx.datastore.preferences.core.Preferences.Key<String>, action: GestureAction) {
        viewModelScope.launch {
            context.saveBottomEdgeGesture(key, action)
        }
    }

    // 设置背面双击开关
    fun setBackTapEnabled(enabled: Boolean) {
        viewModelScope.launch {
            context.saveBackTapEnabled(enabled)
        }
    }

    // 设置背面双击灵敏度
    fun setBackTapSensitivity(sensitivity: Int) {
        viewModelScope.launch {
            context.saveBackTapSensitivity(sensitivity)
        }
    }

    // 设置背面双击检测范围
    fun setBackTapRange(range: Int) {
        viewModelScope.launch {
            context.saveBackTapRange(range)
        }
    }

    // 设置背面双击操作
    fun setBackTapAction(action: GestureAction) {
        viewModelScope.launch {
            context.saveBackTapAction(action)
        }
    }

    // 设置背面双击工作模式
    fun setBackTapMode(mode: BackTapMode) {
        viewModelScope.launch {
            context.saveBackTapMode(mode)
        }
    }

    // 设置充电时暂停背面双击
    fun setBackTapPauseOnCharging(pause: Boolean) {
        viewModelScope.launch {
            context.saveBackTapPauseOnCharging(pause)
        }
    }

    // 设置震动反馈开关
    fun setVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            context.saveVibrationEnabled(enabled)
            com.edgegesture.evilgodxu.service.core.EdgeGestureAccessibilityService.updateSettings(context)
        }
    }

    // 设置二次滑动开关
    fun setDoubleSwipeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            context.saveDoubleSwipeEnabled(enabled)
        }
    }

}

// 检查无障碍服务是否已启用
private fun isAccessibilityServiceEnabled(context: android.content.Context): Boolean {
    val enabledServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    val packageName = context.packageName
    return enabledServices.contains(packageName)
}
