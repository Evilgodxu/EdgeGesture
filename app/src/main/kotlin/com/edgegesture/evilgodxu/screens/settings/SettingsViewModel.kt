package com.edgegesture.evilgodxu.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.edgegesture.evilgodxu.data.gesture.gestureSettingsFlow
import com.edgegesture.evilgodxu.data.gesture.saveVibrationEnabled
import com.edgegesture.evilgodxu.data.launchblock.LaunchBlockRule
import com.edgegesture.evilgodxu.data.launchblock.addLaunchBlockRule
import com.edgegesture.evilgodxu.data.launchblock.launchBlockFlow
import com.edgegesture.evilgodxu.data.launchblock.removeLaunchBlockRule
import com.edgegesture.evilgodxu.data.launchblock.saveLaunchBlockRules
import com.edgegesture.evilgodxu.data.launchblock.setLaunchBlockEnabled
import com.edgegesture.evilgodxu.data.launchblock.updateLaunchBlockRule
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// 设置页面 ViewModel，管理主题、语言、震动反馈等设置状态
class SettingsViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val context get() = getApplication<Application>().applicationContext

    val uiState: StateFlow<SettingsUiState> = combine(
        context.settingsFlow(),
        context.gestureSettingsFlow(),
        context.launchBlockFlow(),
    ) { settings, gestureSettings, launchBlockState ->
        SettingsUiState(
            isLoading = false,
            themeMode = settings.themeMode,
            vibrationEnabled = gestureSettings.vibrationEnabled,
            launchBlockEnabled = launchBlockState.enabled,
            launchBlockRules = launchBlockState.rules,
        )
    }.stateIn(
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

    // 设置震动反馈开关
    fun setVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            context.saveVibrationEnabled(enabled)
        }
    }

    // 设置启动拦截开关
    fun setLaunchBlockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            context.setLaunchBlockEnabled(enabled)
        }
    }

    // 添加启动拦截规则
    fun addLaunchBlockRule(rule: LaunchBlockRule) {
        viewModelScope.launch {
            context.addLaunchBlockRule(rule)
        }
    }

    // 更新启动拦截规则
    fun updateLaunchBlockRule(rule: LaunchBlockRule) {
        viewModelScope.launch {
            context.updateLaunchBlockRule(rule)
        }
    }

    // 删除启动拦截规则
    fun removeLaunchBlockRule(ruleId: String) {
        viewModelScope.launch {
            context.removeLaunchBlockRule(ruleId)
        }
    }
}
