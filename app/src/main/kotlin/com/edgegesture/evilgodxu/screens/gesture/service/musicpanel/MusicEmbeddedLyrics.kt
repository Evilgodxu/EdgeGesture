package com.edgegesture.evilgodxu.screens.gesture.service.musicpanel

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets

// 从音频文件容器中读取内嵌歌词：
// MP3( ID3v2 USLT/SYLT/TXXX ) 、FLAC/Ogg( Vorbis 注释 LYRICS ) 、MP4/M4A( ©lyr )
// 平台未提供读取内嵌歌词的原生 API，故按容器格式自行解析标签。
internal object MusicEmbeddedLyrics {

    // 非 MP3 仅读取文件头部窗口：歌词注释紧邻文件头，避免整体载入造成内存与 I/O 峰值
    private const val HEAD_WINDOW_BYTES = 1 * 1024 * 1024
    private const val MP4_HEAD_WINDOW_BYTES = 8 * 1024 * 1024
    private const val MAX_ID3_TAG_BYTES = 8 * 1024 * 1024

    fun read(path: String): String? {
        if (path.isBlank()) return null
        val file = File(path)
        if (!file.isFile || file.length() == 0L) return null
        val head = readRange(file, 0, 10) ?: return null
        return when {
            // MP3 按 ID3v2 头声明的大小精确读取标签，无需读取音频数据
            head.startsWith("ID3") && head.size >= 10 -> {
                val tagSize = minOf(10L + syncsafe(head, 6), MAX_ID3_TAG_BYTES.toLong())
                readRange(file, 0, tagSize)?.let { readId3(it) }
            }
            head.startsWith("fLaC") -> readRange(file, 0, HEAD_WINDOW_BYTES.toLong())?.let { readFlac(it) }
            head.startsWith("OggS") -> readRange(file, 0, HEAD_WINDOW_BYTES.toLong())?.let { readOgg(it) }
            isMp4(head) -> readRange(file, 0, MP4_HEAD_WINDOW_BYTES.toLong())?.let { readMp4(it) }
            else -> null
        }
    }

    private fun readRange(file: File, offset: Long, length: Long): ByteArray? = try {
        val fileLength = file.length()
        if (offset >= fileLength) null else {
            val limit = minOf(length, fileLength - offset).toInt()
            file.inputStream().use { input ->
                input.skip(offset)
                val buffer = ByteArray(limit)
                var read = 0
                while (read < limit) {
                    val count = input.read(buffer, read, limit - read)
                    if (count < 0) break
                    read += count
                }
                if (read == 0) null else if (read == limit) buffer else buffer.copyOf(read)
            }
        }
    } catch (_: Exception) {
        null
    }

    // ===== MP3 / ID3v2 =====

    private fun readId3(b: ByteArray): String? {
        if (b.size < 10) return null
        val version = b[3].toInt() and 0xff
        if (version !in 3..4) return null
        val flags = b[5].toInt() and 0xff
        val tagEnd = minOf(10 + syncsafe(b, 6), b.size)
        var p = 10
        if (flags and 0x40 != 0) { // 跳过扩展头
            if (p + 4 > tagEnd) return null
            val extSize = if (version >= 4) syncsafe(b, p) else int32(b, p)
            p += if (version >= 4) extSize.coerceAtLeast(0) else extSize + 4
        }
        var txxxLyrics: String? = null
        while (p + 10 <= tagEnd) {
            val id = String(b, p, 4, StandardCharsets.ISO_8859_1)
            if (id[0] == '\u0000') break
            val size = if (version >= 4) syncsafe(b, p + 4) else int32(b, p + 4)
            if (size <= 0 || p + 10 + size > tagEnd) break
            val start = p + 10
            when (id) {
                "USLT" -> readUnsynchronisedLyric(b, start, size)?.let { return it }
                "SYLT" -> readSynchronisedLyric(b, start, size)?.let { return it }
                "TXXX" -> readUserTextFrame(b, start, size)
                    ?.takeIf { it.first.uppercase().contains("LYRIC") }
                    ?.let { txxxLyrics = it.second }
            }
            p += 10 + size
        }
        return txxxLyrics
    }

    // USLT：编码(1) + 语言(3) + 描述(以 0 结尾) + 歌词文本
    private fun readUnsynchronisedLyric(b: ByteArray, start: Int, size: Int): String? {
        if (size < 5) return null
        val encoding = b[start].toInt() and 0xff
        val end = start + size
        val (_, next) = readTerminatedString(b, start + 4, end, encoding)
        return decodeString(b, next, end, encoding).takeIf { it.isNotBlank() }
    }

    // SYLT：编码(1) + 语言(3) + 时间格式(1) + 内容类型(1) + 描述(以 0 结尾) + 若干 [文本(以 0 结尾) + 时间戳(4)]
    private fun readSynchronisedLyric(b: ByteArray, start: Int, size: Int): String? {
        if (size < 6) return null
        val encoding = b[start].toInt() and 0xff
        val timeFormat = b[start + 4].toInt() and 0xff
        if (timeFormat != 2) return null // 仅支持毫秒时间戳
        val end = start + size
        var p = readTerminatedString(b, start + 6, end, encoding).second
        val lines = mutableListOf<String>()
        while (p < end) {
            val (text, afterText) = readTerminatedString(b, p, end, encoding)
            p = afterText
            if (p + 4 > end) break
            p += 4
            if (text.isNotBlank()) lines += text.trim()
        }
        return lines.joinToString("\n").takeIf { it.isNotBlank() }
    }

    // TXXX：编码(1) + 描述(以 0 结尾) + 文本
    private fun readUserTextFrame(b: ByteArray, start: Int, size: Int): Pair<String, String>? {
        if (size < 2) return null
        val encoding = b[start].toInt() and 0xff
        val end = start + size
        val (description, next) = readTerminatedString(b, start + 1, end, encoding)
        return description to decodeString(b, next, end, encoding)
    }

    // ===== FLAC / Ogg（Vorbis 注释）=====

    private fun readFlac(b: ByteArray): String? {
        var p = 4
        while (p + 4 <= b.size) {
            val header = b[p].toInt() and 0xff
            val type = header and 0x7f
            val length = (b[p + 1].toInt() and 0xff shl 16) or
                (b[p + 2].toInt() and 0xff shl 8) or (b[p + 3].toInt() and 0xff)
            if (p + 4 + length > b.size) return null
            if (type == 4) return readVorbisComment(b, p + 4, p + 4 + length)
            p += 4 + length
            if (header and 0x80 != 0) break
        }
        return null
    }

    private fun readOgg(b: ByteArray): String? {
        val opus = b.indexOf("OpusTags".toByteArray(StandardCharsets.US_ASCII))
        if (opus >= 0) return readVorbisComment(b, opus + 8, b.size)
        val vorbis = b.indexOf(byteArrayOf(3) + "vorbis".toByteArray(StandardCharsets.US_ASCII))
        if (vorbis >= 0) return readVorbisComment(b, vorbis + 7, b.size)
        return null
    }

    private fun readVorbisComment(b: ByteArray, start: Int, end: Int): String? {
        var p = start
        if (p + 4 > end) return null
        val vendorLength = intLE(b, p)
        if (vendorLength < 0 || p + 4 + vendorLength > end) return null
        p += 4 + vendorLength
        if (p + 4 > end) return null
        val count = intLE(b, p)
        p += 4
        repeat(count.coerceIn(0, MAX_COMMENT_FIELDS)) {
            if (p + 4 > end) return null
            val length = intLE(b, p)
            p += 4
            if (length < 0 || p + length > end) return null
            val field = String(b, p, length, StandardCharsets.UTF_8)
            p += length
            val key = field.substringBefore('=').uppercase()
            if (key in LYRICS_KEYS) {
                val value = field.substringAfter('=', "")
                if (value.isNotBlank()) return value
            }
        }
        return null
    }

    // ===== MP4 / M4A =====

    private fun readMp4(b: ByteArray): String? {
        // ©lyr 的类型字段后紧跟其唯一子 atom data
        val typeIndex = b.indexOf(LYRIC_ATOM)
        if (typeIndex < 0) return null
        val atomStart = typeIndex + 4
        if (atomStart + 8 > b.size) return null
        val atomSize = int32(b, atomStart)
        if (String(b, atomStart + 4, 4, StandardCharsets.ISO_8859_1) != "data") return null
        val valueStart = atomStart + 8 + 8 // version/type(4) + locale(4)
        val valueEnd = atomStart + atomSize
        if (valueStart > valueEnd || valueEnd > b.size) return null
        return String(b, valueStart, valueEnd - valueStart, StandardCharsets.UTF_8).takeIf { it.isNotBlank() }
    }

    // ===== 通用解码 =====

    // 读取以 0 结尾的字符串（UTF-16 为双 0），返回文本与下一位置
    private fun readTerminatedString(b: ByteArray, start: Int, end: Int, encoding: Int): Pair<String, Int> {
        val step = if (encoding == 1 || encoding == 2) 2 else 1
        var p = start
        while (p + step <= end) {
            val terminated = b[p].toInt() == 0 && (step == 1 || b[p + 1].toInt() == 0)
            if (terminated) return decodeString(b, start, p, encoding) to (p + step)
            p += step
        }
        return decodeString(b, start, end, encoding) to end
    }

    private fun decodeString(b: ByteArray, from: Int, to: Int, encoding: Int): String {
        if (to <= from) return ""
        return try {
            when (encoding) {
                0 -> String(b, from, to - from, StandardCharsets.ISO_8859_1)
                1 -> String(b, from, to - from, StandardCharsets.UTF_16) // BOM 决定端序
                2 -> String(b, from, to - from, StandardCharsets.UTF_16BE)
                else -> String(b, from, to - from, StandardCharsets.UTF_8)
            }
        } catch (_: Exception) {
            ""
        }
    }

    private fun isMp4(b: ByteArray) = b.size >= 12 && String(b, 4, 4, StandardCharsets.ISO_8859_1) == "ftyp"

    private fun ByteArray.startsWith(value: String) =
        size >= value.length && String(this, 0, value.length, StandardCharsets.US_ASCII) == value

    private fun ByteArray.indexOf(value: ByteArray): Int {
        if (value.isEmpty() || size < value.size) return -1
        for (start in 0..size - value.size) {
            var matched = true
            for (offset in value.indices) {
                if (this[start + offset] != value[offset]) {
                    matched = false
                    break
                }
            }
            if (matched) return start
        }
        return -1
    }

    private fun syncsafe(b: ByteArray, p: Int) = ((b[p].toInt() and 0x7f) shl 21) or
        ((b[p + 1].toInt() and 0x7f) shl 14) or ((b[p + 2].toInt() and 0x7f) shl 7) or (b[p + 3].toInt() and 0x7f)

    private fun int32(b: ByteArray, p: Int) = ByteBuffer.wrap(b, p, 4).order(ByteOrder.BIG_ENDIAN).int
    private fun intLE(b: ByteArray, p: Int) = ByteBuffer.wrap(b, p, 4).order(ByteOrder.LITTLE_ENDIAN).int

    private const val MAX_COMMENT_FIELDS = 100_000
    private val LYRICS_KEYS = setOf("LYRICS", "UNSYNCEDLYRICS", "UNSYNCED LYRICS", "SYNCEDLYRICS", "SYNCED LYRICS")
    private val LYRIC_ATOM = byteArrayOf(0xA9.toByte(), 'l'.code.toByte(), 'y'.code.toByte(), 'r'.code.toByte())
}