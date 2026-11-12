package com.edgegesture.evilgodxu.screens.blacklist

import com.edgegesture.evilgodxu.data.app.AppInfo

// 应用黑名单页 UI 状态：查询、过滤结果、黑名单集合与加载/权限状态
data class AppBlacklistUiState(
    val query: String = "",
    val debouncedQuery: String = "",
    val apps: List<AppInfo> = emptyList(),
    val visibleApps: List<AppInfo> = emptyList(),
    val blacklist: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val hasPermission: Boolean = false,
) {
    val isSearching: Boolean
        get() = debouncedQuery.isNotBlank()

    val showEmptyState: Boolean
        get() = !isLoading && apps.isEmpty()
}
