package com.edgegesture.evilgodxu.screens.gesture.service.musicpanel

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private suspend fun getController(context: Context, state: MusicPlaybackState): MediaController {
    state.mediaController?.let { return it }
    state.appContext = context.applicationContext
    val token = SessionToken(context, ComponentName(context, MusicPlaybackService::class.java))
    val controller = withContext(Dispatchers.IO) {
        MediaController.Builder(context, token).buildAsync().get()
    }
    withContext(Dispatchers.Main) {
        state.mediaController = controller
        state.player = controller
        controller.addListener(state.controllerListener)
    }
    return controller
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
        val items = state.playlist.map(::toMediaItem)

        withContext(Dispatchers.Main) {
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
}

private fun toMediaItem(track: MusicTrack): MediaItem {
    return MediaItem.Builder()
        .setMediaId(track.id.toString())
        .setUri(Uri.parse(track.audioUri))
        .setMediaMetadata(
            androidx.media3.common.MediaMetadata.Builder()
                .setTitle(track.title)
                .setArtist(track.artist)
                .build()
        )
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
