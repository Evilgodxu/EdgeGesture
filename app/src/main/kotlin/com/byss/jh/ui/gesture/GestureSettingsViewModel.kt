package com.byss.jh.ui.gesture

import android.app.Application
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.byss.jh.data.gesture.GestureAction
import com.byss.jh.data.gesture.GestureSettingsState
import com.byss.jh.data.gesture.gestureSettingsFlow
import com.byss.jh.data.gesture.saveBottomEdgeGesture
import com.byss.jh.data.gesture.saveBottomEdgeHeight
import com.byss.jh.data.gesture.saveBottomEdgeWidthPercent
import com.byss.jh.data.gesture.saveBottomSegmentCount
import com.byss.jh.data.gesture.saveGestureEnabled
import com.byss.jh.data.gesture.saveHideFromRecents
import com.byss.jh.data.gesture.saveHideOverlay
import com.byss.jh.data.gesture.saveLeftEdgeGesture
import com.byss.jh.data.gesture.saveLeftEdgeHeightPercent
import com.byss.jh.data.gesture.saveLeftEdgePositionPercent
import com.byss.jh.data.gesture.saveLeftEdgeWidth
import com.byss.jh.data.gesture.saveLeftSegmentCount
import com.byss.jh.data.gesture.saveRightEdgeGesture
import com.byss.jh.data.gesture.saveRightEdgeHeightPercent
import com.byss.jh.data.gesture.saveRightEdgePositionPercent
import com.byss.jh.data.gesture.saveRightEdgeWidth
import com.byss.jh.data.gesture.saveRightSegmentCount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 手势设置页面 ViewModel
 * 管理手势开关、边缘尺寸、手势动作等设置状态
 */
class GestureSettingsViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val context get() = getApplication<Application>().applicationContext

    // 独立的无障碍权限状态流，用于实时刷新
    private val _accessibilityEnabledFlow = MutableStateFlow(isAccessibilityServiceEnabled(context))

    val uiState: StateFlow<GestureSettingsUiState> = combine(
        context.gestureSettingsFlow(),
        _accessibilityEnabledFlow
    ) { settings, isAccessibilityEnabled ->
        // 当无障碍权限未启用时，强制手势开关为关闭状态
        // 避免重复安装或权限被撤销后开关状态不一致的问题
        val effectiveGestureEnabled = settings.gestureEnabled && isAccessibilityEnabled
        GestureSettingsUiState(
            isLoading = false,
            settings = settings.copy(gestureEnabled = effectiveGestureEnabled),
            isAccessibilityEnabled = isAccessibilityEnabled,
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = GestureSettingsUiState(isLoading = true),
        )

    /**
     * 刷新无障碍服务状态
     * 在从系统设置返回时调用
     */
    fun refreshAccessibilityState() {
        _accessibilityEnabledFlow.value = isAccessibilityServiceEnabled(context)
    }

    /**
     * 检查无障碍服务是否已启用
     */
    fun checkAccessibilityEnabled(): Boolean {
        return isAccessibilityServiceEnabled(context)
    }

    /**
     * 设置手势总开关
     */
    fun setGestureEnabled(enabled: Boolean) {
        viewModelScope.launch {
            context.saveGestureEnabled(enabled)
        }
    }

    /**
     * 设置隐藏悬浮窗
     */
    fun setHideOverlay(hide: Boolean) {
        viewModelScope.launch {
            context.saveHideOverlay(hide)
        }
    }

    /**
     * 设置隐藏最近任务
     */
    fun setHideFromRecents(hide: Boolean) {
        viewModelScope.launch {
            context.saveHideFromRecents(hide)
        }
    }

    /**
     * 设置左侧边缘宽度
     */
    fun setLeftEdgeWidth(width: Int) {
        viewModelScope.launch {
            context.saveLeftEdgeWidth(width)
        }
    }

    /**
     * 设置左侧边缘高度百分比
     */
    fun setLeftEdgeHeightPercent(percent: Int) {
        viewModelScope.launch {
            context.saveLeftEdgeHeightPercent(percent)
        }
    }

    /**
     * 设置左侧边缘位置百分比
     */
    fun setLeftEdgePositionPercent(percent: Int) {
        viewModelScope.launch {
            context.saveLeftEdgePositionPercent(percent)
        }
    }

    /**
     * 设置左侧边缘段数
     */
    fun setLeftSegmentCount(count: Int) {
        viewModelScope.launch {
            context.saveLeftSegmentCount(count)
        }
    }

    /**
     * 设置右侧边缘宽度
     */
    fun setRightEdgeWidth(width: Int) {
        viewModelScope.launch {
            context.saveRightEdgeWidth(width)
        }
    }

    /**
     * 设置右侧边缘高度百分比
     */
    fun setRightEdgeHeightPercent(percent: Int) {
        viewModelScope.launch {
            context.saveRightEdgeHeightPercent(percent)
        }
    }

    /**
     * 设置右侧边缘位置百分比
     */
    fun setRightEdgePositionPercent(percent: Int) {
        viewModelScope.launch {
            context.saveRightEdgePositionPercent(percent)
        }
    }

    /**
     * 设置右侧边缘段数
     */
    fun setRightSegmentCount(count: Int) {
        viewModelScope.launch {
            context.saveRightSegmentCount(count)
        }
    }

    /**
     * 设置底部边缘高度
     */
    fun setBottomEdgeHeight(height: Int) {
        viewModelScope.launch {
            context.saveBottomEdgeHeight(height)
        }
    }

    /**
     * 设置底部边缘宽度百分比
     */
    fun setBottomEdgeWidthPercent(percent: Int) {
        viewModelScope.launch {
            context.saveBottomEdgeWidthPercent(percent)
        }
    }

    /**
     * 设置底部边缘段数
     */
    fun setBottomSegmentCount(count: Int) {
        viewModelScope.launch {
            context.saveBottomSegmentCount(count)
        }
    }

    /**
     * 设置左侧边缘手势动作
     */
    fun setLeftEdgeGesture(key: androidx.datastore.preferences.core.Preferences.Key<String>, action: GestureAction) {
        viewModelScope.launch {
            context.saveLeftEdgeGesture(key, action)
        }
    }

    /**
     * 设置右侧边缘手势动作
     */
    fun setRightEdgeGesture(key: androidx.datastore.preferences.core.Preferences.Key<String>, action: GestureAction) {
        viewModelScope.launch {
            context.saveRightEdgeGesture(key, action)
        }
    }

    /**
     * 设置底部边缘手势动作
     */
    fun setBottomEdgeGesture(key: androidx.datastore.preferences.core.Preferences.Key<String>, action: GestureAction) {
        viewModelScope.launch {
            context.saveBottomEdgeGesture(key, action)
        }
    }
}

/**
 * 检查无障碍服务是否已启用
 */
private fun isAccessibilityServiceEnabled(context: android.content.Context): Boolean {
    val enabledServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    val packageName = context.packageName
    return enabledServices.contains(packageName)
}
