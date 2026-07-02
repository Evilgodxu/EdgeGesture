package com.edgegesture.evilgodxu.screens.gesture.service.expandpanel

import android.content.Context
import android.media.AudioManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import com.edgegesture.evilgodxu.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun VerticalSlidersSection() {
    val context = LocalContext.current
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    var brightness by remember { mutableFloatStateOf(getCurrentBrightnessPercent(context)) }

    val maxAlarmVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
    var alarmVolume by remember {
        mutableFloatStateOf(audioManager.getStreamVolume(AudioManager.STREAM_ALARM).toFloat() / maxAlarmVolume)
    }

    val maxRingVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
    var ringVolume by remember {
        mutableFloatStateOf(audioManager.getStreamVolume(AudioManager.STREAM_RING).toFloat() / maxRingVolume)
    }

    val maxMusicVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    var mediaVolume by remember {
        mutableFloatStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxMusicVolume)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        VerticalSliderItem(
            label = stringResource(R.string.expand_panel_brightness),
            icon = Icons.Default.LightMode,
            value = brightness,
            onValueChange = {
                brightness = it
                setBrightness(context, it)
            },
            onValueChangeFinished = {}
        )

        VerticalSliderItem(
            label = stringResource(R.string.expand_panel_alarm_volume),
            icon = Icons.Default.Alarm,
            value = alarmVolume,
            onValueChange = {
                alarmVolume = it
                val newVolume = (it * maxAlarmVolume).toInt()
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, newVolume, 0)
            },
            onValueChangeFinished = {}
        )

        VerticalSliderItem(
            label = stringResource(R.string.expand_panel_ring_volume),
            icon = Icons.Default.Notifications,
            value = ringVolume,
            onValueChange = {
                ringVolume = it
                val newVolume = (it * maxRingVolume).toInt()
                audioManager.setStreamVolume(AudioManager.STREAM_RING, newVolume, 0)
            },
            onValueChangeFinished = {}
        )

        VerticalSliderItem(
            label = stringResource(R.string.expand_panel_media_volume),
            icon = Icons.Default.MusicNote,
            value = mediaVolume,
            onValueChange = {
                mediaVolume = it
                val newVolume = (it * maxMusicVolume).toInt()
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
            },
            onValueChangeFinished = {}
        )
    }
}

@Composable
private fun VerticalSliderItem(
    label: String,
    icon: ImageVector,
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit
) {
    var currentValue by remember { mutableFloatStateOf(value) }
    var isDragging by remember { mutableStateOf(false) }
    var showValueOnClick by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .width(56.dp)
                .height(140.dp)
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = { isDragging = true },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            val delta = -dragAmount / size.height
                            currentValue = (currentValue + delta).coerceIn(0f, 1f)
                            onValueChange(currentValue)
                        },
                        onDragEnd = {
                            isDragging = false
                            onValueChangeFinished()
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            showValueOnClick = true
                            scope.launch {
                                delay(1000)
                                showValueOnClick = false
                            }
                        }
                    )
                },
            contentAlignment = Alignment.BottomCenter
        ) {
            // 背景轨道
            val trackColor = MaterialTheme.colorScheme.surfaceVariant
            val fillColor = MaterialTheme.colorScheme.primary
            val cornerRadius = 12.dp
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(cornerRadius))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cornerRadiusPx = cornerRadius.toPx()
                    // 绘制背景
                    drawRoundRect(
                        color = trackColor,
                        topLeft = Offset.Zero,
                        size = Size(size.width, size.height),
                        cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
                    )
                    // 绘制填充进度
                    val fillHeight = size.height * currentValue
                    if (fillHeight > 0) {
                        drawRect(
                            color = fillColor,
                            topLeft = Offset(0f, size.height - fillHeight),
                            size = Size(size.width, fillHeight)
                        )
                    }
                }
            }

            // 中心点位置对应的颜色（数值文本在中心）
            val centerFillRatio = currentValue.coerceIn(0f, 1f)
            val centerTextColor = if (centerFillRatio > 0.5f) {
                Color.White
            } else {
                MaterialTheme.colorScheme.primary
            }

            // 底部位置对应的颜色（图标在底部，约占总高度的 15%）
            val iconPositionRatio = (currentValue - 0.15f).coerceIn(0f, 1f)
            val iconTintColor = if (iconPositionRatio > 0f) {
                Color.White
            } else {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            }

            // 滑动时或点击后 1000ms 内显示数值
            if (isDragging || showValueOnClick) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${(currentValue * 100).toInt()}",
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = centerTextColor
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .align(Alignment.BottomCenter),
                contentAlignment = Alignment.BottomCenter
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = iconTintColor
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
