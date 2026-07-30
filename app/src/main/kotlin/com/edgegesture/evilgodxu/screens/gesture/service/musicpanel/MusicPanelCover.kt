package com.edgegesture.evilgodxu.screens.gesture.service.musicpanel

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.isActive
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.edgegesture.evilgodxu.R

@Composable
internal fun CurrentCover(
    track: MusicTrack?,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isActive) {
                rotation.animateTo(
                    targetValue = rotation.value + 360f,
                    animationSpec = tween(
                        durationMillis = 12_000,
                        easing = LinearEasing
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
internal fun AlbumArt(track: MusicTrack?, modifier: Modifier = Modifier) {
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
internal fun TrackInfo(
    playbackState: MusicPlaybackState,
    onClick: () -> Unit = {},
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    val track = playbackState.currentTrack
                    if (track != null) {
                        copyToClipboard(context, "${track.title} - ${track.artist}")
                    }
                }
            )
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
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            modifier = Modifier
                .basicMarquee(iterations = Int.MAX_VALUE)
                .combinedClickable(
                    onClick = {},
                    onLongClick = { copyToClipboard(context, title) }
                )
        )
        Text(
            text = playbackState.currentTrack?.artist ?: "",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.combinedClickable(
                onClick = {},
                onLongClick = {
                    val artist = playbackState.currentTrack?.artist
                    if (!artist.isNullOrBlank()) copyToClipboard(context, artist)
                }
            )
        )
    }
}

@Composable
internal fun TrackInfo(playbackState: MusicPlaybackState) {
    val context = LocalContext.current
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
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            modifier = Modifier
                .basicMarquee(iterations = Int.MAX_VALUE)
                .combinedClickable(
                    onClick = {},
                    onLongClick = { copyToClipboard(context, title) }
                )
        )
        Text(
            text = playbackState.currentTrack?.artist ?: "",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.combinedClickable(
                onClick = {},
                onLongClick = {
                    val artist = playbackState.currentTrack?.artist
                    if (!artist.isNullOrBlank()) copyToClipboard(context, artist)
                }
            )
        )
    }
}