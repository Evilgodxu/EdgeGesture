package com.edgegesture.evilgodxu.screens.gesture

import android.app.Application
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.edgegesture.evilgodxu.data.gesture.GestureAction
import com.edgegesture.evilgodxu.data.gesture.GestureSettingsState
import com.edgegesture.evilgodxu.data.gesture.gestureSettingsFlow
import com.edgegesture.evilgodxu.data.gesture.saveBottomEdgeGesture
import com.edgegesture.evilgodxu.data.gesture.saveBottomEdgeHeight
import com.edgegesture.evilgodxu.data.gesture.saveBottomEdgeWidthPercent
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
import com.edgegesture.evilgodxu.data.gesture.saveBottomSegmentCount
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// 单边缘配置页 ViewModel：订阅手势设置流，集中处理该页的所有保存操作
class EdgeGestureConfigViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val context get() = getApplication<Application>().applicationContext

    val settings: StateFlow<GestureSettingsState?> = context.gestureSettingsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null,
        )

    // 保存边缘宽度（底部边缘对应高度槽）
    fun saveWidth(edgeType: EdgeType, value: Int) {
        viewModelScope.launch {
            when (edgeType) {
                EdgeType.LEFT -> context.saveLeftEdgeWidth(value)
                EdgeType.RIGHT -> context.saveRightEdgeWidth(value)
                EdgeType.BOTTOM -> context.saveBottomEdgeHeight(value)
            }
        }
    }

    // 保存边缘高度百分比（底部边缘对应宽度百分比槽）
    fun saveHeightPercent(edgeType: EdgeType, value: Int) {
        viewModelScope.launch {
            when (edgeType) {
                EdgeType.LEFT -> context.saveLeftEdgeHeightPercent(value)
                EdgeType.RIGHT -> context.saveRightEdgeHeightPercent(value)
                EdgeType.BOTTOM -> context.saveBottomEdgeWidthPercent(value)
            }
        }
    }

    // 保存边缘位置百分比（底部边缘无此设置）
    fun savePositionPercent(edgeType: EdgeType, value: Int) {
        viewModelScope.launch {
            when (edgeType) {
                EdgeType.LEFT -> context.saveLeftEdgePositionPercent(value)
                EdgeType.RIGHT -> context.saveRightEdgePositionPercent(value)
                EdgeType.BOTTOM -> {}
            }
        }
    }

    // 保存分段数量
    fun saveSegmentCount(edgeType: EdgeType, count: Int) {
        viewModelScope.launch {
            when (edgeType) {
                EdgeType.LEFT -> context.saveLeftSegmentCount(count)
                EdgeType.RIGHT -> context.saveRightSegmentCount(count)
                EdgeType.BOTTOM -> context.saveBottomSegmentCount(count)
            }
        }
    }

    // 保存手势动作
    fun saveGestureAction(edgeType: EdgeType, key: Preferences.Key<String>, action: GestureAction) {
        viewModelScope.launch {
            when (edgeType) {
                EdgeType.LEFT -> context.saveLeftEdgeGesture(key, action)
                EdgeType.RIGHT -> context.saveRightEdgeGesture(key, action)
                EdgeType.BOTTOM -> context.saveBottomEdgeGesture(key, action)
            }
        }
    }
}
