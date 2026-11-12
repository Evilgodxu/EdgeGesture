package com.edgegesture.evilgodxu.screens.launchblock.component.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.edgegesture.evilgodxu.R
import com.edgegesture.evilgodxu.data.launchblock.LaunchBlockRule
import com.edgegesture.evilgodxu.data.shizuku.ShizukuManager
import com.edgegesture.evilgodxu.ui.theme.AppSwitch

// 拦截规则编辑弹窗：文本字段、启用状态、终止开关与拦截时机，提交时组装为规则
@Composable
fun LaunchBlockRuleDialog(
    rule: LaunchBlockRule? = null,
    onDismiss: () -> Unit,
    onConfirm: (LaunchBlockRule) -> Unit,
    onDelete: ((String) -> Unit)? = null,
) {
    val form = rememberLaunchBlockRuleForm(rule)
    val shizukuState by ShizukuManager.state.collectAsStateWithLifecycle()
    val isEditing = rule != null

    // 高频启动检测与终止被启动者都关闭时，允许终止系统程序失去意义，自动关闭
    LaunchedEffect(form.enableKill, form.enableKillTarget) {
        if (!form.enableKill && !form.enableKillTarget && form.allowKillSystemApp) {
            form.allowKillSystemApp = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (isEditing) {
                    stringResource(R.string.launch_block_edit_title)
                } else {
                    stringResource(R.string.launch_block_add_title)
                },
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                OutlinedTextField(
                    value = form.launcherApp,
                    onValueChange = {
                        form.launcherApp = it
                        if (it.isBlank()) form.enableKill = false
                    },
                    label = { Text(stringResource(R.string.launch_block_launcher_label)) },
                    placeholder = { Text(stringResource(R.string.launch_block_launcher_hint)) },
                    supportingText = { Text(stringResource(R.string.launch_block_launcher_supporting)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                OutlinedTextField(
                    value = form.targetApp,
                    onValueChange = {
                        form.targetApp = it
                        form.showTargetError = false
                    },
                    label = { Text(stringResource(R.string.launch_block_target_label)) },
                    placeholder = { Text(stringResource(R.string.launch_block_target_hint)) },
                    supportingText = {
                        if (form.showTargetError && form.targetApp.isBlank()) {
                            Text(
                                stringResource(R.string.launch_block_target_required),
                                color = MaterialTheme.colorScheme.error,
                            )
                        } else {
                            Text(stringResource(R.string.launch_block_target_supporting))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = form.showTargetError && form.targetApp.isBlank(),
                )

                HorizontalDivider()

                EnabledSwitchRow(
                    checked = form.enabled,
                    onCheckedChange = { form.enabled = it },
                )

                HorizontalDivider()

                RuleKillSwitches(form = form, shizukuState = shizukuState)

                HorizontalDivider()

                DelayChips(
                    blockDelay = form.blockDelay,
                    onDelaySelected = { form.blockDelay = it },
                )

                DialogActions(
                    ruleId = rule?.id,
                    onDelete = onDelete,
                    onDismiss = onDismiss,
                    onConfirm = {
                        if (form.targetApp.isBlank()) {
                            form.showTargetError = true
                            return@DialogActions
                        }
                        onConfirm(form.toRule(rule))
                    },
                )
            }
        },
        confirmButton = {},
        dismissButton = {},
    )
}

// 规则启用开关
@Composable
private fun EnabledSwitchRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.launch_block_rule_enabled_title),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = stringResource(R.string.launch_block_rule_enabled_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AppSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

// 拦截时机：立即 / 500ms / 1000ms
@Composable
private fun DelayChips(
    blockDelay: Int,
    onDelaySelected: (Int) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.launch_block_delay_title),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = stringResource(R.string.launch_block_delay_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            listOf(0, 500, 1000).forEach { delay ->
                FilterChip(
                    selected = blockDelay == delay,
                    onClick = { onDelaySelected(delay) },
                    label = {
                        Text(
                            text = when (delay) {
                                0 -> stringResource(R.string.launch_block_delay_immediate)
                                500 -> stringResource(R.string.launch_block_delay_500ms)
                                else -> stringResource(R.string.launch_block_delay_1000ms)
                            },
                            style = MaterialTheme.typography.bodySmall,
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                )
            }
        }
    }
}

// 底部操作：删除（编辑态）/ 取消 / 确认
@Composable
private fun DialogActions(
    ruleId: String?,
    onDelete: ((String) -> Unit)?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (ruleId != null && onDelete != null) {
            Button(
                onClick = { onDelete(ruleId) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = Color.White,
                ),
            ) {
                Text(stringResource(R.string.dialog_delete), fontWeight = FontWeight.SemiBold)
            }
        }

        Button(
            onClick = onDismiss,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
        ) {
            Text(stringResource(R.string.dialog_cancel), fontWeight = FontWeight.SemiBold)
        }

        Button(
            onClick = onConfirm,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
        ) {
            Text(stringResource(R.string.dialog_confirm), fontWeight = FontWeight.SemiBold)
        }
    }
}
