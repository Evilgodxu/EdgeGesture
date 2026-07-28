package com.edgegesture.evilgodxu.screens.gesture.service.musicpanel

import com.edgegesture.evilgodxu.data.gesture.gestureDataStore
import androidx.compose.runtime.getValue
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import androidx.compose.runtime.setValue
import com.edgegesture.evilgodxu.R
import org.json.JSONArray
import org.json.JSONObject
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.withLock

// 音乐播放器状态持有者（悬浮窗级共享状态）
class MusicPlaybackState {

    private val savedUriKey = stringPreferencesKey("music_saved_uri")
    private val savedPositionKey = longPreferencesKey("music_saved_position")
    private val savedModeKey = intPreferencesKey("music_saved_mode")
    private val playlistCacheKey = "music_playlist_cache"
    private val playlistCachePreferences = "music_playlist_cache_preferences"
    private val searchHistoryKey = "music_search_history"
    private val searchHistoryPreferences = "music_search_history_preferences"
    private var persistenceJob: Job? = null
    var appContext: Context? = null
    var mediaController: MediaController? by mutableStateOf(null)
    var player: Player? by mutableStateOf(null)
    // 在线歌曲播放失败重试计数器，key=trackId
    private val retryCounts = mutableMapOf<Long, Int>()
    private val maxRetries = 2
    val controllerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            syncPlaybackState()
        }

        override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
            if (stopAfterCurrentTrack && reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                // 定时关闭：当前曲目自然结束 → 停止播放
                stopAfterCurrentTrack = false
                timerAutoStopped = true
                release()
                playbackScope.launch {
                    appContext?.let { clearSavedPosition(it) }
                }
                return
            }
            val id = mediaItem?.mediaId?.toLongOrNull() ?: return
            val index = playlist.indexOfFirst { it.id == id }
            if (index >= 0) {
                currentIndex = index
                currentTrack = playlist[index]
                isPrepared = false
                currentPosition = 0L
                duration = 0L
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val controller = mediaController ?: return
            when (playbackState) {
                Player.STATE_READY -> {
                    isPrepared = true
                    syncPlaybackState()
                }
                Player.STATE_ENDED -> {
                    isPlaying = false
                    currentPosition = duration
                    if (stopAfterCurrentTrack) {
                        stopAfterCurrentTrack = false
                        timerAutoStopped = true
                        release()
                        playbackScope.launch {
                            appContext?.let { clearSavedPosition(it) }
                        }
                        return
                    }
                    val next = autoNextIndex()
                    if (next >= 0) {
                        playbackScope.launch {
                            playTrackAt(appContext ?: return@launch, this@MusicPlaybackState, next)
                        }
                    }
                }
            }
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            errorMsg = appContext?.getString(R.string.music_panel_play_failed) ?: "播放失败"
            isPlaying = false
            isPrepared = false
            val failedTrack = currentTrack
            val failedTrackId = failedTrack?.id
            if (failedTrackId != null && failedTrack.path.isNullOrBlank() && failedTrack.neteaseId != 0L) {
                val attempt = retryCounts.getOrDefault(failedTrackId, 0)
                if (attempt < maxRetries) {
                    retryCounts[failedTrackId] = attempt + 1
                    playbackScope.launch {
                        retryFailedTrack(failedTrack)
                    }
                } else {
                    retryCounts.remove(failedTrackId)
                    // 重试用尽 → 尝试自动播放搜索结果列表中下一首
                    val ctx = appContext
                    if (ctx != null && playNextPendingSearchResult(ctx, failedTrack)) return
                    playbackScope.launch {
                        removeTrack(failedTrackId)
                    }
                }
            }
        }

        /** 尝试自动播放搜索结果待播队列中的下一首，返回 true 表示已接手播放 */
        private fun playNextPendingSearchResult(ctx: Context, failedTrack: MusicTrack): Boolean {
            val remaining = pendingSearchResults
            if (remaining.isEmpty()) return false
            // 查找失败曲目在队列中的位置，播其后一首
            val failedPos = remaining.indexOfFirst { it.id == failedTrack.neteaseId }
            val nextIdx = if (failedPos >= 0 && failedPos + 1 < remaining.size) failedPos + 1 else 0
            val next = remaining[nextIdx]
            pendingSearchResults = remaining.drop(nextIdx + 1)

            playbackScope.launch {
                try {
                    val url = withContext(Dispatchers.IO) {
                        NeteaseMusicApi.getSongUrlWithFallback(next.id)
                    }
                    if (url == null) {
                        // 这一首也无法播放，递归尝试再下一首（函数开头有空检查，无需重复判断）
                        playNextPendingSearchResult(ctx, failedTrack)
                        return@launch
                    }
                    val trackId = next.id + 1000000L
                    val track = MusicTrack(
                        id = trackId, path = "", audioUri = url,
                        title = next.title, artist = next.artist,
                        duration = next.duration, albumId = 0L,
                        neteaseId = next.id, neteaseCoverUrl = next.coverUrl.orEmpty()
                    )
                    withContext(Dispatchers.Main) {
                        val failedIdx = playlist.indexOfFirst { it.id == failedTrack.id }
                        val targetIdx = if (failedIdx >= 0) {
                            playlist = playlist.toMutableList().apply { set(failedIdx, track) }
                            failedIdx
                        } else {
                            playlist = playlist + track
                            playlist.size - 1
                        }
                        currentIndex = targetIdx
                        currentTrack = playlist[targetIdx]
                        errorMsg = null
                        persistPlaylist()
                        playTrackAt(ctx, this@MusicPlaybackState, targetIdx)
                    }
                    // 后台加载歌词
                    playbackScope.launch(Dispatchers.IO) {
                        try {
                            val lyric = NeteaseMusicApi.lyric(next.id)
                            if (lyric.lines.isNotEmpty()) {
                                val lyricPath = MusicMetadataCache.saveLyrics(ctx, next.id, lyric.lines).orEmpty()
                                withContext(Dispatchers.Main) {
                                    val idx = playlist.indexOfFirst { it.id == trackId }
                                    if (idx >= 0) {
                                        val updated = playlist[idx].copy(lyricCachePath = lyricPath, lyricLines = lyric.lines)
                                        playlist = playlist.toMutableList().apply { set(idx, updated) }
                                        if (currentTrack?.id == trackId) currentTrack = updated
                                    }
                                }
                            }
                        } catch (_: Exception) { }
                    }
                } catch (_: Exception) {
                    playNextPendingSearchResult(ctx, failedTrack)
                }
            }
            return true
        }

        private suspend fun retryFailedTrack(track: MusicTrack) {
            val ctx = appContext ?: return
            try {
                val newUrl = NeteaseMusicApi.getSongUrlWithFallback(track.neteaseId)
                if (newUrl != null && newUrl != track.audioUri) {
                    val updatedTrack = track.copy(audioUri = newUrl)
                    withContext(Dispatchers.Main) {
                        val idx = playlist.indexOfFirst { it.id == track.id }
                        if (idx < 0) return@withContext
                        playlist = playlist.toMutableList().apply { set(idx, updatedTrack) }
                        currentTrack = updatedTrack
                        persistPlaylist()
                        errorMsg = null
                        playTrackAt(ctx, this@MusicPlaybackState, idx)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        removeTrack(track.id)
                    }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    removeTrack(track.id)
                }
            }
        }
    }
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
    var isLyricsVisible by mutableStateOf(false)

    // 在线搜索相关状态
    var isSearchMode by mutableStateOf(false)
    var searchQuery by mutableStateOf("")
    var searchResults by mutableStateOf<List<NeteaseSongSearchResult>>(emptyList())
    var searchHistory by mutableStateOf<List<String>>(emptyList())
    var isSearching by mutableStateOf(false)
    var showSearchResults by mutableStateOf(false)
    /** 在线搜索结果列表剩余待播曲目（播放失败时自动播下一首） */
    var pendingSearchResults by mutableStateOf<List<NeteaseSongSearchResult>>(emptyList())

    private fun hasUriAccess(context: Context, audioUri: String): Boolean {
        val uri = Uri.parse(audioUri)
        if (context.contentResolver.persistedUriPermissions.none {
                it.uri == uri && it.isReadPermission
            }) return false
        return try {
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { true } ?: false
        } catch (_: Exception) {
            false
        }
    }

    suspend fun removeUnavailableExternalTracks(context: Context) {
        withContext(Dispatchers.Main) {
            val unavailableIds = playlist
                .filter { track ->
                    track.path.isBlank() &&
                        track.audioUri.isNotBlank() &&
                        runCatching { Uri.parse(track.audioUri).scheme == ContentResolver.SCHEME_CONTENT }.getOrElse { false } &&
                        !hasUriAccess(context, track.audioUri)
                }
                .map { it.id }
                .toSet()
            if (unavailableIds.isEmpty()) return@withContext

            val currentWasRemoved = currentTrack?.id in unavailableIds
            playlist = playlist.filterNot { it.id in unavailableIds }
            currentIndex = playlist.indexOfFirst { it.id == currentTrack?.id }
            if (currentWasRemoved) {
                mediaController?.stop()
                currentTrack = null
                currentIndex = -1
                isPlaying = false
                isPrepared = false
                currentPosition = 0L
                duration = 0L
                clearSavedState(context)
            }
            persistPlaylist()
        }
    }

    private suspend fun clearSavedState(context: Context) {
        withContext(Dispatchers.IO) {
            context.gestureDataStore.edit { preferences ->
                preferences.remove(savedUriKey)
                preferences.remove(savedPositionKey)
            }
        }
    }

    // 仅清除持久化的播放位置（定时关闭时使用，保留歌曲 URI）
    private suspend fun clearSavedPosition(context: Context) {
        withContext(Dispatchers.IO) {
            context.gestureDataStore.edit { preferences ->
                preferences.remove(savedPositionKey)
            }
        }
    }

    fun removeTrack(trackId: Long) {
        if (playlist.none { it.id == trackId }) return
        playlist = playlist.filterNot { it.id == trackId }
        if (currentTrack?.id == trackId) {
            mediaController?.stop()
            currentTrack = null
            currentIndex = -1
            isPlaying = false
            isPrepared = false
            currentPosition = 0L
            duration = 0L
        } else {
            currentIndex = playlist.indexOfFirst { it.id == currentTrack?.id }
        }
        persistPlaylist()
    }

    // USB 独占模式相关状态
    var isUsbDeviceConnected by mutableStateOf(false)
    var isUsbExclusiveMode by mutableStateOf(false)
    var usbExclusiveEnabled by mutableStateOf(true)   // 用户偏好：是否启用 USB 独占（默认开启）
    var usbDeviceName by mutableStateOf("")

    // 蓝牙耳机相关状态
    var isBluetoothHeadsetConnected by mutableStateOf(false)
    var bluetoothHeadsetName by mutableStateOf("")

    // 音频会话 ID（由 MusicPlaybackService 在创建 ExoPlayer 后设置）
    var audioSessionId: Int = 0
        set(value) {
            field = value
            if (value > 0) {
                audioEffectManager.audioSessionId = value
            }
        }

    val audioEffectManager: AudioEffectManager by lazy {
        AudioEffectManager(appContext ?: error("appContext 尚未初始化"))
    }

    /** 当前选中的音效（Compose 响应式，驱动 UI 实时更新高亮） */
    var selectedSoundEffect by mutableStateOf<SoundEffect?>(null)

    // 收藏的歌曲 ID 集合（面板级内存状态）
    var likedIds by mutableStateOf<Set<Long>>(emptySet())

    // 定时关闭相关状态（后台计时）
    var timerMinutes by mutableIntStateOf(10)
    var timerRemaining by mutableIntStateOf(0)
    var timerAutoStopped by mutableStateOf(false)
    private val timerJob = SupervisorJob()
    private val timerScope = CoroutineScope(timerJob + Dispatchers.Main)
    private var countdownJob: Job? = null
    private var stopAfterCurrentTrack = false

    // 播放控制协程作用域（用于曲目结束自动下一首）
    private val playbackJob = SupervisorJob()
    val playbackScope = CoroutineScope(playbackJob + Dispatchers.Main)

    // 防止手动切歌与自动切歌并发导致状态错乱
    val playTrackMutex = Mutex()

    val hasTrack: Boolean get() = currentTrack != null

    suspend fun restoreSavedState(context: Context) {
        appContext = context.applicationContext
        searchHistory = withContext(Dispatchers.IO) {
            context.getSharedPreferences(searchHistoryPreferences, Context.MODE_PRIVATE)
                .getString(searchHistoryKey, "")
                ?.split("\n")
                ?.filter(String::isNotBlank)
                .orEmpty()
        }
        val preferences = withContext(Dispatchers.IO) {
            context.gestureDataStore.data.first()
        }
        val cachedPlaylist = withContext(Dispatchers.IO) {
            loadCachedPlaylist(context)
        }
        val savedUri = preferences[savedUriKey]
        val savedPosition = preferences[savedPositionKey] ?: 0L
        val savedMode = preferences[savedModeKey] ?: PlayMode.RepeatAll.ordinal
        withContext(Dispatchers.Main) {
            if (cachedPlaylist.isNotEmpty()) {
                likedIds = cachedPlaylist
                    .filter { it.isFavorite }
                    .map { it.id }
                    .toSet()
            }
            if (playlist.isEmpty() && cachedPlaylist.isNotEmpty()) {
                playlist = cachedPlaylist.map { it.copy(isFavorite = likedIds.contains(it.id)) }
            }
            pendingSavedUri = savedUri
            pendingResumePosition = savedPosition
            currentPosition = savedPosition
            playMode = PlayMode.entries.getOrElse(savedMode) { PlayMode.RepeatAll }
            // 从持久化存储恢复音效选中状态
            selectedSoundEffect = SoundEffect.entries.firstOrNull { audioEffectManager.isEffectEnabled(it) }
        }
    }

    fun persistPlaylist() {
        val context = appContext ?: return
        playbackScope.launch {
            withContext(Dispatchers.IO) {
                saveCachedPlaylist(context, playlist)
            }
        }
    }

    fun addSearchHistory(query: String) {
        val normalized = query.trim()
        if (normalized.isBlank()) return
        searchHistory = listOf(normalized) + searchHistory.filterNot { it == normalized }
        searchHistory = searchHistory.take(10)
        persistSearchHistory()
    }

    fun removeSearchHistory(query: String) {
        searchHistory = searchHistory.filterNot { it == query }
        persistSearchHistory()
    }

    fun clearSearchHistory() {
        searchHistory = emptyList()
        persistSearchHistory()
    }

    private fun persistSearchHistory() {
        val context = appContext ?: return
        playbackScope.launch(Dispatchers.IO) {
            context.getSharedPreferences(searchHistoryPreferences, Context.MODE_PRIVATE)
                .edit()
                .putString(searchHistoryKey, searchHistory.joinToString("\n"))
                .apply()
        }
    }

    private fun loadCachedPlaylist(context: Context): List<MusicTrack> {
        val json = context.getSharedPreferences(playlistCachePreferences, Context.MODE_PRIVATE)
            .getString(playlistCacheKey, null) ?: return emptyList()
        return try {
            val array = JSONArray(json)
            List(array.length()) { index ->
                val item = array.getJSONObject(index)
                MusicTrack(
                    id = item.getLong("id"),
                    path = item.getString("path"),
                    audioUri = item.getString("audioUri"),
                    title = item.getString("title"),
                    artist = item.getString("artist"),
                    duration = item.getLong("duration"),
                    albumId = item.getLong("albumId"),
                    neteaseId = item.optLong("neteaseId", 0L),
                    neteaseCoverUrl = item.optString("neteaseCoverUrl", ""),
                    coverCachePath = item.optString("coverCachePath", ""),
                    isFavorite = item.optBoolean("isFavorite", false),
                    lyricCachePath = item.optString("lyricCachePath", ""),
                    lyricLines = item.optString("lyricCachePath", "")
                        .takeIf { it.isNotBlank() && MusicMetadataCache.isValid(it) }
                        ?.let(MusicMetadataCache::loadLyrics)
                        .orEmpty()
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveCachedPlaylist(context: Context, tracks: List<MusicTrack>) {
        val array = JSONArray()
        tracks.forEach { track ->
            array.put(JSONObject().apply {
                put("id", track.id)
                put("path", track.path)
                put("audioUri", track.audioUri)
                put("title", track.title)
                put("artist", track.artist)
                put("duration", track.duration)
                put("albumId", track.albumId)
                put("neteaseId", track.neteaseId)
                put("neteaseCoverUrl", track.neteaseCoverUrl)
                put("coverCachePath", track.coverCachePath)
                put("lyricCachePath", track.lyricCachePath)
                put("isFavorite", track.isFavorite)
            })
        }
        context.getSharedPreferences(playlistCachePreferences, Context.MODE_PRIVATE)
            .edit()
            .putString(playlistCacheKey, array.toString())
            .apply()
    }

    var pendingSavedUri: String? = null
    var pendingResumePosition: Long = 0L

    fun persistState() {
        val context = appContext ?: return
        val track = currentTrack ?: return
        val position = currentPosition
        val mode = playMode.ordinal
        persistenceJob?.cancel()
        persistenceJob = CoroutineScope(Dispatchers.IO).launch {
            context.gestureDataStore.edit { preferences ->
                preferences[savedUriKey] = track.audioUri
                preferences[savedPositionKey] = position
                preferences[savedModeKey] = mode
            }
        }
    }

    fun release() {
        persistState()
        currentTrack?.let { track ->
            pendingSavedUri = track.audioUri
            pendingResumePosition = currentPosition
        }
        mediaController?.let { controller ->
            playbackScope.launch {
                controller.stop()
                controller.removeListener(controllerListener)
                controller.release()
            }
        }

        mediaController = null
        player = null
        currentPosition = 0L
        isPlaying = false
        isPrepared = false
        duration = 0L
        errorMsg = null
        audioEffectManager.releaseAll()
        stopTimer()
    }

    // ======================== 音效设置 ========================

    /** 查询指定音效是否已启用 */
    fun isSoundEffectEnabled(effect: SoundEffect): Boolean =
        audioEffectManager.isEffectEnabled(effect)

    /** 启用/禁用指定音效 */
    fun setSoundEffectEnabled(effect: SoundEffect, enabled: Boolean) {
        audioEffectManager.setEffectEnabled(effect, enabled)
        selectedSoundEffect = if (enabled) effect else null
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
            // 计时结束：当前歌曲播放完成后停止并释放资源
            if (isPlaying) {
                stopAfterCurrentTrack = true
                withContext(Dispatchers.Main) {
                    mediaController?.let { controller ->
                        controller.repeatMode = Player.REPEAT_MODE_OFF
                        controller.shuffleModeEnabled = false
                    }
                }
            } else {
                release()
                playbackScope.launch {
                    appContext?.let { clearSavedPosition(it) }
                }
            }
        }
    }

    // 取消定时关闭
    fun stopTimer() {
        stopAfterCurrentTrack = false
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
        persistPlaylist()
    }

    // 更新当前播放位置（用于 UI 进度条）
    fun updateTrack(updated: MusicTrack) {
        playlist = playlist.map { if (it.id == updated.id) updated.copy(isFavorite = likedIds.contains(it.id)) else it }
        currentTrack = currentTrack?.let { if (it.id == updated.id) updated else it }
        persistPlaylist()
    }

    fun syncPlaybackState() {
        val controller = mediaController ?: return
        val mediaId = controller.currentMediaItem?.mediaId?.toLongOrNull()
        val index = mediaId?.let { id -> playlist.indexOfFirst { it.id == id } } ?: -1
        if (index >= 0) {
            currentIndex = index
            currentTrack = playlist[index]
        }
        val controllerDuration = controller.duration
        if (controllerDuration > 0L) duration = controllerDuration
        val controllerPosition = controller.currentPosition
        if (controllerPosition >= 0L && duration > 0L) {
            currentPosition = controllerPosition.coerceIn(0L, duration)
        }
        isPlaying = controller.isPlaying
    }

    fun updatePosition() {
        val controller = mediaController ?: return
        val controllerDuration = controller.duration
        if (controllerDuration > 0L) duration = controllerDuration
        val controllerPosition = controller.currentPosition
        if (controllerPosition >= 0L && duration > 0L) {
            currentPosition = controllerPosition.coerceIn(0L, duration)
        }
        isPlaying = controller.isPlaying
    }

    private fun calculateIndex(direction: Int, repeatOne: Boolean): Int {
        if (playlist.isEmpty()) return -1
        val validCurrentIndex = currentIndex.takeIf { it in playlist.indices } ?: 0
        return when {
            playMode == PlayMode.RepeatOne && repeatOne -> validCurrentIndex
            playMode == PlayMode.Shuffle -> {
                if (playlist.size == 1) 0
                else playlist.indices.filter { it != validCurrentIndex }.random()
            }
            direction < 0 -> (validCurrentIndex - 1 + playlist.size) % playlist.size
            else -> (validCurrentIndex + 1) % playlist.size
        }
    }

    private fun autoNextIndex(): Int = calculateIndex(direction = 1, repeatOne = true)

    // 构造下一首索引
    fun nextIndex(): Int = calculateIndex(direction = 1, repeatOne = false)

    // 构造上一首索引
    fun previousIndex(): Int = calculateIndex(direction = -1, repeatOne = false)
}
