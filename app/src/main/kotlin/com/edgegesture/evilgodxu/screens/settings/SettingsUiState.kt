package com.edgegesture.evilgodxu.screens.settings

import com.edgegesture.evilgodxu.data.launchblock.LaunchBlockRule
import com.edgegesture.evilgodxu.screens.settings.data.ThemeMode

// 设置页面 UI 状态
data class SettingsUiState(
    val isLoading: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val vibrationEnabled: Boolean = false,
    val launchBlockEnabled: Boolean = false,
    val launchBlockRules: List<LaunchBlockRule> = emptyList(),
)
