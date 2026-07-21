package com.edgegesture.evilgodxu.screens.settings.components

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.edgegesture.evilgodxu.R
import com.edgegesture.evilgodxu.data.app.AppRepository
import com.edgegesture.evilgodxu.data.app.AppInfo
import com.edgegesture.evilgodxu.data.gesture.addToAppSwitchBlacklist
import com.edgegesture.evilgodxu.data.gesture.appSwitchBlacklistFlow
import com.edgegesture.evilgodxu.data.gesture.removeFromAppSwitchBlacklist
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

// 应用切换黑名单对话框
// 使用 AppRepository 缓存实现即时加载，无需等待扫描
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSwitchBlacklistDialog(
    onDismiss: () -> Unit,
    appRepository: AppRepository = koinInject()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val blacklist by context.appSwitchBlacklistFlow().collectAsState(initial = emptySet())

    // 从缓存仓库获取应用列表，实现即时显示
    val allApps by appRepository.appsFlow.collectAsState()
    val isLoading by appRepository.isLoading.collectAsState()
    val hasPermission = remember { appRepository.hasQueryPermission() }

    var searchQuery by remember { mutableStateOf("") }
    var debouncedQuery by remember { mutableStateOf("") }

    // 防抖：用户停止输入 300ms 后才触发搜索过滤
    LaunchedEffect(searchQuery) {
        delay(300)
        debouncedQuery = searchQuery
    }

    val filteredApps by remember(allApps, debouncedQuery) {
        derivedStateOf {
            if (debouncedQuery.isBlank()) {
                allApps
            } else {
                val lowerQuery = debouncedQuery.lowercase()
                allApps
                    .filter {
                        it.appName.lowercase().contains(lowerQuery) ||
                        it.packageName.lowercase().contains(lowerQuery)
                    }
                    .sortedByDescending { app ->
                        val name = app.appName.lowercase()
                        val pkg = app.packageName.lowercase()
                        when {
                            name == lowerQuery -> 4
                            name.startsWith(lowerQuery) -> 3
                            name.contains(lowerQuery) -> 2
                            pkg.contains(lowerQuery) -> 1
                            else -> 0
                        }
                    }
            }
        }
    }

    val isSearching = debouncedQuery.isNotBlank()

    // 是否需要显示空状态（无数据且无加载中）
    val showEmptyState = !isLoading && allApps.isEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(R.string.settings_blacklist_dialog_title),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxHeight(0.7f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when {
                    // 加载中状态
                    isLoading && allApps.isEmpty() -> {
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.settings_blacklist_loading),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    // 空状态：无权限或无数据
                    showEmptyState -> {
                        EmptyStateView(
                            hasPermission = hasPermission,
                            onRetry = {
                                scope.launch {
                                    appRepository.refreshAppsIfPermitted()
                                }
                            }
                        )
                    }
                    // 正常显示应用列表
                    else -> {
                        // 应用列表（或搜索无结果提示）
                        if (filteredApps.isEmpty() && isSearching) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.settings_blacklist_no_results, debouncedQuery),
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            LazyColumn(modifier = Modifier.weight(1f)) {
                                items(filteredApps, key = { it.packageName }) { app ->
                                    val isBlacklisted = app.packageName in blacklist
                                    AppListItem(
                                        app = app,
                                        isBlacklisted = isBlacklisted,
                                        onToggle = { checked ->
                                            scope.launch {
                                                if (checked) {
                                                    context.addToAppSwitchBlacklist(setOf(app.packageName))
                                                } else {
                                                    context.removeFromAppSwitchBlacklist(setOf(app.packageName))
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 搜索框在底部
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text(stringResource(R.string.settings_blacklist_search_hint)) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = null)
                                    }
                                }
                            },
                            singleLine = true
                        )
                    }
                }

                Spacer(modifier = Modifier.padding(4.dp))
            }
        },
        confirmButton = {}
    )
}

// 空状态视图
@Composable
private fun EmptyStateView(
    hasPermission: Boolean,
    onRetry: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (!hasPermission) {
            Text(
                text = stringResource(R.string.settings_blacklist_no_permission),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                // 打开应用信息界面，用户可手动开启权限
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = "package:${context.packageName}".toUri()
                }
                context.startActivity(intent)
            }) {
                Text(stringResource(R.string.settings_blacklist_go_settings))
            }
        } else {
            Text(
                text = stringResource(R.string.settings_blacklist_empty),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.settings_blacklist_refresh))
            }
        }
    }
}

@Composable
private fun AppListItem(
    app: AppInfo,
    isBlacklisted: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!isBlacklisted) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isBlacklisted,
            onCheckedChange = onToggle
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.appName,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = app.packageName,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
        if (app.isSystemApp) {
            Text(
                text = stringResource(R.string.settings_blacklist_system_tag),
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
