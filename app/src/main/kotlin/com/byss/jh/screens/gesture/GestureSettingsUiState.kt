package com.byss.jh.screens.gesture

import com.byss.jh.data.gesture.GestureSettingsState

// 手势设置页面 UI 状态
data class GestureSettingsUiState(
    val isLoading: Boolean = true,
    val settings: GestureSettingsState = GestureSettingsState(),
    val isAccessibilityEnabled: Boolean = false,
)
