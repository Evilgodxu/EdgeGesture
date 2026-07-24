package com.edgegesture.evilgodxu.screens.gesture.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edgegesture.evilgodxu.R

/**
 * 触发区域预览组件 - 匹配新版 UI 设计
 *
 * 展示手机轮廓 + 左/右/底边缘手势触发区域的可视化预览
 * 使用 Canvas 绘制确保精确位置和外观
 */
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
    onLeftEdgeClick: (() -> Unit)? = null,
    onRightEdgeClick: (() -> Unit)? = null,
    onBottomEdgeClick: (() -> Unit)? = null
) {
    val accentColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val outlineColor = MaterialTheme.colorScheme.outlineVariant

    Column(modifier = modifier.fillMaxWidth()) {
        // 区域标题
        Text(
            text = stringResource(R.string.gesture_preview_title),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, top = 20.dp, bottom = 10.dp)
        )

        // 手机轮廓（边框由 Canvas 在最上层绘制，确保边缘区域被边框覆盖）
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(width = 110.dp, height = 170.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(surfaceColor)
        ) {
            // 使用 Canvas 精确绘制边缘触发区和分段线
            Canvas(modifier = Modifier.size(width = 110.dp, height = 170.dp)) {
                    val pw = size.width
                    val ph = size.height
                    val edgeAlpha = 0.7f
                    val cr = 16.dp.toPx()

                    // 1. 手机背景（填充色）
                    drawRoundRect(color = surfaceColor, topLeft = Offset.Zero, size = Size(pw, ph), cornerRadius = CornerRadius(cr))

                    // 2. 边缘触发区域
                    // ====== 左侧边缘（宽度减半以消除重叠） ======
                    val lw = ((leftEdgeWidth / 2).dp.toPx()).coerceAtLeast(3.dp.toPx())
                    val lh = ph * leftEdgeHeightPercent / 100f
                    val lt = (ph - lh) * leftEdgePositionPercent / 100f
                    drawEdgeRect(
                        left = 0f, top = lt, w = lw, h = lh,
                        color = accentColor, alpha = edgeAlpha,
                        segments = leftSegmentCount, horizontal = false,
                        dividerColor = surfaceColor,
                        roundRight = true
                    )

                    // ====== 右侧边缘（宽度减半以消除重叠） ======
                    val rw = ((rightEdgeWidth / 2).dp.toPx()).coerceAtLeast(3.dp.toPx())
                    val rh = ph * rightEdgeHeightPercent / 100f
                    val rt = (ph - rh) * rightEdgePositionPercent / 100f
                    drawEdgeRect(
                        left = pw - rw, top = rt, w = rw, h = rh,
                        color = accentColor, alpha = edgeAlpha,
                        segments = rightSegmentCount, horizontal = false,
                        dividerColor = surfaceColor,
                        roundLeft = true
                    )

                    // ====== 底部边缘（高度减半以消除重叠） ======
                    val bh = ((bottomEdgeHeight / 2).dp.toPx()).coerceAtLeast(3.dp.toPx())
                    val bw = pw * bottomEdgeWidthPercent / 100f
                    val bl = (pw - bw) / 2f
                    drawEdgeRect(
                        left = bl, top = ph - bh, w = bw, h = bh,
                        color = accentColor, alpha = edgeAlpha,
                        segments = bottomSegmentCount, horizontal = true,
                        dividerColor = surfaceColor,
                        roundTop = true
                    )

                    // 3. 手机边框（在最上层绘制，边缘触发区域被边框覆盖在屏幕内）
                    drawRoundRect(color = outlineColor, topLeft = Offset.Zero, size = Size(pw, ph), cornerRadius = CornerRadius(cr), style = Stroke(3.dp.toPx()))
                }
            }
        }
    }

/** 在 Canvas 上绘制一个边缘触发区域矩形 + 分段线 */
private fun DrawScope.drawEdgeRect(
    left: Float, top: Float, w: Float, h: Float,
    color: Color, alpha: Float,
    segments: Int, horizontal: Boolean,
    dividerColor: Color,
    roundRight: Boolean = false,
    roundLeft: Boolean = false,
    roundTop: Boolean = false
) {
    val r = 3.dp.toPx()
    val path = Path()

    if (roundRight) {
        path.moveTo(left, top)
        path.lineTo(left + w - r, top)
        path.quadraticTo(left + w, top, left + w, top + r)
        path.lineTo(left + w, top + h - r)
        path.quadraticTo(left + w, top + h, left + w - r, top + h)
        path.lineTo(left, top + h)
        path.close()
    } else if (roundLeft) {
        path.moveTo(left + r, top)
        path.lineTo(left + w, top)
        path.lineTo(left + w, top + h)
        path.lineTo(left + r, top + h)
        path.quadraticTo(left, top + h, left, top + h - r)
        path.lineTo(left, top + r)
        path.quadraticTo(left, top, left + r, top)
        path.close()
    } else if (roundTop) {
        path.moveTo(left + r, top)
        path.quadraticTo(left, top, left, top + r)
        path.lineTo(left, top + h)
        path.lineTo(left + w, top + h)
        path.lineTo(left + w, top + r)
        path.quadraticTo(left + w, top, left + w - r, top)
        path.close()
    } else {
        path.addRect(Rect(left, top, left + w, top + h))
    }

    drawPath(path, color = color.copy(alpha = alpha))

    // 分段指示线
    if (segments > 1) {
        val lineColor = dividerColor.copy(alpha = 0.7f)
        val sw = 1.dp.toPx()
        for (i in 1 until segments) {
            val f = i.toFloat() / segments
            if (horizontal) {
                val dx = left + w * f
                drawLine(lineColor, Offset(dx, top), Offset(dx, top + h), sw)
            } else {
                val dy = top + h * f
                drawLine(lineColor, Offset(left, dy), Offset(left + w, dy), sw)
            }
        }
    }
}
