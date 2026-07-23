package com.edgegesture.evilgodxu.screens.launchblock

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.edgegesture.evilgodxu.R
import com.edgegesture.evilgodxu.data.launchblock.LaunchBlockRule
import com.edgegesture.evilgodxu.data.launchblock.LaunchBlockState
import com.edgegesture.evilgodxu.data.launchblock.launchBlockFlow
import com.edgegesture.evilgodxu.data.launchblock.addLaunchBlockRule
import com.edgegesture.evilgodxu.data.launchblock.removeLaunchBlockRule
import com.edgegesture.evilgodxu.data.launchblock.updateLaunchBlockRule
import com.edgegesture.evilgodxu.data.launchblock.setLaunchBlockEnabled
import com.edgegesture.evilgodxu.screens.launchblock.compact.CompactAssembly
import com.edgegesture.evilgodxu.screens.launchblock.overlay.LaunchBlockRuleDialog
import kotlinx.coroutines.launch

// 启动拦截页 — 页面入口 Composable
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LaunchBlockScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val launchBlockState by context.launchBlockFlow().collectAsState(initial = LaunchBlockState())

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
        CompactAssembly(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            enabled = launchBlockState.enabled,
            rules = launchBlockState.rules,
            onToggleEnabled = { enabled ->
                scope.launch {
                    context.setLaunchBlockEnabled(enabled)
                }
            },
            onAddRule = {
                editingRule = null
                showRuleDialog = true
            },
            onEditRule = { rule ->
                editingRule = rule
                showRuleDialog = true
            },
            onDeleteRule = { ruleId ->
                scope.launch {
                    context.removeLaunchBlockRule(ruleId)
                }
            },
        )
    }

    // 规则编辑/添加对话框
    if (showRuleDialog) {
        LaunchBlockRuleDialog(
            rule = editingRule,
            onDismiss = { showRuleDialog = false },
            onConfirm = { rule ->
                scope.launch {
                    if (editingRule != null) {
                        context.updateLaunchBlockRule(rule)
                    } else {
                        context.addLaunchBlockRule(rule)
                    }
                }
                showRuleDialog = false
            },
            onDelete = if (editingRule != null) { { ruleId ->
                scope.launch {
                    context.removeLaunchBlockRule(ruleId)
                }
                showRuleDialog = false
            } } else null
        )
    }
}
