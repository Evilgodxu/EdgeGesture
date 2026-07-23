package com.edgegesture.evilgodxu.screens.edge_config.reuse

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

// 自定义滑块，页面内多处复用
@Composable
fun SliderSetting(
    label: String,
    value: Int,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    suffix: String,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "$value$suffix",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        val trackHeight = 6.dp
        val thumbRadius = 9.dp
        val fraction = ((value - range.start) / (range.endInclusive - range.start))
            .coerceIn(0f, 1f)
        val primary = MaterialTheme.colorScheme.primary
        val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
        val surface = MaterialTheme.colorScheme.surface

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val widthPx = size.width.toFloat()
                        val ratio = (offset.x / widthPx).coerceIn(0f, 1f)
                        val newValue = range.start + ratio * (range.endInclusive - range.start)
                        val stepped = if (steps > 0) {
                            val stepSize = (range.endInclusive - range.start) / (steps + 1)
                            Math.round((newValue - range.start) / stepSize) * stepSize + range.start
                        } else newValue
                        onValueChange(stepped.toInt().coerceIn(range.start.toInt(), range.endInclusive.toInt()))
                    }
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { change, _ ->
                        change.consume()
                        val widthPx = size.width.toFloat()
                        val ratio = (change.position.x / widthPx).coerceIn(0f, 1f)
                        val newValue = range.start + ratio * (range.endInclusive - range.start)
                        val stepped = if (steps > 0) {
                            val stepSize = (range.endInclusive - range.start) / (steps + 1)
                            Math.round((newValue - range.start) / stepSize) * stepSize + range.start
                        } else newValue
                        onValueChange(stepped.toInt().coerceIn(range.start.toInt(), range.endInclusive.toInt()))
                    }
                },
            contentAlignment = Alignment.CenterStart
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val trackY = h / 2f
                val trackH = trackHeight.toPx()
                val thumbR = thumbRadius.toPx()
                val thumbX = w * fraction

                drawRoundRect(
                    color = surfaceVariant,
                    topLeft = Offset(0f, trackY - trackH / 2f),
                    size = Size(w, trackH),
                    cornerRadius = CornerRadius(trackH / 2f)
                )
                drawRoundRect(
                    color = primary,
                    topLeft = Offset(0f, trackY - trackH / 2f),
                    size = Size(thumbX, trackH),
                    cornerRadius = CornerRadius(trackH / 2f)
                )
                drawCircle(
                    color = surface,
                    radius = thumbR + 1.5.dp.toPx(),
                    center = Offset(thumbX, trackY)
                )
                drawCircle(
                    color = primary,
                    radius = thumbR,
                    center = Offset(thumbX, trackY)
                )
            }
        }
    }
}
