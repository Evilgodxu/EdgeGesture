package com.edgegesture.evilgodxu.screens.settings.compact.appearance_area.theme_selector

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.edgegesture.evilgodxu.R
import com.edgegesture.evilgodxu.screens.settings.data.ThemeMode
import com.edgegesture.evilgodxu.screens.settings.reuse.SettingsClickableItem

// 主题选择器职责组件
@Composable
fun ThemeSelector(
    state: ThemeSelectorState,
    onClick: () -> Unit,
) {
    SettingsClickableItem(
        icon = Icons.Default.Palette,
        title = stringResource(R.string.settings_theme_title),
        subtitle = when (state.currentTheme) {
            ThemeMode.SYSTEM -> stringResource(R.string.settings_theme_system)
            ThemeMode.DARK -> stringResource(R.string.settings_theme_dark)
            ThemeMode.LIGHT -> stringResource(R.string.settings_theme_light)
        },
        onClick = onClick
    )
}
