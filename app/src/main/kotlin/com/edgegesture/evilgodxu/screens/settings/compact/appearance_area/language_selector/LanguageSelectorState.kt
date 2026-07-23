package com.edgegesture.evilgodxu.screens.settings.compact.appearance_area.language_selector

import com.edgegesture.evilgodxu.screens.settings.data.AppLanguage

// 语言选择器业务状态
data class LanguageSelectorState(
    val currentLanguage: AppLanguage = AppLanguage.SYSTEM,
)
