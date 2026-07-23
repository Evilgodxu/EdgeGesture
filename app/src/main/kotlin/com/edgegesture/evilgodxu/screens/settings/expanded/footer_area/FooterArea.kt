package com.edgegesture.evilgodxu.screens.settings.expanded.footer_area

import androidx.compose.runtime.Composable
import com.edgegesture.evilgodxu.screens.settings.compact.footer_area.version_info.VersionInfo

// 宽屏底部信息 Area — 仅负责组件排列
@Composable
fun FooterArea(
    versionName: String,
) {
    VersionInfo(
        versionName = versionName,
        onVersionClick = {},
    )
}
