package com.edgegesture.evilgodxu.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.edgegesture.evilgodxu.data.shizuku.ShizukuManager
import com.edgegesture.evilgodxu.data.shizuku.ShizukuState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// 设置页面 ViewModel，管理主题、语言、Shizuku 等设置状态
class SettingsViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val context get() = getApplication<Application>().applicationContext

    // 请求 Shizuku 权限的请求码，与 UI 层注册的权限结果监听共用
    val shizukuPermissionCode = 1001

    private val _shizukuState = MutableStateFlow<ShizukuState>(ShizukuState.NotRunning)
    val shizukuState: StateFlow<ShizukuState> = _shizukuState.asStateFlow()

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
}
