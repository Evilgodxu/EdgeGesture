package com.edgegesture.evilgodxu.screens.blacklist.assembly

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.edgegesture.evilgodxu.R
import com.edgegesture.evilgodxu.screens.blacklist.AppBlacklistUiState
import com.edgegesture.evilgodxu.screens.blacklist.component.AppListItem
import com.edgegesture.evilgodxu.screens.blacklist.component.BlacklistEmptyState
import com.edgegesture.evilgodxu.screens.blacklist.component.BlacklistSearchField
import com.edgegesture.evilgodxu.ui.component.AppTopBar

// 应用黑名单页组装器：列表型页面，滚动区域占据剩余高度，搜索框固定在底部
@Composable
fun AppBlacklistAssembly(
    state: AppBlacklistUiState,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onToggle: (String, Boolean) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            AppTopBar(
                title = stringResource(R.string.settings_blacklist_title),
                onBack = onBack,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                // innerPadding 已含导航栏内边距，消费后再补 IME 内边距，避免重复叠加
                .consumeWindowInsets(innerPadding)
                .imePadding()
                .padding(horizontal = 16.dp),
        ) {
            when {
                state.isLoading && state.apps.isEmpty() -> LoadingState()
                state.showEmptyState -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    BlacklistEmptyState(hasPermission = state.hasPermission, onRetry = onRetry)
                }
                else -> {
                    if (state.visibleApps.isEmpty() && state.isSearching) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(R.string.settings_blacklist_no_results, state.debouncedQuery),
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                        ) {
                            items(state.visibleApps, key = { it.packageName }) { app ->
                                AppListItem(
                                    app = app,
                                    isBlacklisted = app.packageName in state.blacklist,
                                    onToggle = { checked -> onToggle(app.packageName, checked) },
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    BlacklistSearchField(
                        query = state.query,
                        onQueryChange = onQueryChange,
                        onClearQuery = onClearQuery,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// 首次加载：列表为空且有加载中标记时展示
@Composable
private fun ColumnScope.LoadingState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.settings_blacklist_loading),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
