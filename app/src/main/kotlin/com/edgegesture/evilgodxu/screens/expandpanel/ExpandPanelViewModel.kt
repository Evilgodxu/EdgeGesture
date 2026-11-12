package com.edgegesture.evilgodxu.screens.expandpanel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.edgegesture.evilgodxu.data.gesture.ExpandPanelShortcutsState
import com.edgegesture.evilgodxu.data.gesture.expandPanelShortcutsFlow
import com.edgegesture.evilgodxu.data.gesture.saveExpandPanelShortcut
import com.edgegesture.evilgodxu.data.gesture.saveExpandPanelShortcutFreeform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// 扩展面板设置页 ViewModel：订阅快捷方式配置流，集中处理快捷方式保存与槽位选择状态
class ExpandPanelViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val context get() = getApplication<Application>().applicationContext

    val shortcuts: StateFlow<ExpandPanelShortcutsState?> = context.expandPanelShortcutsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null,
        )

    private val _uiState = MutableStateFlow(ExpandPanelUiState())
    val uiState: StateFlow<ExpandPanelUiState> = _uiState.asStateFlow()

    // 打开指定槽位的应用选择器
    fun showAppPicker(index: Int) {
        _uiState.update { it.copy(pickerIndex = index) }
    }

    fun dismissAppPicker() {
        _uiState.update { it.copy(pickerIndex = null) }
    }

    // 保存单个快捷方式（包名为 null 时清空槽位）
    fun saveShortcut(index: Int, packageName: String?) {
        viewModelScope.launch {
            context.saveExpandPanelShortcut(index, packageName)
        }
    }

    // 保存单个快捷方式的小窗启动开关
    fun saveShortcutFreeform(index: Int, enabled: Boolean) {
        viewModelScope.launch {
            context.saveExpandPanelShortcutFreeform(index, enabled)
        }
    }
}
