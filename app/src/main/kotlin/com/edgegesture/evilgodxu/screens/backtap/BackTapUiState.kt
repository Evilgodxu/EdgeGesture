package com.edgegesture.evilgodxu.screens.backtap

// 背面双击页 UI 状态：动作选择与应用选择两个弹窗的显隐
data class BackTapUiState(
    val showActionDialog: Boolean = false,
    val showAppPicker: Boolean = false,
)
