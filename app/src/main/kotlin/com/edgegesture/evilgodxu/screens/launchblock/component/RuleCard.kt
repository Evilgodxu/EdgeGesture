package com.edgegesture.evilgodxu.screens.launchblock.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.edgegesture.evilgodxu.R
import com.edgegesture.evilgodxu.data.launchblock.LaunchBlockRule

// 单条拦截规则卡片：显示来源/目标应用与拦截开关状态，提供编辑与删除入口
@Composable
fun RuleCard(
    rule: LaunchBlockRule,
    appNames: Map<String, String>,
    onResolveAppName: (String) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
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
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (!rule.enabled) {
                    Text(
                        text = stringResource(R.string.launch_block_rule_disabled),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (rule.launcherApp.isNotBlank()) {
                    Text(
                        text = stringResource(R.string.launch_block_rule_launcher, launcherName),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Text(
                    text = stringResource(R.string.launch_block_rule_target, targetName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                if (rule.enableKillOnFrequentLaunch || rule.enableKillTarget) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            modifier = Modifier.padding(top = 2.dp),
                            tint = MaterialTheme.colorScheme.error,
                        )
                        Text(
                            text = stringResource(R.string.launch_block_rule_kill_enabled),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            Row {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.launch_block_edit_rule),
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.launch_block_delete_rule),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}
