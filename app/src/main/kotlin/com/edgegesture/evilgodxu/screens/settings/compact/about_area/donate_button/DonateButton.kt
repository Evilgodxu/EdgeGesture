package com.edgegesture.evilgodxu.screens.settings.compact.about_area.donate_button

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.edgegesture.evilgodxu.R
import com.edgegesture.evilgodxu.screens.settings.reuse.SettingsClickableItem

// 捐赠按钮职责组件
@Composable
fun DonateButton(
    onClick: () -> Unit,
) {
    SettingsClickableItem(
        icon = Icons.Default.Favorite,
        title = stringResource(R.string.settings_donate),
        subtitle = stringResource(R.string.settings_donate_desc),
        onClick = onClick
    )
}
