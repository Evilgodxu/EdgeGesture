package com.edgegesture.evilgodxu.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.edgegesture.evilgodxu.data.shizuku.ShizukuManager
import com.edgegesture.evilgodxu.data.shizuku.ShizukuState
import com.edgegesture.evilgodxu.utils.localization.LocalizationManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// 设置页面 ViewModel，管理主题、语言、Shizuku 等设置状态
class SettingsViewModel(
    application: Application,
    private val localizationManager: LocalizationManager,
) : AndroidViewModel(application) {

    private val context get() = getApplication<Application>().applicationContext

    // 请求 Shizuku 权限的请求码，与 UI 层注册的权限结果监听共用
    val shizukuPermissionCode = 1001

    private val _shizukuState = MutableStateFlow<ShizukuState>(ShizukuState.NotRunning)
    val shizukuState: StateFlow<ShizukuState> = _shizukuState.asStateFlow()

    val uiState: StateFlow<SettingsUiState> = combine(
        context.settingsFlow(),
        context.appLanguageFlow(),
    ) { settings, language ->
        SettingsUiState(
            isLoading = false,
            themeMode = settings.themeMode,
            language = language,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState(isLoading = true),
    )

    // 初始化 Shizuku（内部幂等，可重复调用），并同步当前状态
    fun initShizuku() {
        ShizukuManager.init(context)
        _shizukuState.value = ShizukuManager.state.value
    }

    // 接收权限结果回调，更新 Shizuku 状态
    fun setShizukuPermissionResult(granted: Boolean) {
        _shizukuState.value = if (granted) ShizukuState.Granted else ShizukuState.Denied
    }

    // 设置主题模式
    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            context.saveThemeMode(mode)
        }
    }

    // 设置应用语言：先同步 app/activity 层 Resources，再写入 DataStore 驱动 Compose 热切换
    fun setLanguage(language: AppLanguage) {
        localizationManager.applyAppLocale(localizationManager.resolveLanguage(language))
        viewModelScope.launch {
            context.saveAppLanguage(language)
        }
    }
}
