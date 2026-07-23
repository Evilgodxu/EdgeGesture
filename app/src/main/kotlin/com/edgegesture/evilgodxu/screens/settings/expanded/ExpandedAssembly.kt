package com.edgegesture.evilgodxu.screens.settings.expanded

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.edgegesture.evilgodxu.screens.settings.compact.appearance_area.theme_selector.ThemeSelectorState
import com.edgegesture.evilgodxu.screens.settings.data.AppLanguage
import com.edgegesture.evilgodxu.screens.settings.expanded.about_area.AboutArea
import com.edgegesture.evilgodxu.screens.settings.expanded.appearance_area.AppearanceArea
import com.edgegesture.evilgodxu.screens.settings.expanded.footer_area.FooterArea

// 展开视图物理空间组装器
// 使用 Row 双列布局以充分利用宽屏空间
@Composable
fun ExpandedAssembly(
    themeSelectorState: ThemeSelectorState,
    currentLanguage: AppLanguage,
    versionName: String,
    onThemeClick: () -> Unit,
    onLanguageClick: () -> Unit,
    onDonateClick: () -> Unit,
    onOpenSourceClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // 左侧列：外观设置
            Column(
                modifier = Modifier.weight(1f),
            ) {
                AppearanceArea(
                    themeSelectorState = themeSelectorState,
                    currentLanguage = currentLanguage,
                    onThemeClick = onThemeClick,
                    onLanguageClick = onLanguageClick,
                )
            }

            // 右侧列：关于设置
            Column(
                modifier = Modifier.weight(1f),
            ) {
                AboutArea(
                    onDonateClick = onDonateClick,
                    onOpenSourceClick = onOpenSourceClick,
                )
            }
        }

        FooterArea(
            versionName = versionName,
        )
    }
}
