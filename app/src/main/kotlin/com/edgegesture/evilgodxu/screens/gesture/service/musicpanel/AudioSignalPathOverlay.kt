package com.edgegesture.evilgodxu.screens.gesture.service.musicpanel

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AudioSignalPathOverlay(
    visible: Boolean,
    playbackState: MusicPlaybackState,
    onDismiss: () -> Unit,
) {
    AnimatedContent(
        targetState = visible,
        transitionSpec = {
            (slideInVertically { it } + fadeIn()).togetherWith(
                slideOutVertically { it } + fadeOut()
            )
        },
        label = "播放链路",
    ) { show ->
        if (show) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss,
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "播放链路",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    )
                    HeaderIconButton(
                        icon = Icons.Default.Close,
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp),
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                AudioSignalPathRows(playbackState)
            }
        } else {
            androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun AudioSignalPathRows(playbackState: MusicPlaybackState) {
    val format = playbackState.audioSignalPathFormat
    val rows = listOf(
        "音频格式" to (format?.format ?: "-") .replace("audio/", ""),
        "源采样率" to (format?.sampleRate?.let(::formatAudioRate) ?: "-"),
        "输出采样率" to (format?.outputRate?.let(::formatAudioRate) ?: "-"),
        "位深" to (format?.bitDepth?.let { "$it 位" } ?: "-"),
        "声道" to (format?.channels?.let(::formatChannels) ?: "-"),
        "播放引擎" to playbackState.audioSignalPathEngine.replace("Media3 / ExoPlayer", "Media3 / ExoPlayer"),
        "输出策略" to playbackState.audioSignalPathStrategy.toChineseAudioPathValue(),
        "输出设备" to playbackState.audioSignalPathOutputDevice,
        "音量控制" to playbackState.audioSignalPathVolume.toChineseAudioPathValue(),
        "音频路由" to playbackState.audioSignalPathRoute.toChineseAudioPathValue(),
        "重采样" to playbackState.audioSignalPathResampler.toChineseAudioPathValue(),
        "音频直通" to playbackState.audioSignalPathPassthrough.toChineseAudioPathValue(),
        "USB 状态" to playbackState.audioSignalPathUsb.toChineseAudioPathValue(),
        "验证 / 回退" to playbackState.audioSignalPathVerification.toChineseAudioPathValue(),
        "DSD 模式" to playbackState.audioSignalPathDsdMode,
    )

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        items(rows.size) { index ->
            val (label, value) = rows[index]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (index % 2 == 0) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        else androidx.compose.ui.graphics.Color.Transparent,
                        RoundedCornerShape(8.dp),
                    )
                    .padding(horizontal = 8.dp, vertical = 8.dp),
            ) {
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = value,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

private fun String.toChineseAudioPathValue(): String = when (this) {
    "Direct" -> "直出"
    "Mixer" -> "混音"
    "System" -> "系统"
    "Connected · Direct" -> "已连接 · 直出"
    "Not active" -> "未启用"
    "Inactive" -> "未启用"
    "Unknown" -> "未知"
    "Verified" -> "已验证"
    "Fallback" -> "已回退"
    else -> this
}

private fun formatAudioRate(rate: Int): String = if (rate >= 1000) {
    "${rate / 1000.0} kHz"
} else {
    "$rate Hz"
}

private fun formatChannels(channels: Int): String = when (channels) {
    1 -> "单声道"
    2 -> "立体声"
    else -> "$channels 声道"
}
