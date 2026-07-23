package com.edgegesture.evilgodxu.screens.blacklist.compact.app_list_area

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.edgegesture.evilgodxu.R
import com.edgegesture.evilgodxu.data.app.AppInfo
import com.edgegesture.evilgodxu.screens.blacklist.compact.app_list_area.app_list_item.AppListItem
import com.edgegesture.evilgodxu.screens.blacklist.compact.app_list_area.empty_state.EmptyState
import com.edgegesture.evilgodxu.screens.blacklist.compact.app_list_area.loading_state.LoadingState
import com.edgegesture.evilgodxu.screens.blacklist.compact.app_list_area.search_bar.SearchBar

// 应用列表 Area — 仅负责组件路由与排列，不含业务 UI 实现
@Composable
fun AppListArea(
    isLoading: Boolean,
    hasPermission: Boolean,
    filteredApps: List<AppInfo>,
    isSearching: Boolean,
    searchQuery: String,
    blacklist: Set<String>,
    debouncedQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onRefreshApps: () -> Unit,
    onToggleApp: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    // 加载中 → LoadingState
    if (isLoading && filteredApps.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            LoadingState()
        }
        return
    }

    // 空数据（无搜索） → EmptyState
    if (!isLoading && filteredApps.isEmpty() && !isSearching) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState(
                hasPermission = hasPermission,
                onRefreshApps = onRefreshApps,
            )
        }
        return
    }

    // 正常/搜索状态 → 列表（或搜索无结果）+ 搜索框
    Column(modifier = modifier) {
        if (filteredApps.isEmpty() && isSearching) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.settings_blacklist_no_results, debouncedQuery),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(filteredApps, key = { it.packageName }) { app ->
                    val isBlacklisted = app.packageName in blacklist
                    AppListItem(
                        app = app,
                        isBlacklisted = isBlacklisted,
                        onToggle = { checked ->
                            onToggleApp(app.packageName, checked)
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        SearchBar(
            query = searchQuery,
            onQueryChange = onSearchQueryChange,
            onClear = onClearSearch,
        )
    }
}
