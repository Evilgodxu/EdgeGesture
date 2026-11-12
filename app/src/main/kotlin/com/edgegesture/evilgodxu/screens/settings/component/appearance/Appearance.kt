package com.edgegesture.evilgodxu.screens.settings.component.appearance

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import com.edgegesture.evilgodxu.R
import com.edgegesture.evilgodxu.data.settings.ThemeMode
import com.edgegesture.evilgodxu.screens.settings.component.SettingsClickableItem
import com.edgegesture.evilgodxu.screens.settings.component.SettingsSection

// 外观区块：主题入口，点击位置用于圆形揭示过渡的起点
@Composable
fun Appearance(
    themeMode: ThemeMode,
    onThemeClick: (Offset) -> Unit,
) {
    SettingsSection(title = stringResource(R.string.settings_section_appearance)) {
        SettingsClickableItem(
            icon = Icons.Default.Palette,
            title = stringResource(R.string.settings_theme_title),
            subtitle = when (themeMode) {
                ThemeMode.SYSTEM -> stringResource(R.string.settings_theme_system)
                ThemeMode.DARK -> stringResource(R.string.settings_theme_dark)
                ThemeMode.LIGHT -> stringResource(R.string.settings_theme_light)
            },
            onClick = onThemeClick,
        )
    }
}
