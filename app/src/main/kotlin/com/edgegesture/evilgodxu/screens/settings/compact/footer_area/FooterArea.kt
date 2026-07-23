package com.edgegesture.evilgodxu.screens.settings.compact.footer_area

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.edgegesture.evilgodxu.screens.settings.compact.footer_area.version_info.VersionInfo

// 底部信息 Area — 组装版本信息和项目链接
@Composable
fun FooterArea(
    versionName: String,
    onVersionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    VersionInfo(
        versionName = versionName,
        onVersionClick = onVersionClick,
    )
}
