package com.edgegesture.evilgodxu.screens.gesture.compact.preview

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edgegesture.evilgodxu.R

// 手势预览组件 — 手机轮廓 + 边缘触发区域可视化
@Composable
fun GesturePreview(
    leftEdgeWidth: Int,
    leftEdgeHeightPercent: Int,
    leftEdgePositionPercent: Int,
    leftSegmentCount: Int,
    rightEdgeWidth: Int,
    rightEdgeHeightPercent: Int,
    rightEdgePositionPercent: Int,
    rightSegmentCount: Int,
    bottomEdgeHeight: Int,
    bottomEdgeWidthPercent: Int,
    bottomSegmentCount: Int,
    modifier: Modifier = Modifier,
) {
    val accentColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.gesture_preview_title),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, top = 20.dp, bottom = 10.dp)
        )
        Box(
            modifier = Modifier.align(Alignment.CenterHorizontally).size(width = 110.dp, height = 170.dp)
                .border(width = 2.dp, color = MaterialTheme.colorScheme.outlineVariant, shape = RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp)).background(surfaceColor)
        ) {
            Canvas(modifier = Modifier.size(width = 110.dp, height = 170.dp)) {
                val pw = size.width; val ph = size.height; val edgeAlpha = 0.7f
                val lw = ((leftEdgeWidth / 2).dp.toPx()).coerceAtLeast(3.dp.toPx())
                val lh = ph * leftEdgeHeightPercent / 100f; val lt = (ph - lh) * leftEdgePositionPercent / 100f
                drawEdgeRect(left = 0f, top = lt, w = lw, h = lh, color = accentColor, alpha = edgeAlpha, segments = leftSegmentCount, horizontal = false, dividerColor = surfaceColor, roundRight = true)
                val rw = ((rightEdgeWidth / 2).dp.toPx()).coerceAtLeast(3.dp.toPx())
                val rh = ph * rightEdgeHeightPercent / 100f; val rt = (ph - rh) * rightEdgePositionPercent / 100f
                drawEdgeRect(left = pw - rw, top = rt, w = rw, h = rh, color = accentColor, alpha = edgeAlpha, segments = rightSegmentCount, horizontal = false, dividerColor = surfaceColor, roundLeft = true)
                val bh = ((bottomEdgeHeight / 2).dp.toPx()).coerceAtLeast(3.dp.toPx())
                val bw = pw * bottomEdgeWidthPercent / 100f; val bl = (pw - bw) / 2f
                drawEdgeRect(left = bl, top = ph - bh, w = bw, h = bh, color = accentColor, alpha = edgeAlpha, segments = bottomSegmentCount, horizontal = true, dividerColor = surfaceColor, roundTop = true)
            }
        }
    }
}

private fun DrawScope.drawEdgeRect(
    left: Float, top: Float, w: Float, h: Float,
    color: Color, alpha: Float,
    segments: Int, horizontal: Boolean,
    dividerColor: Color,
    roundRight: Boolean = false,
    roundLeft: Boolean = false,
    roundTop: Boolean = false,
) {
    val r = 3.dp.toPx(); val path = Path()
    when {
        roundRight -> { path.moveTo(left, top); path.lineTo(left + w - r, top); path.quadraticTo(left + w, top, left + w, top + r); path.lineTo(left + w, top + h - r); path.quadraticTo(left + w, top + h, left + w - r, top + h); path.lineTo(left, top + h); path.close() }
        roundLeft -> { path.moveTo(left + r, top); path.lineTo(left + w, top); path.lineTo(left + w, top + h); path.lineTo(left + r, top + h); path.quadraticTo(left, top + h, left, top + h - r); path.lineTo(left, top + r); path.quadraticTo(left, top, left + r, top); path.close() }
        roundTop -> { path.moveTo(left + r, top); path.quadraticTo(left, top, left, top + r); path.lineTo(left, top + h); path.lineTo(left + w, top + h); path.lineTo(left + w, top + r); path.quadraticTo(left + w, top, left + w - r, top); path.close() }
        else -> path.addRect(Rect(left, top, left + w, top + h))
    }
    drawPath(path, color = color.copy(alpha = alpha))
    if (segments > 1) {
        val lineColor = dividerColor.copy(alpha = 0.7f); val sw = 1.dp.toPx()
        for (i in 1 until segments) {
            val f = i.toFloat() / segments
            if (horizontal) { val dx = left + w * f; drawLine(lineColor, Offset(dx, top), Offset(dx, top + h), sw) }
            else { val dy = top + h * f; drawLine(lineColor, Offset(left, dy), Offset(left + w, dy), sw) }
        }
    }
}
