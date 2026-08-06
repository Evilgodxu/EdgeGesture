package com.edgegesture.evilgodxu.screens.gesture.service.musicpanel

import android.content.Context
import android.media.MediaScannerConnection
import com.edgegesture.evilgodxu.log.CrashLogManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.KeyNotFoundException
import org.jaudiotagger.tag.Tag
import org.jaudiotagger.tag.images.ArtworkFactory
import org.jaudiotagger.tag.reference.PictureTypes
import java.io.File

// 通过 Jaudiotagger 将手动修改的元数据（封面/标题/艺术家）写入音频文件
internal object MusicMetadataWriter {

    // 手动刷新封面后写入音频文件元数据；成功后返回 true
    suspend fun writeCover(
        context: Context,
        track: MusicTrack,
        coverBytes: ByteArray,
    ): Boolean = withContext(Dispatchers.IO) {
        write(context, track.path) { tag ->
            val artwork = ArtworkFactory.getNew()
            artwork.setBinaryData(coverBytes)
            artwork.setMimeType(sniffMimeType(coverBytes))
            artwork.setDescription("")
            artwork.setPictureType(PictureTypes.DEFAULT_ID)
            // 注意：不可调用 setImageFromData() —— StandardArtwork 依赖 javax.imageio（Android 上不存在）
            // 且 MP3/MP4/FLAC 封面帧写入并不需要图片尺寸，直接省略
            // 先清理旧封面再写入，避免部分格式残留多个封面帧
            try {
                tag.deleteArtworkField()
            } catch (_: KeyNotFoundException) {
                // 文件原本无封面，无需清理
            }
            tag.setField(artwork)
        }
    }

    // 手动重命名标题/艺术家后写入音频文件元数据；成功后返回 true
    suspend fun writeTitleArtist(
        context: Context,
        track: MusicTrack,
        title: String,
        artist: String,
    ): Boolean = withContext(Dispatchers.IO) {
        write(context, track.path) { tag ->
            tag.setField(FieldKey.TITLE, title)
            if (artist.isNotBlank()) {
                tag.setField(FieldKey.ARTIST, artist)
            }
        }
    }

    private fun write(
        context: Context,
        path: String,
        block: (Tag) -> Unit,
    ): Boolean {
        if (path.isBlank()) return false
        return try {
            val audioFile = AudioFileIO.read(File(path))
            val tag = audioFile.getTagOrCreateAndSetDefault()
            block(tag)
            audioFile.commit()
            // 通知 MediaStore 重新扫描，使系统音乐库与本次写入保持一致
            MediaScannerConnection.scanFile(context, arrayOf(path), null, null)
            true
        } catch (e: Throwable) {
            // Jaudiotagger 在 Android 上部分类（javax.imageio/java.awt）缺失时会抛 Error 级异常，
            // 元数据写入为后台兜底操作，需捕获 Throwable 避免应用崩溃
            CrashLogManager.logException("MusicMetadataWriter", "写入音频文件元数据失败", e)
            false
        }
    }

    // 根据文件头识别图片 MIME 类型，供 ID3/FLAC 封面帧使用
    private fun sniffMimeType(bytes: ByteArray): String = when {
        bytes.size >= 3 && bytes[0] == 0xFF.toByte() &&
                bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte() -> "image/jpeg"
        bytes.size >= 8 && bytes[0] == 0x89.toByte() &&
                bytes[1] == 0x50.toByte() && bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte() -> "image/png"
        bytes.size >= 6 && bytes[0] == 0x47.toByte() &&
                bytes[1] == 0x49.toByte() && bytes[2] == 0x46.toByte() && bytes[3] == 0x38.toByte() -> "image/gif"
        bytes.size >= 12 && bytes[0] == 0x52.toByte() &&
                bytes[1] == 0x49.toByte() && bytes[2] == 0x46.toByte() && bytes[3] == 0x46.toByte() &&
                bytes[8] == 0x57.toByte() && bytes[9] == 0x45.toByte() &&
                bytes[10] == 0x42.toByte() && bytes[11] == 0x50.toByte() -> "image/webp"
        else -> "image/jpeg"
    }
}
