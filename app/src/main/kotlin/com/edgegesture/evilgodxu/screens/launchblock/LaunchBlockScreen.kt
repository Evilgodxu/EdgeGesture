package com.edgegesture.evilgodxu.screens.launchblock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.edgegesture.evilgodxu.R
import com.edgegesture.evilgodxu.data.launchblock.LaunchBlockRule
import com.edgegesture.evilgodxu.screens.gesture.components.GestureSettingsSwitchItem
import com.edgegesture.evilgodxu.screens.settings.components.LaunchBlockRuleDialog
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LaunchBlockScreen(
    onNavigateBack: () -> Unit,
    viewModel: LaunchBlockViewModel = koinViewModel(),
) {
    val launchBlockState by viewModel.state.collectAsStateWithLifecycle()
    val appNames by viewModel.appNames.collectAsStateWithLifecycle()

    var showRuleDialog by remember { mutableStateOf(false) }
    var editingRule by remember { mutableStateOf<LaunchBlockRule?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_launch_block_title),
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.nav_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // 启动拦截开关
            Text(
                text = stringResource(R.string.settings_launch_block_title),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.2.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 10.dp)
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                GestureSettingsSwitchItem(
                    title = stringResource(R.string.launch_block_rule_enabled_title),
                    subtitle = stringResource(R.string.settings_launch_block_desc),
                    checked = launchBlockState.enabled,
                    onCheckedChange = { enabled ->
                        viewModel.setEnabled(enabled)
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 拦截规则标题
            Text(
                text = stringResource(R.string.launch_block_rules_title) + " (" +
                    stringResource(R.string.launch_block_rules_count, launchBlockState.rules.size) + ")",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.2.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
            )

            // 添加规则按钮
            Button(
                onClick = {
                    editingRule = null
                    showRuleDialog = true
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    text = stringResource(R.string.launch_block_add_rule),
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 规则列表
            if (launchBlockState.rules.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        launchBlockState.rules.forEach { rule ->
                            RuleCard(
                                rule = rule,
                                appNames = appNames,
                                onResolveAppName = viewModel::resolveAppName,
                                onEdit = {
                                    editingRule = rule
                                    showRuleDialog = true
                                },
                                onDelete = {
                                    viewModel.removeRule(rule.id)
                                }
                            )
                        }
                    }
                }
            } else {
                // 空状态提示
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Text(
                        text = stringResource(R.string.launch_block_no_rules),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // 规则编辑/添加对话框
    if (showRuleDialog) {
        LaunchBlockRuleDialog(
            rule = editingRule,
            onDismiss = { showRuleDialog = false },
            onConfirm = { rule ->
                if (editingRule != null) {
                    viewModel.updateRule(rule)
                } else {
                    viewModel.addRule(rule)
                }
                showRuleDialog = false
            },
            onDelete = if (editingRule != null) { { ruleId ->
                viewModel.removeRule(ruleId)
                showRuleDialog = false
            } } else null
        )
    }
}

@Composable
private fun RuleCard(
    rule: LaunchBlockRule,
    appNames: Map<String, String>,
    onResolveAppName: (String) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    // 应用名在组合外异步解析，避免同步 IPC 卡顿主线程；解析完成后经 appNames 刷新
    LaunchedEffect(rule.launcherApp, rule.targetApp) {
        onResolveAppName(rule.launcherApp)
        onResolveAppName(rule.targetApp)
    }
    val launcherName = if (rule.launcherApp.isNotBlank()) {
        appNames[rule.launcherApp] ?: rule.launcherApp
    } else ""
    val targetName = appNames[rule.targetApp] ?: rule.targetApp

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (!rule.enabled) {
                    Text(
                        text = stringResource(R.string.launch_block_rule_disabled),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (rule.launcherApp.isNotBlank()) {
                    Text(
                        text = stringResource(
                            R.string.launch_block_rule_launcher,
                            launcherName
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Text(
                    text = stringResource(
                        R.string.launch_block_rule_target,
                        targetName
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
                if (rule.enableKillOnFrequentLaunch || rule.enableKillTarget) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            modifier = Modifier.padding(top = 2.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = stringResource(R.string.launch_block_rule_kill_enabled),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Row {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.launch_block_edit_rule)
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.launch_block_delete_rule),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
