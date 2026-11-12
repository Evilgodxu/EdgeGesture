package com.edgegesture.evilgodxu.screens.blacklist

import com.edgegesture.evilgodxu.data.app.AppInfo

// 应用搜索的过滤与排序：完全匹配 > 前缀匹配 > 名称包含 > 包名包含。
// 与界面无关的纯逻辑，单独放置便于单测覆盖
fun filterAppsByQuery(apps: List<AppInfo>, query: String): List<AppInfo> {
    if (query.isBlank()) return apps
    val lowerQuery = query.lowercase()
    return apps
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
