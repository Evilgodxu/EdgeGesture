package com.edgegesture.evilgodxu.screens.gesture.service.musicpanel

import android.content.Intent
import android.view.KeyEvent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.ForwardingPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.edgegesture.evilgodxu.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(UnstableApi::class)
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
        player.addListener(object : Player.Listener {
            override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
                val format = tracks.groups.firstOrNull { it.isSelected }?.getTrackFormat(0)
                val state = MusicPanelStateHolder.state
                val currentTrack = state.currentTrack
                // 无论 format 是否为空，每次轨道切换都更新信号路径状态
                val fileFormat = format?.let { f ->
                    currentTrack?.path
                        ?.substringAfterLast('.', "")
                        ?.takeIf { it.isNotBlank() }
                        ?.uppercase()
                        ?.let { if (it == "MPEG") "MP3" else it }
                        ?: when (f.sampleMimeType) {
                            "audio/mpeg" -> "MP3"
                            "audio/flac" -> "FLAC"
                            "audio/wav", "audio/x-wav" -> "WAV"
                            "audio/ogg" -> "OGG"
                            "audio/mp4", "audio/aac" -> "AAC"
                            else -> f.sampleMimeType?.substringAfterLast('/')?.uppercase()
                        }
                }
                if (format != null) {
                    val sampleRate = format.sampleRate.takeIf { it > 0 } ?: 48000
                    val channels = format.channelCount.takeIf { it > 0 } ?: 2
                    val encoding = if (format.pcmEncoding > 0) format.pcmEncoding else android.media.AudioFormat.ENCODING_PCM_16BIT
                    // 同一曲目与音频源重复上报时沿用已展示的位深：文件元数据补齐的源位深不能被解码输出位深顶掉
                    val shownBitDepth = state.audioSignalPathFormat
                        ?.takeIf {
                            state.audioSignalPathTrackId == currentTrack?.id &&
                                state.audioSignalPathSourceUri == currentTrack?.audioUri
                        }
                        ?.bitDepth
                    state.audioSignalPathFormat = AudioSignalPathFormat(
                        format = fileFormat ?: "PCM",
                        sampleRate = sampleRate,
                        outputRate = sampleRate,
                        bitDepth = shownBitDepth ?: when (encoding) {
                            android.media.AudioFormat.ENCODING_PCM_8BIT -> 8
                            android.media.AudioFormat.ENCODING_PCM_24BIT_PACKED -> 24
                            android.media.AudioFormat.ENCODING_PCM_FLOAT -> 32
                            else -> 16
                        },
                        channels = channels,
                        // Format.bitrate 单位为 bps，统一换算为 kbps；VBR 曲目 bitrate 未知时回退 averageBitrate
                        bitrate = maxOf(format.bitrate, format.averageBitrate)
                            .takeIf { it > 0 }
                            ?.let { it / 1000 } ?: 0,
                    )
                    // 记录格式信息归属，供信息条判定是否为当前曲目的当前音频源
                    state.audioSignalPathTrackId = currentTrack?.id
                    state.audioSignalPathSourceUri = currentTrack?.audioUri
                    // 解码头对 FLAC/VBR 不给出比特率，位深也只是解码输出位深，回读文件元数据补齐
                    if (currentTrack != null) {
                        refreshFormatFromFile(state, currentTrack)
                    }
                }
                // 每次轨道切换都刷新状态，确保信号路径始终有值
                updateSignalPathState(state)
            }

            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                if (mediaItem != null) {
                    player.playWhenReady = true
                }
            }
        })
        mediaSession = MediaSession.Builder(this, SkipProxyPlayer(player))
            .setCallback(sessionCallback)
            .build()
    }

    /** 拦截系统媒体面板和耳机/蓝牙媒体键的上一首/下一首操作 */
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

    /**
     * 包装 ExoPlayer，把系统媒体面板/通知栏的上一首/下一首操作
     * 映射到应用自己的切歌逻辑，避免默认行为中「回退到当前曲目开头」。
     */
    private inner class SkipProxyPlayer(player: Player) : ForwardingPlayer(player) {
        override fun seekToPrevious() {
            handlePreviousTrack()
        }

        override fun seekToPreviousMediaItem() {
            handlePreviousTrack()
        }

        override fun seekToNext() {
            handleNextTrack()
        }

        override fun seekToNextMediaItem() {
            handleNextTrack()
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

    private fun resolveOutputDeviceName(): String {
        val audioManager = getSystemService(AUDIO_SERVICE) as android.media.AudioManager
        return audioManager.getDevices(android.media.AudioManager.GET_DEVICES_OUTPUTS)
            .firstOrNull { device ->
                device.type == android.media.AudioDeviceInfo.TYPE_BUILTIN_SPEAKER ||
                    device.type == android.media.AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
            }
            ?.productName
            ?.toString()
            ?.takeIf { it.isNotBlank() }
            ?: getString(R.string.signal_path_speaker)
    }

    /**
     * 从文件元数据补齐格式信息：解码头对 FLAC/VBR 不给出比特率，位深也可能只是解码输出位深。
     * 读取切到 IO 线程；回写前校验曲目与音频源未变，避免切歌或换源后把上一首的信息写到新曲目上。
     */
    private fun refreshFormatFromFile(state: MusicPlaybackState, track: MusicTrack) {
        // 解码头已给出比特率时不回读；位深仅 FLAC/WAV 需要容器头，其余格式在判定阶段即返回
        val needBitrate = (state.audioSignalPathFormat?.bitrate ?: 0) <= 0
        state.playbackScope.launch {
            val bitDepth = withContext(Dispatchers.IO) {
                TrackAudioInfoReader.readBitDepth(applicationContext, track)
            }
            val bitrate = if (needBitrate) {
                withContext(Dispatchers.IO) {
                    TrackAudioInfoReader.readBitrateKbps(applicationContext, track)
                }
            } else {
                null
            }
            if (state.audioSignalPathTrackId != track.id ||
                state.audioSignalPathSourceUri != track.audioUri
            ) {
                return@launch
            }
            val shown = state.audioSignalPathFormat ?: return@launch
            val nextBitDepth = bitDepth ?: shown.bitDepth
            val nextBitrate = bitrate ?: shown.bitrate
            // 读到的值与已展示的一致时不回写，避免无意义的状态刷新
            if (nextBitDepth == shown.bitDepth && nextBitrate == shown.bitrate) return@launch
            state.audioSignalPathFormat = shown.copy(bitDepth = nextBitDepth, bitrate = nextBitrate)
        }
    }

    /** 刷新播放链路面板的状态行 */
    private fun updateSignalPathState(state: MusicPlaybackState) {
        state.audioSignalPathStrategy = "Mixer"
        state.audioSignalPathOutputDevice = resolveOutputDeviceName()
        state.audioSignalPathRoute = "System"
    }
}
