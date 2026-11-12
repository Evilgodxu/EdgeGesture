package com.edgegesture.evilgodxu.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.edgegesture.evilgodxu.R
import com.edgegesture.evilgodxu.log.CrashLogManager
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

// APK 下载与安装：复用已校验的同版本安装包，下载后校验 SHA-256 再拉起系统安装界面
class UpdateDownloader(
    private val context: Context,
) {

    /**
     * 下载 APK 并引导安装
     *
     * @param onProgress 进度回调，-1f 表示失败
     * @return true 表示已启动安装界面
     */
    suspend fun downloadAndInstall(
        updateInfo: UpdateInfo,
        onProgress: (Float) -> Unit = {},
    ): Boolean {
        val fileName = "EdgeGesture_${updateInfo.latestVersion}.apk"
        val outFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)

        // 下载、轮询、文件操作全部在 IO 线程执行，避免 DownloadManager IPC 阻塞主线程
        return withContext(Dispatchers.IO) {
            // 复用上次已下载但未安装的同一版本安装包，避免重复下载
            if (outFile.exists()) {
                if (verifyApkHash(outFile, updateInfo.sha256)) {
                    onProgress(1f)
                    installApk(outFile)
                    return@withContext true
                }
                outFile.delete()
            }

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val request = DownloadManager.Request(requireHttps(updateInfo.downloadUrl).toUri())
                .setTitle(context.getString(R.string.update_notification_title))
                .setDescription(context.getString(R.string.update_notification_downloading, updateInfo.latestVersion))
                .setDestinationUri(Uri.fromFile(outFile))
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            val downloadId = downloadManager.enqueue(request)
            var lastProgressBytes = -1L
            var stallCount = 0

            // 轮询下载进度
            while (true) {
                val (status, done, total) = queryProgress(downloadManager, downloadId) ?: break

                when (status) {
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        onProgress(1f)
                        // 完整性校验：与 GitHub 提供的 SHA-256 比对，不符或不可校验一律拒绝安装
                        if (!verifyApkHash(outFile, updateInfo.sha256)) {
                            outFile.delete()
                            onProgress(-1f)
                            return@withContext false
                        }
                        installApk(outFile)
                        return@withContext true
                    }

                    DownloadManager.STATUS_FAILED -> {
                        onProgress(-1f)
                        return@withContext false
                    }

                    else -> {
                        if (total > 0) {
                            onProgress(done.toFloat() / total)
                        }

                        // 进度停滞超过阈值判定失败，避免无限等待
                        if (done == lastProgressBytes) {
                            stallCount++
                            if (stallCount >= STALL_POLLS) {
                                downloadManager.remove(downloadId)
                                onProgress(-1f)
                                return@withContext false
                            }
                        } else {
                            lastProgressBytes = done
                            stallCount = 0
                        }
                        delay(POLL_INTERVAL_MS)
                    }
                }
            }
            onProgress(-1f)
            false
        }
    }

    private fun queryProgress(
        downloadManager: DownloadManager,
        downloadId: Long,
    ): Triple<Int, Long, Long>? {
        return downloadManager.query(DownloadManager.Query().setFilterById(downloadId)).use { cursor ->
            if (!cursor.moveToFirst()) return null
            Triple(
                cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)),
                cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)),
                cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)),
            )
        }
    }

    // 通过 FileProvider 打开系统安装界面
    private fun installApk(apkFile: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(installIntent)
    }

    // 校验已下载 APK 的 SHA-256：期望哈希缺失视为不可校验，与不符一律判失败
    private fun verifyApkHash(file: File, expected: String): Boolean {
        if (expected.isBlank()) {
            CrashLogManager.logException(TAG, "APK 缺少期望哈希，拒绝安装: ${file.name}")
            return false
        }
        val actual = runCatching { sha256Hex(file) }.getOrElse { e ->
            CrashLogManager.logException(TAG, "计算 APK 哈希失败: ${file.name}", e)
            return false
        }
        if (!actual.equals(expected, ignoreCase = true)) {
            CrashLogManager.logException(TAG, "APK 哈希不匹配（期望 $expected，实际 $actual），拒绝安装")
            return false
        }
        return true
    }

    // 流式计算文件 SHA-256（小写十六进制），避免整包读入内存
    private fun sha256Hex(file: File): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }

    // 仅允许 HTTPS 下载地址，防止下载降级到明文传输
    private fun requireHttps(url: String): String {
        if (!url.startsWith("https://")) {
            throw IllegalArgumentException("不安全的下载地址: $url")
        }
        return url
    }

    private companion object {
        const val TAG = "UpdateDownloader"

        const val POLL_INTERVAL_MS = 500L

        // 30 次无进度 * 500ms = 15 秒
        const val STALL_POLLS = 30
    }
}
