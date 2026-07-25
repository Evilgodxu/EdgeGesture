package com.edgegesture.evilgodxu.screens.gesture.service.musicpanel

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// 本地音乐扫描器（基于 MediaStore）
object MusicScanner {

    // 扫描设备本地音乐文件，过滤时长 >= 30 秒的音频
    suspend fun scan(context: Context): List<MusicTrack> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<MusicTrack>()
        val contentResolver = context.contentResolver
        try {
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.IS_MUSIC,
            )
            val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 30000"
            } else {
                // 低版本 DURATION 字段可能不可靠，不过滤时长
                "${MediaStore.Audio.Media.IS_MUSIC} != 0"
            }
            contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                "${MediaStore.Audio.Media.TITLE} ASC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(MediaStore.Audio.Media._ID)
                val titleIdx = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
                val artistIdx = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                val dataIdx = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                val durationIdx = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)
                val albumIdIdx = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
                if (idIdx < 0 || titleIdx < 0 || dataIdx < 0) return@withContext tracks
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idIdx)
                    val path = cursor.getString(dataIdx) ?: continue
                    val title = cursor.getString(titleIdx)?.takeIf { it.isNotBlank() }
                        ?: path.substringAfterLast('/').substringBeforeLast('.')
                    val artist = cursor.getString(artistIdx)?.takeIf { it.isNotBlank() } ?: "未知艺术家"
                    val duration = if (durationIdx >= 0) cursor.getLong(durationIdx) else 0L
                    val albumId = if (albumIdIdx >= 0) cursor.getLong(albumIdIdx) else 0L
                    val art = loadAlbumArt(contentResolver, albumId, path)
                    tracks.add(
                        MusicTrack(
                            id = id,
                            path = path,
                            title = title,
                            artist = artist,
                            duration = duration,
                            albumId = albumId,
                            albumArt = art
                        )
                    )
                }
            }
        } catch (_: Exception) {
        }
        tracks
    }

    private fun loadAlbumArt(contentResolver: ContentResolver, albumId: Long, fallbackPath: String): Bitmap? {
        // 尝试从 MediaStore 专辑封面 URI 加载
        if (albumId > 0) {
            try {
                val uri = Uri.parse("content://media/external/audio/albumart/$albumId")
                contentResolver.openInputStream(uri)?.use { input ->
                    return BitmapFactory.decodeStream(input)
                }
            } catch (_: Exception) {
            }
        }
        // 兜底：从音频文件元数据提取封面
        return extractEmbeddedArt(fallbackPath)
    }

    private fun extractEmbeddedArt(path: String): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(path)
            val art = retriever.embeddedPicture
            art?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
        } catch (_: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }
}