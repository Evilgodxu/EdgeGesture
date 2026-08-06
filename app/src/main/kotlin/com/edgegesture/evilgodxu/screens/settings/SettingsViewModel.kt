package com.edgegesture.evilgodxu.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// 设置页面 ViewModel，管理主题、语言等设置状态
class SettingsViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val context get() = getApplication<Application>().applicationContext

    val uiState: StateFlow<SettingsUiState> = context.settingsFlow()
        .map { settings ->
            SettingsUiState(
                isLoading = false,
                themeMode = settings.themeMode,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SettingsUiState(isLoading = true),
        )

    // 设置主题模式
    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            context.saveThemeMode(mode)
        }
    }
}
