package com.edgegesture.evilgodxu.screens.gesture.service.musicpanel

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.automirrored.outlined.PlaylistPlay
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edgegesture.evilgodxu.R
import com.edgegesture.evilgodxu.ui.theme.DarkColorScheme
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

    // 音乐面板严格遵循设计稿深色毛玻璃风格，不跟随系统/应用主题
    val colorScheme = DarkColorScheme
    val scope = rememberCoroutineScope()

    // 进度条自动更新
    LaunchedEffect(playbackState.isPlaying, playbackState.currentTrack) {
        while (isActive && playbackState.isPlaying) {
            playbackState.updatePosition()
            delay(200)
        }
    }

    var showPlaylist by remember { mutableStateOf(false) }
    var showTimer by remember { mutableStateOf(false) }

    // 移除本地计时器：统一由 MusicPlaybackState 在后台维护，
    // 计时结束后自动停止播放并释放资源。

    MaterialTheme(colorScheme = colorScheme) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { if (!showPlaylist && !showTimer) onDismiss() }
                ),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(max = 380.dp)
                    .fillMaxWidth(0.92f)
                    .wrapContentHeight()
                    .heightIn(max = 320.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { /* 阻止点击穿透 */ }
                    ),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF161B22).copy(alpha = 0.72f),
                border = BorderStroke(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.06f)
                ),
                shadowElevation = 0.dp
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 12.dp)
                    ) {
                        HeaderRow(
                            playbackState = playbackState,
                            timerRemaining = playbackState.timerRemaining,
                            onTimerClick = { showTimer = true },
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        AlbumCarousel(
                            playbackState = playbackState,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        TrackInfo(playbackState = playbackState)

                        ProgressSection(playbackState = playbackState)

                        ControlBar(
                            playbackState = playbackState,
                            onPlaylistClick = { showPlaylist = true }
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        VisualizerSection(isPlaying = playbackState.isPlaying)
                    }

                    PlaylistOverlay(
                        visible = showPlaylist,
                        playbackState = playbackState,
                        onTrackSelected = { index ->
                            scope.launch {
                                playTrackAt(context, playbackState, index)
                            }
                            showPlaylist = false
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
                }
            }
        }
    }
}

@Composable
private fun HeaderRow(
    playbackState: MusicPlaybackState,
    timerRemaining: Int,
    onTimerClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var liked by remember { mutableStateOf(false) }
    // 当前歌曲变更时重置本地收藏状态（设计稿中收藏为面板级状态）
    val currentTrackId = playbackState.currentTrack?.id
    LaunchedEffect(currentTrackId) { liked = false }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            HeaderIconButton(icon = Icons.Default.Timer, onClick = onTimerClick)
            if (timerRemaining > 0) {
                Text(
                    text = "${timerRemaining}m",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 10.sp,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
        HeaderIconButton(
            icon = if (liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            onClick = { liked = !liked },
            tint = if (liked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HeaderIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(28.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = tint)
    }
}

@Composable
private fun AlbumCarousel(
    playbackState: MusicPlaybackState,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val playlist = playbackState.playlist
    val current = if (playlist.isEmpty()) -1 else playbackState.currentIndex.coerceIn(playlist.indices)
    val prevIndex = if (current < 0) -1 else (current - 1 + playlist.size) % playlist.size
    val nextIndex = if (current < 0) -1 else (current + 1) % playlist.size

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SideCover(
            track = playlist.getOrNull(prevIndex),
            onClick = {
                if (prevIndex >= 0) scope.launch { playTrackAt(context, playbackState, prevIndex) }
            }
        )
        CenterCover(track = playlist.getOrNull(current), isPlaying = playbackState.isPlaying)
        SideCover(
            track = playlist.getOrNull(nextIndex),
            onClick = {
                if (nextIndex >= 0) scope.launch { playTrackAt(context, playbackState, nextIndex) }
            }
        )
    }
}

@Composable
private fun SideCover(track: MusicTrack?, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .graphicsLayer(scaleX = 0.85f, scaleY = 0.85f)
            .alpha(0.4f)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        AlbumArt(track = track, modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun CenterCover(track: MusicTrack?, isPlaying: Boolean) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .shadow(4.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.5f), spotColor = Color.Black.copy(alpha = 0.5f))
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        AlbumArt(track = track, modifier = Modifier.fillMaxSize())

        // 唱片高光：模拟封面反光
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        listOf(
                            Color.White.copy(alpha = 0.10f),
                            Color.White.copy(alpha = 0.04f),
                            Color.Transparent
                        ),
                        center = androidx.compose.ui.geometry.Offset(0.30f, 0.20f),
                        radius = 0.65f
                    )
                )
        )

        if (!isPlaying && track != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

@Composable
private fun AlbumArt(track: MusicTrack?, modifier: Modifier = Modifier) {
    if (track?.albumArt != null) {
        Image(
            bitmap = track.albumArt.asImageBitmap(),
            contentDescription = track.title,
            modifier = modifier.background(Color.Black),
        )
    } else {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun TrackInfo(playbackState: MusicPlaybackState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val title = playbackState.currentTrack?.title ?: stringResource(R.string.music_panel_empty)
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
            maxLines = 1,
            modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
        )
        Text(
            text = playbackState.currentTrack?.artist ?: "",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ProgressSection(playbackState: MusicPlaybackState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = formatTime(playbackState.currentPosition),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 9.sp,
            modifier = Modifier.width(24.dp),
            textAlign = TextAlign.Start
        )

        val progress = if (playbackState.duration > 0) {
            (playbackState.currentPosition.toFloat() / playbackState.duration).coerceIn(0f, 1f)
        } else 0f
        var seekFraction by remember { mutableFloatStateOf(progress) }
        var isSeeking by remember { mutableStateOf(false) }
        val displayProgress = if (isSeeking) seekFraction else progress

        Box(
            modifier = Modifier
                .weight(1f)
                .height(12.dp)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val pos = event.changes.first().position.x / size.width
                            seekFraction = pos.coerceIn(0f, 1f)
                            isSeeking = true
                            if (event.changes.first().pressed) {
                                playbackState.exoPlayer?.seekTo((seekFraction * playbackState.duration).toLong())
                                playbackState.currentPosition = (seekFraction * playbackState.duration).toLong()
                            }
                            if (event.changes.all { !it.pressed }) {
                                isSeeking = false
                            }
                        }
                    }
                },
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(2.dp))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(displayProgress)
                    .height(3.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                )
            }
        }

        Text(
            text = formatTime(playbackState.duration),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 9.sp,
            modifier = Modifier.width(24.dp),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun ControlBar(
    playbackState: MusicPlaybackState,
    onPlaylistClick: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 循环模式
        val modeIcon = when (playbackState.playMode) {
            PlayMode.RepeatAll -> Icons.Default.Repeat
            PlayMode.RepeatOne -> Icons.Default.RepeatOne
            PlayMode.Shuffle -> Icons.Default.Shuffle
        }
        ControlIconButton(
            icon = modeIcon,
            onClick = {
                playbackState.playMode = when (playbackState.playMode) {
                    PlayMode.RepeatAll -> PlayMode.RepeatOne
                    PlayMode.RepeatOne -> PlayMode.Shuffle
                    PlayMode.Shuffle -> PlayMode.RepeatAll
                }
            },
            size = 32.dp,
            iconSize = 18.dp
        )

        ControlIconButton(
            icon = Icons.Default.SkipPrevious,
            onClick = {
                val prev = playbackState.previousIndex()
                if (prev >= 0) scope.launch { playTrackAt(context, playbackState, prev) }
            },
            enabled = playbackState.playlist.isNotEmpty(),
            size = 32.dp,
            iconSize = 18.dp
        )

        // 播放/暂停
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(36.dp),
            shadowElevation = 0.dp,
            onClick = {
                playbackState.exoPlayer?.let { player ->
                    if (player.isPlaying) {
                        player.pause()
                        playbackState.isPlaying = false
                    } else if (playbackState.isPrepared) {
                        player.play()
                        playbackState.isPlaying = true
                    }
                }
            }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        ControlIconButton(
            icon = Icons.Default.SkipNext,
            onClick = {
                val next = playbackState.nextIndex()
                if (next >= 0) scope.launch { playTrackAt(context, playbackState, next) }
            },
            enabled = playbackState.playlist.isNotEmpty(),
            size = 32.dp,
            iconSize = 18.dp
        )

        ControlIconButton(
            icon = Icons.AutoMirrored.Outlined.PlaylistPlay,
            onClick = onPlaylistClick,
            size = 32.dp,
            iconSize = 18.dp
        )
    }
}

@Composable
private fun ControlIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    enabled: Boolean = true,
    size: androidx.compose.ui.unit.Dp = 32.dp,
    iconSize: androidx.compose.ui.unit.Dp = 18.dp,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(size)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(iconSize),
            tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        )
    }
}

@Composable
private fun VisualizerSection(isPlaying: Boolean) {
    val barCount = 28
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(20.dp)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.Bottom
    ) {
        repeat(barCount) { index ->
            val target = remember { mutableFloatStateOf(0.1f) }
            val animatedHeight by animateFloatAsState(
                targetValue = if (isPlaying) target.value else 0.04f,
                animationSpec = tween(120),
                label = "visualizer_$index"
            )
            LaunchedEffect(isPlaying, index) {
                while (isActive && isPlaying) {
                    target.value = 0.1f + kotlin.random.Random.nextFloat() * 0.55f
                    delay(80 + (index * 15).toLong())
                }
            }
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight(animatedHeight)
                    .background(
                        if (isPlaying) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                        else Color.White.copy(alpha = 0.06f),
                        RoundedCornerShape(0.dp)
                    )
            )
        }
    }
}

@Composable
private fun PlaylistOverlay(
    visible: Boolean,
    playbackState: MusicPlaybackState,
    onTrackSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AnimatedContent(
        targetState = visible,
        transitionSpec = {
            (slideInVertically { it } + fadeIn()).togetherWith(slideOutVertically { it } + fadeOut())
        },
        label = "playlist"
    ) { show ->
        if (show) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF161B22).copy(alpha = 0.90f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { /* 阻止穿透 */ }
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.music_panel_playlist_title),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${playbackState.playlist.size} 首",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                        HeaderIconButton(
                            icon = Icons.Default.Close,
                            onClick = onDismiss,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                if (playbackState.isScanning) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    }
                } else {
                    val listState = rememberLazyListState()
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        itemsIndexed(playbackState.playlist) { index, track ->
                            val isActive = index == playbackState.currentIndex
                            PlaylistRow(
                                track = track,
                                isActive = isActive,
                                isPlaying = isActive && playbackState.isPlaying,
                                onClick = { onTrackSelected(index) },
                                onFavoriteClick = { playbackState.toggleFavorite(track.id) }
                            )
                        }
                    }
                    LaunchedEffect(playbackState.currentIndex) {
                        if (playbackState.currentIndex >= 0 && playbackState.playlist.isNotEmpty()) {
                            listState.animateScrollToItem(
                                playbackState.currentIndex.coerceIn(0, playbackState.playlist.size - 1)
                            )
                        }
                    }
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun PlaylistRow(
    track: MusicTrack,
    isActive: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
) {
    val bg by animateColorAsState(
        targetValue = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
        else Color.Transparent,
        label = "playlist_bg"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(6.dp))
        ) {
            AlbumArt(track = track, modifier = Modifier.fillMaxSize())
            if (isActive && isPlaying) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        repeat(3) { i ->
                            val height by animateFloatAsState(
                                targetValue = 0.4f + kotlin.random.Random.nextFloat() * 0.5f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
                                label = "wave_$i"
                            )
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .height((height * 10).dp)
                                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(1.dp))
                            )
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = track.title,
                color = if (isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = if (isActive) androidx.compose.ui.text.font.FontWeight.Medium else androidx.compose.ui.text.font.FontWeight.Normal,
                modifier = if (track.title.length > 12) Modifier.basicMarquee(iterations = Int.MAX_VALUE) else Modifier
            )
            Text(
                text = track.artist,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        HeaderIconButton(
            icon = if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            onClick = onFavoriteClick,
            tint = if (track.isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun TimerOverlay(
    visible: Boolean,
    minutes: Int,
    onMinutesChange: (Int) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    AnimatedContent(
        targetState = visible,
        transitionSpec = {
            (slideInVertically { it } + fadeIn()).togetherWith(slideOutVertically { it } + fadeOut())
        },
        label = "timer"
    ) { show ->
        if (show) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF161B22).copy(alpha = 0.92f))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.music_panel_timer_title),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    TimerAdjustButton(text = "−", onClick = { onMinutesChange((minutes - 5).coerceAtLeast(1)) })
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = minutes.toString(),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 28.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.music_panel_timer_minutes),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                    TimerAdjustButton(text = "+", onClick = { onMinutesChange((minutes + 5).coerceAtMost(999)) })
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.widthIn(max = 200.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                        onClick = onCancel
                    ) {
                        Text(
                            text = stringResource(R.string.music_panel_timer_cancel),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primary,
                        onClick = onConfirm
                    ) {
                        Text(
                            text = stringResource(R.string.music_panel_timer_confirm),
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 13.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun TimerAdjustButton(text: String, onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
        modifier = Modifier.size(40.dp),
        onClick = onClick
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
            )
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}