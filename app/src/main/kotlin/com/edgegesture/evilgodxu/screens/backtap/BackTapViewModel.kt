package com.edgegesture.evilgodxu.screens.backtap

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.edgegesture.evilgodxu.data.gesture.BackTapMode
import com.edgegesture.evilgodxu.data.gesture.GestureAction
import com.edgegesture.evilgodxu.data.gesture.GestureSettingsState
import com.edgegesture.evilgodxu.data.gesture.gestureSettingsFlow
import com.edgegesture.evilgodxu.data.gesture.saveBackTapAction
import com.edgegesture.evilgodxu.data.gesture.saveBackTapEnabled
import com.edgegesture.evilgodxu.data.gesture.saveBackTapMode
import com.edgegesture.evilgodxu.data.gesture.saveBackTapPauseOnCharging
import com.edgegesture.evilgodxu.data.gesture.saveBackTapRange
import com.edgegesture.evilgodxu.data.gesture.saveBackTapSensitivity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// 背面双击配置页 ViewModel：订阅手势设置流，集中处理该页的保存操作
class BackTapViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val context get() = getApplication<Application>().applicationContext

    val gestureSettings: StateFlow<GestureSettingsState?> = context.gestureSettingsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null,
        )

    fun setBackTapEnabled(enabled: Boolean) {
        viewModelScope.launch { context.saveBackTapEnabled(enabled) }
    }

    fun setBackTapSensitivity(value: Int) {
        viewModelScope.launch { context.saveBackTapSensitivity(value) }
    }

    fun setBackTapRange(value: Int) {
        viewModelScope.launch { context.saveBackTapRange(value) }
    }

    fun setBackTapMode(mode: BackTapMode) {
        viewModelScope.launch { context.saveBackTapMode(mode) }
    }

    fun setBackTapPauseOnCharging(pause: Boolean) {
        viewModelScope.launch { context.saveBackTapPauseOnCharging(pause) }
    }

    fun setBackTapAction(action: GestureAction) {
        viewModelScope.launch { context.saveBackTapAction(action) }
    }
}
