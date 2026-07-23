package com.edgegesture.evilgodxu.screens.settings.compact.about_area.open_source_button

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.edgegesture.evilgodxu.R
import com.edgegesture.evilgodxu.screens.settings.reuse.SettingsClickableItem

// 开源许可按钮职责组件
@Composable
fun OpenSourceButton(
    onClick: () -> Unit,
) {
    SettingsClickableItem(
        icon = Icons.Default.Code,
        title = stringResource(R.string.settings_open_source),
        subtitle = stringResource(R.string.settings_open_source_desc),
        onClick = onClick
    )
}
