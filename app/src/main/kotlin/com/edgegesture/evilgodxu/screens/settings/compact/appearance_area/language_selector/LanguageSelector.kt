package com.edgegesture.evilgodxu.screens.settings.compact.appearance_area.language_selector

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.edgegesture.evilgodxu.R
import com.edgegesture.evilgodxu.screens.settings.data.AppLanguage
import com.edgegesture.evilgodxu.screens.settings.reuse.SettingsClickableItem

// 语言选择器职责组件
@Composable
fun LanguageSelector(
    currentLanguage: AppLanguage,
    onClick: () -> Unit,
) {
    SettingsClickableItem(
        icon = Icons.Default.Language,
        title = stringResource(R.string.settings_language_title),
        subtitle = when (currentLanguage) {
            AppLanguage.SYSTEM -> stringResource(R.string.settings_language_system)
            AppLanguage.CHINESE -> stringResource(R.string.settings_language_chinese)
            AppLanguage.ENGLISH -> stringResource(R.string.settings_language_english)
            AppLanguage.JAPANESE -> stringResource(R.string.settings_language_japanese)
            AppLanguage.KOREAN -> stringResource(R.string.settings_language_korean)
            AppLanguage.RUSSIAN -> stringResource(R.string.settings_language_russian)
            AppLanguage.GERMAN -> stringResource(R.string.settings_language_german)
        },
        onClick = onClick
    )
}
