package com.edgegesture.evilgodxu

import com.edgegesture.evilgodxu.data.settings.AppLanguage
import com.edgegesture.evilgodxu.data.settings.ThemeMode

// 应用级 UI 状态：聚合全局主题、语言与版本，供主题、本地化与各页面共同消费
data class AppUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val version: String = "",
)
