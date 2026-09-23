package com.edgegesture.evilgodxu.screens.gesture.service.musicpanel

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
private const val MAX_LRC_CANDIDATES = 500
private const val MAX_SCAN_DEPTH = 3
// 内嵌歌词为无时间轴的纯文本时，按该间隔顺序铺开，避免全部挤在同一时间点
private const val UNSYNCED_LINE_MS = 3000L

// 扫描设备常见目录下的 .lrc 文件作为本地歌词候选，按与歌曲名相关度排序
internal suspend fun scanLocalLrcFiles(track: MusicTrack): List<LocalLyric> {
    val titleKey = normalizeTitle(track.title)
    val artistKey = normalizeTitle(track.artist)
    return scanAllLrcFiles()
        .sortedByDescending { scoreLyric(it.name, titleKey, artistKey) }
        .take(MAX_LRC_FILES)
}

// 扫描设备全部本地歌词候选，供自动匹配时一次性复用
internal suspend fun scanAllLrcFiles(): List<LocalLyric> = withContext(Dispatchers.IO) {
    val storageRoot = Environment.getExternalStorageDirectory()
    val roots = listOf(
        File(storageRoot, Environment.DIRECTORY_MUSIC),
        File(storageRoot, Environment.DIRECTORY_DOWNLOADS)
    )
    val files = mutableListOf<File>()
    roots.filter { it.isDirectory }.forEach { root -> collectLrcFiles(root, 0, files) }
    files.take(MAX_LRC_CANDIDATES).map { LocalLyric(it.name, it.absolutePath) }
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

// 自动补全歌词：优先读取音频内嵌歌词，其次按文件名自动匹配本地 .lrc。
// 无论是否找到均标记 lyricResolved，避免本次会话内重复扫描。
internal suspend fun resolveTrackLyrics(
    track: MusicTrack,
    lrcCandidates: List<LocalLyric>,
): MusicTrack = withContext(Dispatchers.IO) {
    val lines = try {
        readEmbeddedLyricLines(track) ?: matchLocalLrcLines(track, lrcCandidates)
    } catch (e: Exception) {
        CrashLogManager.logException("MusicLyrics", "自动补全歌词失败", e)
        null
    }
    if (lines.isNullOrEmpty()) {
        track.copy(lyricResolved = true)
    } else {
        track.copy(lyricResolved = true, lyricLines = lines)
    }
}

private fun readEmbeddedLyricLines(track: MusicTrack): List<LyricLine>? {
    val raw = MusicEmbeddedLyrics.read(track.path)?.takeIf { it.isNotBlank() } ?: return null
    return textToLyricLines(raw, track.duration).takeIf { it.isNotEmpty() }
}

private fun matchLocalLrcLines(track: MusicTrack, candidates: List<LocalLyric>): List<LyricLine>? {
    if (normalizeTitle(track.title).isBlank()) return null
    val match = candidates.firstOrNull { matchesLrcName(normalizeTitle(it.name.substringBeforeLast('.')), track) }
        ?: return null
    return parseLrcText(File(match.path).readText()).takeIf { it.isNotEmpty() }
}

// 文件名归一化后需与「标题」或「标题+歌手 / 歌手+标题」完全一致，避免误匹配
private fun matchesLrcName(nameKey: String, track: MusicTrack): Boolean {
    val titleKey = normalizeTitle(track.title)
    if (nameKey == titleKey) return true
    val artistKey = normalizeTitle(track.artist)
    if (artistKey.isBlank()) return false
    return nameKey == normalizeTitle("${track.title} - ${track.artist}") ||
        nameKey == normalizeTitle("${track.artist} - ${track.title}")
}

// 内嵌歌词优先按 LRC 解析时间轴，无时间标签时按行顺序铺开
internal fun textToLyricLines(raw: String, durationMs: Long): List<LyricLine> {
    val timed = parseLrcText(raw)
    if (timed.isNotEmpty()) return timed
    val lines = raw.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
    if (lines.isEmpty()) return emptyList()
    val step = if (durationMs > 0) durationMs / lines.size else UNSYNCED_LINE_MS
    return lines.mapIndexed { index, text -> LyricLine(timeMs = index * step, text = text) }
}

// 读取本地 .lrc 文件并作为当前歌曲歌词，成功后写入播放状态
internal suspend fun importLocalLyrics(
    playbackState: MusicPlaybackState,
    track: MusicTrack,
    lyric: LocalLyric,
): Boolean = withContext(Dispatchers.IO) {
    try {
        val lines = parseLrcText(File(lyric.path).readText())
        if (lines.isEmpty()) return@withContext false
        val updated = track.copy(lyricLines = lines, lyricResolved = true)
        withContext(Dispatchers.Main) {
            playbackState.updateTrack(updated)
        }
        true
    } catch (e: Exception) {
        CrashLogManager.logException("MusicLyrics", "导入本地歌词失败", e)
        false
    }
}