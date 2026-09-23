package com.edgegesture.evilgodxu.screens.gesture.service.musicpanel

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.edgegesture.evilgodxu.log.CrashLogManager
import java.io.File
import java.io.FileInputStream

// 本地音频文件元数据读取：解码头未给出比特率、或只给出解码输出位深时，从文件本身补齐信息条内容。
// 只依赖文件与系统元数据接口，不持有播放器引用，可在任意线程调用
internal object TrackAudioInfoReader {

    private val FLAC_MAGIC = byteArrayOf(0x66, 0x4C, 0x61, 0x43)
    private val RIFF_MAGIC = byteArrayOf(0x52, 0x49, 0x46, 0x46)
    private val WAVE_MAGIC = byteArrayOf(0x57, 0x41, 0x56, 0x45)

    // 读取比特率（kbps）：优先取系统解析出的媒体元数据，该值对部分 FLAC/VBR 文件缺失，
    // 此时按文件大小与时长估算平均比特率兜底；音频源不在本机时无法定位文件，返回 null
    fun readBitrateKbps(context: Context, track: MusicTrack): Int? {
        if (!hasLocalSource(track)) return null
        val retriever = MediaMetadataRetriever()
        try {
            setDataSource(retriever, context, track)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                ?.toLongOrNull()
                ?.takeIf { it > 0 }
                // 元数据单位为 bps，换算为 kbps；下限取 1，避免截断为 0 后被信息条当作未知
                ?.let { return (it / 1000).toInt().coerceAtLeast(1) }
        } catch (e: Exception) {
            CrashLogManager.logException("TrackAudioInfoReader", "读取曲目比特率失败", e)
        } finally {
            runCatching { retriever.release() }
        }
        return estimateAverageBitrateKbps(context, track)
    }

    // 读取源文件位深（bit）：FLAC 解析 STREAMINFO、WAV 解析 RIFF fmt 块。
    // 位深是源文件属性，解码输出位深在部分曲目上与之不同（高解析度常以降位或浮点输出），故以容器头为准；
    // 无法从容器头判定的格式返回 null，由调用方沿用解码信息
    fun readBitDepth(context: Context, track: MusicTrack): Int? {
        if (!hasLocalSource(track)) return null
        return when (extensionOf(track)) {
            "FLAC" -> readFlacBitDepth(context, track)
            "WAV", "WAVE" -> readWavBitDepth(context, track)
            else -> null
        }
    }

    // FLAC STREAMINFO 的位深是 5 位域，横跨字节 20 最低位与字节 21 高 4 位。
    // 规范值为位域值 + 1，须拼接后再加：逐字节加会让 32bit（位域 31）的低 4 位进位吞掉高位
    private fun readFlacBitDepth(context: Context, track: MusicTrack): Int? =
        readHeader(context, track, 42) { bytes ->
            if (!bytes.copyOfRange(0, 4).contentEquals(FLAC_MAGIC)) return@readHeader null
            (((bytes[20].toInt() and 0x01) shl 4) or ((bytes[21].toInt() and 0xF0) ushr 4)) + 1
        }

    // WAV RIFF 的 fmt 块位深位于偏移 34-35，小端存储
    private fun readWavBitDepth(context: Context, track: MusicTrack): Int? =
        readHeader(context, track, 44) { bytes ->
            if (!bytes.copyOfRange(0, 4).contentEquals(RIFF_MAGIC) ||
                !bytes.copyOfRange(8, 12).contentEquals(WAVE_MAGIC)
            ) return@readHeader null
            ((bytes[35].toInt() and 0xFF) shl 8) or (bytes[34].toInt() and 0xFF)
        }

    // 读取文件头部定长字节交给解析：文件路径优先，否则经 ContentResolver 打开。
    // 实际读到的字节不足所需长度时判定为无法解析
    private inline fun readHeader(
        context: Context,
        track: MusicTrack,
        size: Int,
        parse: (ByteArray) -> Int?,
    ): Int? {
        val stream = if (track.path.isNotBlank()) {
            runCatching { FileInputStream(track.path) }.getOrNull()
        } else {
            runCatching { context.contentResolver.openInputStream(Uri.parse(track.audioUri)) }.getOrNull()
        }
        val bytes = ByteArray(size)
        val read = if (stream != null) {
            runCatching { stream.use { it.read(bytes) } }.getOrNull() ?: 0
        } else {
            0
        }
        if (read < size) return null
        return parse(bytes)?.takeIf { it > 0 }
    }

    // 按字节数 × 8 ÷ 时长（秒）估算平均比特率，作为元数据缺失时的兜底
    private fun estimateAverageBitrateKbps(context: Context, track: MusicTrack): Int? {
        val sizeBytes = readFileSize(context, track) ?: return null
        val durationSeconds = track.duration / 1000
        if (sizeBytes <= 0 || durationSeconds <= 0) return null
        return (sizeBytes * 8 / durationSeconds / 1000).toInt().takeIf { it > 0 }
    }

    // 读取音频文件字节数：有文件路径直接取长度，否则经 ContentResolver 打开音频 URI
    private fun readFileSize(context: Context, track: MusicTrack): Long? {
        if (track.path.isNotBlank()) {
            val file = File(track.path)
            if (file.isFile) return file.length()
        }
        return runCatching {
            context.contentResolver.openFileDescriptor(Uri.parse(track.audioUri), "r")
                ?.use { descriptor -> descriptor.statSize }
        }.getOrNull()
    }

    // 文件路径与 URI 两种数据源分别交给原生接口，依据是扫描结果中文件路径是否可得
    private fun setDataSource(retriever: MediaMetadataRetriever, context: Context, track: MusicTrack) {
        if (track.path.isNotBlank()) {
            retriever.setDataSource(track.path)
        } else {
            retriever.setDataSource(context, Uri.parse(track.audioUri))
        }
    }

    // 音频源是否位于本机：有文件路径，或 URI 指向 content/file 提供者
    private fun hasLocalSource(track: MusicTrack): Boolean =
        track.path.isNotBlank() ||
            track.audioUri.startsWith("content:") ||
            track.audioUri.startsWith("file:")

    // 取文件路径扩展名作为容器判定依据；路径缺失或不含扩展名时返回空串
    private fun extensionOf(track: MusicTrack): String =
        track.path.substringAfterLast('.', "").uppercase()
}
