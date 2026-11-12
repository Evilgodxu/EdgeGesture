package com.edgegesture.evilgodxu.update

import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

// 版本检查：拉取 GitHub Release、比较版本并给出待更新信息；网络与解析异常向上抛出
class UpdateChecker(
    private val currentVersion: String,
) {

    suspend fun check(ignoredVersion: String?): UpdateInfo? {
        val release = withContext(Dispatchers.IO) {
            json.decodeFromString<GitHubRelease>(readJson(UpdateSource.LATEST_RELEASE_API))
        }

        val latest = normalizeVersion(release.tag_name)
        val ignored = ignoredVersion?.let(::normalizeVersion)

        if (!isNewerVersion(latest, normalizeVersion(currentVersion)) || latest == ignored) {
            return null
        }

        val apkAsset = release.assets.firstOrNull { it.name.endsWith(".apk") }
            ?: throw IllegalStateException("GitHub Release 未提供可用 APK")
        val downloadUrl = apkAsset.browser_download_url.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("GitHub Release 未提供可用 APK")

        // 期望哈希取自 asset digest（形如 "sha256:<hex>"），归一为小写十六进制；
        // 缺失时留空，下载阶段会因无法校验而拒绝安装
        val sha256 = apkAsset.digest.substringAfter("sha256:", "").trim().lowercase()

        return UpdateInfo(
            latestVersion = latest,
            downloadUrl = downloadUrl,
            changelog = release.body,
            sha256 = sha256,
        )
    }

    private fun readJson(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("User-Agent", UpdateSource.USER_AGENT)
            .build()
        return client.newCall(request).execute().use { response ->
            val text = response.body.string()
            if (!response.isSuccessful) {
                throw IllegalStateException("更新服务响应异常: HTTP ${response.code}")
            }
            text
        }
    }

    private fun normalizeVersion(version: String): String = version.trim().trimStart('v', 'V')

    // GitHub Release 响应模型（仅解析需要的字段）
    @Serializable
    private data class GitHubRelease(
        val tag_name: String = "",
        val body: String = "",
        val assets: List<GitHubAsset> = emptyList(),
    )

    @Serializable
    private data class GitHubAsset(
        val name: String = "",
        val browser_download_url: String = "",
        // GitHub API 自 2025-06 起为 release asset 提供 "sha256:<hex>" 摘要；缺失或旧资源可能为空
        val digest: String = "",
    )

    private companion object {
        val json = Json { ignoreUnknownKeys = true }

        // 更新检查为独立的一次性请求，使用私有客户端避免与其它模块的连接池互相影响
        val client: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }
}

// 版本号语义比较：逐段按数值比较，缺位按 0 处理
internal fun isNewerVersion(latest: String, current: String): Boolean {
    val latestParts = latest.split(".").mapNotNull { it.toIntOrNull() }
    val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }
    for (index in 0 until maxOf(latestParts.size, currentParts.size)) {
        val latestValue = latestParts.getOrElse(index) { 0 }
        val currentValue = currentParts.getOrElse(index) { 0 }
        if (latestValue > currentValue) return true
        if (latestValue < currentValue) return false
    }
    return false
}
