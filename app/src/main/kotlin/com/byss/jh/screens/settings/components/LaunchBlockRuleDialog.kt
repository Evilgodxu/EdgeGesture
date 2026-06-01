package com.byss.jh.screens.settings.components

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.byss.jh.R
import com.byss.jh.data.launchblock.LaunchBlockRule
import com.byss.jh.data.shizuku.ShizukuManager
import com.byss.jh.data.shizuku.ShizukuState
import rikka.shizuku.Shizuku

@Composable
fun LaunchBlockRuleDialog(
    rule: LaunchBlockRule? = null,
    onDismiss: () -> Unit,
    onConfirm: (LaunchBlockRule) -> Unit,
    onDelete: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    var launcherApp by remember { mutableStateOf(rule?.launcherApp ?: "") }
    var targetApp by remember { mutableStateOf(rule?.targetApp ?: "") }
    var enableKill by remember { mutableStateOf(rule?.enableKillOnFrequentLaunch ?: false) }
    var enableKillTarget by remember { mutableStateOf(rule?.enableKillTarget ?: false) }
    var allowKillSystemApp by remember { mutableStateOf(rule?.allowKillSystemApp ?: false) }
    var showError by remember { mutableStateOf(false) }

    // Shizuku 状态
    var shizukuState by remember { mutableStateOf<ShizukuState>(ShizukuState.NotRunning) }
    val shizukuPermissionCode = 1002

    DisposableEffect(Unit) {
        ShizukuManager.init(context)
        shizukuState = ShizukuManager.state.value

        val listener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == shizukuPermissionCode) {
                shizukuState = if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    ShizukuState.Granted
                } else {
                    ShizukuState.Denied
                }
            }
        }
        ShizukuManager.addPermissionListener(listener)

        onDispose {
            ShizukuManager.removePermissionListener()
        }
    }

    val isEditing = rule != null
    val isKillSwitchEnabled = launcherApp.isNotBlank()

    // 当高频启动检测和终止被启动者都关闭时，自动关闭允许终止系统程序
    LaunchedEffect(enableKill, enableKillTarget) {
        if (!enableKill && !enableKillTarget && allowKillSystemApp) {
            allowKillSystemApp = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (isEditing) 
                    stringResource(R.string.launch_block_edit_title) 
                else 
                    stringResource(R.string.launch_block_add_title)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = launcherApp,
                    onValueChange = { 
                        launcherApp = it
                        if (it.isBlank()) enableKill = false
                    },
                    label = { Text(stringResource(R.string.launch_block_launcher_label)) },
                    placeholder = { Text(stringResource(R.string.launch_block_launcher_hint)) },
                    supportingText = { Text(stringResource(R.string.launch_block_launcher_supporting)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = targetApp,
                    onValueChange = { 
                        targetApp = it
                        showError = false
                    },
                    label = { Text(stringResource(R.string.launch_block_target_label)) },
                    placeholder = { Text(stringResource(R.string.launch_block_target_hint)) },
                    supportingText = { 
                        if (showError && targetApp.isBlank()) {
                            Text(
                                stringResource(R.string.launch_block_target_required),
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            Text(stringResource(R.string.launch_block_target_supporting))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = showError && targetApp.isBlank()
                )

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.launch_block_kill_switch_title),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = stringResource(R.string.launch_block_kill_switch_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!isKillSwitchEnabled) {
                            Text(
                                text = stringResource(R.string.launch_block_kill_switch_disabled_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        } else if (shizukuState !is ShizukuState.Granted) {
                            val shizukuHint = when (shizukuState) {
                                is ShizukuState.NotInstalled -> stringResource(R.string.launch_block_shizuku_not_installed_short)
                                is ShizukuState.NotRunning -> stringResource(R.string.launch_block_shizuku_not_running_short)
                                is ShizukuState.Denied -> stringResource(R.string.launch_block_shizuku_denied_short)
                                else -> ""
                            }
                            if (shizukuHint.isNotEmpty()) {
                                Text(
                                    text = shizukuHint,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                    Switch(
                        checked = enableKill,
                        onCheckedChange = { checked ->
                            if (checked && shizukuState !is ShizukuState.Granted) {
                                // 需要 Shizuku 权限
                                when (shizukuState) {
                                    is ShizukuState.NotInstalled -> {
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            data = Uri.parse("https://shizuku.rikka.app/")
                                        }
                                        context.startActivity(intent)
                                    }
                                    is ShizukuState.NotRunning -> {
                                        val intent = context.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
                                        if (intent != null) {
                                            context.startActivity(intent)
                                        }
                                    }
                                    is ShizukuState.Denied, is ShizukuState.Waiting -> {
                                        ShizukuManager.requestPermission(shizukuPermissionCode)
                                    }
                                    else -> {}
                                }
                            } else {
                                enableKill = checked
                            }
                        },
                        enabled = isKillSwitchEnabled
                    )
                }

                HorizontalDivider()

                // 终止被启动者开关
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.launch_block_kill_target_title),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = stringResource(R.string.launch_block_kill_target_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (shizukuState !is ShizukuState.Granted) {
                            val shizukuHint = when (shizukuState) {
                                is ShizukuState.NotInstalled -> stringResource(R.string.launch_block_shizuku_not_installed_short)
                                is ShizukuState.NotRunning -> stringResource(R.string.launch_block_shizuku_not_running_short)
                                is ShizukuState.Denied -> stringResource(R.string.launch_block_shizuku_denied_short)
                                else -> ""
                            }
                            if (shizukuHint.isNotEmpty()) {
                                Text(
                                    text = shizukuHint,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                    Switch(
                        checked = enableKillTarget,
                        onCheckedChange = { checked ->
                            if (checked && shizukuState !is ShizukuState.Granted) {
                                when (shizukuState) {
                                    is ShizukuState.NotInstalled -> {
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            data = Uri.parse("https://shizuku.rikka.app/")
                                        }
                                        context.startActivity(intent)
                                    }
                                    is ShizukuState.NotRunning -> {
                                        val intent = context.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
                                        if (intent != null) {
                                            context.startActivity(intent)
                                        }
                                    }
                                    is ShizukuState.Denied, is ShizukuState.Waiting -> {
                                        ShizukuManager.requestPermission(shizukuPermissionCode)
                                    }
                                    else -> {}
                                }
                            } else {
                                enableKillTarget = checked
                            }
                        }
                    )
                }

                HorizontalDivider()

                // 允许终止系统程序开关
                val canEnableAllowKillSystem = enableKill || enableKillTarget
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.launch_block_allow_kill_system_title),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (canEnableAllowKillSystem) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        Text(
                            text = stringResource(R.string.launch_block_allow_kill_system_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (shizukuState !is ShizukuState.Granted) {
                            val shizukuHint = when (shizukuState) {
                                is ShizukuState.NotInstalled -> stringResource(R.string.launch_block_shizuku_not_installed_short)
                                is ShizukuState.NotRunning -> stringResource(R.string.launch_block_shizuku_not_running_short)
                                is ShizukuState.Denied -> stringResource(R.string.launch_block_shizuku_denied_short)
                                else -> ""
                            }
                            if (shizukuHint.isNotEmpty()) {
                                Text(
                                    text = shizukuHint,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                    Switch(
                        checked = allowKillSystemApp,
                        onCheckedChange = { checked ->
                            if (checked && shizukuState !is ShizukuState.Granted) {
                                when (shizukuState) {
                                    is ShizukuState.NotInstalled -> {
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            data = Uri.parse("https://shizuku.rikka.app/")
                                        }
                                        context.startActivity(intent)
                                    }
                                    is ShizukuState.NotRunning -> {
                                        val intent = context.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
                                        if (intent != null) {
                                            context.startActivity(intent)
                                        }
                                    }
                                    is ShizukuState.Denied, is ShizukuState.Waiting -> {
                                        ShizukuManager.requestPermission(shizukuPermissionCode)
                                    }
                                    else -> {}
                                }
                            } else {
                                allowKillSystemApp = checked
                            }
                        },
                        enabled = canEnableAllowKillSystem
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (targetApp.isBlank()) {
                        showError = true
                        return@TextButton
                    }
                    val newRule = if (isEditing && rule != null) {
                        rule.copy(
                            launcherApp = launcherApp.trim(),
                            targetApp = targetApp.trim(),
                            enableKillOnFrequentLaunch = enableKill,
                            enableKillTarget = enableKillTarget,
                            allowKillSystemApp = allowKillSystemApp
                        )
                    } else {
                        LaunchBlockRule(
                            launcherApp = launcherApp.trim(),
                            targetApp = targetApp.trim(),
                            enableKillOnFrequentLaunch = enableKill,
                            enableKillTarget = enableKillTarget,
                            allowKillSystemApp = allowKillSystemApp
                        )
                    }
                    onConfirm(newRule)
                }
            ) {
                Text(stringResource(R.string.dialog_confirm))
            }
        },
        dismissButton = {
            Row {
                if (isEditing && onDelete != null && rule != null) {
                    TextButton(
                        onClick = { onDelete(rule.id) },
                        colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(stringResource(R.string.dialog_delete))
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        }
    )
}
