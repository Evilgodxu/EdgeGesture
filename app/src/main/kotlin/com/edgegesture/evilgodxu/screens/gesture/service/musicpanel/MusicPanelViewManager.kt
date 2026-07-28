package com.edgegesture.evilgodxu.screens.gesture.service.musicpanel

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.KeyEvent
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

// 音乐面板悬浮窗管理器
class MusicPanelViewManager(
    private val context: Context,
    private val onDismiss: () -> Unit,
    private val onShowFailed: ((WindowManager.BadTokenException) -> Unit)? = null
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var composeView: ComposeView? = null
    private var isDismissing = false
    private val managerJob = SupervisorJob()
    private val managerScope = CoroutineScope(managerJob + Dispatchers.IO)

    private val playbackState = MusicPanelStateHolder.state
    private var pendingExternalUri: android.net.Uri? = null

    // USB 音频独占监听器
    private val usbAudioMonitor = UsbAudioMonitor(
        context = context,
        onUsbDeviceAttached = { deviceName ->
            playbackState.usbDeviceName = deviceName
            playbackState.isUsbDeviceConnected = true
            // 根据用户偏好自动启用 USB 独占
            if (playbackState.usbExclusiveEnabled) {
                managerScope.launch {
                    val success = UsbAudioMonitor.setPreferredUsbDevice(context, true)
                    if (success) {
                        withContext(Dispatchers.Main) {
                            playbackState.isUsbExclusiveMode = true
                        }
                    }
                }
            }
        },
        onUsbDeviceDetached = {
            playbackState.isUsbDeviceConnected = false
            playbackState.isUsbExclusiveMode = false
            playbackState.usbDeviceName = ""
            // 移除首选设备设置，让音频回退到系统默认路由
            managerScope.launch {
                UsbAudioMonitor.setPreferredUsbDevice(context, false)
            }
        }
    )
    // 蓝牙耳机监听器
    private val bluetoothHeadsetMonitor = BluetoothHeadsetMonitor(
        context = context,
        onHeadsetConnected = { deviceName ->
            playbackState.isBluetoothHeadsetConnected = true
            playbackState.bluetoothHeadsetName = deviceName
            // 连接蓝牙耳机时自动降低媒体音量到 35%
            BluetoothHeadsetMonitor.reduceMediaVolume(context, 0.35f)
        },
        onHeadsetDisconnected = {
            playbackState.isBluetoothHeadsetConnected = false
            playbackState.bluetoothHeadsetName = ""
        }
    )
    private val externalTrackMutex = Mutex()
    private val scanMutex = Mutex()
    private var initialization: Deferred<Unit>? = null
    private var mediaObserverRegistered = false
    private var refreshJob: Job? = null
    private val mediaObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            refreshJob?.cancel()
            refreshJob = managerScope.launch {
                delay(300)
                refreshPlaylist()
            }
        }
    }

    fun playExternalUri(uri: android.net.Uri) {
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) {
            // 外部应用可能只授予临时读取权限，仍需继续播放当前 URI
        }
        pendingExternalUri = uri
        if (composeView == null) {
            show()
        }
        loadExternalTrack()
    }

    private val lifecycleOwner = object : LifecycleOwner {
        private val lifecycleRegistry = LifecycleRegistry(this)
        override val lifecycle: Lifecycle get() = lifecycleRegistry
        fun handleLifecycleEvent(event: Lifecycle.Event) = lifecycleRegistry.handleLifecycleEvent(event)
    }

    private val viewModelStoreOwner = object : ViewModelStoreOwner {
        private val store = ViewModelStore()
        override val viewModelStore: ViewModelStore get() = store
    }

    private val savedStateRegistryOwner = object : SavedStateRegistryOwner {
        private val controller = SavedStateRegistryController.create(this)
        override val savedStateRegistry: SavedStateRegistry get() = controller.savedStateRegistry
        override val lifecycle: Lifecycle get() = lifecycleOwner.lifecycle
        fun performAttach() = controller.performAttach()
        fun performRestore() = controller.performRestore(null)
    }

    // 显示音乐面板悬浮窗
    @SuppressLint("ClickableViewAccessibility")
    fun show() {
        if (composeView != null) return

        // 清除上一次定时关闭残留的过期信号，防止面板被误关
        playbackState.timerAutoStopped = false

        val flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    WindowManager.LayoutParams.FLAG_BLUR_BEHIND
                } else 0

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                blurBehindRadius = 80
            }
        }

        // 在 UI 渲染前同步检查已连接的蓝牙设备，确保首次显示时状态正确
        bluetoothHeadsetMonitor.checkExistingSync()

        val view = ComposeView(context).apply {
            alpha = 0f
            scaleX = 0.8f
            scaleY = 0.8f
            setContent {
                MusicPanelOverlay(
                playbackState = playbackState,
                onScan = { requestScan() },
                onDismiss = { dismiss() }
            )
            }
        }

        savedStateRegistryOwner.performAttach()
        savedStateRegistryOwner.performRestore()
        view.setViewTreeLifecycleOwner(lifecycleOwner)
        view.setViewTreeViewModelStoreOwner(viewModelStoreOwner)
        view.setViewTreeSavedStateRegistryOwner(savedStateRegistryOwner)

        view.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                dismiss()
                true
            } else {
                false
            }
        }
        view.isFocusableInTouchMode = true
        view.requestFocus()

        composeView = view
        try {
            windowManager.addView(view, params)
        } catch (e: WindowManager.BadTokenException) {
            composeView = null
            onShowFailed?.invoke(e)
            return
        }
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        view.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(250)
            .setInterpolator(DecelerateInterpolator())
            .start()

        initialization = managerScope.async {
            playbackState.restoreSavedState(context)
            playbackState.removeUnavailableExternalTracks(context)
            if (playbackState.playlist.isEmpty()) {
                scanAndPlay()
            } else {
                withContext(Dispatchers.Main) {
                    restoreCurrentTrack()
                }
                enrichPlaylistMetadata()
            }
            withContext(Dispatchers.Main) {
                playbackState.syncPlaybackState()
                playbackState.updatePosition()
            }
            registerMediaObserver()
            usbAudioMonitor.register()
            bluetoothHeadsetMonitor.register()
        }
    }

    private fun normalizedAudioUri(audioUri: String): String {
        return Uri.parse(audioUri)
            .normalizeScheme()
            .buildUpon()
            .clearQuery()
            .fragment(null)
            .build()
            .toString()
    }

    private fun resolveAudioPath(uri: Uri): String? {
        if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            context.contentResolver.query(
                uri,
                arrayOf(android.provider.MediaStore.Audio.Media.DATA),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    return cursor.getString(0)
                }
            }
        }
        return uri.path
    }

    private fun isSameAudioTrack(track: MusicTrack, targetUri: Uri, targetPath: String?): Boolean {
        return normalizedAudioUri(track.audioUri) == normalizedAudioUri(targetUri.toString()) ||
                (targetPath != null && track.path.isNotBlank() && track.path == targetPath)
    }

    private fun registerMediaObserver() {
        if (mediaObserverRegistered) return
        context.contentResolver.registerContentObserver(
            android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            true,
            mediaObserver
        )
        mediaObserverRegistered = true
    }

    private fun requestScan() {
        if (playbackState.isScanning || isDismissing) return
        refreshJob?.cancel()
        refreshJob = managerScope.launch {
            scanAndPlay()
        }
    }

    private suspend fun refreshPlaylist() = scanMutex.withLock {
        val started = withContext(Dispatchers.Main) {
            if (playbackState.isScanning) false else {
                playbackState.isScanning = true
                true
            }
        }
        if (!started) return@withLock
        try {
            val tracks = MusicScanner.scan(context)
            val externalTracks = withContext(Dispatchers.Main) {
                playbackState.playlist.filter { it.path.isBlank() }
            }
            val mergedTracks = mergeTrackMetadata(deduplicateTracks(tracks + externalTracks))
            withContext(Dispatchers.Main) {
                playbackState.setSortedPlaylist(mergedTracks)
                playbackState.persistPlaylist()
            }
        } finally {
            withContext(Dispatchers.Main + kotlinx.coroutines.NonCancellable) {
                playbackState.isScanning = false
            }
        }
    }

    private fun mergeTrackMetadata(tracks: List<MusicTrack>): List<MusicTrack> {
        val previous = playbackState.playlist.associateBy { normalizedAudioUri(it.audioUri) }
        return tracks.map { track ->
            val cached = previous[normalizedAudioUri(track.audioUri)] ?: return@map track
            track.copy(
                albumArt = track.albumArt ?: cached.albumArt,
                neteaseId = cached.neteaseId,
                neteaseCoverUrl = cached.neteaseCoverUrl,
                coverCachePath = cached.coverCachePath,
                lyricCachePath = cached.lyricCachePath,
                lyricLines = cached.lyricLines
            )
        }
    }

    private fun deduplicateTracks(tracks: List<MusicTrack>): List<MusicTrack> {
        return tracks.distinctBy { normalizedAudioUri(it.audioUri) }
    }

    private fun loadExternalTrack() {
        val uri = pendingExternalUri ?: return
        managerScope.launch {
            externalTrackMutex.withLock {
                initialization?.await()
                if (pendingExternalUri != uri) return@withLock
                val track = MusicScanner.fromUri(context, uri) ?: return@withLock
                val targetUri = Uri.parse(track.audioUri).normalizeScheme()
                val targetPath = resolveAudioPath(targetUri)
                val targetIndex = withContext(Dispatchers.Main) {
                    val existingIndex = playbackState.playlist.indexOfFirst {
                        isSameAudioTrack(it, targetUri, targetPath)
                    }
                    if (existingIndex >= 0) {
                        existingIndex
                    } else {
                        playbackState.playlist = deduplicateTracks(playbackState.playlist + track)
                        playbackState.playlist.indexOfFirst {
                            isSameAudioTrack(it, targetUri, targetPath)
                        }
                    }
                }
                if (targetIndex < 0) return@withLock
                playbackState.persistPlaylist()
                withContext(Dispatchers.Main) {
                    playbackState.currentIndex = targetIndex
                    playbackState.currentTrack = playbackState.playlist[targetIndex]
                }
                withContext(Dispatchers.Main) {
                    playTrackAt(context, playbackState, targetIndex)
                }
                pendingExternalUri = null
            }
        }
    }

    private suspend fun scanAndPlay() = scanMutex.withLock {
        val started = withContext(Dispatchers.Main) {
            if (playbackState.isScanning) false else {
                playbackState.isScanning = true
                true
            }
        }
        if (!started) return@withLock
        try {
            val tracks = MusicScanner.scan(context)
            withContext(Dispatchers.Main) {
                val externalTracks = playbackState.playlist.filter { it.path.isBlank() }
                val mergedTracks = mergeTrackMetadata(deduplicateTracks(tracks + externalTracks))
                playbackState.setSortedPlaylist(mergedTracks)
                playbackState.persistPlaylist()
                restoreCurrentTrack()
            }
            enrichPlaylistMetadata()
        } finally {
            // 使用非取消式上下文确保 isScanning 一定被重置（防止竟态导致卡死）
            withContext(Dispatchers.Main + kotlinx.coroutines.NonCancellable) {
                playbackState.isScanning = false
            }
        }
    }

    private suspend fun enrichPlaylistMetadata() {
        val tracks = withContext(Dispatchers.Main) { playbackState.playlist.toList() }
        tracks.forEach { track ->
            val hasCover = MusicMetadataCache.isCurrentCoverPath(track.coverCachePath)
            val hasLyrics = MusicMetadataCache.isValid(track.lyricCachePath) && track.lyricLines.isNotEmpty()
            if (track.neteaseId != 0L && hasCover && hasLyrics) return@forEach
            try {
                val match = NeteaseMusicApi.match(track.title, track.artist, track.duration)
                    ?: return@forEach
                val coverBytes = NeteaseMusicApi.loadCoverBytes(match.coverUrl.orEmpty())
                val lyric = NeteaseMusicApi.lyric(match.id)
                val coverPath = coverBytes?.let { MusicMetadataCache.saveCover(context, match.id, it) }.orEmpty()
                val cover = MusicMetadataCache.loadCover(coverPath)
                val lyricPath = MusicMetadataCache.saveLyrics(context, match.id, lyric.lines).orEmpty()
                withContext(Dispatchers.Main) {
                    playbackState.updateTrack(
                        track.copy(
                            albumArt = cover ?: track.albumArt,
                            neteaseId = match.id,
                            neteaseCoverUrl = match.coverUrl.orEmpty(),
                            coverCachePath = coverPath,
                            lyricCachePath = lyricPath,
                            lyricLines = lyric.lines
                        )
                    )
                }
            } catch (error: Exception) {
            }
        }
    }

    private fun restoreCurrentTrack() {
        if (playbackState.playlist.isEmpty() || playbackState.currentTrack != null) return
        val savedUri = playbackState.pendingSavedUri
        val index = savedUri?.let { uri -> playbackState.playlist.indexOfFirst { it.audioUri == uri } }
            ?.takeIf { it >= 0 }
            ?: 0
        playbackState.currentIndex = index
        playbackState.currentTrack = playbackState.playlist[index]
    }

    // 关闭音乐面板（保留播放状态与 ExoPlayer，下次显示直接恢复）
    fun dismiss() {
        val view = composeView ?: return
        if (isDismissing) return
        isDismissing = true

        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)

        view.animate()
            .alpha(0f)
            .scaleX(0.9f)
            .scaleY(0.9f)
            .setDuration(200)
            .setInterpolator(AccelerateInterpolator())
            .withEndAction {
                lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
                lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
                try {
                    if (view.windowToken != null) {
                        windowManager.removeView(view)
                    }
                } catch (_: Exception) {
                }
                composeView = null
                isDismissing = false
                if (mediaObserverRegistered) {
                    context.contentResolver.unregisterContentObserver(mediaObserver)
                    mediaObserverRegistered = false
                }
                usbAudioMonitor.unregister()
                bluetoothHeadsetMonitor.unregister()
                playbackState.updatePosition()
                if (!playbackState.isPlaying) {
                    playbackState.release()
                }
                onDismiss()
                managerJob.cancel()
            }
            .start()
    }

    companion object {
        private const val TAG = "MusicPanelViewManager"
    }
}