package com.edgegesture.evilgodxu.screens.gesture.service.musicpanel

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import coil3.compose.AsyncImage
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.automirrored.outlined.PlaylistPlay
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import com.edgegesture.evilgodxu.R
import com.edgegesture.evilgodxu.screens.settings.ThemeMode
import com.edgegesture.evilgodxu.screens.settings.settingsFlow
import com.edgegesture.evilgodxu.ui.theme.DarkColorScheme
import com.edgegesture.evilgodxu.ui.theme.LightColorScheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import android.os.Environment
import android.content.ContentValues
import android.provider.MediaStore
import android.os.Build
import android.net.Uri

@Composable
fun MusicPanelOverlay(
    playbackState: MusicPlaybackState,
    onScan: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current

    // 根据系统/应用设置适配浅色/深色主题
    val settings by context.settingsFlow().collectAsState(initial = null)
    val isSystemDark = isSystemInDarkTheme()
    val isDarkTheme = when (settings?.themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        else -> isSystemDark
    }
    val colorScheme = if (isDarkTheme) DarkColorScheme else LightColorScheme
    val scope = rememberCoroutineScope()

    // 进度条自动更新
    LaunchedEffect(playbackState.isPlaying, playbackState.currentTrack) {
        while (isActive && playbackState.isPlaying) {
            playbackState.updatePosition()
            delay(200)
        }
    }

    // 定时关闭自动停止后关闭面板
    LaunchedEffect(playbackState.timerAutoStopped) {
        if (playbackState.timerAutoStopped) {
            playbackState.timerAutoStopped = false
            onDismiss()
        }
    }

    var showPlaylist by remember { mutableStateOf(false) }
    var showTimer by remember { mutableStateOf(false) }
    val currentTrackId = playbackState.currentTrack?.id
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var deleteTargetTrack by remember { mutableStateOf<MusicTrack?>(null) }

    MaterialTheme(colorScheme = colorScheme) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        if (!showPlaylist && !showTimer) {
                            if (playbackState.isSearchMode) {
                                playbackState.isSearchMode = false
                                playbackState.showSearchResults = false
                            } else {
                                onDismiss()
                            }
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

                    if (playbackState.isSearchMode && !playbackState.showSearchResults) {
                        // 搜索模式：显示搜索输入框
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
                                .pointerInput(Unit) {
                                    detectHorizontalDragGestures(
                                        onDragEnd = { },
                                        onHorizontalDrag = { _, dragAmount ->
                                            if (dragAmount > 50 && !showPlaylist && !showTimer) {
                                                playbackState.isSearchMode = true
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
                                        onClick = { playbackState.isLyricsVisible = true }
                                    )
                                }
                            }
                            if (!playbackState.isLyricsVisible) {
                                TrackInfo(
                                    playbackState = playbackState,
                                    onClick = { playbackState.isLyricsVisible = true }
                                )
                            }
                            ProgressSection(playbackState = playbackState)

                            ControlBar(
                                playbackState = playbackState,
                                onPlaylistClick = { showPlaylist = true }
                            )
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
                            // 提取为本地函数以支持失败时自动播下一首的递归调用
                            suspend fun playSearchResult(target: NeteaseSongSearchResult) {
                                // 1. 优先匹配本地同名歌曲
                                val normalizedTitle = normalizeTitle(target.title)
                                val normalizedArtist = normalizeTitle(target.artist)
                                val localMatch = playbackState.playlist.firstOrNull { t ->
                                    t.path.isNotBlank() &&
                                    normalizeTitle(t.title) == normalizedTitle &&
                                    (normalizedArtist.isBlank() || normalizeTitle(t.artist) == normalizedArtist)
                                }
                                if (localMatch != null) {
                                    val idx = playbackState.playlist.indexOfFirst { it.id == localMatch.id }
                                    if (idx >= 0) {
                                        // 后台更新该本地歌曲的网易云元数据（封面/歌词）
                                        scope.launch {
                                            enrichOnlineMetadata(context, playbackState, localMatch, target)
                                        }
                                        playbackState.errorMsg = null
                                        playbackState.currentIndex = idx
                                        playbackState.currentTrack = playbackState.playlist[idx]
                                        playbackState.isSearchMode = false
                                        playbackState.showSearchResults = false
                                        playbackState.searchQuery = ""
                                        playbackState.searchResults = emptyList()
                                        playTrackAt(context, playbackState, idx)
                                        return
                                    }
                                }

                                // 2. 保存待播队列（当前结果之后的曲目），播放失败时自动播下一首
                                val clickedIndex = playbackState.searchResults.indexOfFirst { it.id == target.id }
                                playbackState.pendingSearchResults = if (clickedIndex >= 0) {
                                    playbackState.searchResults.drop(clickedIndex + 1)
                                } else emptyList()

                                // 3. 先用 songDetail 补全元数据，再获取播放 URL，然后播放
                                val fullResult = if (target.coverUrl.isNullOrBlank() || target.duration <= 0L) {
                                    withContext(Dispatchers.IO) {
                                        NeteaseMusicApi.songDetail(target.id) ?: target
                                    }
                                } else target

                                val url = withContext(Dispatchers.IO) {
                                    NeteaseMusicApi.getSongUrlWithFallback(fullResult.id)
                                }
                                if (url != null) {
                                    playbackState.errorMsg = null
                                    downloadAndPlay(context, playbackState, fullResult, url)
                                    playbackState.isSearchMode = false
                                    playbackState.showSearchResults = false
                                    playbackState.searchQuery = ""
                                    playbackState.searchResults = emptyList()
                                } else {
                                    playbackState.errorMsg = "该歌曲暂时无法播放（可能为VIP歌曲）"
                                    // URL 获取失败也尝试播下一首
                                    val pending = playbackState.pendingSearchResults
                                    if (pending.isNotEmpty()) {
                                        playbackState.pendingSearchResults = pending.drop(1)
                                        playSearchResult(pending.first())
                                    } else {
                                        playbackState.pendingSearchResults = emptyList()
                                    }
                                }
                            }

                            scope.launch {
                                playSearchResult(result)
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
    val currentTrackId = playbackState.currentTrack?.id
    val isLiked = currentTrackId?.let { id -> playbackState.likedIds.contains(id) } ?: false

    var memoryUsageMb by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        while (isActive) {
            val runtime = Runtime.getRuntime()
            val usedBytes = runtime.totalMemory() - runtime.freeMemory()
            memoryUsageMb = usedBytes / (1024f * 1024f)
            delay(2000)
        }
    }

    Box(
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            HeaderIconButton(
                icon = Icons.Default.Timer,
                onClick = onTimerClick,
                modifier = Modifier.offset(y = 4.dp)
            )
            if (timerRemaining > 0) {
                Text(
                    text = "${timerRemaining}m",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 10.sp,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .offset(y = 4.dp)
                        .clickable { playbackState.stopTimer() }
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .align(Alignment.Center)
                .alpha(0.6f)
        ) {
            Icon(
                imageVector = Icons.Default.Memory,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = "${memoryUsageMb.toInt()} MB",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
            )
        }

        HeaderIconButton(
            icon = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            onClick = {
                currentTrackId?.let { playbackState.toggleFavorite(it) }
            },
            tint = if (isLiked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(y = 4.dp)
        )
    }
}

@Composable
private fun HeaderIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.size(32.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(21.dp),
            tint = if (enabled) tint else tint.copy(alpha = 0.3f)
        )
    }
}

@Composable
private fun CurrentCover(
    track: MusicTrack?,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rotation = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isActive) {
                rotation.animateTo(
                    targetValue = rotation.value + 360f,
                    animationSpec = androidx.compose.animation.core.tween(
                        durationMillis = 12_000,
                        easing = androidx.compose.animation.core.LinearEasing
                    )
                )
            }
        }
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .graphicsLayer { rotationZ = rotation.value }
                .clip(CircleShape)
        ) {
            AlbumArt(track = track, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun LyricsPanel(
    playbackState: MusicPlaybackState,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    var lyricPosition by remember { mutableLongStateOf(playbackState.currentPosition) }
    LaunchedEffect(playbackState.isPlaying, playbackState.currentTrack?.id) {
        while (isActive) {
            lyricPosition = playbackState.mediaController?.currentPosition
                ?.takeIf { it >= 0L }
                ?: playbackState.currentPosition
            delay(if (playbackState.isPlaying) 50L else 200L)
        }
    }

    val lines = playbackState.currentTrack?.lyricLines.orEmpty()
    // 播放器尚未到达第一句时也以第一句作为当前行，保证第一句从第三行开始。
    val activeIndex = lines.indexOfLast { it.timeMs <= lyricPosition }.coerceAtLeast(0)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(top = 4.dp, bottom = 0.dp),
        contentAlignment = Alignment.Center
    ) {
        if (lines.isEmpty()) {
            Text("暂无歌词", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        } else {
            AnimatedContent(
                targetState = activeIndex,
                transitionSpec = {
                    val movingForward = targetState > initialState
                    val distance = { height: Int -> (height / 4).coerceAtLeast(1) }
                    if (movingForward) {
                        (slideInVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { it }
                            + fadeIn(animationSpec = tween(180))) togetherWith
                            (slideOutVertically(animationSpec = tween(260)) { -distance(it) }
                                + fadeOut(animationSpec = tween(180)))
                    } else {
                        (slideInVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { -distance(it) }
                            + fadeIn(animationSpec = tween(180))) togetherWith
                            (slideOutVertically(animationSpec = tween(260)) { it }
                                + fadeOut(animationSpec = tween(180)))
                    }
                },
                label = "lyric_column_scroll"
            ) { renderedActiveIndex ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    repeat(5) { row ->
                        val index = renderedActiveIndex - 2 + row
                        val line = lines.getOrNull(index)
                        if (line == null) {
                            LyricSpacer()
                            return@repeat
                        }
                        val isCurrent = index == activeIndex
                        val emphasis by animateFloatAsState(
                            targetValue = if (isCurrent) 1f else 0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            label = "lyric_emphasis"
                        )
                        val scale = 0.98f + 0.16f * emphasis
                        val nextTimeMs = lines.getOrNull(index + 1)?.timeMs ?: line.timeMs + 3000L
                        val liftProgress = if (isCurrent) {
                            ((lyricPosition - line.timeMs).toFloat() /
                                (nextTimeMs - line.timeMs).coerceAtLeast(1L)).coerceIn(0f, 1f)
                        } else 0f
                        val lift = -2.dp * (1f - (1f - liftProgress) * (1f - liftProgress) * (1f - liftProgress))
                        LyricText(
                            line = line,
                            nextTimeMs = nextTimeMs,
                            positionMs = lyricPosition,
                            isCurrent = isCurrent,
                            text = buildLyricText(line, nextTimeMs, lyricPosition, isCurrent),
                            fontSize = 12.sp,
                            fontWeight = if (isCurrent) androidx.compose.ui.text.font.FontWeight.Medium
                            else androidx.compose.ui.text.font.FontWeight.Normal,
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    translationY = lift.toPx()
                                }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    // 固定五行：当前行居中，上下各显示两行歌词。
                }
            }
        }
    }
}

@Composable
private fun LyricSpacer() {
    Spacer(modifier = Modifier.height(18.dp))
}

@Composable
private fun LyricText(
    line: LyricLine,
    nextTimeMs: Long,
    positionMs: Long,
    isCurrent: Boolean,
    text: androidx.compose.ui.text.AnnotatedString,
    fontSize: androidx.compose.ui.unit.TextUnit,
    fontWeight: androidx.compose.ui.text.font.FontWeight,
    modifier: Modifier = Modifier,
) {
    val pendingColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
    val activeColor = MaterialTheme.colorScheme.primary
    val duration = (nextTimeMs - line.timeMs).coerceAtLeast(1L)
    val progress = when {
        !isCurrent || positionMs <= line.timeMs -> 0f
        positionMs >= nextTimeMs -> 1f
        else -> ((positionMs - line.timeMs).toFloat() / duration).coerceIn(0f, 1f)
    }
    val lyricBrush = when {
        progress <= 0f -> Brush.horizontalGradient(listOf(pendingColor, pendingColor))
        progress >= 1f -> Brush.horizontalGradient(listOf(activeColor, activeColor))
        else -> Brush.horizontalGradient(
            colorStops = arrayOf(
                0f to activeColor,
                progress to activeColor,
                progress to pendingColor,
                1f to pendingColor
            )
        )
    }

    Text(
        text = line.text,
        style = androidx.compose.ui.text.TextStyle(
            brush = lyricBrush,
            shadow = if (progress > 0f) androidx.compose.ui.graphics.Shadow(
                activeColor.copy(alpha = 0.65f),
                blurRadius = 7f
            ) else null
        ),
        fontSize = fontSize,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        fontWeight = fontWeight,
        modifier = modifier
    )
}

@Composable
private fun buildLyricText(
    line: LyricLine,
    nextTimeMs: Long,
    positionMs: Long,
    isCurrent: Boolean,
): androidx.compose.ui.text.AnnotatedString {
    val primary = MaterialTheme.colorScheme.primary
    val idle = MaterialTheme.colorScheme.onSurfaceVariant
    // 未唱部分始终使用普通灰色，避免当前行在演唱前整行显示主题色。
    val pendingColor = idle.copy(alpha = 0.72f)
    val activeColor = primary.copy(alpha = 1f)
    val highlightShadow = androidx.compose.ui.graphics.Shadow(
        primary.copy(alpha = 0.65f),
        blurRadius = 7f
    )

    val tokens = if (line.words.isNotEmpty()) {
        line.words
    } else {
        val parts = splitLyricText(line.text)
        val duration = (nextTimeMs - line.timeMs).coerceAtLeast(1L)
        val partDuration = (duration / parts.size.coerceAtLeast(1)).coerceAtLeast(1L)
        parts.mapIndexed { index, part ->
            LyricWord(
                startMs = line.timeMs + index * partDuration,
                durationMs = if (index == parts.lastIndex) {
                    (nextTimeMs - (line.timeMs + index * partDuration)).coerceAtLeast(1L)
                } else partDuration,
                text = part
            )
        }
    }

    return buildAnnotatedString {
        tokens.forEachIndexed { tokenIndex, token ->
            // 示例：非最后一段平滑过渡到下一段的开始时间；最后一段使用自身
            // 的 duration，避免最后一个字在下一句开始前提前完成。
            val tokenEndMs = if (tokenIndex + 1 < tokens.size) {
                tokens[tokenIndex + 1].startMs.coerceAtLeast(token.startMs + 1L)
            } else {
                token.startMs + token.durationMs.coerceAtLeast(1L)
            }
            // 只有当前演唱行显示逐字进度；已唱行恢复为未演唱色。
            val progress = if (isCurrent) {
                ((positionMs - token.startMs).toFloat() / (tokenEndMs - token.startMs)).coerceIn(0f, 1f)
            } else 0f
            val split = (token.text.length * progress).toInt()
            withStyle(
                SpanStyle(
                    color = activeColor,
                    shadow = if (split > 0) highlightShadow else null
                )
            ) { append(token.text.take(split)) }
            withStyle(SpanStyle(color = pendingColor)) { append(token.text.drop(split)) }
        }
    }
}

private fun splitLyricText(text: String): List<String> {
    if (text.isBlank()) return listOf(text)
    val result = mutableListOf<String>()
    var index = 0
    while (index < text.length) {
        val start = index
        val isSpace = text[index].isWhitespace()
        if (isSpace) {
            while (index < text.length && text[index].isWhitespace()) index++
        } else if (text[index].isLetterOrDigit() && text[index].code < 128) {
            while (index < text.length && text[index].isLetterOrDigit() && text[index].code < 128) index++
        } else {
            index++
        }
        result += text.substring(start, index)
    }
    return result
}

@Composable
private fun TrackInfo(
    playbackState: MusicPlaybackState,
    onClick: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(top = 4.dp, bottom = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val title = when {
            playbackState.currentTrack != null -> playbackState.currentTrack!!.title
            playbackState.isScanning -> stringResource(R.string.music_panel_scanning)
            else -> stringResource(R.string.music_panel_empty)
        }
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
private fun AlbumArt(track: MusicTrack?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val model = track?.coverCachePath?.takeIf { MusicMetadataCache.isValid(it) }
        ?: track?.neteaseCoverUrl?.takeIf { it.isNotBlank() }
    if (model != null) {
        AsyncImage(
            model = model,
            contentDescription = track?.title,
            contentScale = ContentScale.Crop,
            modifier = modifier.background(Color.Black),
        )
    } else if (track?.albumArt != null) {
        Image(
            bitmap = track.albumArt.asImageBitmap(),
            contentDescription = track.title,
            contentScale = ContentScale.Crop,
            modifier = modifier.background(Color.Black),
        )
    } else {
        Box(
            modifier = modifier
                .background(MaterialTheme.colorScheme.surfaceVariant),
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
        val title = when {
            playbackState.currentTrack != null -> playbackState.currentTrack!!.title
            playbackState.isScanning -> stringResource(R.string.music_panel_scanning)
            else -> stringResource(R.string.music_panel_empty)
        }
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp, bottom = 2.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        VisualizerSection(
            isPlaying = playbackState.isPlaying,
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .alpha(0.45f)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
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
                .height(20.dp)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val pos = event.changes.first().position.x / size.width
                            seekFraction = pos.coerceIn(0f, 1f)
                            isSeeking = true
                            if (event.changes.first().pressed) {
                                seekTo(playbackState, (seekFraction * playbackState.duration).toLong())
                                playbackState.currentPosition = (seekFraction * playbackState.duration).toLong().coerceIn(0L, playbackState.duration)
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
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(2.dp))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(displayProgress)
                    .height(3.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
            )
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
                playbackState.mediaController?.let { controller ->
                    applyPlaybackMode(controller, playbackState.playMode)
                }
                playbackState.persistState()
            },
            size = 32.dp,
            iconSize = 21.dp
        )

        ControlIconButton(
            icon = Icons.Default.SkipPrevious,
            onClick = {
                val prev = playbackState.previousIndex()
                if (prev >= 0) scope.launch { playTrackAt(context, playbackState, prev) }
            },
            enabled = playbackState.playlist.isNotEmpty(),
            size = 32.dp,
            iconSize = 21.dp
        )

        // 播放/暂停
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(36.dp),
            shadowElevation = 0.dp,
            onClick = {
                togglePlayPause(playbackState)
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
            iconSize = 21.dp
        )

        ControlIconButton(
            icon = Icons.AutoMirrored.Outlined.PlaylistPlay,
            onClick = onPlaylistClick,
            size = 32.dp,
            iconSize = 21.dp
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
private fun VisualizerSection(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
) {
    val barCount = 28
    val primary = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    Row(
        modifier = modifier
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
                        if (isPlaying) primary.copy(alpha = 0.7f) else inactiveColor,
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
    onScan: () -> Unit,
    onTrackSelected: (Int) -> Unit,
    onTrackLongPress: (MusicTrack) -> Unit,
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
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.94f))
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
                            icon = Icons.Default.Refresh,
                            onClick = { if (!playbackState.isScanning) onScan() },
                            modifier = Modifier.size(24.dp),
                            enabled = !playbackState.isScanning
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
                        itemsIndexed(
                            items = playbackState.playlist,
                            key = { _, track -> track.audioUri }
                        ) { index, track ->
                            val isActive = index == playbackState.currentIndex
                            PlaylistRow(
                                track = track,
                                isActive = isActive,
                                isPlaying = isActive && playbackState.isPlaying,
                                onClick = { onTrackSelected(index) },
                                onLongClick = { onTrackLongPress(track) },
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
    onLongClick: () -> Unit,
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
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
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
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
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

// ===================== 在线搜索 =====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchOverlay(
    playbackState: MusicPlaybackState,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    Column(
        modifier = modifier.pointerInput(Unit) {
            detectHorizontalDragGestures { _, dragAmount ->
                if (dragAmount < -50f) {
                    playbackState.isSearchMode = false
                    playbackState.showSearchResults = false
                }
            }
        }
    ) {
        Text(
            text = stringResource(R.string.music_panel_search_title),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Material3 DockedSearchBar 风格迷你搜索框
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(24.dp)
                ),
            contentAlignment = Alignment.CenterStart
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(start = 16.dp)
                    .size(20.dp)
            )
            BasicTextField(
                value = playbackState.searchQuery,
                onValueChange = { playbackState.searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 48.dp, end = 44.dp),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        val query = playbackState.searchQuery.trim()
                        if (query.isNotBlank()) {
                            scope.launch {
                                performSearch(playbackState, context)
                            }
                        }
                    }
                ),
                decorationBox = { innerTextField ->
                    Box {
                        if (playbackState.searchQuery.isEmpty()) {
                            Text(
                                text = stringResource(R.string.music_panel_search_placeholder),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                fontSize = 14.sp
                            )
                        }
                        innerTextField()
                    }
                }
            )
            if (playbackState.searchQuery.isNotEmpty()) {
                IconButton(
                    onClick = { playbackState.searchQuery = "" },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        if (playbackState.searchHistory.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "搜索历史",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                )
                IconButton(
                    onClick = { playbackState.clearSearchHistory() },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "清理全部历史",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(playbackState.searchHistory, key = { it }) { query ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                playbackState.searchQuery = query
                                scope.launch { performSearch(playbackState, context) }
                            }
                            .padding(start = 8.dp, end = 2.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = query,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { playbackState.removeSearchHistory(query) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "删除历史",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultsOverlay(
    visible: Boolean,
    playbackState: MusicPlaybackState,
    context: android.content.Context,
    onClose: () -> Unit,
    onRefresh: () -> Unit,
    onTrackSelected: (NeteaseSongSearchResult) -> Unit,
) {
    AnimatedContent(
        targetState = visible,
        transitionSpec = {
            (slideInVertically { it } + fadeIn()).togetherWith(slideOutVertically { it } + fadeOut())
        },
        label = "search_results"
    ) { show ->
        if (show) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.94f))
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
                        text = stringResource(R.string.music_panel_search_title),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${playbackState.searchResults.size} 首",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                        HeaderIconButton(
                            icon = Icons.Default.Refresh,
                            onClick = { if (!playbackState.isSearching) onRefresh() },
                            modifier = Modifier.size(24.dp),
                            enabled = !playbackState.isSearching
                        )
                        HeaderIconButton(
                            icon = Icons.Default.Close,
                            onClick = onClose,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 错误消息提示
                val errorMsg = playbackState.errorMsg
                if (errorMsg != null) {
                    Text(
                        text = errorMsg,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.error.copy(alpha = 0.08f),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                if (playbackState.isSearching) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    }
                } else if (playbackState.searchResults.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "未找到相关歌曲",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    val listState = rememberLazyListState()
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        itemsIndexed(
                            items = playbackState.searchResults,
                            key = { _, result -> result.id }
                        ) { index, result ->
                            SearchResultRow(
                                result = result,
                                onClick = { onTrackSelected(result) }
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
private fun SearchResultRow(
    result: NeteaseSongSearchResult,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            // 使用 CDN 缩略图（coverThumbUrl）以加快加载速度，与 QPlayer 的 SongRow 一致
            val coverModel = (result.coverThumbUrl ?: result.coverUrl)?.takeIf { it.isNotBlank() }
            if (coverModel != null) {
                AsyncImage(
                    model = coverModel,
                    contentDescription = result.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = result.title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = result.artist,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // 来源标签，参考 QPlayer SearchRow.kindLabel
        Text(
            text = "网易云",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 9.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
            modifier = Modifier
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                    RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 5.dp, vertical = 2.dp)
        )
    }
}

// ===================== 长按删除确认 =====================

@Composable
private fun DeleteConfirmOverlay(
    visible: Boolean,
    track: MusicTrack?,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    AnimatedContent(
        targetState = visible,
        transitionSpec = {
            (slideInVertically { it } + fadeIn()).togetherWith(slideOutVertically { it } + fadeOut())
        },
        label = "delete_confirm"
    ) { show ->
        if (show) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onCancel
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.music_panel_delete_title),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = track?.title ?: "",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(24.dp))
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
                                text = stringResource(R.string.music_panel_delete_cancel),
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
                            color = MaterialTheme.colorScheme.error,
                            onClick = onConfirm
                        ) {
                            Text(
                                text = stringResource(R.string.music_panel_delete_confirm),
                                color = MaterialTheme.colorScheme.onError,
                                fontSize = 13.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 10.dp)
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

// ===================== 搜索与下载辅助函数 =====================

private suspend fun performSearch(
    playbackState: MusicPlaybackState,
    context: android.content.Context,
) {
    val query = playbackState.searchQuery.trim()
    if (query.isBlank()) return
    playbackState.isSearching = true
    playbackState.searchResults = emptyList()
    playbackState.errorMsg = null
    try {
        val results = NeteaseMusicApi.searchSongs(query)
        playbackState.searchResults = results
        if (results.isNotEmpty()) playbackState.addSearchHistory(query)
        playbackState.showSearchResults = true
    } catch (_: Exception) {
        playbackState.searchResults = emptyList()
    } finally {
        playbackState.isSearching = false
    }
}

private suspend fun downloadAndPlay(
    context: android.content.Context,
    playbackState: MusicPlaybackState,
    result: NeteaseSongSearchResult,
    url: String,
) {
    val trackId = result.id + 1000000L
    val track = MusicTrack(
        id = trackId,
        path = "",
        audioUri = url,  // 先用在线流地址
        title = result.title,
        artist = result.artist,
        duration = result.duration,
        albumId = 0L,
        neteaseId = result.id,
        neteaseCoverUrl = result.coverUrl.orEmpty()
    )

    // 2. 立即加入播放列表并开始播放
    withContext(Dispatchers.Main) {
        val existingIndex = playbackState.playlist.indexOfFirst { it.id == trackId }
        val targetIndex = if (existingIndex >= 0) {
            existingIndex
        } else {
            playbackState.playlist = playbackState.playlist + track
            playbackState.playlist.size - 1
        }
        playbackState.currentIndex = targetIndex
        playbackState.currentTrack = playbackState.playlist[targetIndex]
        playbackState.persistPlaylist()
        playTrackAt(context, playbackState, targetIndex)
    }

    // 3. 后台加载歌词
    playbackState.playbackScope.launch(Dispatchers.IO) {
        try {
            val lyric = NeteaseMusicApi.lyric(result.id)
            if (lyric.lines.isNotEmpty()) {
                val lyricPath = MusicMetadataCache.saveLyrics(context, result.id, lyric.lines).orEmpty()
                withContext(Dispatchers.Main) {
                    val idx = playbackState.playlist.indexOfFirst { it.id == trackId }
                    if (idx >= 0) {
                        val updated = playbackState.playlist[idx].copy(
                            lyricCachePath = lyricPath,
                            lyricLines = lyric.lines
                        )
                        val list = playbackState.playlist.toMutableList()
                        list[idx] = updated
                        playbackState.playlist = list
                        if (playbackState.currentTrack?.id == trackId) {
                            playbackState.currentTrack = updated
                        }
                    }
                }
            }
        } catch (_: Exception) { }
    }

    // 4. 后台缓存到系统下载目录（不阻塞播放）
    playbackState.playbackScope.launch(Dispatchers.IO) {
        cacheToDownloads(context, result, url, trackId, playbackState)
    }
}

private suspend fun cacheToDownloads(
    context: android.content.Context,
    result: NeteaseSongSearchResult,
    url: String,
    trackId: Long,
    playbackState: MusicPlaybackState,
) {
    try {
        val fileName = "${sanitizeFileName(result.title)} - ${sanitizeFileName(result.artist)}.mp3"

        // 先检查是否已存在同名缓存文件
        val existingUri = findExistingDownload(context, fileName)
        if (existingUri != null) {
            withContext(Dispatchers.Main) {
                updateTrackAudioUri(playbackState, trackId, existingUri)
            }
            return
        }

        val connection = URL(url).openConnection()
        connection.connectTimeout = 15000
        connection.readTimeout = 60000
        val bytes = (connection as java.net.HttpURLConnection).inputStream.use { it.readBytes() }

        val audioUri: String

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "audio/mpeg")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/EdgeGesture")
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { os -> os.write(bytes) }
                audioUri = uri.toString()
            } else {
                return
            }
        } else {
            @Suppress("DEPRECATION")
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(dir, fileName)
            file.writeBytes(bytes)
            audioUri = Uri.fromFile(file).toString()
        }

        // 更新播放列表中该曲目的 audioUri 为本地缓存路径
        withContext(Dispatchers.Main) {
            updateTrackAudioUri(playbackState, trackId, audioUri)
        }
    } catch (_: Exception) {
        // 缓存失败不影响已开始的播放
    }
}

private fun sanitizeFileName(name: String): String {
    return name.replace(Regex("[/\\\\:*?\"<>|]"), "_")
        .take(80)
        .trim()
}

// ===================== 辅助函数（本地匹配/元数据补全/缓存去重） =====================

/** 归一化标题/艺术家用于模糊匹配 */
private fun normalizeTitle(value: String): String {
    return value.lowercase()
        .replace(Regex("[\\s　（）()\\[\\]【】「」『』《》〈〉、，。！？\"'“”‘’]+"), "")
        .trim()
}

/** 后台补全本地匹配歌曲的网易云元数据（封面 + 歌词） */
private suspend fun enrichOnlineMetadata(
    context: android.content.Context,
    playbackState: MusicPlaybackState,
    track: MusicTrack,
    result: NeteaseSongSearchResult,
) {
    if (track.neteaseId != 0L && track.lyricLines.isNotEmpty()) return
    try {
        val lyric = NeteaseMusicApi.lyric(result.id)
        val lyricPath = if (lyric.lines.isNotEmpty()) {
            MusicMetadataCache.saveLyrics(context, result.id, lyric.lines).orEmpty()
        } else ""
        withContext(Dispatchers.Main) {
            val idx = playbackState.playlist.indexOfFirst { it.id == track.id }
            if (idx < 0) return@withContext
            val updated = playbackState.playlist[idx].copy(
                neteaseId = result.id,
                neteaseCoverUrl = result.coverUrl.orEmpty(),
                lyricCachePath = lyricPath,
                lyricLines = lyric.lines
            )
            val list = playbackState.playlist.toMutableList()
            list[idx] = updated
            playbackState.playlist = list
            if (playbackState.currentTrack?.id == track.id) {
                playbackState.currentTrack = updated
            }
        }
    } catch (_: Exception) { }
}

/** 在 MediaStore Downloads 中查找是否已存在同名缓存文件 */
private suspend fun findExistingDownload(
    context: android.content.Context,
    fileName: String,
): String? = withContext(Dispatchers.IO) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        @Suppress("DEPRECATION")
        val file = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            fileName
        )
        return@withContext if (file.exists()) Uri.fromFile(file).toString() else null
    }
    try {
        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Downloads._ID)
        val selection = "${MediaStore.Downloads.DISPLAY_NAME} = ? AND " +
                "${MediaStore.Downloads.RELATIVE_PATH} = ?"
        val args = arrayOf(fileName, Environment.DIRECTORY_DOWNLOADS + "/EdgeGesture/")
        context.contentResolver.query(collection, projection, selection, args, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(0)
                return@withContext Uri.withAppendedPath(collection, id.toString()).toString()
            }
        }
    } catch (_: Exception) { }
    null
}

/** 更新播放列表中指定曲目的 audioUri（播放中和持久化同步更新） */
private fun updateTrackAudioUri(
    playbackState: MusicPlaybackState,
    trackId: Long,
    audioUri: String,
) {
    val idx = playbackState.playlist.indexOfFirst { it.id == trackId }
    if (idx < 0) return
    val updated = playbackState.playlist[idx].copy(audioUri = audioUri)
    val list = playbackState.playlist.toMutableList()
    list[idx] = updated
    playbackState.playlist = list
    if (playbackState.currentTrack?.id == trackId) {
        playbackState.currentTrack = updated
    }
    playbackState.persistPlaylist()
}