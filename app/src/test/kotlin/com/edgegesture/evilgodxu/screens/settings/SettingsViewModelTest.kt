package com.edgegesture.evilgodxu.screens.settings

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// 设置页弹窗状态：各弹窗独立开关，主题弹窗保留点击起点
class SettingsViewModelTest {

    @Test
    fun `打开主题弹窗记录点击位置`() {
        val viewModel = SettingsViewModel()

        viewModel.showThemeDialog(Offset(12f, 34f))

        assertTrue(viewModel.uiState.value.showThemeDialog)
        assertEquals(Offset(12f, 34f), viewModel.uiState.value.pendingThemeClickPosition)
    }

    @Test
    fun `关闭主题弹窗不影响其他弹窗`() {
        val viewModel = SettingsViewModel()
        viewModel.showLanguageDialog()

        viewModel.showThemeDialog(Offset.Zero)
        viewModel.dismissThemeDialog()

        assertEquals(false, viewModel.uiState.value.showThemeDialog)
        assertTrue(viewModel.uiState.value.showLanguageDialog)
    }

    @Test
    fun `捐赠与手势配置弹窗各自独立开关`() {
        val viewModel = SettingsViewModel()

        viewModel.showDonateDialog()
        viewModel.showGestureConfigDialog()
        viewModel.dismissDonateDialog()

        assertEquals(false, viewModel.uiState.value.showDonateDialog)
        assertTrue(viewModel.uiState.value.showGestureConfigDialog)
    }
}
