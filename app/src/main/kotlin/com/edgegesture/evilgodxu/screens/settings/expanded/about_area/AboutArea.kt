package com.edgegesture.evilgodxu.screens.settings.expanded.about_area

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.edgegesture.evilgodxu.R
import com.edgegesture.evilgodxu.screens.settings.compact.about_area.donate_button.DonateButton
import com.edgegesture.evilgodxu.screens.settings.compact.about_area.open_source_button.OpenSourceButton
import com.edgegesture.evilgodxu.screens.settings.reuse.SettingsSection

// 宽屏关于 Area — 将捐赠和开源许可按钮组装在同一个视觉分区
@Composable
fun AboutArea(
    onDonateClick: () -> Unit,
    onOpenSourceClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsSection(
        title = stringResource(R.string.settings_about),
    ) {
        DonateButton(onClick = onDonateClick)
        HorizontalDivider()
        OpenSourceButton(onClick = onOpenSourceClick)
    }
}
