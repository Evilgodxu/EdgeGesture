package com.edgegesture.evilgodxu.screens.gesture.service.musicpanel

import android.content.Intent
import android.view.KeyEvent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import kotlinx.coroutines.launch

class MusicPlaybackService : MediaSessionService() {
    private lateinit var player: ExoPlayer
    private var mediaSession: MediaSession? = null
    private var controllerCount = 0

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
        // 音效管理器需要音频会话 ID 来绑定音效（Equalizer / BassBoost 等），
        // 但 ExoPlayer 在创建时 audioSessionId 为 0，直到开始播放后才分配真实 ID。
        // 通过 onAudioSessionIdChanged 监听会话就绪，同步给音效管理器。
        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                if (mediaItem != null) {
                    player.playWhenReady = true
                }
            }

            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                if (audioSessionId > 0) {
                    MusicPanelStateHolder.state.audioSessionId = audioSessionId
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    val sessionId = player.audioSessionId
                    if (sessionId > 0) {
                        MusicPanelStateHolder.state.audioSessionId = sessionId
                    }
                }
            }
        })
        mediaSession = MediaSession.Builder(this, player)
            .setCallback(sessionCallback)
            .build()
    }

    /** 拦截耳机/蓝牙媒体键，直接切歌，避免系统"双击才切上一首"的问题 */
    private val sessionCallback = object : MediaSession.Callback {
        override fun onMediaButtonEvent(
            session: MediaSession,
            controllerInfo: MediaSession.ControllerInfo,
            intent: Intent,
        ): Boolean {
            val keyEvent = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
            if (keyEvent?.action == KeyEvent.ACTION_DOWN) {
                when (keyEvent.keyCode) {
                    KeyEvent.KEYCODE_MEDIA_PREVIOUS,
                    KeyEvent.KEYCODE_MEDIA_SKIP_BACKWARD -> {
                        handlePreviousTrack()
                        return true
                    }
                    KeyEvent.KEYCODE_MEDIA_NEXT,
                    KeyEvent.KEYCODE_MEDIA_SKIP_FORWARD -> {
                        handleNextTrack()
                        return true
                    }
                }
            }
            return super.onMediaButtonEvent(session, controllerInfo, intent)
        }
    }

    private fun handlePreviousTrack() {
        val state = MusicPanelStateHolder.state
        if (state.currentTrack != null && state.playlist.isNotEmpty()) {
            val prev = state.previousIndex()
            if (prev >= 0) {
                state.playbackScope.launch {
                    playTrackAt(this@MusicPlaybackService, state, prev)
                }
            }
        }
    }

    private fun handleNextTrack() {
        val state = MusicPanelStateHolder.state
        if (state.currentTrack != null && state.playlist.isNotEmpty()) {
            val next = state.nextIndex()
            if (next >= 0) {
                state.playbackScope.launch {
                    playTrackAt(this@MusicPlaybackService, state, next)
                }
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {
        super.onUpdateNotification(session, startInForegroundRequired)
        if (!player.isPlaying && player.playbackState == Player.STATE_IDLE) {
            stopSelf()
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!player.isPlaying) {
            stopSelf()
        }
    }

    fun stopPlayback() {
        player.stop()
        mediaSession?.release()
        mediaSession = null
        stopSelf()
    }

    override fun onDestroy() {
        mediaSession?.release()
        mediaSession = null
        player.release()
        super.onDestroy()
    }
}
