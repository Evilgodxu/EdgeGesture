package com.edgegesture.evilgodxu.screens.gesture.component.config

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edgegesture.evilgodxu.R

// 手势配置区块：左/右/底部三个边缘的配置入口
@Composable
fun GestureSummarySection(
    onLeftClick: () -> Unit,
    onRightClick: () -> Unit,
    onBottomClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.gesture_config_section_title),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, top = 20.dp, bottom = 10.dp),
        )

        SummaryCard(
            icon = { PhoneEdgeIcon(EdgeZone.LEFT) },
            title = stringResource(R.string.gesture_config_left),
            onClick = onLeftClick,
        )
        SummaryCard(
            icon = { PhoneEdgeIcon(EdgeZone.RIGHT) },
            title = stringResource(R.string.gesture_config_right),
            onClick = onRightClick,
        )
        SummaryCard(
            icon = { PhoneEdgeIcon(EdgeZone.BOTTOM) },
            title = stringResource(R.string.gesture_config_bottom),
            onClick = onBottomClick,
        )
    }
}

// 边缘位置示意：手机轮廓 + 高亮触发区
private enum class EdgeZone { LEFT, RIGHT, BOTTOM }

@Composable
private fun PhoneEdgeIcon(zone: EdgeZone) {
    val accent = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outlineVariant
    val phoneBg = MaterialTheme.colorScheme.surface
    Canvas(modifier = Modifier.size(width = 26.dp, height = 42.dp)) {
        val pw = size.width * 0.82f
        val ph = size.height * 0.85f
        val px = (size.width - pw) / 2f
        val py = (size.height - ph) / 2f
        val cr = 3.dp.toPx()
        drawRoundRect(
            color = phoneBg,
            topLeft = Offset(px, py),
            size = Size(pw, ph),
            cornerRadius = CornerRadius(cr),
        )
        drawRoundRect(
            color = outline,
            topLeft = Offset(px, py),
            size = Size(pw, ph),
            cornerRadius = CornerRadius(cr),
            style = Stroke(2.dp.toPx()),
        )
        when (zone) {
            EdgeZone.LEFT -> {
                val edgeW = 3.dp.toPx()
                val edgeH = ph * 0.5f
                drawRoundRect(
                    color = accent,
                    topLeft = Offset(px + 1.dp.toPx(), py + ph * 0.25f),
                    size = Size(edgeW, edgeH),
                    cornerRadius = CornerRadius(1.dp.toPx()),
                )
            }

            EdgeZone.RIGHT -> {
                val edgeW = 3.dp.toPx()
                val edgeH = ph * 0.5f
                drawRoundRect(
                    color = accent,
                    topLeft = Offset(px + pw - edgeW - 1.dp.toPx(), py + ph * 0.25f),
                    size = Size(edgeW, edgeH),
                    cornerRadius = CornerRadius(1.dp.toPx()),
                )
            }

            EdgeZone.BOTTOM -> {
                val edgeH = 3.dp.toPx()
                val edgeW = pw * 0.5f
                drawRoundRect(
                    color = accent,
                    topLeft = Offset(px + pw * 0.25f, py + ph - edgeH - 1.dp.toPx()),
                    size = Size(edgeW, edgeH),
                    cornerRadius = CornerRadius(1.dp.toPx()),
                )
            }
        }
    }
}

@Composable
private fun SummaryCard(
    icon: @Composable () -> Unit,
    title: String,
    onClick: (() -> Unit)? = null,
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 4.dp)
                .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    icon()
                }

                Text(
                    text = title,
                    modifier = Modifier.weight(1f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
