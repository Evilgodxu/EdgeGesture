package com.byss.jh.data.app

// 应用信息数据类
// 精简字段优化序列化性能和内存占用
data class AppInfo(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean,
    val lastUpdateTime: Long = System.currentTimeMillis()
)
