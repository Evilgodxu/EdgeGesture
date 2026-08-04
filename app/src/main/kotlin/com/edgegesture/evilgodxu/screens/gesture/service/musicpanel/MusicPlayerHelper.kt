package com.edgegesture.evilgodxu.screens.gesture.service.musicpanel

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private suspend fun getController(context: Context, state: MusicPlaybackState): MediaController {
    state.mediaController?.let { return it }
    state.appContext = context.applicationContext
    val token = SessionToken(context, ComponentName(context, MusicPlaybackService::class.java))
    val controller = withContext(Dispatchers.Main) {
        MediaController.Builder(context, token).buildAsync().await()
    }
    withContext(Dispatchers.Main) {
        state.mediaController = controller
        state.player = controller
        controller.addListener(state.controllerListener)
        applyPlaybackMode(controller, state.playMode)
    }
    return controller
}

fun applyPlaybackMode(controller: MediaController, mode: PlayMode) {
    controller.repeatMode = when (mode) {
        PlayMode.RepeatOne -> androidx.media3.common.Player.REPEAT_MODE_ONE
        PlayMode.RepeatAll, PlayMode.Shuffle -> androidx.media3.common.Player.REPEAT_MODE_ALL
    }
    controller.shuffleModeEnabled = mode == PlayMode.Shuffle
}

suspend fun playTrackAt(
    context: Context,
    state: MusicPlaybackState,
    index: Int,
    autoPlay: Boolean = true,
) {
    state.playTrackMutex.withLock {
        val track = state.playlist.getOrNull(index) ?: return
        val controller = getController(context, state)
        val items = state.cachedMediaItems ?: withContext(Dispatchers.IO) {
            state.playlist.map { trackItem -> toMediaItem(context, trackItem) }.also {
                state.cachedMediaItems = it
            }
        }

        withContext(Dispatchers.Main) {
            applyPlaybackMode(controller, state.playMode)
            val resumePosition = if (state.pendingSavedUri == track.audioUri) {
                state.pendingResumePosition.coerceAtLeast(0L)
            } else {
                0L
            }
            val sameQueue = controller.mediaItemCount == items.size &&
                    (0 until controller.mediaItemCount).all {
                        controller.getMediaItemAt(it).mediaId == items[it].mediaId
                    }
            val sameTrack = controller.currentMediaItem?.mediaId == track.id.toString()

            state.currentIndex = index
            state.currentTrack = track
            state.errorMsg = null
            if (!sameQueue) {
                controller.setMediaItems(items, index, resumePosition)
                controller.prepare()
            } else if (!sameTrack) {
                controller.seekToDefaultPosition(index)
            } else if (resumePosition > 0L && controller.currentPosition == 0L) {
                controller.seekTo(resumePosition)
            }
            if (autoPlay) {
                controller.play()
            } else {
                controller.pause()
            }
            state.pendingSavedUri = null
            state.pendingResumePosition = 0L
        }
    }

    // 歌词放到播放时才加载
    state.playbackScope.launch(Dispatchers.IO) {
        loadLyricsForTrack(context, state, index)
    }
}

private suspend fun loadLyricsForTrack(
    context: Context,
    state: MusicPlaybackState,
    index: Int,
) {
    val track = state.playlist.getOrNull(index) ?: return
    // 已加载歌词，跳过
    if (track.lyricLines.isNotEmpty()) return
    // 从缓存文件加载
    if (track.lyricCachePath.isNotBlank() && MusicMetadataCache.isValid(track.lyricCachePath)) {
        val lines = MusicMetadataCache.loadLyrics(track.lyricCachePath)
        if (lines.isNotEmpty()) {
            withContext(Dispatchers.Main) {
                state.updateTrack(track.copy(lyricLines = lines))
            }
            return
        }
    }
    // 在线匹配歌词
    try {
        val neteaseId = if (track.neteaseId != 0L) track.neteaseId
        else {
            NeteaseMusicApi.match(track.title, track.artist, track.duration)?.id ?: return
        }
        val lyric = NeteaseMusicApi.lyric(neteaseId)
        if (lyric.lines.isNotEmpty()) {
            val lyricPath = MusicMetadataCache.saveLyrics(context, neteaseId, lyric.lines).orEmpty()
            withContext(Dispatchers.Main) {
                state.updateTrack(track.copy(lyricCachePath = lyricPath, lyricLines = lyric.lines))
            }
        }
    } catch (_: Exception) { }
}

private fun toMediaItem(context: Context, track: MusicTrack): MediaItem {
    val artworkData = MusicMetadataCache.loadCoverBytes(track.coverCachePath)
        ?: track.albumArt?.let(MusicMetadataCache::bitmapToBytes)
    val metadata = androidx.media3.common.MediaMetadata.Builder()
        .setTitle(track.title)
        .setArtist(track.artist)
    if (artworkData != null) {
        metadata.setArtworkData(artworkData, null)
    } else if (track.neteaseCoverUrl.isNotBlank()) {
        metadata.setArtworkUri(Uri.parse(track.neteaseCoverUrl))
    }
    return MediaItem.Builder()
        .setMediaId(track.id.toString())
        .setUri(Uri.parse(track.audioUri))
        .setMediaMetadata(metadata.build())
        .build()
}

fun togglePlayPause(state: MusicPlaybackState) {
    state.playbackScope.launch {
        val controller = state.mediaController
        if (controller == null) {
            val context = state.appContext ?: return@launch
            val index = state.currentIndex
            if (index >= 0) {
                playTrackAt(context, state, index)
            }
            return@launch
        }
        if (controller.isPlaying) controller.pause() else controller.play()
    }
}

fun seekTo(state: MusicPlaybackState, positionMs: Long) {
    state.mediaController?.let { controller ->
        state.playbackScope.launch { controller.seekTo(positionMs) }
    }
}
