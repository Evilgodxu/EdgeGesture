package com.edgegesture.evilgodxu.screens.settings

import androidx.compose.ui.geometry.Offset

// 设置页 UI 状态：仅页面级弹窗显隐与主题切换动效起点；
// 主题、语言与版本属应用级，由 AppUiState 提供
data class SettingsUiState(
    val showThemeDialog: Boolean = false,
    val showLanguageDialog: Boolean = false,
    val showDonateDialog: Boolean = false,
    val showGestureConfigDialog: Boolean = false,
    val pendingThemeClickPosition: Offset = Offset.Zero,
)
