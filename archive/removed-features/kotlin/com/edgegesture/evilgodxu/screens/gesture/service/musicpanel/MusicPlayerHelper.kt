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
            state.playlist.map { trackItem -> toMediaItem(trackItem) }.also {
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
            // 封面更新后需要刷新系统媒体面板的 MediaItem
            val needRefreshItems = state.mediaItemsDirty

            state.currentIndex = index
            state.currentTrack = track
            state.errorMsg = null
            state.mediaItemsDirty = false
            if (!sameQueue || needRefreshItems) {
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
}

private fun toMediaItem(track: MusicTrack): MediaItem {
    val metadata = androidx.media3.common.MediaMetadata.Builder()
        .setTitle(track.title)
        .setArtist(track.artist)
    // 系统面板封面直接取 MediaProvider 的条目级专辑封面，应用不再为系统面板另存封面文件
    artworkUri(track)?.let { metadata.setArtworkUri(it) }
    return MediaItem.Builder()
        .setMediaId(track.id.toString())
        .setUri(Uri.parse(track.audioUri))
        .setMediaMetadata(metadata.build())
        .build()
}

// 系统封面 URI：索引曲目在音频条目 URI 上追加 albumart 段，MediaProvider 以
// audio/media/#/albumart 匹配；非索引曲目（外部应用传入）无系统封面，返回 null
private fun artworkUri(track: MusicTrack): Uri? {
    if (!track.audioUri.startsWith("content://media/")) return null
    return Uri.parse(track.audioUri)
        .buildUpon()
        .clearQuery()
        .fragment(null)
        .appendPath("albumart")
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

// 歌词拖拽跳转：定位到目标行并从该处继续播放
fun seekToAndPlay(state: MusicPlaybackState, positionMs: Long) {
    state.mediaController?.let { controller ->
        state.playbackScope.launch {
            controller.seekTo(positionMs)
            controller.play()
        }
    }
}
