package com.edgegesture.evilgodxu.screens.gesture.service.musicpanel

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.edgegesture.evilgodxu.R

@Composable
internal fun HeaderRow(
    playbackState: MusicPlaybackState,
    timerRemaining: Int,
    onTimerClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentTrackId = playbackState.currentTrack?.id
    val isLiked = currentTrackId?.let { id -> playbackState.likedIds.contains(id) } ?: false
    val processMemoryMb by rememberProcessMemoryMb()

    Box(
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            HeaderIconButton(
                icon = Icons.Default.Timer,
                contentDescription = stringResource(R.string.music_panel_timer_title),
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
            modifier = Modifier
                .align(Alignment.Center)
                .alpha(0.72f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Memory,
                contentDescription = stringResource(R.string.music_panel_memory_usage),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(15.dp),
            )
            Text(
                text = stringResource(R.string.music_panel_memory_usage_value, processMemoryMb),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                maxLines = 1,
            )
        }

        HeaderIconButton(
            icon = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = stringResource(R.string.music_panel_favorite),
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

// 定期刷新本应用进程的内存占用，供头部状态区展示
@Composable
private fun rememberProcessMemoryMb(): State<Int> = produceState(initialValue = processMemoryMb()) {
    while (true) {
        value = processMemoryMb()
        delay(2000)
    }
}

private fun processMemoryMb(): Int {
    val runtime = Runtime.getRuntime()
    return ((runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)).toInt()
}

@Composable
internal fun HeaderIconButton(
    icon: ImageVector,
    contentDescription: String? = null,
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
            contentDescription = contentDescription,
            modifier = Modifier.size(21.dp),
            tint = if (enabled) tint else tint.copy(alpha = 0.3f)
        )
    }
}
