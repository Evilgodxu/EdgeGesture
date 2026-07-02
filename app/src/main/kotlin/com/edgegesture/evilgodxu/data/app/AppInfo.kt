package com.edgegesture.evilgodxu.data.app

// 应用信息数据类
// 精简字段优化序列化性能和内存占用
// iconPath 为本地缓存图标路径，空表示未缓存
// lastUpdateTime 记录缓存时间，用于过期判断
// sourcePath 为应用安装包路径，用于后续扩展功能（如导出 APK）
// versionName 为应用版本名称，用于后续扩展功能
data class AppInfo(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean,
    val iconPath: String = "",
    val versionName: String = "",
    val sourcePath: String = "",
    val lastUpdateTime: Long = System.currentTimeMillis()
)
