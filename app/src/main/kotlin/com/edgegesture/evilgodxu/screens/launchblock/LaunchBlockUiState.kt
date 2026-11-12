package com.edgegesture.evilgodxu.screens.launchblock

import com.edgegesture.evilgodxu.data.launchblock.LaunchBlockRule

// 启动拦截页 UI 状态：规则编辑弹窗的显隐与当前编辑对象
data class LaunchBlockUiState(
    val showRuleDialog: Boolean = false,
    val editingRule: LaunchBlockRule? = null,
)
