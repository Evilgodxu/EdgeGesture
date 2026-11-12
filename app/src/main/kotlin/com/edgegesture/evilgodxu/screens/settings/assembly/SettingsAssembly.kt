package com.edgegesture.evilgodxu.screens.settings.assembly

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import com.edgegesture.evilgodxu.AppUiState
import com.edgegesture.evilgodxu.R
import com.edgegesture.evilgodxu.screens.settings.component.appearance.Appearance
import com.edgegesture.evilgodxu.screens.settings.component.info.AppAboutSection
import com.edgegesture.evilgodxu.screens.settings.component.info.AppFooter
import com.edgegesture.evilgodxu.screens.settings.component.language.Language

// 设置页组装器：宽窗两列（外观/语言 | 关于），窄窗单列，与重构前的排布保持一致
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsAssembly(
    windowSizeClass: WindowSizeClass,
    appUiState: AppUiState,
    onBack: () -> Unit,
    onShowThemeDialog: (Offset) -> Unit,
    onShowLanguageDialog: () -> Unit,
    onShowDonateDialog: () -> Unit,
    onShowGestureConfigDialog: () -> Unit,
    onCheckForUpdate: () -> Unit,
    onOpenRepository: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.nav_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .consumeWindowInsets(innerPadding)
            .padding(innerPadding)
            .padding(16.dp)

        if (windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)) {
            // 宽屏双列：左侧外观与语言，右侧关于
            Row(
                modifier = contentModifier.verticalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Appearance(themeMode = appUiState.themeMode, onThemeClick = onShowThemeDialog)
                    Language(language = appUiState.language, onShowDialog = onShowLanguageDialog)
                }
                Column(modifier = Modifier.weight(1f)) {
                    AppAboutSection(
                        onShowGestureConfig = onShowGestureConfigDialog,
                        onShowDonate = onShowDonateDialog,
                    )
                }
            }

            // 宽屏的版本号不可点击，与原实现一致
            AppFooter(
                version = appUiState.version,
                onOpenRepository = onOpenRepository,
            )
        } else {
            // 窄屏单列，版本号可点击检查更新
            Column(modifier = contentModifier.verticalScroll(rememberScrollState())) {
                Appearance(themeMode = appUiState.themeMode, onThemeClick = onShowThemeDialog)
                Language(language = appUiState.language, onShowDialog = onShowLanguageDialog)
                AppAboutSection(
                    onShowGestureConfig = onShowGestureConfigDialog,
                    onShowDonate = onShowDonateDialog,
                )
                AppFooter(
                    version = appUiState.version,
                    onOpenRepository = onOpenRepository,
                    onCheckForUpdate = onCheckForUpdate,
                )
            }
        }
    }
}
