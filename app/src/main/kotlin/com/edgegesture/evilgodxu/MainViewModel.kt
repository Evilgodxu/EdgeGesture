package com.edgegesture.evilgodxu

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edgegesture.evilgodxu.data.repository.SettingsRepository
import com.edgegesture.evilgodxu.data.settings.AppLanguage
import com.edgegesture.evilgodxu.data.settings.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// 应用级（Activity 作用域）UI 状态持有者：以 DataStore 为单一事实源，聚合主题、语言与版本，
// 供全局主题、本地化与各页面共同消费，界面层不直连数据源
class MainViewModel(
    private val settingsRepository: SettingsRepository,
    // 首帧状态由宿主同步读取持久化值注入，避免冷启动先渲染默认主题再切换
    initialState: AppUiState,
    // 语言落库前同步非 Compose 资源（Toast、通知等），由宿主注入
    private val applyLocale: (AppLanguage) -> Unit = {},
) : ViewModel() {

    private val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    init {
        // 数据流回流更新 UI 状态：写入后经同一持有者广播，不另存副本
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _uiState.update { it.copy(themeMode = settings.themeMode) }
            }
        }
        viewModelScope.launch {
            settingsRepository.appLanguage.collect { language ->
                _uiState.update { it.copy(language = language) }
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.saveThemeMode(mode) }
    }

    fun setLanguage(language: AppLanguage) {
        applyLocale(language)
        viewModelScope.launch { settingsRepository.setAppLanguage(language) }
    }
}

// 供界面树消费的组合局部，由宿主 Activity 提供
val LocalMainViewModel = staticCompositionLocalOf<MainViewModel> {
    error("MainViewModel is not provided")
}
