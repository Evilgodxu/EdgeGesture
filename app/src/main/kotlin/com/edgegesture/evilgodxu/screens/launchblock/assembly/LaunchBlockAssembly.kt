package com.edgegesture.evilgodxu.screens.launchblock.assembly

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.edgegesture.evilgodxu.R
import com.edgegesture.evilgodxu.data.launchblock.LaunchBlockRule
import com.edgegesture.evilgodxu.data.launchblock.LaunchBlockState
import com.edgegesture.evilgodxu.screens.launchblock.component.RuleSection
import com.edgegesture.evilgodxu.ui.component.AppTopBar
import com.edgegesture.evilgodxu.ui.component.SectionCard
import com.edgegesture.evilgodxu.ui.component.SwitchRow

// 启动拦截页组装器：顶栏 + 总开关卡片 + 规则区块，滚动容器沿用重构前的内边距与顺序
@Composable
fun LaunchBlockAssembly(
    state: LaunchBlockState,
    appNames: Map<String, String>,
    onBack: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onResolveAppName: (String) -> Unit,
    onAddRule: () -> Unit,
    onEditRule: (LaunchBlockRule) -> Unit,
    onDeleteRule: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            AppTopBar(
                title = stringResource(R.string.settings_launch_block_title),
                onBack = onBack,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            SectionCard(
                title = stringResource(R.string.settings_launch_block_title),
                titleTopPadding = 8.dp,
            ) {
                SwitchRow(
                    title = stringResource(R.string.launch_block_rule_enabled_title),
                    subtitle = stringResource(R.string.settings_launch_block_desc),
                    checked = state.enabled,
                    onCheckedChange = onToggleEnabled,
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            RuleSection(
                rules = state.rules,
                appNames = appNames,
                onResolveAppName = onResolveAppName,
                onAddRule = onAddRule,
                onEditRule = onEditRule,
                onDeleteRule = onDeleteRule,
            )

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
