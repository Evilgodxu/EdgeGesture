package com.edgegesture.evilgodxu.screens.settings.compact.appearance_area

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.edgegesture.evilgodxu.R
import com.edgegesture.evilgodxu.screens.settings.compact.appearance_area.language_selector.LanguageSelector
import com.edgegesture.evilgodxu.screens.settings.compact.appearance_area.theme_selector.ThemeSelector
import com.edgegesture.evilgodxu.screens.settings.compact.appearance_area.theme_selector.ThemeSelectorState
import com.edgegesture.evilgodxu.screens.settings.data.AppLanguage
import com.edgegesture.evilgodxu.screens.settings.reuse.SettingsSection

// 外观设置 Area — 将主题选择器和语言选择器组装在同一个视觉分区
@Composable
fun AppearanceArea(
    themeSelectorState: ThemeSelectorState,
    currentLanguage: AppLanguage,
    onThemeClick: () -> Unit,
    onLanguageClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsSection(
        title = stringResource(R.string.settings_section_appearance),
    ) {
        ThemeSelector(
            state = themeSelectorState,
            onClick = onThemeClick,
        )
        LanguageSelector(
            currentLanguage = currentLanguage,
            onClick = onLanguageClick,
        )
    }
}
