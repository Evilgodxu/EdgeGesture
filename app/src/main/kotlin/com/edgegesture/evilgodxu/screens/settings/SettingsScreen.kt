package com.edgegesture.evilgodxu.screens.settings

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.net.toUri
import com.edgegesture.evilgodxu.LocalMainViewModel
import com.edgegesture.evilgodxu.screens.settings.assembly.SettingsAssembly
import com.edgegesture.evilgodxu.screens.settings.component.dialog.DonateDialog
import com.edgegesture.evilgodxu.screens.settings.component.dialog.GestureConfigDialog
import com.edgegesture.evilgodxu.screens.settings.component.dialog.LanguageSelectionDialog
import com.edgegesture.evilgodxu.screens.settings.component.dialog.ThemeSelectionDialog
import com.edgegesture.evilgodxu.ui.adaptive.currentWindowSizeClass
import com.edgegesture.evilgodxu.ui.theme.LocalThemeTransitionController
import com.edgegesture.evilgodxu.update.LocalUpdateViewModel
import com.edgegesture.evilgodxu.update.UpdateManager

// 页面入口：提升状态与事件，窗口尺寸类在此计算后作为参数下传，布局交给组装器；
// 弹层与形态无关，统一挂在骨架层
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(),
) {
    val context = LocalContext.current
    val mainViewModel = LocalMainViewModel.current
    val updateViewModel = LocalUpdateViewModel.current
    val themeTransitionController = LocalThemeTransitionController.current

    val appUiState by mainViewModel.uiState.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SettingsAssembly(
        windowSizeClass = currentWindowSizeClass(),
        appUiState = appUiState,
        onBack = onNavigateBack,
        onShowThemeDialog = viewModel::showThemeDialog,
        onShowLanguageDialog = viewModel::showLanguageDialog,
        onShowDonateDialog = viewModel::showDonateDialog,
        onShowGestureConfigDialog = viewModel::showGestureConfigDialog,
        onCheckForUpdate = { updateViewModel.checkForUpdate(force = true) },
        onOpenRepository = {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, UpdateManager.GITHUB_REPOSITORY_URL.toUri()),
            )
        },
    )

    if (uiState.showThemeDialog) {
        ThemeSelectionDialog(
            currentTheme = appUiState.themeMode,
            onDismiss = viewModel::dismissThemeDialog,
            // 先从点击位置播放圆形揭示，再落库主题：揭示动画需要旧主题画面作为底图
            onThemeSelected = { themeMode ->
                themeTransitionController.revealAt(uiState.pendingThemeClickPosition)
                mainViewModel.setThemeMode(themeMode)
                viewModel.dismissThemeDialog()
            },
        )
    }

    if (uiState.showLanguageDialog) {
        LanguageSelectionDialog(
            currentLanguage = appUiState.language,
            onDismiss = viewModel::dismissLanguageDialog,
            // 先关闭弹窗再切换语言：语言变化会就地重建弹窗内文案，视觉上会看到一次闪跳
            onLanguageSelected = { language ->
                viewModel.dismissLanguageDialog()
                mainViewModel.setLanguage(language)
            },
        )
    }

    if (uiState.showDonateDialog) {
        DonateDialog(onDismiss = viewModel::dismissDonateDialog)
    }

    if (uiState.showGestureConfigDialog) {
        GestureConfigDialog(onDismiss = viewModel::dismissGestureConfigDialog)
    }
}
