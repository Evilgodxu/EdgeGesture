package com.edgegesture.evilgodxu.screens.edge_config.compact.trigger_area.edge_preview

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.edgegesture.evilgodxu.screens.edge_config.EdgeType

// 手机预览职责组件 — Canvas 绘制手机+边缘触发区
@Composable
fun EdgePreview(
    edgeType: EdgeType,
    width: Int,
    heightPercent: Int,
    positionPercent: Int,
    segmentCount: Int,
    selectedSegment: Int,
    modifier: Modifier = Modifier,
) {
    val accentColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val outlineColor = MaterialTheme.colorScheme.outlineVariant
    val highlightColor = Color(0xFFFFD600)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(80.dp, 100.dp)) {
            val pw = size.width; val ph = size.height; val cr = 8.dp.toPx(); val r = 3.dp.toPx()
            val edgeAlpha = 0.7f

            drawRoundRect(color = surfaceColor, topLeft = Offset.Zero, size = Size(pw, ph), cornerRadius = CornerRadius(cr))

            when (edgeType) {
                EdgeType.LEFT -> {
                    val zoneW = (width / 2f).dp.toPx().coerceAtLeast(3.dp.toPx())
                    val zoneH = ph * (heightPercent / 100f)
                    val startY = (ph - zoneH) * (positionPercent / 100f); val clampedY = startY.coerceIn(0f, ph - zoneH)
                    val segH = zoneH / segmentCount
                    val basePath = Path().apply {
                        moveTo(0f, clampedY); lineTo(zoneW - r, clampedY)
                        quadraticTo(zoneW, clampedY, zoneW, clampedY + r); lineTo(zoneW, clampedY + zoneH - r)
                        quadraticTo(zoneW, clampedY + zoneH, zoneW - r, clampedY + zoneH); lineTo(0f, clampedY + zoneH); close()
                    }
                    drawPath(basePath, color = accentColor.copy(alpha = edgeAlpha))
                    if (segmentCount > 1) {
                        for (i in 1 until segmentCount) drawLine(surfaceColor.copy(alpha = 0.7f), Offset(0f, clampedY + segH * i), Offset(zoneW, clampedY + segH * i), 1.dp.toPx())
                        val selTop = clampedY + segH * (selectedSegment - 1)
                        val selPath = Path().apply {
                            moveTo(0f, selTop); lineTo(zoneW - r, selTop)
                            quadraticTo(zoneW, selTop, zoneW, selTop + r); lineTo(zoneW, selTop + segH - r)
                            quadraticTo(zoneW, selTop + segH, zoneW - r, selTop + segH); lineTo(0f, selTop + segH); close()
                        }
                        drawPath(selPath, color = highlightColor.copy(alpha = 0.45f))
                    }
                }
                EdgeType.RIGHT -> {
                    val zoneW = (width / 2f).dp.toPx().coerceAtLeast(3.dp.toPx())
                    val zoneH = ph * (heightPercent / 100f)
                    val startY = (ph - zoneH) * (positionPercent / 100f); val clampedY = startY.coerceIn(0f, ph - zoneH)
                    val segH = zoneH / segmentCount
                    val basePath = Path().apply {
                        moveTo(pw - zoneW + r, clampedY); lineTo(pw, clampedY); lineTo(pw, clampedY + zoneH)
                        lineTo(pw - zoneW + r, clampedY + zoneH)
                        quadraticTo(pw - zoneW, clampedY + zoneH, pw - zoneW, clampedY + zoneH - r)
                        lineTo(pw - zoneW, clampedY + r)
                        quadraticTo(pw - zoneW, clampedY, pw - zoneW + r, clampedY); close()
                    }
                    drawPath(basePath, color = accentColor.copy(alpha = edgeAlpha))
                    if (segmentCount > 1) {
                        for (i in 1 until segmentCount) drawLine(surfaceColor.copy(alpha = 0.7f), Offset(pw - zoneW, clampedY + segH * i), Offset(pw, clampedY + segH * i), 1.dp.toPx())
                        val selTop = clampedY + segH * (selectedSegment - 1)
                        val selPath = Path().apply {
                            moveTo(pw - zoneW + r, selTop); lineTo(pw, selTop); lineTo(pw, selTop + segH)
                            lineTo(pw - zoneW + r, selTop + segH)
                            quadraticTo(pw - zoneW, selTop + segH, pw - zoneW, selTop + segH - r)
                            lineTo(pw - zoneW, selTop + r)
                            quadraticTo(pw - zoneW, selTop, pw - zoneW + r, selTop); close()
                        }
                        drawPath(selPath, color = highlightColor.copy(alpha = 0.45f))
                    }
                }
                EdgeType.BOTTOM -> {
                    val zoneH = (width / 2f).dp.toPx().coerceAtLeast(3.dp.toPx())
                    val zoneW = pw * (heightPercent / 100f); val segW = zoneW / segmentCount; val startX = (pw - zoneW) / 2f
                    val basePath = Path().apply {
                        moveTo(startX + r, ph - zoneH); quadraticTo(startX, ph - zoneH, startX, ph - zoneH + r)
                        lineTo(startX, ph); lineTo(startX + zoneW, ph)
                        lineTo(startX + zoneW, ph - zoneH + r)
                        quadraticTo(startX + zoneW, ph - zoneH, startX + zoneW - r, ph - zoneH); close()
                    }
                    drawPath(basePath, color = accentColor.copy(alpha = edgeAlpha))
                    if (segmentCount > 1) {
                        for (i in 1 until segmentCount) drawLine(surfaceColor.copy(alpha = 0.7f), Offset(startX + segW * i, ph - zoneH), Offset(startX + segW * i, ph), 1.dp.toPx())
                        val selStartX = startX + segW * (selectedSegment - 1)
                        val selPath = Path().apply {
                            moveTo(selStartX + r, ph - zoneH); quadraticTo(selStartX, ph - zoneH, selStartX, ph - zoneH + r)
                            lineTo(selStartX, ph); lineTo(selStartX + segW, ph)
                            lineTo(selStartX + segW, ph - zoneH + r)
                            quadraticTo(selStartX + segW, ph - zoneH, selStartX + segW - r, ph - zoneH); close()
                        }
                        drawPath(selPath, color = highlightColor.copy(alpha = 0.45f))
                    }
                }
            }
            drawRoundRect(color = outlineColor, topLeft = Offset.Zero, size = Size(pw, ph), cornerRadius = CornerRadius(cr), style = Stroke(2.dp.toPx()))
        }
    }
}
