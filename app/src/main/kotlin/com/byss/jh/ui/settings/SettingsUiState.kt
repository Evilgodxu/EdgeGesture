package com.byss.jh.ui.settings

// 设置页面 UI 状态
data class SettingsUiState(
    val isLoading: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val vibrationEnabled: Boolean = false,
)
