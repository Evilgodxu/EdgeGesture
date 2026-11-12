package com.edgegesture.evilgodxu.screens.launchblock

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.edgegesture.evilgodxu.screens.launchblock.assembly.LaunchBlockAssembly
import com.edgegesture.evilgodxu.screens.launchblock.component.dialog.LaunchBlockRuleDialog
import com.edgegesture.evilgodxu.ui.adaptive.currentWindowSizeClass

// 页面入口：状态与事件上提，窗口尺寸类在此计算后下传，规则弹窗挂在骨架层
@Composable
fun LaunchBlockScreen(
    onNavigateBack: () -> Unit,
    viewModel: LaunchBlockViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val appNames by viewModel.appNames.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchBlockAssembly(
        windowSizeClass = currentWindowSizeClass(),
        state = state,
        appNames = appNames,
        onBack = onNavigateBack,
        onToggleEnabled = viewModel::setEnabled,
        onResolveAppName = viewModel::resolveAppName,
        onAddRule = viewModel::showAddRule,
        onEditRule = viewModel::showEditRule,
        onDeleteRule = viewModel::removeRule,
    )

    val editingRule = uiState.editingRule
    if (uiState.showRuleDialog) {
        LaunchBlockRuleDialog(
            rule = editingRule,
            onDismiss = viewModel::dismissRuleDialog,
            onConfirm = { rule ->
                if (editingRule != null) {
                    viewModel.updateRule(rule)
                } else {
                    viewModel.addRule(rule)
                }
                viewModel.dismissRuleDialog()
            },
            onDelete = if (editingRule != null) {
                { ruleId ->
                    viewModel.removeRule(ruleId)
                    viewModel.dismissRuleDialog()
                }
            } else {
                null
            },
        )
    }
}
