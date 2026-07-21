package com.edgegesture.evilgodxu.screens.gesture

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edgegesture.evilgodxu.R
import com.edgegesture.evilgodxu.data.gesture.GestureAction
import com.edgegesture.evilgodxu.data.gesture.GestureSettingsKeys
import com.edgegesture.evilgodxu.data.gesture.gestureSettingsFlow
import com.edgegesture.evilgodxu.data.gesture.saveBottomEdgeGesture
import com.edgegesture.evilgodxu.data.gesture.saveBottomEdgeHeight
import com.edgegesture.evilgodxu.data.gesture.saveBottomEdgeWidthPercent
import com.edgegesture.evilgodxu.data.gesture.saveBottomSegmentCount
import com.edgegesture.evilgodxu.data.gesture.saveLeftEdgeGesture
import com.edgegesture.evilgodxu.data.gesture.saveLeftEdgeHeightPercent
import com.edgegesture.evilgodxu.data.gesture.saveLeftEdgePositionPercent
import com.edgegesture.evilgodxu.data.gesture.saveLeftEdgeWidth
import com.edgegesture.evilgodxu.data.gesture.saveLeftSegmentCount
import com.edgegesture.evilgodxu.data.gesture.saveRightEdgeGesture
import com.edgegesture.evilgodxu.data.gesture.saveRightEdgeHeightPercent
import com.edgegesture.evilgodxu.data.gesture.saveRightEdgePositionPercent
import com.edgegesture.evilgodxu.data.gesture.saveRightEdgeWidth
import com.edgegesture.evilgodxu.data.gesture.saveRightSegmentCount
import com.edgegesture.evilgodxu.data.gesture.saveBottomSegmentCount as saveBottomSegmentCount1
import com.edgegesture.evilgodxu.screens.gesture.components.ActionSelectionDialog
import com.edgegesture.evilgodxu.screens.gesture.components.getActionDisplayName
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class EdgeType { LEFT, RIGHT, BOTTOM }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EdgeGestureConfigScreen(
    edgeType: EdgeType,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by context.gestureSettingsFlow().collectAsState(initial = null)

    var selectedSegment by remember { mutableIntStateOf(1) }
    var showActionDialog by remember { mutableStateOf(false) }
    var currentActionKey by remember { mutableStateOf<androidx.datastore.preferences.core.Preferences.Key<String>?>(null) }

    val title = when (edgeType) {
        EdgeType.LEFT -> stringResource(R.string.gesture_config_left)
        EdgeType.RIGHT -> stringResource(R.string.gesture_config_right)
        EdgeType.BOTTOM -> stringResource(R.string.gesture_config_bottom)
    }

    val currentSettings = settings

    // 当前边缘的本地状态，滑块变化时立即更新预览
    val initW = when (edgeType) {
        EdgeType.LEFT -> currentSettings?.leftEdgeWidth ?: 20
        EdgeType.RIGHT -> currentSettings?.rightEdgeWidth ?: 20
        EdgeType.BOTTOM -> currentSettings?.bottomEdgeHeight ?: 20
    }
    val initH = when (edgeType) {
        EdgeType.LEFT -> currentSettings?.leftEdgeHeightPercent ?: 60
        EdgeType.RIGHT -> currentSettings?.rightEdgeHeightPercent ?: 60
        EdgeType.BOTTOM -> currentSettings?.bottomEdgeWidthPercent ?: 80
    }
    val initP = when (edgeType) {
        EdgeType.LEFT -> currentSettings?.leftEdgePositionPercent ?: 90
        EdgeType.RIGHT -> currentSettings?.rightEdgePositionPercent ?: 90
        else -> 0
    }
    var localWidth by remember(currentSettings, edgeType) { mutableIntStateOf(initW) }
    var localHeightPercent by remember(currentSettings, edgeType) { mutableIntStateOf(initH) }
    var localPositionPercent by remember(currentSettings, edgeType) { mutableIntStateOf(initP) }

    val maxSegments = when (edgeType) {
        EdgeType.LEFT -> currentSettings?.leftSegmentCount ?: 1
        EdgeType.RIGHT -> currentSettings?.rightSegmentCount ?: 1
        EdgeType.BOTTOM -> currentSettings?.bottomSegmentCount ?: 1
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = title, fontWeight = FontWeight.SemiBold)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.nav_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // 触发区域标题（左上角小字）
            Text(
                text = stringResource(R.string.gesture_config_section_title),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.2.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
            )

            // 触发区域设置卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // 手机预览
                    EdgePreview(
                        edgeType = edgeType,
                        width = localWidth,
                        heightPercent = localHeightPercent,
                        positionPercent = localPositionPercent,
                        segmentCount = maxSegments,
                        selectedSegment = selectedSegment,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (edgeType == EdgeType.BOTTOM) {
                        BottomDimensionSliders(
                            height = localWidth,
                            widthPercent = localHeightPercent,
                            onHeightChange = { v -> localWidth = v; scope.launch { context.saveBottomEdgeHeight(v) } },
                            onWidthPercentChange = { v -> localHeightPercent = v; scope.launch { context.saveBottomEdgeWidthPercent(v) } }
                        )
                    } else {
                        EdgeDimensionSliders(
                            width = localWidth,
                            heightPercent = localHeightPercent,
                            positionPercent = localPositionPercent,
                            onWidthChange = { v ->
                                localWidth = v
                                scope.launch {
                                    if (edgeType == EdgeType.LEFT) context.saveLeftEdgeWidth(v)
                                    else context.saveRightEdgeWidth(v)
                                }
                            },
                            onHeightPercentChange = { v ->
                                localHeightPercent = v
                                scope.launch {
                                    if (edgeType == EdgeType.LEFT) context.saveLeftEdgeHeightPercent(v)
                                    else context.saveRightEdgeHeightPercent(v)
                                }
                            },
                            onPositionPercentChange = { v ->
                                localPositionPercent = v
                                scope.launch {
                                    if (edgeType == EdgeType.LEFT) context.saveLeftEdgePositionPercent(v)
                                    else context.saveRightEdgePositionPercent(v)
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 分段数量
                    SegmentCountSlider(
                        segmentCount = maxSegments,
                        onSegmentCountChange = { count ->
                            scope.launch {
                                when (edgeType) {
                                    EdgeType.LEFT -> context.saveLeftSegmentCount(count)
                                    EdgeType.RIGHT -> context.saveRightSegmentCount(count)
                                    EdgeType.BOTTOM -> context.saveBottomSegmentCount1(count)
                                }
                            }
                            if (selectedSegment > count) selectedSegment = count
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 手势操作区域标题
            Text(
                text = stringResource(R.string.gesture_operation_section_title),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.2.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
            )

            // 段标签（设计风格）
            if (maxSegments > 1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    (1..maxSegments).forEach { segment ->
                        val isSelected = selectedSegment == segment
                        FilledTonalButton(
                            onClick = { selectedSegment = segment },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (isSelected)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isSelected)
                                    MaterialTheme.colorScheme.onPrimary
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Text(
                                text = stringResource(R.string.gesture_segment_label, segment),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 手势动作列表卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val gestureLabels = getGestureLabels(edgeType)
                    val gestures = getGestureActions(edgeType, selectedSegment, currentSettings)
                    gestures.forEachIndexed { index, (action, key) ->
                        val rot = when {
                            gestureLabels[index].contains(stringResource(R.string.gesture_swipe_right)) -> 0f
                            gestureLabels[index].contains(stringResource(R.string.gesture_swipe_left)) -> 180f
                            gestureLabels[index].contains(stringResource(R.string.gesture_swipe_up)) -> -90f
                            gestureLabels[index].contains(stringResource(R.string.gesture_swipe_down)) -> 90f
                            else -> 0f
                        }
                        GestureActionRow(
                            label = gestureLabels[index],
                            actionName = getActionDisplayName(action),
                            isLongPress = index % 2 == 1,
                            iconRotation = rot,
                            onClick = {
                                currentActionKey = key
                                showActionDialog = true
                            }
                        )
                        if (index < gestures.lastIndex) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // 动作选择对话框
    if (showActionDialog && currentActionKey != null && currentSettings != null) {
        ActionSelectionDialog(
            currentAction = getCurrentActionForDialog(edgeType, selectedSegment, currentSettings, currentActionKey),
            onDismiss = { showActionDialog = false },
            onActionSelected = { action ->
                scope.launch {
                    when (edgeType) {
                        EdgeType.LEFT -> context.saveLeftEdgeGesture(currentActionKey!!, action)
                        EdgeType.RIGHT -> context.saveRightEdgeGesture(currentActionKey!!, action)
                        EdgeType.BOTTOM -> context.saveBottomEdgeGesture(currentActionKey!!, action)
                    }
                }
                showActionDialog = false
            },
            getActionDisplayName = { getActionDisplayName(it) }
        )
    }
}

@Composable
private fun EdgePreview(
    edgeType: EdgeType,
    width: Int,
    heightPercent: Int,
    positionPercent: Int,
    segmentCount: Int,
    selectedSegment: Int,
    modifier: Modifier = Modifier
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
            val pw = size.width
            val ph = size.height
            val cr = 8.dp.toPx()
            val r = 3.dp.toPx()
            val edgeAlpha = 0.7f

            // 1. 手机主体背景
            drawRoundRect(color = surfaceColor, topLeft = Offset.Zero, size = Size(pw, ph), cornerRadius = CornerRadius(cr))

            // 2. 边缘区域（复用 GesturePreview 的 drawEdgeRect 逻辑）
            when (edgeType) {
                EdgeType.LEFT -> {
                    val zoneW = (width / 2f).dp.toPx().coerceAtLeast(3.dp.toPx())
                    val zoneH = ph * (heightPercent / 100f)
                    val startY = (ph - zoneH) * (positionPercent / 100f)
                    val clampedY = startY.coerceIn(0f, ph - zoneH)
                    val segH = zoneH / segmentCount

                    // 整体绘制：右侧圆角
                    val basePath = Path().apply {
                        moveTo(0f, clampedY)
                        lineTo(zoneW - r, clampedY)
                        quadraticTo(zoneW, clampedY, zoneW, clampedY + r)
                        lineTo(zoneW, clampedY + zoneH - r)
                        quadraticTo(zoneW, clampedY + zoneH, zoneW - r, clampedY + zoneH)
                        lineTo(0f, clampedY + zoneH)
                        close()
                    }
                    drawPath(basePath, color = accentColor.copy(alpha = edgeAlpha))

                    // 分段指示线
                    if (segmentCount > 1) {
                        for (i in 1 until segmentCount) {
                            val dy = clampedY + segH * i
                            drawLine(surfaceColor.copy(alpha = 0.7f), Offset(0f, dy), Offset(zoneW, dy), 1.dp.toPx())
                        }
                        // 选中段黄色高亮
                        val selTop = clampedY + segH * (selectedSegment - 1)
                        val selPath = Path().apply {
                            moveTo(0f, selTop)
                            lineTo(zoneW - r, selTop)
                            quadraticTo(zoneW, selTop, zoneW, selTop + r)
                            lineTo(zoneW, selTop + segH - r)
                            quadraticTo(zoneW, selTop + segH, zoneW - r, selTop + segH)
                            lineTo(0f, selTop + segH)
                            close()
                        }
                        drawPath(selPath, color = highlightColor.copy(alpha = 0.45f))
                    }
                }
                EdgeType.RIGHT -> {
                    val zoneW = (width / 2f).dp.toPx().coerceAtLeast(3.dp.toPx())
                    val zoneH = ph * (heightPercent / 100f)
                    val startY = (ph - zoneH) * (positionPercent / 100f)
                    val clampedY = startY.coerceIn(0f, ph - zoneH)
                    val segH = zoneH / segmentCount

                    // 整体绘制：左侧圆角
                    val basePath = Path().apply {
                        moveTo(pw - zoneW + r, clampedY)
                        lineTo(pw, clampedY)
                        lineTo(pw, clampedY + zoneH)
                        lineTo(pw - zoneW + r, clampedY + zoneH)
                        quadraticTo(pw - zoneW, clampedY + zoneH, pw - zoneW, clampedY + zoneH - r)
                        lineTo(pw - zoneW, clampedY + r)
                        quadraticTo(pw - zoneW, clampedY, pw - zoneW + r, clampedY)
                        close()
                    }
                    drawPath(basePath, color = accentColor.copy(alpha = edgeAlpha))

                    // 分段指示线
                    if (segmentCount > 1) {
                        for (i in 1 until segmentCount) {
                            val dy = clampedY + segH * i
                            drawLine(surfaceColor.copy(alpha = 0.7f), Offset(pw - zoneW, dy), Offset(pw, dy), 1.dp.toPx())
                        }
                        // 选中段黄色高亮
                        val selTop = clampedY + segH * (selectedSegment - 1)
                        val selPath = Path().apply {
                            moveTo(pw - zoneW + r, selTop)
                            lineTo(pw, selTop)
                            lineTo(pw, selTop + segH)
                            lineTo(pw - zoneW + r, selTop + segH)
                            quadraticTo(pw - zoneW, selTop + segH, pw - zoneW, selTop + segH - r)
                            lineTo(pw - zoneW, selTop + r)
                            quadraticTo(pw - zoneW, selTop, pw - zoneW + r, selTop)
                            close()
                        }
                        drawPath(selPath, color = highlightColor.copy(alpha = 0.45f))
                    }
                }
                EdgeType.BOTTOM -> {
                    val zoneH = (width / 2f).dp.toPx().coerceAtLeast(3.dp.toPx())
                    val zoneW = pw * (heightPercent / 100f)
                    val segW = zoneW / segmentCount
                    val startX = (pw - zoneW) / 2f

                    // 整体绘制：顶部圆角
                    val basePath = Path().apply {
                        moveTo(startX + r, ph - zoneH)
                        quadraticTo(startX, ph - zoneH, startX, ph - zoneH + r)
                        lineTo(startX, ph)
                        lineTo(startX + zoneW, ph)
                        lineTo(startX + zoneW, ph - zoneH + r)
                        quadraticTo(startX + zoneW, ph - zoneH, startX + zoneW - r, ph - zoneH)
                        close()
                    }
                    drawPath(basePath, color = accentColor.copy(alpha = edgeAlpha))

                    // 分段指示线
                    if (segmentCount > 1) {
                        for (i in 1 until segmentCount) {
                            val dx = startX + segW * i
                            drawLine(surfaceColor.copy(alpha = 0.7f), Offset(dx, ph - zoneH), Offset(dx, ph), 1.dp.toPx())
                        }
                        // 选中段黄色高亮
                        val selStartX = startX + segW * (selectedSegment - 1)
                        val selPath = Path().apply {
                            moveTo(selStartX + r, ph - zoneH)
                            quadraticTo(selStartX, ph - zoneH, selStartX, ph - zoneH + r)
                            lineTo(selStartX, ph)
                            lineTo(selStartX + segW, ph)
                            lineTo(selStartX + segW, ph - zoneH + r)
                            quadraticTo(selStartX + segW, ph - zoneH, selStartX + segW - r, ph - zoneH)
                            close()
                        }
                        drawPath(selPath, color = highlightColor.copy(alpha = 0.45f))
                    }
                }
            }

            // 3. 手机边框（在最上层，形成边缘覆盖在边框上的视觉效果）
            drawRoundRect(color = outlineColor, topLeft = Offset.Zero, size = Size(pw, ph), cornerRadius = CornerRadius(cr), style = Stroke(2.dp.toPx()))
        }
    }
}

@Composable
private fun EdgeDimensionSliders(
    width: Int,
    heightPercent: Int,
    positionPercent: Int,
    onWidthChange: (Int) -> Unit,
    onHeightPercentChange: (Int) -> Unit,
    onPositionPercentChange: (Int) -> Unit
) {
    SliderSetting(
        label = stringResource(R.string.edge_width),
        value = width,
        range = 10f..50f,
        steps = 39,
        suffix = "dp",
        onValueChange = onWidthChange
    )
    Spacer(modifier = Modifier.height(12.dp))
    SliderSetting(
        label = stringResource(R.string.edge_height_percent),
        value = heightPercent,
        range = 20f..100f,
        steps = 79,
        suffix = "%",
        onValueChange = onHeightPercentChange
    )
    Spacer(modifier = Modifier.height(12.dp))
    SliderSetting(
        label = stringResource(R.string.edge_position_percent),
        value = positionPercent,
        range = 0f..100f,
        steps = 99,
        suffix = "%",
        onValueChange = onPositionPercentChange
    )
}

@Composable
private fun BottomDimensionSliders(
    height: Int,
    widthPercent: Int,
    onHeightChange: (Int) -> Unit,
    onWidthPercentChange: (Int) -> Unit
) {
    SliderSetting(
        label = stringResource(R.string.edge_height),
        value = height,
        range = 10f..50f,
        steps = 39,
        suffix = "dp",
        onValueChange = onHeightChange
    )
    Spacer(modifier = Modifier.height(12.dp))
    SliderSetting(
        label = stringResource(R.string.edge_width_percent),
        value = widthPercent,
        range = 20f..100f,
        steps = 79,
        suffix = "%",
        onValueChange = onWidthPercentChange
    )
}

@Composable
private fun SliderSetting(
    label: String,
    value: Int,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    suffix: String,
    onValueChange: (Int) -> Unit
) {
    val density = LocalDensity.current

    Column(modifier = Modifier.fillMaxWidth()) {
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

        // 设计风格滑动条
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
                        } else {
                            newValue
                        }
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
                        } else {
                            newValue
                        }
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

                // 轨道背景
                drawRoundRect(
                    color = surfaceVariant,
                    topLeft = Offset(0f, trackY - trackH / 2f),
                    size = Size(w, trackH),
                    cornerRadius = CornerRadius(trackH / 2f)
                )

                // 轨道填充
                drawRoundRect(
                    color = primary,
                    topLeft = Offset(0f, trackY - trackH / 2f),
                    size = Size(thumbX, trackH),
                    cornerRadius = CornerRadius(trackH / 2f)
                )

                // 滑块
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

@Composable
private fun SegmentCountSlider(
    segmentCount: Int,
    onSegmentCountChange: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.edge_segment_count),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${segmentCount}${stringResource(R.string.edge_segment_unit)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        val fraction = ((segmentCount - 1) / 2f).coerceIn(0f, 1f)
        val primary = MaterialTheme.colorScheme.primary
        val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
        val surface = MaterialTheme.colorScheme.surface
        val trackHeight = 6.dp
        val thumbRadius = 9.dp

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val widthPx = size.width.toFloat()
                        val ratio = (offset.x / widthPx).coerceIn(0f, 1f)
                        val newCount = (1 + ratio * 2).roundToInt().coerceIn(1, 3)
                        onSegmentCountChange(newCount)
                    }
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { change, _ ->
                        change.consume()
                        val widthPx = size.width.toFloat()
                        val ratio = (change.position.x / widthPx).coerceIn(0f, 1f)
                        val newCount = (1 + ratio * 2).roundToInt().coerceIn(1, 3)
                        onSegmentCountChange(newCount)
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

@Composable
private fun GestureActionRow(
    label: String,
    actionName: String,
    isLongPress: Boolean,
    iconRotation: Float,
    onClick: () -> Unit
) {
    val iconColor = if (isLongPress) Color(0xFFa371f7) else MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 左侧方向图标（无背景，长按紫色标记）
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                modifier = Modifier
                    .size(16.dp)
                    .rotate(iconRotation),
                tint = iconColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = actionName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.size(4.dp))
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


// 获取指定边缘和段的动作列表
private fun getGestureActions(
    edgeType: EdgeType,
    segment: Int,
    settings: com.edgegesture.evilgodxu.data.gesture.GestureSettingsState?
): List<Pair<GestureAction, androidx.datastore.preferences.core.Preferences.Key<String>>> {
    if (settings == null) return emptyList()

    return when (edgeType) {
        EdgeType.LEFT -> {
            val edge = when (segment) {
                2 -> settings.leftEdgeSegment2
                3 -> settings.leftEdgeSegment3
                else -> settings.leftEdge
            }
            listOf(
                edge.swipeRight to GestureSettingsKeys.LEFT_SWIPE_RIGHT,
                edge.swipeRightLong to GestureSettingsKeys.LEFT_SWIPE_RIGHT_LONG,
                edge.swipeUp to GestureSettingsKeys.LEFT_SWIPE_UP,
                edge.swipeUpLong to GestureSettingsKeys.LEFT_SWIPE_UP_LONG,
                edge.swipeDown to GestureSettingsKeys.LEFT_SWIPE_DOWN,
                edge.swipeDownLong to GestureSettingsKeys.LEFT_SWIPE_DOWN_LONG
            )
        }
        EdgeType.RIGHT -> {
            val edge = when (segment) {
                2 -> settings.rightEdgeSegment2
                3 -> settings.rightEdgeSegment3
                else -> settings.rightEdge
            }
            listOf(
                edge.swipeLeft to GestureSettingsKeys.RIGHT_SWIPE_LEFT,
                edge.swipeLeftLong to GestureSettingsKeys.RIGHT_SWIPE_LEFT_LONG,
                edge.swipeUp to GestureSettingsKeys.RIGHT_SWIPE_UP,
                edge.swipeUpLong to GestureSettingsKeys.RIGHT_SWIPE_UP_LONG,
                edge.swipeDown to GestureSettingsKeys.RIGHT_SWIPE_DOWN,
                edge.swipeDownLong to GestureSettingsKeys.RIGHT_SWIPE_DOWN_LONG
            )
        }
        EdgeType.BOTTOM -> {
            val edge = when (segment) {
                2 -> settings.bottomEdgeSegment2
                3 -> settings.bottomEdgeSegment3
                else -> settings.bottomEdge
            }
            listOf(
                edge.swipeUp to GestureSettingsKeys.BOTTOM_SWIPE_UP,
                edge.swipeUpLong to GestureSettingsKeys.BOTTOM_SWIPE_UP_LONG,
                edge.swipeLeft to GestureSettingsKeys.BOTTOM_SWIPE_LEFT,
                edge.swipeLeftLong to GestureSettingsKeys.BOTTOM_SWIPE_LEFT_LONG,
                edge.swipeRight to GestureSettingsKeys.BOTTOM_SWIPE_RIGHT,
                edge.swipeRightLong to GestureSettingsKeys.BOTTOM_SWIPE_RIGHT_LONG
            )
        }
    }
}

@Composable
private fun getGestureLabels(edgeType: EdgeType): List<String> {
    return when (edgeType) {
        EdgeType.LEFT -> listOf(
            stringResource(R.string.gesture_swipe_right),
            stringResource(R.string.gesture_swipe_right_long),
            stringResource(R.string.gesture_swipe_up),
            stringResource(R.string.gesture_swipe_up_long),
            stringResource(R.string.gesture_swipe_down),
            stringResource(R.string.gesture_swipe_down_long)
        )
        EdgeType.RIGHT -> listOf(
            stringResource(R.string.gesture_swipe_left),
            stringResource(R.string.gesture_swipe_left_long),
            stringResource(R.string.gesture_swipe_up),
            stringResource(R.string.gesture_swipe_up_long),
            stringResource(R.string.gesture_swipe_down),
            stringResource(R.string.gesture_swipe_down_long)
        )
        EdgeType.BOTTOM -> listOf(
            stringResource(R.string.gesture_swipe_up),
            stringResource(R.string.gesture_swipe_up_long),
            stringResource(R.string.gesture_swipe_left),
            stringResource(R.string.gesture_swipe_left_long),
            stringResource(R.string.gesture_swipe_right),
            stringResource(R.string.gesture_swipe_right_long)
        )
    }
}

// 获取当前选中动作（用于对话框）
private fun getCurrentActionForDialog(
    edgeType: EdgeType,
    segment: Int,
    settings: com.edgegesture.evilgodxu.data.gesture.GestureSettingsState?,
    key: androidx.datastore.preferences.core.Preferences.Key<String>?
): GestureAction {
    val gestures = getGestureActions(edgeType, segment, settings)
    return gestures.firstOrNull { it.second == key }?.first ?: GestureAction.NONE
}
