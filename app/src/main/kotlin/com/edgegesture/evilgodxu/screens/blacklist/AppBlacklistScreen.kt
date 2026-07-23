package com.edgegesture.evilgodxu.screens.blacklist

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
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
import com.edgegesture.evilgodxu.data.app.AppRepository
import com.edgegesture.evilgodxu.data.gesture.addToAppSwitchBlacklist
import com.edgegesture.evilgodxu.data.gesture.appSwitchBlacklistFlow
import com.edgegesture.evilgodxu.data.gesture.removeFromAppSwitchBlacklist
import com.edgegesture.evilgodxu.screens.blacklist.compact.CompactAssembly
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

// 应用切换黑名单页 — 页面入口 Composable
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBlacklistScreen(
    onNavigateBack: () -> Unit,
    appRepository: AppRepository = koinInject()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val blacklist by context.appSwitchBlacklistFlow().collectAsState(initial = emptySet())

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_blacklist_title),
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
            isLoading = isLoading,
            hasPermission = hasPermission,
            filteredApps = filteredApps,
            isSearching = isSearching,
            searchQuery = searchQuery,
            blacklist = blacklist,
            debouncedQuery = debouncedQuery,
            onSearchQueryChange = { searchQuery = it },
            onClearSearch = { searchQuery = "" },
            onRefreshApps = {
                scope.launch {
                    appRepository.refreshAppsIfPermitted()
                }
            },
            onToggleApp = { packageName, checked ->
                scope.launch {
                    if (checked) {
                        context.addToAppSwitchBlacklist(setOf(packageName))
                    } else {
                        context.removeFromAppSwitchBlacklist(setOf(packageName))
                    }
                }
            },
        )
    }
}
