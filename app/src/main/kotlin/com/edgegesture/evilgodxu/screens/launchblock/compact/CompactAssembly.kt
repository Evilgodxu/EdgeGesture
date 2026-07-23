package com.edgegesture.evilgodxu.screens.launchblock.compact

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.edgegesture.evilgodxu.data.launchblock.LaunchBlockRule
import com.edgegesture.evilgodxu.screens.launchblock.compact.rules_area.RulesArea
import com.edgegesture.evilgodxu.screens.launchblock.compact.switch_area.SwitchArea

// 紧凑视图物理空间组装器
// 仅定义 Column 容器和排列顺序，不包含业务逻辑
@Composable
fun CompactAssembly(
    enabled: Boolean,
    rules: List<LaunchBlockRule>,
    onToggleEnabled: (Boolean) -> Unit,
    onAddRule: () -> Unit,
    onEditRule: (LaunchBlockRule) -> Unit,
    onDeleteRule: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        SwitchArea(
            enabled = enabled,
            onToggleEnabled = onToggleEnabled,
        )

        RulesArea(
            rules = rules,
            onAddRule = onAddRule,
            onEditRule = onEditRule,
            onDeleteRule = onDeleteRule,
        )
    }
}
