package com.edgegesture.evilgodxu.screens.settings.component.language

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.edgegesture.evilgodxu.R
import com.edgegesture.evilgodxu.data.settings.AppLanguage
import com.edgegesture.evilgodxu.ui.component.SectionCard
import com.edgegesture.evilgodxu.screens.settings.component.SettingsClickableItem

// 语言区块：进入语言选择弹窗
@Composable
fun Language(
    language: AppLanguage,
    onShowDialog: () -> Unit,
) {
    SectionCard(title = stringResource(R.string.settings_section_language)) {
        SettingsClickableItem(
            icon = Icons.Default.Language,
            title = stringResource(R.string.settings_language_title),
            subtitle = when (language) {
                AppLanguage.SYSTEM -> stringResource(R.string.settings_language_system)
                AppLanguage.CHINESE -> stringResource(R.string.settings_language_chinese)
                AppLanguage.ENGLISH -> stringResource(R.string.settings_language_english)
            },
            onClick = onShowDialog,
        )
    }
}
