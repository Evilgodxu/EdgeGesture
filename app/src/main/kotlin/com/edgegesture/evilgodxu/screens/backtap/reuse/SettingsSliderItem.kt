package com.edgegesture.evilgodxu.screens.backtap.reuse

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
import androidx.compose.foundation.layout.size
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

// 自定义滑杆，本页面内 ≥2 处复用
@Composable
fun SettingsSliderItem(
    label: String,
    value: Int,
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
                text = "$value",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        val fraction = ((value - 1) / 9f).coerceIn(0f, 1f)
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
                        onValueChange((1 + ratio * 9).roundToInt().coerceIn(1, 10))
                    }
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { change, _ ->
                        change.consume()
                        val widthPx = size.width.toFloat()
                        val ratio = (change.position.x / widthPx).coerceIn(0f, 1f)
                        onValueChange((1 + ratio * 9).roundToInt().coerceIn(1, 10))
                    }
                },
            contentAlignment = Alignment.CenterStart
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val trackY = h / 2f
                val trackH = 6.dp.toPx()
                val thumbR = 9.dp.toPx()
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
