package com.edgegesture.evilgodxu.screens.launchblock.component.dialog

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import com.edgegesture.evilgodxu.R
import com.edgegesture.evilgodxu.data.shizuku.ShizukuManager
import com.edgegesture.evilgodxu.data.shizuku.ShizukuState
import com.edgegesture.evilgodxu.ui.theme.AppSwitch

// 终止相关开关：需要 Shizuku 权限，未授权时先引导安装/启动/授权
@Composable
fun RuleKillSwitches(
    form: LaunchBlockRuleForm,
    shizukuState: ShizukuState,
) {
    val context = LocalContext.current
    val shizukuHint = shizukuHint(shizukuState)
    val killSwitchHint = when {
        !form.isKillSwitchEnabled -> stringResource(R.string.launch_block_kill_switch_disabled_hint)
        shizukuState !is ShizukuState.Granted -> shizukuHint
        else -> null
    }

    RuleSwitchRow(
        title = stringResource(R.string.launch_block_kill_switch_title),
        description = stringResource(R.string.launch_block_kill_switch_desc),
        hint = killSwitchHint,
        checked = form.enableKill,
        enabled = form.isKillSwitchEnabled,
        onCheckedChange = { checked ->
            form.enableKill = resolveKillSwitch(checked, shizukuState, context)
        },
    )

    HorizontalDivider()

    RuleSwitchRow(
        title = stringResource(R.string.launch_block_kill_target_title),
        description = stringResource(R.string.launch_block_kill_target_desc),
        hint = shizukuHint.takeIf { shizukuState !is ShizukuState.Granted },
        checked = form.enableKillTarget,
        onCheckedChange = { checked ->
            form.enableKillTarget = resolveKillSwitch(checked, shizukuState, context)
        },
    )

    HorizontalDivider()

    val canAllowKillSystem = form.enableKill || form.enableKillTarget
    RuleSwitchRow(
        title = stringResource(R.string.launch_block_allow_kill_system_title),
        description = stringResource(R.string.launch_block_allow_kill_system_desc),
        hint = shizukuHint.takeIf { shizukuState !is ShizukuState.Granted },
        checked = form.allowKillSystemApp,
        enabled = canAllowKillSystem,
        onCheckedChange = { checked ->
            form.allowKillSystemApp = resolveKillSwitch(checked, shizukuState, context)
        },
    )
}

// 开关行：标题 + 说明 + 可选提示 + 开关
@Composable
private fun RuleSwitchRow(
    title: String,
    description: String,
    hint: String?,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!hint.isNullOrEmpty()) {
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        AppSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
        )
    }
}

// 未授权时先走引导：已安装则拉起或请求授权，未安装则打开官网
private fun resolveKillSwitch(
    checked: Boolean,
    shizukuState: ShizukuState,
    context: Context,
): Boolean {
    if (!checked || shizukuState is ShizukuState.Granted) return checked
    when (shizukuState) {
        is ShizukuState.NotInstalled -> {
            context.startActivity(Intent(Intent.ACTION_VIEW, "https://shizuku.rikka.app/".toUri()))
        }

        is ShizukuState.NotRunning -> {
            context.packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE)?.let(context::startActivity)
        }

        is ShizukuState.Denied, is ShizukuState.Waiting ->
            ShizukuManager.requestPermission(SHIZUKU_PERMISSION_CODE)
    }
    // 授权未完成前不打开开关，避免写入不可用的配置
    return false
}

@Composable
private fun shizukuHint(state: ShizukuState): String = when (state) {
    is ShizukuState.NotInstalled -> stringResource(R.string.launch_block_shizuku_not_installed_short)
    is ShizukuState.NotRunning -> stringResource(R.string.launch_block_shizuku_not_running_short)
    is ShizukuState.Denied -> stringResource(R.string.launch_block_shizuku_denied_short)
    else -> ""
}

private const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"

private const val SHIZUKU_PERMISSION_CODE = 1002
