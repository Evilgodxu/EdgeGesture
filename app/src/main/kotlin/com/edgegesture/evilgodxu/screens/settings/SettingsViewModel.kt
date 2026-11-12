package com.edgegesture.evilgodxu.screens.settings

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// 设置页 ViewModel：只持有页面级弹窗状态；
// 主题、语言、版本与更新检查均属应用级，分别由 MainViewModel、UpdateViewModel 提供
class SettingsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    // 点击主题项：记录动效起点并打开主题弹窗
    fun showThemeDialog(position: Offset) {
        _uiState.update { it.copy(showThemeDialog = true, pendingThemeClickPosition = position) }
    }

    fun dismissThemeDialog() {
        _uiState.update { it.copy(showThemeDialog = false) }
    }

    fun showLanguageDialog() {
        _uiState.update { it.copy(showLanguageDialog = true) }
    }

    fun dismissLanguageDialog() {
        _uiState.update { it.copy(showLanguageDialog = false) }
    }

    fun showDonateDialog() {
        _uiState.update { it.copy(showDonateDialog = true) }
    }

    fun dismissDonateDialog() {
        _uiState.update { it.copy(showDonateDialog = false) }
    }

    fun showGestureConfigDialog() {
        _uiState.update { it.copy(showGestureConfigDialog = true) }
    }

    fun dismissGestureConfigDialog() {
        _uiState.update { it.copy(showGestureConfigDialog = false) }
    }
}
