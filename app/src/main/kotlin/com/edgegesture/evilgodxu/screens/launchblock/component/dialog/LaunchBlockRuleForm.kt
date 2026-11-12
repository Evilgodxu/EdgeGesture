package com.edgegesture.evilgodxu.screens.launchblock.component.dialog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.edgegesture.evilgodxu.data.launchblock.LaunchBlockRule

// 规则编辑表单：字段与校验状态集中在一处，弹窗只负责渲染与提交
@Stable
class LaunchBlockRuleForm(initial: LaunchBlockRule?) {

    var launcherApp by mutableStateOf(initial?.launcherApp ?: "")
    var targetApp by mutableStateOf(initial?.targetApp ?: "")
    var enabled by mutableStateOf(initial?.enabled ?: true)
    var enableKill by mutableStateOf(initial?.enableKillOnFrequentLaunch ?: false)
    var enableKillTarget by mutableStateOf(initial?.enableKillTarget ?: false)
    var allowKillSystemApp by mutableStateOf(initial?.allowKillSystemApp ?: false)
    var blockDelay by mutableIntStateOf(initial?.blockDelay ?: 0)
    var showTargetError by mutableStateOf(false)

    // 终止相关开关以「已填写来源应用」为前提
    val isKillSwitchEnabled: Boolean
        get() = launcherApp.isNotBlank()

    fun toRule(existing: LaunchBlockRule?): LaunchBlockRule = if (existing != null) {
        existing.copy(
            enabled = enabled,
            launcherApp = launcherApp.trim(),
            targetApp = targetApp.trim(),
            blockDelay = blockDelay,
            enableKillOnFrequentLaunch = enableKill,
            enableKillTarget = enableKillTarget,
            allowKillSystemApp = allowKillSystemApp,
        )
    } else {
        LaunchBlockRule(
            enabled = enabled,
            launcherApp = launcherApp.trim(),
            targetApp = targetApp.trim(),
            blockDelay = blockDelay,
            enableKillOnFrequentLaunch = enableKill,
            enableKillTarget = enableKillTarget,
            allowKillSystemApp = allowKillSystemApp,
        )
    }
}

// 按当前编辑对象创建表单；切换规则时重建
@Composable
fun rememberLaunchBlockRuleForm(rule: LaunchBlockRule?): LaunchBlockRuleForm =
    remember(rule) { LaunchBlockRuleForm(rule) }
