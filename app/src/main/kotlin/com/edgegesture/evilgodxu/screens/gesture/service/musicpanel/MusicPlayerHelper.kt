package com.edgegesture.evilgodxu.screens.gesture.service.musicpanel

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

// 在指定索引处播放曲目
suspend fun playTrackAt(context: Context, state: MusicPlaybackState, index: Int) = withContext(Dispatchers.Main) {
    state.playTrackMutex.withLock<Unit> {
        val track = state.playlist.getOrNull(index) ?: return@withLock

        // 复用已有 ExoPlayer 实例，避免反复创建
        val player = state.exoPlayer ?: ExoPlayer.Builder(context).build().also { state.exoPlayer = it }

    // 移除旧监听器并清理状态
    state.playerListener?.let { player.removeListener(it) }
    player.clearMediaItems()
    state.isPrepared = false
    state.isPlaying = false
    state.currentPosition = 0L
    state.duration = 0L

    val uri = if (track.path.startsWith("content://")) Uri.parse(track.path) else Uri.fromFile(File(track.path))
    player.setMediaItem(MediaItem.fromUri(uri))
    player.prepare()
    player.play()

    state.currentIndex = index
    state.currentTrack = track

    val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            state.isPlaying = isPlaying
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_READY -> {
                    state.isPrepared = true
                    state.duration = player.duration.coerceAtLeast(0L)
                }
                Player.STATE_ENDED -> {
                    // 播放结束自动按当前播放模式切到下一首/重播，实现循环播放
                    state.isPlaying = false
                    state.currentPosition = state.duration
                    val next = state.nextIndex()
                    if (next >= 0) {
                        playNextInScope(context, state, next)
                    }
                }
                else -> {}
            }
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            state.errorMsg = "播放失败"
            state.isPlaying = false
            state.isPrepared = false
        }
    }
        player.addListener(listener)
        state.playerListener = listener
    }
}

// 在后台作用域中播放下一首（避免 ExoPlayer 监听器内直接递归调用 playTrackAt）
private fun playNextInScope(context: Context, state: MusicPlaybackState, index: Int) {
    state.playbackScope.launch(Dispatchers.Main) {
        playTrackAt(context, state, index)
    }
}

// 暂停或继续播放
fun togglePlayPause(state: MusicPlaybackState) {
    state.exoPlayer?.let { player ->
        if (player.isPlaying) {
            player.pause()
            state.isPlaying = false
        } else if (state.isPrepared) {
            player.play()
            state.isPlaying = true
        }
    }
}
