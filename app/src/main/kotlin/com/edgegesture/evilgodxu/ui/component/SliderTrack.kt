package com.edgegesture.evilgodxu.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

// 自定义滑动条：只处理 0..1 的位置与手势，数值换算交给调用方。
// 各页面共用同一视觉与交互，避免每处重复画轨道与滑块
@Composable
fun SliderTrack(
    fraction: Float,
    onFractionChange: (Float) -> Unit,
    onFractionChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val surface = MaterialTheme.colorScheme.surface
    val trackHeight = 6.dp
    val thumbRadius = 9.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onFractionChange((offset.x / size.width.toFloat()).coerceIn(0f, 1f))
                    onFractionChangeFinished()
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = onFractionChangeFinished,
                    onDragCancel = onFractionChangeFinished,
                ) { change, _ ->
                    change.consume()
                    onFractionChange((change.position.x / size.width.toFloat()).coerceIn(0f, 1f))
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val trackY = h / 2f
            val trackH = trackHeight.toPx()
            val thumbR = thumbRadius.toPx()
            val thumbX = w * fraction.coerceIn(0f, 1f)

            drawRoundRect(
                color = surfaceVariant,
                topLeft = Offset(0f, trackY - trackH / 2f),
                size = Size(w, trackH),
                cornerRadius = CornerRadius(trackH / 2f),
            )
            drawRoundRect(
                color = primary,
                topLeft = Offset(0f, trackY - trackH / 2f),
                size = Size(thumbX, trackH),
                cornerRadius = CornerRadius(trackH / 2f),
            )
            drawCircle(
                color = surface,
                radius = thumbR + 1.5.dp.toPx(),
                center = Offset(thumbX, trackY),
            )
            drawCircle(color = primary, radius = thumbR, center = Offset(thumbX, trackY))
        }
    }
}
