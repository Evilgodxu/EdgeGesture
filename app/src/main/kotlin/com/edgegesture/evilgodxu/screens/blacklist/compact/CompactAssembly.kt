package com.edgegesture.evilgodxu.screens.blacklist.compact

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.edgegesture.evilgodxu.data.app.AppInfo
import com.edgegesture.evilgodxu.screens.blacklist.compact.app_list_area.AppListArea

// 紧凑视图物理空间组装器
@Composable
fun CompactAssembly(
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
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        AppListArea(
            isLoading = isLoading,
            hasPermission = hasPermission,
            filteredApps = filteredApps,
            isSearching = isSearching,
            searchQuery = searchQuery,
            blacklist = blacklist,
            debouncedQuery = debouncedQuery,
            onSearchQueryChange = onSearchQueryChange,
            onClearSearch = onClearSearch,
            onRefreshApps = onRefreshApps,
            onToggleApp = onToggleApp,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}
