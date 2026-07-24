package com.edgegesture.evilgodxu.screens.gesture.compact.edge_config_area

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.edgegesture.evilgodxu.data.gesture.GestureSettingsState
import com.edgegesture.evilgodxu.screens.gesture.compact.edge_config_area.summary_card.SummaryCard

// 边缘配置 Area — 左/右/底三张摘要卡片
@Composable
fun EdgeConfigArea(
    settings: GestureSettingsState,
    onLeftClick: (() -> Unit)? = null,
    onRightClick: (() -> Unit)? = null,
    onBottomClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.gesture_config_section_title),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, top = 20.dp, bottom = 10.dp)
        )

        val accent = MaterialTheme.colorScheme.primary
        val outline = MaterialTheme.colorScheme.outlineVariant
        val phoneBg = MaterialTheme.colorScheme.surface

        // 左侧
        SummaryCard(
            icon = {
                Canvas(modifier = Modifier.size(width = 26.dp, height = 42.dp)) {
                    val pw = size.width * 0.82f; val ph = size.height * 0.85f; val px = (size.width - pw) / 2f; val py = (size.height - ph) / 2f; val cr = 3.dp.toPx()
                    drawRoundRect(color = phoneBg, topLeft = Offset(px, py), size = Size(pw, ph), cornerRadius = CornerRadius(cr))
                    drawRoundRect(color = outline, topLeft = Offset(px, py), size = Size(pw, ph), cornerRadius = CornerRadius(cr), style = Stroke(2.dp.toPx()))
                    val edgeW = 3.dp.toPx(); val edgeH = ph * 0.5f; val edgeTop = py + ph * 0.25f
                    drawRoundRect(color = accent, topLeft = Offset(px + 1.dp.toPx(), edgeTop), size = Size(edgeW, edgeH), cornerRadius = CornerRadius(1.dp.toPx()))
                }
            },
            title = stringResource(R.string.gesture_config_left),
            onClick = onLeftClick
        )

        // 右侧
        SummaryCard(
            icon = {
                Canvas(modifier = Modifier.size(width = 26.dp, height = 42.dp)) {
                    val pw = size.width * 0.82f; val ph = size.height * 0.85f; val px = (size.width - pw) / 2f; val py = (size.height - ph) / 2f; val cr = 3.dp.toPx()
                    drawRoundRect(color = phoneBg, topLeft = Offset(px, py), size = Size(pw, ph), cornerRadius = CornerRadius(cr))
                    drawRoundRect(color = outline, topLeft = Offset(px, py), size = Size(pw, ph), cornerRadius = CornerRadius(cr), style = Stroke(2.dp.toPx()))
                    val edgeW = 3.dp.toPx(); val edgeH = ph * 0.5f; val edgeTop = py + ph * 0.25f
                    drawRoundRect(color = accent, topLeft = Offset(px + pw - edgeW - 1.dp.toPx(), edgeTop), size = Size(edgeW, edgeH), cornerRadius = CornerRadius(1.dp.toPx()))
                }
            },
            title = stringResource(R.string.gesture_config_right),
            onClick = onRightClick
        )

        // 底部
        SummaryCard(
            icon = {
                Canvas(modifier = Modifier.size(width = 26.dp, height = 42.dp)) {
                    val pw = size.width * 0.82f; val ph = size.height * 0.85f; val px = (size.width - pw) / 2f; val py = (size.height - ph) / 2f; val cr = 3.dp.toPx()
                    drawRoundRect(color = phoneBg, topLeft = Offset(px, py), size = Size(pw, ph), cornerRadius = CornerRadius(cr))
                    drawRoundRect(color = outline, topLeft = Offset(px, py), size = Size(pw, ph), cornerRadius = CornerRadius(cr), style = Stroke(2.dp.toPx()))
                    val edgeH = 3.dp.toPx(); val edgeW = pw * 0.5f; val edgeLeft = px + pw * 0.25f
                    drawRoundRect(color = accent, topLeft = Offset(edgeLeft, py + ph - edgeH - 1.dp.toPx()), size = Size(edgeW, edgeH), cornerRadius = CornerRadius(1.dp.toPx()))
                }
            },
            title = stringResource(R.string.gesture_config_bottom),
            onClick = onBottomClick
        )
    }
}
