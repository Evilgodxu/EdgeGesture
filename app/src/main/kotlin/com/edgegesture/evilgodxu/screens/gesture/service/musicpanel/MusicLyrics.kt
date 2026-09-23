package com.edgegesture.evilgodxu.screens.gesture.service.musicpanel

import android.content.Context
import android.os.Environment
import com.edgegesture.evilgodxu.log.CrashLogManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class LyricWord(val startMs: Long, val durationMs: Long, val text: String)

data class LyricLine(
    val timeMs: Long,
    val text: String,
    val words: List<LyricWord> = emptyList()
)

// 设备本地歌词文件候选
data class LocalLyric(val name: String, val path: String)

// 标准 LRC 解析：兼容 [mm:ss.xx] 与 [mm:ss] 两种时间标签
internal fun parseLrcText(lrc: String): List<LyricLine> {
    return lrc.lineSequence().mapNotNull { line ->
        val match = Regex("\\[(\\d+):(\\d+)(?:\\.(\\d+))?](.*)").find(line) ?: return@mapNotNull null
        LyricLine(
            timeMs = match.groupValues[1].toLong() * 60_000 +
                    match.groupValues[2].toLong() * 1_000 +
                    match.groupValues[3].padEnd(3, '0').take(3).toLong(),
            text = match.groupValues[4].trim()
        ).takeIf { it.text.isNotBlank() }
    }.sortedBy { it.timeMs }.toList()
}

private const val MAX_LRC_FILES = 20
private const val MAX_SCAN_DEPTH = 3

// 扫描设备常见目录下的 .lrc 文件作为本地歌词候选，按与歌曲名相关度排序
internal suspend fun scanLocalLrcFiles(track: MusicTrack): List<LocalLyric> =
    withContext(Dispatchers.IO) {
        val storageRoot = Environment.getExternalStorageDirectory()
        val roots = listOf(
            File(storageRoot, Environment.DIRECTORY_MUSIC),
            File(storageRoot, Environment.DIRECTORY_DOWNLOADS)
        )
        val files = mutableListOf<File>()
        roots.filter { it.isDirectory }.forEach { root -> collectLrcFiles(root, 0, files) }
        if (files.isEmpty()) return@withContext emptyList()
        val titleKey = normalizeTitle(track.title)
        val artistKey = normalizeTitle(track.artist)
        files
            .map { LocalLyric(it.name, it.absolutePath) }
            .sortedByDescending { scoreLyric(it.name, titleKey, artistKey) }
            .take(MAX_LRC_FILES)
    }

private fun collectLrcFiles(dir: File, depth: Int, out: MutableList<File>) {
    if (depth > MAX_SCAN_DEPTH) return
    val children = dir.listFiles() ?: return
    for (child in children) {
        if (child.isDirectory) {
            collectLrcFiles(child, depth + 1, out)
        } else if (child.name.endsWith(".lrc", ignoreCase = true) && child.length() > 0) {
            out += child
        }
    }
}

// 文件名与歌曲标题/歌手越接近，排序越靠前
private fun scoreLyric(fileName: String, titleKey: String, artistKey: String): Int {
    if (titleKey.isBlank()) return 0
    val nameKey = normalizeTitle(fileName.substringBeforeLast('.'))
    var score = 0
    if (nameKey == titleKey) score += 3
    if (nameKey.contains(titleKey)) score += 2
    if (artistKey.isNotBlank() && nameKey.contains(artistKey)) score += 1
    return score
}

// 读取本地 .lrc 文件并作为当前歌曲歌词，成功后写入缓存与播放状态
internal suspend fun importLocalLyrics(
    context: Context,
    playbackState: MusicPlaybackState,
    track: MusicTrack,
    lyric: LocalLyric,
): Boolean = withContext(Dispatchers.IO) {
    try {
        val lines = parseLrcText(File(lyric.path).readText())
        if (lines.isEmpty()) return@withContext false
        val path = MusicMetadataCache.saveLyrics(context, track.id, lines) ?: return@withContext false
        val updated = track.copy(lyricCachePath = path, lyricLines = lines)
        withContext(Dispatchers.Main) {
            playbackState.updateTrack(updated)
        }
        true
    } catch (e: Exception) {
        CrashLogManager.logException("MusicLyrics", "导入本地歌词失败", e)
        false
    }
}