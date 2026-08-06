package com.edgegesture.evilgodxu.screens.gesture.service.musicpanel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import com.edgegesture.evilgodxu.log.CrashLogManager
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File

internal object MusicMetadataCache {
    private fun root(context: Context) = File(context.filesDir, "music_metadata")
    private fun coverFile(context: Context, id: Long) = File(File(root(context), "covers_v2"), "$id.webp")
    private fun originalCoverFile(context: Context, id: Long) = File(File(root(context), "covers_original"), "$id.image")
    private fun lyricFile(context: Context, id: Long) = File(File(root(context), "lyrics"), "$id.json")
    private val coverMemoryCache = object : LruCache<String, Bitmap>(8 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }

    @Synchronized
    fun loadCover(path: String): Bitmap? {
        if (path.isBlank()) return null
        coverMemoryCache.get(path)?.let { return it }
        val bitmap = BitmapFactory.decodeFile(path) ?: return null
        coverMemoryCache.put(path, bitmap)
        return bitmap
    }

    @Synchronized
    fun putCover(path: String, bitmap: Bitmap) {
        if (path.isNotBlank()) coverMemoryCache.put(path, bitmap)
    }

    @Synchronized
    fun removeCover(path: String) {
        if (path.isNotBlank()) coverMemoryCache.remove(path)
    }

    fun saveCover(context: Context, id: Long, originalBytes: ByteArray): String? = try {
        val bitmap = BitmapFactory.decodeByteArray(originalBytes, 0, originalBytes.size)
        val convertedFile = coverFile(context, id)
        if (bitmap != null) {
            convertedFile.parentFile?.mkdirs()
            var success = false
            convertedFile.outputStream().use { output ->
                success = bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 85, output)
            }
            bitmap.recycle()
            if (success && isValid(convertedFile.absolutePath) &&
                BitmapFactory.decodeFile(convertedFile.absolutePath) != null
            ) {
                val path = convertedFile.absolutePath
                loadCover(path)?.let { putCover(path, it) }
                return path
            }
        }
        // WEBP 转换失败，回退到原始格式
        originalCoverFile(context, id).apply {
            parentFile?.mkdirs()
            writeBytes(originalBytes)
        }.absolutePath
    } catch (e: Exception) {
        CrashLogManager.logException("MusicMetadataCache", "保存封面失败", e)
        null
    }

    fun saveLyrics(context: Context, id: Long, lines: List<LyricLine>): String? = try {
        val file = lyricFile(context, id)
        file.parentFile?.mkdirs()
        val array = JSONArray()
        lines.forEach { line ->
            array.put(JSONObject().apply {
                put("timeMs", line.timeMs)
                put("text", line.text)
                put("words", JSONArray().also { words ->
                    line.words.forEach { word ->
                        words.put(JSONObject().apply {
                            put("startMs", word.startMs)
                            put("durationMs", word.durationMs)
                            put("text", word.text)
                        })
                    }
                })
            })
        }
        file.writeText(array.toString())
        file.absolutePath
    } catch (e: Exception) {
        CrashLogManager.logException("MusicMetadataCache", "保存歌词失败", e)
        null
    }

    fun loadLyrics(path: String): List<LyricLine> = try {
        val array = JSONArray(File(path).readText())
        List(array.length()) { index ->
            val item = array.getJSONObject(index)
            val words = item.optJSONArray("words") ?: JSONArray()
            LyricLine(
                timeMs = item.getLong("timeMs"),
                text = item.getString("text"),
                words = List(words.length()) { wordIndex ->
                    val word = words.getJSONObject(wordIndex)
                    LyricWord(word.getLong("startMs"), word.getLong("durationMs"), word.getString("text"))
                }
            )
        }
    } catch (e: Exception) {
        CrashLogManager.logException("MusicMetadataCache", "加载歌词失败", e)
        emptyList()
    }

    fun isValid(path: String): Boolean = path.isNotBlank() && File(path).let { it.isFile && it.length() > 0 }

    fun isCurrentCoverPath(path: String): Boolean = isValid(path) && File(path).parentFile?.name == "covers_v2"

    fun loadCoverBytes(path: String): ByteArray? = try {
        if (!isValid(path)) null else File(path).readBytes()
    } catch (e: Exception) {
        CrashLogManager.logException("MusicMetadataCache", "读取封面文件失败", e)
        null
    }

    fun deleteCoverFile(path: String) {
        if (path.isNotBlank()) {
            removeCover(path)
            File(path).delete()
        }
    }

    fun deleteLyricFile(path: String) {
        if (path.isNotBlank()) {
            File(path).delete()
        }
    }

    fun bitmapToBytes(bitmap: Bitmap): ByteArray? = try {
        ByteArrayOutputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            output.toByteArray()
        }
    } catch (e: Exception) {
        CrashLogManager.logException("MusicMetadataCache", "位图转字节失败", e)
        null
    }
}
