package com.edgegesture.evilgodxu.update

// 版本更新信息
data class UpdateInfo(
    val latestVersion: String,
    val downloadUrl: String,
    val changelog: String,
    // APK 期望哈希：GitHub API 提供的 asset digest，已归一为小写十六进制；空表示不可校验
    val sha256: String = "",
    val isDownloading: Boolean = false,
    val downloadId: Long? = null,
)

// 下载状态
sealed class DownloadState {
    data object Idle : DownloadState()
    data class Downloading(val progress: Float) : DownloadState()
    data object Success : DownloadState()
    data class Failed(val errorMessage: String) : DownloadState()
}

// 更新来源：仓库地址与 Release 接口，供界面展示与检查共用
object UpdateSource {
    private const val GITHUB_OWNER = "Evilgodxu"
    private const val GITHUB_REPO = "EdgeGesture"

    const val REPOSITORY_URL = "https://github.com/$GITHUB_OWNER/$GITHUB_REPO"

    internal const val LATEST_RELEASE_API =
        "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"

    internal const val USER_AGENT = "EdgeGesture/$GITHUB_REPO"
}
