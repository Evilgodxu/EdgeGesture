package com.edgegesture.evilgodxu.screens.settings.compact

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.edgegesture.evilgodxu.screens.settings.compact.about_area.AboutArea
import com.edgegesture.evilgodxu.screens.settings.compact.appearance_area.AppearanceArea
import com.edgegesture.evilgodxu.screens.settings.compact.appearance_area.theme_selector.ThemeSelectorState
import com.edgegesture.evilgodxu.screens.settings.compact.footer_area.FooterArea
import com.edgegesture.evilgodxu.screens.settings.data.AppLanguage

// 紧凑视图物理空间组装器
// 仅定义 Column 容器和排列顺序，不包含业务逻辑
@Composable
fun CompactAssembly(
    themeSelectorState: ThemeSelectorState,
    currentLanguage: AppLanguage,
    versionName: String,
    onThemeClick: () -> Unit,
    onLanguageClick: () -> Unit,
    onDonateClick: () -> Unit,
    onOpenSourceClick: () -> Unit,
    onVersionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        AppearanceArea(
            themeSelectorState = themeSelectorState,
            currentLanguage = currentLanguage,
            onThemeClick = onThemeClick,
            onLanguageClick = onLanguageClick,
        )

        AboutArea(
            onDonateClick = onDonateClick,
            onOpenSourceClick = onOpenSourceClick,
        )

        FooterArea(
            versionName = versionName,
            onVersionClick = onVersionClick,
        )
    }
}
