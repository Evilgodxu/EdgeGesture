package com.edgegesture.evilgodxu.screens.gesture.service.musicpanel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// 音乐播放器状态持有者（悬浮窗级共享状态）
class MusicPlaybackState {

    var exoPlayer: ExoPlayer? by mutableStateOf(null)
    var playerListener: Player.Listener? by mutableStateOf(null)
    var isPlaying by mutableStateOf(false)
    var isPrepared by mutableStateOf(false)
    var duration by mutableLongStateOf(0L)
    var currentPosition by mutableLongStateOf(0L)
    var playlist by mutableStateOf<List<MusicTrack>>(emptyList())
    var currentIndex by mutableIntStateOf(-1)
    var currentTrack by mutableStateOf<MusicTrack?>(null)
    var playMode by mutableStateOf(PlayMode.RepeatAll)
    var errorMsg by mutableStateOf<String?>(null)
    var isScanning by mutableStateOf(false)

    // 收藏的歌曲 ID 集合（面板级内存状态）
    var likedIds by mutableStateOf<Set<Long>>(emptySet())

    // 定时关闭相关状态（后台计时）
    var timerMinutes by mutableIntStateOf(10)
    var timerRemaining by mutableIntStateOf(0)
    private val timerJob = SupervisorJob()
    private val timerScope = CoroutineScope(timerJob + Dispatchers.Default)
    private var countdownJob: Job? = null

    // 播放控制协程作用域（用于曲目结束自动下一首）
    private val playbackJob = SupervisorJob()
    val playbackScope = CoroutineScope(playbackJob + Dispatchers.Main)

    // 防止手动切歌与自动切歌并发导致状态错乱
    val playTrackMutex = Mutex()

    val hasTrack: Boolean get() = currentTrack != null

    fun release() {
        playerListener?.let { exoPlayer?.removeListener(it) }
        playerListener = null
        exoPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        exoPlayer = null
        isPlaying = false
        isPrepared = false
        duration = 0L
        currentPosition = 0L
        currentTrack = null
        currentIndex = -1
        errorMsg = null
        stopTimer()
    }

    // 启动定时关闭（分钟），计时结束后停止播放并释放资源
    fun startTimer(minutes: Int) {
        stopTimer()
        timerMinutes = minutes
        timerRemaining = minutes
        countdownJob = timerScope.launch {
            while (timerRemaining > 0) {
                delay(60_000L)
                timerRemaining--
            }
            // 计时结束：停止播放并释放全部资源
            exoPlayer?.stop()
            release()
        }
    }

    // 取消定时关闭
    fun stopTimer() {
        countdownJob?.cancel()
        countdownJob = null
        timerRemaining = 0
    }

    // 将原始曲目列表按收藏优先排序，并保留当前曲目索引
    fun setSortedPlaylist(tracks: List<MusicTrack>) {
        val currentId = currentTrack?.id
        val sorted = tracks
            .map { it.copy(isFavorite = likedIds.contains(it.id)) }
            .sortedWith(compareByDescending<MusicTrack> { it.isFavorite }.thenBy { it.title })
        playlist = sorted
        currentIndex = sorted.indexOfFirst { it.id == currentId }.coerceAtLeast(-1)
    }

    // 切换指定曲目的收藏状态并重排列表
    fun toggleFavorite(trackId: Long) {
        likedIds = if (likedIds.contains(trackId)) likedIds - trackId else likedIds + trackId
        setSortedPlaylist(playlist)
    }

    // 更新当前播放位置（用于 UI 进度条）
    fun updatePosition() {
        exoPlayer?.let { player ->
            if (isPrepared) {
                currentPosition = player.currentPosition.coerceIn(0, duration)
            }
        }
    }

    // 构造下一首索引
    fun nextIndex(): Int {
        if (playlist.isEmpty()) return -1
        return when (playMode) {
            PlayMode.RepeatOne -> currentIndex
            PlayMode.RepeatAll -> (currentIndex + 1) % playlist.size
            PlayMode.Shuffle -> {
                if (playlist.size == 1) 0
                else playlist.indices.filter { it != currentIndex }.random()
            }
        }
    }

    // 构造上一首索引
    fun previousIndex(): Int {
        if (playlist.isEmpty()) return -1
        return when (playMode) {
            PlayMode.RepeatOne -> currentIndex
            PlayMode.RepeatAll -> (currentIndex - 1 + playlist.size) % playlist.size
            PlayMode.Shuffle -> {
                if (playlist.size == 1) 0
                else playlist.indices.filter { it != currentIndex }.random()
            }
        }
    }
}
