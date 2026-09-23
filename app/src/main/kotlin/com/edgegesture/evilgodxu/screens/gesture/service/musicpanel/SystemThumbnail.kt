package com.edgegesture.evilgodxu.screens.gesture.service.musicpanel

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// 封面显示统一请求的尺寸：面板与列表行都只显示小图，取该边长足够清晰
internal const val COVER_SIZE_PX = 256

// 封面内存缓存：封面每次滚入视口都会重新向媒体库查询并解码，以内存换重复读取。
// 仅驻留内存，进程结束即失效，不产生任何落盘产出。
// 尺寸参与缓存键：解码输出随请求尺寸变化，不同尺寸的结果不可互相顶替。
internal object SystemThumbnailCache {

    // 256px 位图约 256KB，上限约容纳百来张，足以覆盖可见列表与当前曲目
    private const val MAX_BYTES = 32 * 1024 * 1024

    private data class Key(val audioUri: String, val sizePx: Int)

    private val cache = object : LruCache<Key, Bitmap>(MAX_BYTES) {
        override fun sizeOf(key: Key, value: Bitmap): Int = value.allocationByteCount
    }

    fun get(audioUri: String, sizePx: Int): Bitmap? = cache.get(Key(audioUri, sizePx))

    fun put(audioUri: String, sizePx: Int, bitmap: Bitmap) {
        cache.put(Key(audioUri, sizePx), bitmap)
    }
}

// 取曲目封面位图：命中内存缓存即复用，否则读取系统略缩图（非索引曲目回退内嵌封面）并回填
internal suspend fun loadTrackCover(context: Context, track: MusicTrack, sizePx: Int): Bitmap? {
    SystemThumbnailCache.get(track.audioUri, sizePx)?.let { return it }
    return withContext(Dispatchers.IO) {
        MusicScanner.loadAlbumArt(
            context,
            context.contentResolver,
            Uri.parse(track.audioUri),
            track.albumId,
            track.path,
            sizePx,
        )?.also { SystemThumbnailCache.put(track.audioUri, sizePx, it) }
    }
}

/**
 * 封面显示入口：按最长边 [sizePx] 请求系统略缩图并转为 Compose 位图。
 * 取不到封面时返回 null，由调用方显示占位符。
 * [MusicPlaybackState.coverRevision] 变化（封面被重写）时重新取图，避免命中已作废的旧图。
 */
@Composable
internal fun rememberTrackCover(track: MusicTrack?, sizePx: Int): ImageBitmap? {
    val context = LocalContext.current
    val coverRevision = MusicPanelStateHolder.state.coverRevision
    val cover by produceState<ImageBitmap?>(initialValue = null, track?.audioUri, sizePx, coverRevision) {
        val target = track
        value = target?.let { loadTrackCover(context, it, sizePx)?.asImageBitmap() }
    }
    return cover
}
