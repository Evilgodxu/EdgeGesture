package com.edgegesture.evilgodxu.screens.gesture.service.musicpanel

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.edgegesture.evilgodxu.R
import androidx.compose.ui.unit.dp
import com.edgegesture.evilgodxu.screens.settings.ThemeMode
import com.edgegesture.evilgodxu.screens.settings.settingsFlow
import com.edgegesture.evilgodxu.ui.theme.DarkColorScheme
import com.edgegesture.evilgodxu.ui.theme.LightColorScheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun MusicPanelOverlay(
    playbackState: MusicPlaybackState,
    onScan: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current

    val settings by context.settingsFlow().collectAsState(initial = null)
    val isSystemDark = isSystemInDarkTheme()
    val isDarkTheme = when (settings?.themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        else -> isSystemDark
    }
    val colorScheme = if (isDarkTheme) DarkColorScheme else LightColorScheme
    val scope = rememberCoroutineScope()

    LaunchedEffect(playbackState.isPlaying, playbackState.currentTrack) {
        while (isActive && playbackState.isPlaying) {
            playbackState.updatePosition()
            delay(200)
        }
    }

    LaunchedEffect(playbackState.timerAutoStopped) {
        if (playbackState.timerAutoStopped) {
            playbackState.timerAutoStopped = false
            onDismiss()
        }
    }

    var showPlaylist by remember { mutableStateOf(false) }
    var showTimer by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showSoundEffects by remember { mutableStateOf(false) }
    val currentTrackId = playbackState.currentTrack?.id
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showAudioSignalPath by remember { mutableStateOf(false) }
    var deleteTargetTrack by remember { mutableStateOf<MusicTrack?>(null) }
    var showRename by remember { mutableStateOf(false) }
    var renameIsTitle by remember { mutableStateOf(true) }
    var renameInitValue by remember { mutableStateOf("") }
    var showCoverRefresh by remember { mutableStateOf(false) }
    var showCoverReplace by remember { mutableStateOf(false) }
    var selectedCoverCandidate by remember { mutableStateOf<NeteaseSongSearchResult?>(null) }
    var coverSaveFailed by remember { mutableStateOf(false) }
    var showLyricsRefresh by remember { mutableStateOf(false) }
    var selectedLyricsCandidate by remember { mutableStateOf<NeteaseSongSearchResult?>(null) }

    MaterialTheme(colorScheme = colorScheme) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyUp && event.key == Key.Back) {
                        if (showLyricsRefresh) {
                            showLyricsRefresh = false
                            selectedLyricsCandidate = null
                            playbackState.lyricsCandidates = emptyList()
                            playbackState.lyricsRefreshError = null
                        } else onDismiss()
                        true
                    } else false
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        when {
                            showPlaylist -> showPlaylist = false
                            showTimer -> showTimer = false
                            showSoundEffects -> showSoundEffects = false
                            showAudioSignalPath -> showAudioSignalPath = false
                            showSettings -> showSettings = false
                            showRename -> showRename = false
                            playbackState.showSearchResults -> {
                                playbackState.showSearchResults = false
                                playbackState.errorMsg = null
                            }
                            playbackState.isSearchMode -> {
                                playbackState.isSearchMode = false
                                playbackState.showSearchResults = false
                            }
                            else -> onDismiss()
                        }
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            val cardBackground = if (isDarkTheme) {
                Color(0xFF161B22).copy(alpha = 0.72f)
            } else {
                Color(0xFFF5F5F7).copy(alpha = 0.82f)
            }
            val borderColor = if (isDarkTheme) {
                Color.White.copy(alpha = 0.06f)
            } else {
                Color.Black.copy(alpha = 0.06f)
            }

            Surface(
                modifier = Modifier
                    .widthIn(max = 380.dp)
                    .fillMaxWidth(0.92f)
                    .aspectRatio(4f / 3f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { /* 阻止点击穿透 */ }
                    ),
                shape = RoundedCornerShape(20.dp),
                color = cardBackground,
                border = BorderStroke(width = 1.dp, color = borderColor),
                shadowElevation = 0.dp
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(20.dp))
                ) {
                    val designHeight = 285.dp
                    val scale = (maxHeight / designHeight).coerceAtMost(1f)

                    AnimatedContent(
                        targetState = playbackState.isSearchMode && !playbackState.showSearchResults,
                        transitionSpec = {
                            (slideInVertically { it } + fadeIn()).togetherWith(
                                slideOutVertically { it } + fadeOut()
                            )
                        },
                        label = "search_mode"
                    ) { showSearch ->
                        if (showSearch) {
                            SearchOverlay(
                                playbackState = playbackState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer(
                                        scaleX = scale,
                                        scaleY = scale,
                                        transformOrigin = TransformOrigin(0.5f, 0f)
                                    )
                                    .padding(start = 16.dp, top = 10.dp, end = 16.dp, bottom = 4.dp)
                            )
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer(
                                        scaleX = scale,
                                        scaleY = scale,
                                        transformOrigin = TransformOrigin(0.5f, 0f)
                                    )
                                    .padding(start = 16.dp, top = 10.dp, end = 16.dp, bottom = 4.dp)
                                    .pointerInput(showAudioSignalPath, showPlaylist, showTimer, showSettings) {
                                        var totalDx = 0f
                                        var totalDy = 0f
                                        detectDragGestures(
                                            onDragStart = { totalDx = 0f; totalDy = 0f },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                totalDx += dragAmount.x
                                                totalDy += dragAmount.y
                                            },
                                            onDragEnd = {
                                                if (!showPlaylist && !showTimer && !showSettings) {
                                                    when {
                                                        totalDy < -80f && kotlin.math.abs(totalDy) > kotlin.math.abs(totalDx) -> showAudioSignalPath = true
                                                        totalDx > 50f && kotlin.math.abs(totalDx) > kotlin.math.abs(totalDy) -> playbackState.isSearchMode = true
                                                        totalDx < -50f && kotlin.math.abs(totalDx) > kotlin.math.abs(totalDy) -> showSettings = true
                                                    }
                                                } else if (showAudioSignalPath && totalDy > 80f) {
                                                    showAudioSignalPath = false
                                                }
                                            }
                                        )
                                    }
                            ) {
                                HeaderRow(
                                    playbackState = playbackState,
                                    timerRemaining = playbackState.timerRemaining,
                                    onTimerClick = { showTimer = true },
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (playbackState.isLyricsVisible) {
                                        LyricsPanel(
                                            playbackState = playbackState,
                                            modifier = Modifier.fillMaxSize(),
                                            onClick = { playbackState.isLyricsVisible = false }
                                        )
                                    } else {
                                        CurrentCover(
                                            track = playbackState.currentTrack,
                                            isPlaying = playbackState.isPlaying,
                                            onClick = { playbackState.isLyricsVisible = true },
                                            onRefreshCover = {
                                                showCoverRefresh = true
                                                scope.launch { searchCoverCandidates(playbackState, playbackState.currentTrack!!) }
                                            }
                                        )
                                    }
                                }
                                if (!playbackState.isLyricsVisible) {
                                    TrackInfo(
                                        playbackState = playbackState,
                                        onClick = { playbackState.isLyricsVisible = true },
                                        onRenameRequest = { isTitle, text ->
                                            renameIsTitle = isTitle
                                            renameInitValue = text
                                            showRename = true
                                        }
                                    )
                                }
                                ProgressSection(playbackState = playbackState)

                                ControlBar(
                                    playbackState = playbackState,
                                    onPlaylistClick = { showPlaylist = true },
                                    onLyricsRefreshClick = {
                                        showLyricsRefresh = true
                                        playbackState.currentTrack?.let { track -> scope.launch { searchLyricsCandidates(playbackState, track) } }
                                    }
                                )
                            }
                        }
                    }

                    PlaylistOverlay(
                        visible = showPlaylist,
                        playbackState = playbackState,
                        onScan = onScan,
                        onTrackSelected = { index ->
                            scope.launch {
                                playTrackAt(context, playbackState, index)
                            }
                            showPlaylist = false
                        },
                        onTrackLongPress = { track ->
                            deleteTargetTrack = track
                            showDeleteConfirm = true
                        },
                        onDismiss = { showPlaylist = false }
                    )

                    TimerOverlay(
                        visible = showTimer,
                        minutes = playbackState.timerMinutes,
                        onMinutesChange = { playbackState.timerMinutes = it },
                        onConfirm = {
                            playbackState.startTimer(playbackState.timerMinutes)
                            showTimer = false
                        },
                        onCancel = { showTimer = false }
                    )

                    SearchResultsOverlay(
                        visible = playbackState.showSearchResults,
                        playbackState = playbackState,
                        context = context,
                        onClose = {
                            playbackState.showSearchResults = false
                            playbackState.errorMsg = null
                        },
                        onRefresh = {
                            scope.launch {
                                performSearch(playbackState, context)
                            }
                        },
                        onTrackSelected = { result ->
                            scope.launch {
                                playSearchResult(result, playbackState, context, scope)
                            }
                        }
                    )

                    DeleteConfirmOverlay(
                        visible = showDeleteConfirm,
                        track = deleteTargetTrack,
                        onConfirm = {
                            deleteTargetTrack?.let { track ->
                                playbackState.removeTrack(track.id)
                            }
                            showDeleteConfirm = false
                            deleteTargetTrack = null
                        },
                        onCancel = {
                            showDeleteConfirm = false
                            deleteTargetTrack = null
                        }
                    )

                    RenameOverlay(
                        visible = showRename,
                        isTitle = renameIsTitle,
                        initialValue = renameInitValue,
                        onConfirm = { newValue ->
                            showRename = false
                            val track = playbackState.currentTrack
                            if (track != null) {
                                val updated = if (renameIsTitle) track.copy(title = newValue)
                                              else track.copy(artist = newValue)
                                playbackState.renameTrackMetadata(updated)
                            }
                        },
                        onCancel = { showRename = false }
                    )

                    SettingsOverlay(
                        visible = showSettings,
                        playbackState = playbackState,
                        showSoundEffects = showSoundEffects,
                        onShowSoundEffectsChange = { showSoundEffects = it },
                        onDismiss = { showSettings = false }
                    )

                    CoverRefreshOverlay(
                        visible = showCoverRefresh && !showCoverReplace,
                        track = playbackState.currentTrack,
                        playbackState = playbackState,
                        context = context,
                        selectedId = selectedCoverCandidate?.id,
                        onCandidateSelected = { selectedCoverCandidate = it },
                        onConfirm = {
                            val candidate = selectedCoverCandidate
                            val track = playbackState.currentTrack
                            if (candidate != null && track != null) {
                                val hasCover = track.albumArt != null || MusicMetadataCache.isValid(track.coverCachePath) || track.neteaseCoverUrl.isNotBlank()
                                if (hasCover) {
                                    showCoverReplace = true
                                } else {
                                    scope.launch {
                                        coverSaveFailed = !applyCoverCandidate(context, playbackState, track, candidate)
                                        if (!coverSaveFailed) {
                                            showCoverRefresh = false
                                            selectedCoverCandidate = null
                                        }
                                    }
                                }
                            }
                        },
                        onCancel = {
                            showCoverRefresh = false
                            selectedCoverCandidate = null
                            playbackState.coverCandidates = emptyList()
                        }
                    )

                    LyricsRefreshOverlay(
                        visible = showLyricsRefresh,
                        track = playbackState.currentTrack,
                        playbackState = playbackState,
                        selectedId = selectedLyricsCandidate?.id,
                        onCandidateSelected = { selectedLyricsCandidate = it },
                        onConfirm = {
                            val candidate = selectedLyricsCandidate
                            val track = playbackState.currentTrack
                            if (candidate != null && track != null) scope.launch {
                                val success = applyLyricsCandidate(context, playbackState, track, candidate)
                                if (success) {
                                    showLyricsRefresh = false
                                    selectedLyricsCandidate = null
                                    playbackState.lyricsCandidates = emptyList()
                                } else {
                                    playbackState.lyricsRefreshError = context.getString(R.string.music_panel_lyrics_refresh_failed)
                                }
                            }
                        },
                        onCancel = {
                            showLyricsRefresh = false
                            selectedLyricsCandidate = null
                            playbackState.lyricsCandidates = emptyList()
                            playbackState.lyricsRefreshError = null
                        }
                    )

                    CoverReplaceOverlay(
                        visible = showCoverReplace,
                        track = playbackState.currentTrack,
                        candidate = selectedCoverCandidate,
                        onConfirm = {
                            val candidate = selectedCoverCandidate ?: return@CoverReplaceOverlay
                            val track = playbackState.currentTrack ?: return@CoverReplaceOverlay
                            scope.launch {
                                coverSaveFailed = !applyCoverCandidate(context, playbackState, track, candidate)
                                if (!coverSaveFailed) {
                                    showCoverReplace = false
                                    showCoverRefresh = false
                                    selectedCoverCandidate = null
                                }
                            }
                        },
                        onCancel = { showCoverReplace = false }
                    )
                    if (coverSaveFailed) {
                        Text(
                            text = stringResource(R.string.music_panel_cover_save_failed),
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    AudioSignalPathOverlay(
                        visible = showAudioSignalPath,
                        playbackState = playbackState,
                        onDismiss = { showAudioSignalPath = false },
                    )
                }
            }
        }
    }
}
