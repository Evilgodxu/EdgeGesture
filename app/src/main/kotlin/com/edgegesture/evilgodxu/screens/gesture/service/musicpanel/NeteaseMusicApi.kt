package com.edgegesture.evilgodxu.screens.gesture.service.musicpanel

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

internal data class NeteaseSongMatch(
    val id: Long,
    val title: String,
    val artist: String,
    val coverUrl: String?
)

data class NeteaseSongSearchResult(
    val id: Long,
    val title: String,
    val artist: String,
    val coverUrl: String?,
    /** CDN 缩略图 URL（封面 + ?param=128y128），列表行使用以加快加载 */
    val coverThumbUrl: String? = null,
    val duration: Long = 0L
)

internal data class NeteaseLyricData(val lines: List<LyricLine>)

data class LyricWord(val startMs: Long, val durationMs: Long, val text: String)

data class LyricLine(
    val timeMs: Long,
    val text: String,
    val words: List<LyricWord> = emptyList()
)

internal object NeteaseMusicApi {
    suspend fun loadCoverBytes(url: String): ByteArray? = withContext(Dispatchers.IO) {
        if (url.isBlank()) return@withContext null
        try {
            URL(url).openStream().use { it.readBytes() }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun match(title: String, artist: String, durationMs: Long): NeteaseSongMatch? = withContext(Dispatchers.IO) {
        val keyword = if (artist.isBlank() || artist == "未知艺术家") title else "$title $artist"
        val songs = search(keyword)
        val best = songs.minByOrNull { score(it, title, artist, durationMs) } ?: return@withContext null
        if (!best.coverUrl.isNullOrBlank()) best else detail(best)
    }

    suspend fun lyric(songId: Long): NeteaseLyricData = withContext(Dispatchers.IO) {
        val body = JSONObject().apply {
            put("id", songId)
            put("lv", 0)
            put("kv", 0)
            put("tv", 0)
            put("rv", 0)
            put("yv", 0)
        }
        val root = request("song/lyric/v1", body)
        parseLrc(
            root.optJSONObject("lrc")?.optString("lyric").orEmpty(),
            root.optJSONObject("yrc")?.optString("lyric").orEmpty()
        )
    }

    private fun detail(song: NeteaseSongMatch): NeteaseSongMatch {
        val root = request("v3/song/detail", JSONObject().put("c", "[{\"id\":${song.id}}]"))
        val item = root.optJSONArray("songs")?.optJSONObject(0) ?: return song
        val album = item.optJSONObject("al") ?: item.optJSONObject("album")
        return song.copy(coverUrl = album?.optString("picUrl")?.takeIf { it.isNotBlank() })
    }

    // 公开搜索方法，返回完整的搜索结果显示
    suspend fun searchSongs(keyword: String): List<NeteaseSongSearchResult> = withContext(Dispatchers.IO) {
        val body = JSONObject().apply {
            put("s", keyword)
            put("type", 1)
            put("limit", 20)
            put("offset", 0)
        }
        val root = request("search/get", body)
        val songs = root.optJSONObject("result")?.optJSONArray("songs") ?: JSONArray()
        List(songs.length()) { index ->
            val song = songs.getJSONObject(index)
            val artists = song.optJSONArray("artists") ?: song.optJSONArray("ar") ?: JSONArray()
            val artist = List(artists.length()) { artists.getJSONObject(it).optString("name") }
                .filter { it.isNotBlank() }
                .joinToString(" / ")
            val album = song.optJSONObject("album") ?: song.optJSONObject("al")
            val cover = album?.optString("picUrl")?.takeIf { it.isNotBlank() }
            NeteaseSongSearchResult(
                id = song.optLong("id"),
                title = song.optString("name"),
                artist = artist,
                coverUrl = cover,
                coverThumbUrl = cover?.let { thumbUrl(it) },
                duration = song.optLong("duration", 0L)
            )
        }
    }

    // 获取歌曲播放 URL，返回 null 表示 VIP/不可播
    suspend fun getSongUrl(songId: Long, level: String = "standard"): String? = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("ids", "[$songId]")
                put("level", level)
                put("encodeType", "mp3")
            }
            val root = request("song/enhance/player/url/v1", body)
            val data = root.optJSONArray("data")?.optJSONObject(0) ?: return@withContext null
            val url = data.optString("url", "")
            val hasFreeTrial = data.has("freeTrialInfo") && !data.isNull("freeTrialInfo")
            // 跳过 VIP/试听片段（无完整播放URL）
            if (url.isBlank() || hasFreeTrial) return@withContext null
            url
        } catch (e: Exception) {
            Log.w("NeteaseMusicApi", "获取歌曲URL($level)失败: ${e.message}")
            null
        }
    }

    // 多音质回退获取播放 URL：standard → higher → exhigh
    suspend fun getSongUrlWithFallback(songId: Long): String? {
        for (level in arrayOf("standard", "higher", "exhigh")) {
            val url = getSongUrl(songId, level)
            if (url != null) return url
        }
        return null
    }

    private fun search(keyword: String): List<NeteaseSongMatch> {
        val body = JSONObject().apply {
            put("s", keyword)
            put("type", 1)
            put("limit", 10)
            put("offset", 0)
        }
        val root = request("search/get", body)
        val songs = root.optJSONObject("result")?.optJSONArray("songs") ?: JSONArray()
        return List(songs.length()) { index ->
            val song = songs.getJSONObject(index)
            val artists = song.optJSONArray("artists") ?: song.optJSONArray("ar") ?: JSONArray()
            val artist = List(artists.length()) { artists.getJSONObject(it).optString("name") }
                .filter { it.isNotBlank() }
                .joinToString(" / ")
            val album = song.optJSONObject("album") ?: song.optJSONObject("al")
            NeteaseSongMatch(
                id = song.optLong("id"),
                title = song.optString("name"),
                artist = artist,
                coverUrl = album?.optString("picUrl")?.takeIf { it.isNotBlank() }
            )
        }
    }

    private fun score(song: NeteaseSongMatch, title: String, artist: String, _durationMs: Long): Int {
        val normalizedTitle = normalize(song.title)
        val normalizedArtist = normalize(song.artist)
        var score = if (normalizedTitle == normalize(title)) 0 else 100
        if (artist.isNotBlank() && artist != "未知艺术家" && normalizedArtist != normalize(artist)) score += 30
        return score
    }

    private fun normalize(value: String): String = value.lowercase()
        .replace("（", "(")
        .replace("）", ")")
        .replace(Regex("\\([^)]*\\)|\\[[^]]*]"), "")
        .replace(Regex("\\s+"), "")

    private fun request(path: String, body: JSONObject): JSONObject {
        val encrypted = NeteaseCrypto.weapi(body.toString())
        val connection = URL("https://music.163.com/weapi/$path").openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            connection.setRequestProperty("Referer", "https://music.163.com")
            connection.setRequestProperty("X-Real-IP", randomChinaIp())
            connection.setRequestProperty("X-Forwarded-For", randomChinaIp())
            val form = "params=${java.net.URLEncoder.encode(encrypted.getValue("params"), "UTF-8")}&encSecKey=${java.net.URLEncoder.encode(encrypted.getValue("encSecKey"), "UTF-8")}"
            connection.outputStream.use { it.write(form.toByteArray()) }
            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (responseCode !in 200..299) throw IllegalStateException("HTTP $responseCode: $response")
            JSONObject(response)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseLrc(lrc: String, yrc: String): NeteaseLyricData {
        val wordLines = parseYrc(yrc)
        if (wordLines.isNotEmpty()) return NeteaseLyricData(wordLines)
        val lines = lrc.lineSequence().mapNotNull { line ->
            val match = Regex("\\[(\\d+):(\\d+)(?:\\.(\\d+))?](.*)").find(line) ?: return@mapNotNull null
            LyricLine(
                timeMs = match.groupValues[1].toLong() * 60_000 +
                        match.groupValues[2].toLong() * 1_000 +
                        match.groupValues[3].padEnd(3, '0').take(3).toLong(),
                text = match.groupValues[4].trim()
            ).takeIf { it.text.isNotBlank() }
        }.sortedBy { it.timeMs }.toList()
        return NeteaseLyricData(lines)
    }

    private fun parseYrc(raw: String): List<LyricLine> {
        val linePattern = Regex("\\[(\\d+),(\\d+)](.*)")
        val wordPattern = Regex("\\((\\d+),(\\d+),\\d+\\)([^()]*)")
        return raw.lineSequence().mapNotNull { rawLine ->
            val line = linePattern.find(rawLine) ?: return@mapNotNull null
            val start = line.groupValues[1].toLong()
            val words = wordPattern.findAll(line.groupValues[3]).map {
                LyricWord(
                    startMs = start + it.groupValues[1].toLong(),
                    durationMs = it.groupValues[2].toLong(),
                    text = it.groupValues[3]
                )
            }.filter { it.text.isNotEmpty() }.toList()
            LyricLine(start, words.joinToString("") { it.text }.trim(), words)
                .takeIf { it.text.isNotBlank() }
        }.toList()
    }

    // 随机中国大陆 IP（降低风控概率）
    private val chinaIpPrefixes = intArrayOf(36, 39, 42, 58, 59, 60, 101, 106, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 175, 180, 182, 183, 202, 203, 210, 211, 218, 219, 220, 221, 222, 223)
    private fun randomChinaIp(): String {
        val a = chinaIpPrefixes[kotlin.random.Random.nextInt(chinaIpPrefixes.size)]
        return "$a.${kotlin.random.Random.nextInt(256)}.${kotlin.random.Random.nextInt(256)}.${1 + kotlin.random.Random.nextInt(254)}"
    }

    // CDN 缩略图 URL，与 QPlayer 的 thumbUrl() 一致：追加 ?param=128y128
    private fun thumbUrl(coverUrl: String): String {
        return coverUrl + if (coverUrl.contains("?")) "&param=128y128" else "?param=128y128"
    }
}
