package com.edgegesture.evilgodxu.screens.gesture.compact.status_area.service_status_card.gesture_wave_icon

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

// 手势波浪图标职责组件
@Composable
fun GestureWaveIcon(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Canvas(modifier = modifier.size(20.dp)) {
        val w = size.width; val h = size.height; val strokeWidth = 1.8.dp.toPx(); val halfH = h / 2f

        drawCircle(color = tint, radius = 2.dp.toPx(), center = Offset(5.dp.toPx(), halfH))

        val path = Path().apply {
            val startX = 9.dp.toPx(); val cp1x = startX + 2.dp.toPx(); val cp2x = startX + 6.dp.toPx()
            val endX = startX + 8.dp.toPx(); val amp = 6.dp.toPx()
            moveTo(startX, halfH)
            cubicTo(cp1x, halfH - amp, cp2x, halfH - amp, endX, halfH)
            cubicTo(endX + 6.dp.toPx(), halfH + amp, endX + 6.dp.toPx(), halfH + amp, endX + 8.dp.toPx(), halfH)
        }
        drawPath(path = path, color = tint, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
    }
}
