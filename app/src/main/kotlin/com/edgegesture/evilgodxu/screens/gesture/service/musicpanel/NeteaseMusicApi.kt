package com.edgegesture.evilgodxu.screens.gesture.service.musicpanel

import android.graphics.Bitmap
import android.graphics.BitmapFactory
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

internal data class NeteaseLyricData(val lines: List<LyricLine>)

data class LyricWord(val startMs: Long, val durationMs: Long, val text: String)

data class LyricLine(
    val timeMs: Long,
    val text: String,
    val words: List<LyricWord> = emptyList()
)

internal object NeteaseMusicApi {
    suspend fun loadCover(url: String): Bitmap? = withContext(Dispatchers.IO) {
        if (url.isBlank()) return@withContext null
        try {
            URL(url).openStream().use(BitmapFactory::decodeStream)
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
            connection.setRequestProperty("User-Agent", "Mozilla/5.0")
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
}
