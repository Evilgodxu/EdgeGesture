package com.edgegesture.evilgodxu.screens.gesture.service.musicpanel

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edgegesture.evilgodxu.R
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
internal fun LyricsPanel(
    playbackState: MusicPlaybackState,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    fontSize: TextUnit = 12.sp,
    visibleLines: Int = DEFAULT_VISIBLE_LINES,
    // 上下边缘处理方式：true 用渐隐蒙层；false 改为逐行降低边缘行透明度（音乐面板采用）
    edgeFadeMask: Boolean = true,
) {
    val activeColor = MaterialTheme.colorScheme.primary
    val pendingColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
    // 逐字渲染开关：关闭后整行高亮，不再逐字点亮与跳动
    val context = LocalContext.current
    val wordByWordEnabled by context.wordByWordRenderingFlow().collectAsState(initial = true)
    val track = playbackState.currentTrack
    // 跟随当前曲目：切换歌曲时重置到曲目起点，避免沿用上一首的播放位置定位错行
    var lyricPosition by remember(track?.id) { mutableLongStateOf(0L) }
    // 歌词拖拽跳转后的短时保护窗：窗内本地进度自走、忽略控制器的旧位置，
    // 避免 seek 回报前把刚拖到的行又拉回拖拽前位置
    var seekGuardUntilMs by remember(track?.id) { mutableLongStateOf(0L) }
    LaunchedEffect(playbackState.isPlaying, track?.id) {
        var lastSyncMs = 0L
        while (isActive) {
            val candidate = playbackState.mediaController?.currentPosition
                ?.takeIf { it >= 0L }
                ?: playbackState.currentPosition
            val guarding = System.currentTimeMillis() < seekGuardUntilMs
            if (playbackState.isPlaying) {
                val now = System.currentTimeMillis()
                val elapsed = if (lastSyncMs == 0L) 0L else (now - lastSyncMs).coerceAtLeast(0L)
                // 播放中以真实流逝时间推进，控制器位置仅作锚点：熄屏唤醒后控制器
                // 位置可能停滞，本地位置仍持续前进避免冻结；大幅回退视为手动拖动
                lyricPosition = when {
                    guarding -> lyricPosition + elapsed
                    candidate >= lyricPosition -> candidate
                    lyricPosition - candidate > LYRIC_SEEK_TOLERANCE_MS -> candidate
                    else -> lyricPosition + elapsed
                }
                lastSyncMs = now
            } else {
                // 暂停时保持本地已推进的真实位置，仅当控制器位置前移（正向 seek）或
                // 大幅回退（手动拖动）时跟随；避免把播放期间已领先于控制器滞后回报的
                // 本地位置拉回，导致已唱完的歌词高亮回退
                if (!guarding) {
                    lyricPosition = when {
                        candidate >= lyricPosition -> candidate
                        lyricPosition - candidate > LYRIC_SEEK_TOLERANCE_MS -> candidate
                        else -> lyricPosition
                    }
                }
                lastSyncMs = 0L
            }
            delay(if (playbackState.isPlaying) 50L else 200L)
        }
    }

    val lines = track?.lyricLines.orEmpty()
    // 手势回调在整个 pointerInput 生命周期内保持有效，用 rememberUpdatedState 保证始终读到最新歌词
    val currentLines by rememberUpdatedState(lines)
    val activeIndex = lines.indexOfLast { it.timeMs <= lyricPosition }.coerceAtLeast(0)
    // 当前行居中，上下各显示 (total-1)/2 行（total 为奇数）
    val offset = visibleLines / 2
    // 视口高度按 N 行标准高度计算：超长句换行与译文叠层会让单行高于标准，
    // 视口固定为 N 行标准高，超出部分交由边缘渐隐与裁剪处理
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    // 标准单行高度（含行内上下内边距）：视口上限与窗口内占位行共用，使滚动基准线不随占位行变化。
    // 必须与 LyricChar/Text 的实际渲染同口径——沿用 LocalTextStyle（bodyLarge 自带 24.sp 行高）合并，
    // 否则裸 TextStyle 测出的高度偏小，视口按偏小行距排布会被实际更高的行撑出裁剪，可见行数少于设定值
    val lyricTextStyle = LocalTextStyle.current
    val lyricLineHeightPx = remember(fontSize, density, lyricTextStyle) {
        textMeasurer.measure(
            AnnotatedString("歌词"),
            lyricTextStyle.merge(TextStyle(fontSize = fontSize, fontWeight = FontWeight.Normal)),
        ).size.height
    }
    val maxViewportHeight = remember(visibleLines, lyricLineHeightPx, density) {
        val slotPx = lyricLineHeightPx + with(density) { 4.dp.roundToPx() }
        val spacingPx = with(density) { LYRIC_LINE_SPACING.roundToPx() }
        slotPx * visibleLines + spacingPx * (visibleLines - 1)
    }
    val standardSlotHeight = with(density) { (lyricLineHeightPx + 4.dp.roundToPx()).toDp() }
    // 拖拽换算的兜底行距：仅在窗口尚无实测行高时使用，正常路径均由实测行高换算
    val fallbackSlotPx = with(density) { standardSlotHeight.toPx() + LYRIC_LINE_SPACING.toPx() }

    // 显示位置（浮点行号）：正常播放由跟随动画推进，拖拽与回弹期间由手势/回弹动画驱动。
    // 用普通状态直接承载动画输出，释放瞬间即可同步写入，避免显示回退到过期的动画值
    var scrollPosition by remember(track?.id) { mutableFloatStateOf(0f) }
    // scrubbing 为拖拽中，settling 为释放后的吸附/回弹动画中；两者进行时都挂起跟随 Effect
    var scrubbing by remember(track?.id) { mutableStateOf(false) }
    var settling by remember(track?.id) { mutableStateOf(false) }
    // 拖拽起始位置：未对齐释放时的回弹目标
    var scrubStartPosition by remember(track?.id) { mutableFloatStateOf(0f) }
    // 回弹代数：令被新拖拽打断的旧回弹动画失效，避免它把位置或 settling 改回去
    var settleGeneration by remember(track?.id) { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    // 像素位移 → 行位移的换算依据：由 LyricColumnLayout 测量时写入的普通对象，手势回调只读
    val dragGeometry = remember { LyricScrollGeometry() }

    // 跟随播放：拖拽/回弹期间挂起，避免与手势、回弹动画争抢同一个位置。
    // 跨度大于阈值（拖动进度/切歌）时直接定位，避免长距离滚动
    LaunchedEffect(activeIndex, track?.id, scrubbing, settling) {
        if (scrubbing || settling) return@LaunchedEffect
        val start = scrollPosition
        val target = activeIndex.toFloat()
        if (abs(target - start) <= LYRIC_SCROLL_MAX_STEP) {
            animate(
                initialValue = start,
                targetValue = target,
                animationSpec = tween(LYRIC_SCROLL_DURATION_MS, easing = FastOutSlowInEasing),
            ) { value, _ -> scrollPosition = value }
        } else {
            scrollPosition = target
        }
    }
    // 显示位置：拖拽/回弹期间即手势位置；其余时间跟随播放。与目标跨度较大时（拖动进度/切歌瞬间）
    // 跟随动画尚未归位，本帧直接用目标行定位，避免错误行被居中
    val displayPosition = when {
        scrubbing || settling -> scrollPosition
        abs(scrollPosition - activeIndex) > LYRIC_SCROLL_MAX_STEP -> activeIndex.toFloat()
        else -> scrollPosition
    }
    // 拖拽已对齐到某一行：释放即吸附并从此行起播，基准标识据此切换为确认色。
    // 判定与释放时的吸附条件共用同一个函数，避免标识已确认却回弹
    val scrubAligned = scrubbing && alignedScrubRow(scrollPosition, lines.lastIndex) != null

    Box(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            // 纵向拖拽调进度：过触摸 slop 后消费事件。声明在 combinedClickable 之后（内层），
            // 拖拽时取消点击，同时不触发外层左右滑动切面板；点按/长按不受影响
            .pointerInput(track?.id) {
                // 拖拽起点：与组合期的 displayPosition 同口径，但读实时状态避免闭包过期
                fun liveDisplayPosition(): Float {
                    val liveActive = currentLines.indexOfLast { it.timeMs <= lyricPosition }
                        .coerceAtLeast(0)
                    return if (abs(scrollPosition - liveActive) > LYRIC_SCROLL_MAX_STEP) {
                        liveActive.toFloat()
                    } else {
                        scrollPosition
                    }
                }

                // 释放后的收尾动画：吸附到目标行或弹性回弹。
                // 代数用于让被新拖拽打断的旧动画失效，避免它把位置或 settling 改回去
                fun settleTo(target: Float, animationSpec: AnimationSpec<Float>) {
                    val generation = ++settleGeneration
                    settling = true
                    scope.launch {
                        animate(
                            initialValue = scrollPosition,
                            targetValue = target,
                            animationSpec = animationSpec,
                        ) { value, _ ->
                            if (settleGeneration == generation) scrollPosition = value
                        }
                        if (settleGeneration == generation) settling = false
                    }
                }

                fun finishScrub(cancelled: Boolean) {
                    if (!scrubbing) return
                    scrubbing = false
                    if (currentLines.isEmpty()) return
                    val candidate = alignedScrubRow(scrollPosition, currentLines.lastIndex)
                    if (!cancelled && candidate != null) {
                        // 对齐：吸附到该行并从该行时间点起播。先把当前行切到目标行并开短时保护窗
                        // 忽略控制器旧位置，避免跟随 Effect 在 seek 回报前把内容拉回拖拽前的行
                        val targetMs = currentLines[candidate].timeMs
                        lyricPosition = targetMs
                        seekGuardUntilMs = System.currentTimeMillis() + LYRIC_SCRUB_SEEK_GUARD_MS
                        seekToAndPlay(playbackState, targetMs)
                        settleTo(candidate.toFloat(), tween(LYRIC_SCRUB_SNAP_MS))
                    } else {
                        // 未对齐：不跳转进度，弹性回弹到拖拽前位置
                        settleTo(
                            target = scrubStartPosition,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow,
                            ),
                        )
                    }
                }

                detectVerticalDragGestures(
                    onDragStart = {
                        if (currentLines.isNotEmpty()) {
                            // 新的拖拽作废未完成的回弹，并立即接管显示位置
                            settleGeneration++
                            settling = false
                            scrubStartPosition = liveDisplayPosition()
                            scrollPosition = scrubStartPosition
                            scrubbing = true
                        }
                    },
                    onVerticalDrag = { change, dragAmount ->
                        if (scrubbing) {
                            change.consume()
                            // 像素位移 ÷ 当前行到下一行中心的距离 = 行位移，行高不定也能跟手
                            val slot = dragGeometry.slotAt(scrollPosition)
                            scrollPosition = (scrollPosition - dragAmount / slot)
                                .coerceIn(0f, currentLines.lastIndex.toFloat())
                        }
                    },
                    onDragEnd = { finishScrub(cancelled = false) },
                    onDragCancel = { finishScrub(cancelled = true) },
                )
            }
            .padding(top = 4.dp, bottom = 0.dp),
        contentAlignment = Alignment.Center
    ) {
        if (lines.isEmpty()) {
            // 歌词补全已尝试且仍为空才提示缺失，避免补全过程中闪"暂无歌词"占位
            if (track?.lyricResolved == true) {
                Text(
                    stringResource(R.string.music_panel_no_lyrics),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                )
            }
        } else {
            // 窗口容器固定：滚动都在其内部进行，行溢出与滚出内容经过边缘即被裁剪
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clipToBounds()
                    // 渐隐蒙层仅用于首页等场景；音乐面板改用逐行降低边缘行透明度
                    .then(
                        if (edgeFadeMask) {
                            Modifier.verticalFadeMask(fadeFraction = FADE_TOTAL_LINES / visibleLines)
                        } else {
                            Modifier
                        }
                    )
            ) {
                // 以显示位置所在行为窗口锚点，上下各多渲染 buffer 行：滚动或拖拽时新行已在窗口内、
                // 被移除的行已完全移出视口，两者在同一坐标系整体平移，因此只会连续上移，不会突兀替换
                val anchorIndex = displayPosition.roundToInt().coerceIn(0, lines.lastIndex)
                // 拖拽时高亮跟随中线最近的候选行，作为「释放会选中哪一行」的反馈；其余时间跟随播放
                val highlightIndex = if (scrubbing) anchorIndex else activeIndex
                val windowStart = anchorIndex - offset - LYRIC_WINDOW_BUFFER
                LyricColumnLayout(
                    windowStart = windowStart,
                    animatedPosition = displayPosition,
                    maxViewportHeight = maxViewportHeight,
                    geometry = dragGeometry,
                    fallbackSlotPx = fallbackSlotPx,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    repeat(visibleLines + LYRIC_WINDOW_BUFFER * 2) { row ->
                        val index = windowStart + row
                        // 以行下标为键：窗口平移时同一行的状态（高亮进度等）得以保留，不会跳变
                        key(index) {
                            val line = lines.getOrNull(index)
                            if (line == null) {
                                LyricSpacer(height = standardSlotHeight)
                            } else {
                                val isCurrent = index == highlightIndex
                                val emphasis by animateFloatAsState(
                                    targetValue = if (isCurrent) 1f else 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    ),
                                    label = "lyric_emphasis"
                                )
                                val scale = LYRIC_ROW_SCALE_BASE + LYRIC_ROW_SCALE_AMPLITUDE * emphasis
                                // 非当前行整体降低不透明度，弱化其视觉存在感；随高亮进度平滑过渡
                                val rowAlpha = LYRIC_INACTIVE_ALPHA + (1f - LYRIC_INACTIVE_ALPHA) * emphasis
                                // 不使用渐隐蒙层时：按行距视口中线的距离额外降低边缘行透明度
                                val lineAlpha = if (edgeFadeMask) rowAlpha
                                else rowAlpha * edgeLineAlpha(index, displayPosition, offset)
                                val nextTimeMs = lines.getOrNull(index + 1)?.timeMs ?: line.timeMs + 3000L
                                LyricText(
                                    line = line,
                                    nextTimeMs = nextTimeMs,
                                    positionMs = lyricPosition,
                                    isCurrent = isCurrent,
                                    wordByWordEnabled = wordByWordEnabled,
                                    fontSize = fontSize,
                                    fontWeight = if (isCurrent) FontWeight.Medium else FontWeight.Normal,
                                    activeColor = activeColor,
                                    pendingColor = pendingColor,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .graphicsLayer {
                                            alpha = lineAlpha
                                            scaleX = scale
                                            scaleY = scale
                                        }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
                // 拖拽基准标识：固定在视口中线、不随内容滚动，仅拖拽时淡入；对齐后转为确认色
                LyricScrubMarker(
                    visible = scrubbing,
                    color = activeColor,
                    aligned = scrubAligned,
                )
            }
        }
    }
}

@Composable
internal fun LyricSpacer(height: Dp) {
    Spacer(modifier = Modifier.height(height))
}

// 拖拽调进度的基准标识：视口中线两侧各一段短横线（形如「- 歌词 -」），仅拖拽时淡入，
// 作为「哪一行与中线对齐会被选中」的参考线；已对齐时切换为浅绿色确认态，
// 用户据此判断此刻松手即会从该行起播
@Composable
private fun BoxScope.LyricScrubMarker(visible: Boolean, color: Color, aligned: Boolean) {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(LYRIC_SCRUB_MARKER_FADE_MS),
        label = "lyric_scrub_marker",
    )
    if (alpha <= 0.01f) return
    // 对齐确认色：颜色随对齐状态过渡，跨行擦过时不会闪成不相关的中间色
    val dashColor by animateColorAsState(
        targetValue = if (aligned) LYRIC_SCRUB_CONFIRM_COLOR else color,
        animationSpec = tween(LYRIC_SCRUB_MARKER_CONFIRM_MS),
        label = "lyric_scrub_marker_aligned",
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.Center)
            .graphicsLayer { this.alpha = alpha },
    ) {
        LyricScrubDash(color = dashColor, modifier = Modifier.align(Alignment.CenterStart))
        LyricScrubDash(color = dashColor, modifier = Modifier.align(Alignment.CenterEnd))
    }
}

@Composable
private fun LyricScrubDash(color: Color, modifier: Modifier) {
    Box(
        modifier = modifier
            .width(LYRIC_SCRUB_DASH_WIDTH)
            .height(LYRIC_SCRUB_DASH_HEIGHT)
            .background(
                color = color.copy(alpha = 0.8f),
                shape = RoundedCornerShape(LYRIC_SCRUB_DASH_HEIGHT / 2),
            ),
    )
}

// 拖拽像素 → 行位移的换算依据：由 LyricColumnLayout 测量时写入，手势回调只读。
// 走普通对象而非 Compose 状态，避免手势期间写入测量结果触发额外重组
private class LyricScrollGeometry {
    private var windowStart = 0
    private var rowHeights: IntArray = EMPTY_ROW_HEIGHTS
    private var spacingPx = 0
    private var fallbackSlotPx = 1f

    fun update(windowStart: Int, rowHeights: IntArray, spacingPx: Int, fallbackSlotPx: Float) {
        this.windowStart = windowStart
        this.rowHeights = rowHeights
        this.spacingPx = spacingPx
        this.fallbackSlotPx = fallbackSlotPx
    }

    // 当前位置所在行到下一行中心的距离：像素位移 ÷ 该值 = 行位移
    fun slotAt(position: Float): Float {
        val heights = rowHeights
        if (heights.isEmpty()) return fallbackSlotPx.coerceAtLeast(1f)
        val row = floor(position - windowStart).toInt().coerceIn(0, heights.lastIndex)
        val height = heights[row].toFloat()
        val nextHeight = heights.getOrNull(row + 1)?.toFloat() ?: height
        return (height / 2f + spacingPx + nextHeight / 2f).coerceAtLeast(1f)
    }
}

private val EMPTY_ROW_HEIGHTS = IntArray(0)

// 歌词纵向布局：窗口内按真实行高逐行排布，再以“动画浮点行号”定位目标位置在内容中的中心，
// 整体平移使该中心对齐视口中线。行高随换行而不同，按固定行高偏移会使当前行偏离中线，
// 故用实测高度换算；目标中心在相邻两行中心之间线性插值，跨行切换不会跳变。
@Composable
private fun LyricColumnLayout(
    windowStart: Int,
    animatedPosition: Float,
    maxViewportHeight: Int,
    geometry: LyricScrollGeometry,
    fallbackSlotPx: Float,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val spacingPx = with(LocalDensity.current) { LYRIC_LINE_SPACING.roundToPx() }
    Layout(
        modifier = modifier,
        content = content,
    ) { measurables, constraints ->
        val placeables = measurables.map {
            it.measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))
        }
        // 把手势换算所需的实测几何写入普通对象：拖拽时按真实行距换算，且不触发额外重组
        geometry.update(
            windowStart = windowStart,
            rowHeights = IntArray(placeables.size) { placeables[it].height },
            spacingPx = spacingPx,
            fallbackSlotPx = fallbackSlotPx,
        )
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth
        else placeables.maxOfOrNull { it.width } ?: 0
        // 视口高度固定为 N 行标准高度：窗口内多出的 buffer 行不撑高面板，滚动基准线保持稳定
        val boundedMax = if (constraints.hasBoundedHeight) constraints.maxHeight else Int.MAX_VALUE
        val viewportHeight = minOf(maxViewportHeight, boundedMax).coerceAtLeast(1)
        if (placeables.isEmpty()) {
            layout(width, viewportHeight) {}
        } else {
            // 动画行号换算成窗口内相对行号，取整定位所在行，余数用于在相邻行中心之间插值
            val relative = (animatedPosition - windowStart)
                .coerceIn(0f, placeables.lastIndex.toFloat())
            val row = floor(relative).toInt().coerceIn(0, placeables.lastIndex)
            val fraction = relative - row
            var rowTop = 0
            for (i in 0 until row) rowTop += placeables[i].height + spacingPx
            val rowCenter = rowTop + placeables[row].height / 2f
            val rowToNextCenter = if (row < placeables.lastIndex) {
                placeables[row].height / 2f + spacingPx + placeables[row + 1].height / 2f
            } else {
                0f
            }
            // 平移量 = 视口中线 - 动画行号对应位置的中心，使该位置始终居于中线
            val shift = viewportHeight / 2f - (rowCenter + fraction * rowToNextCenter)
            layout(width, viewportHeight) {
                var y = shift
                placeables.forEachIndexed { i, placeable ->
                    placeable.placeRelative(0, y.roundToInt())
                    y += placeable.height + if (i < placeables.lastIndex) spacingPx else 0
                }
            }
        }
    }
}

// 单行歌词：主歌词按逐字渲染开关与是否有逐字时序选择渲染方式，译文以更小字号静置在主歌词下方
@Composable
internal fun LyricText(
    line: LyricLine,
    nextTimeMs: Long,
    positionMs: Long,
    isCurrent: Boolean,
    wordByWordEnabled: Boolean,
    fontSize: TextUnit,
    fontWeight: FontWeight,
    activeColor: Color,
    pendingColor: Color,
    modifier: Modifier = Modifier,
) {
    var contentWidthPx by remember { mutableIntStateOf(0) }
    // 所有行统一按当前行的放大预留宽度分行：未唱/已唱和正在唱的行数一致，避免换行跳变
    val wrapWidthPx = if (contentWidthPx <= 0) 0 else (contentWidthPx / LYRIC_ROW_SCALE_MAX).roundToInt()
    // 各行的水平缩放余量同样全行统一，保证分行宽度与主歌词排版一致
    val reserveWidthPx = (contentWidthPx * (LYRIC_ROW_SCALE_MAX - 1f) / (2f * LYRIC_ROW_SCALE_MAX)).roundToInt()
    Column(
        modifier = modifier.onSizeChanged { contentWidthPx = it.width },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (!wordByWordEnabled) {
            WholeLineLyricText(
                line = line,
                isCurrent = isCurrent,
                fontSize = fontSize,
                fontWeight = fontWeight,
                activeColor = activeColor,
                pendingColor = pendingColor,
                reserveWidthPx = reserveWidthPx,
            )
        } else if (line.words.isNotEmpty()) {
            WordSplitLyricText(
                line = line,
                nextTimeMs = nextTimeMs,
                positionMs = positionMs,
                isCurrent = isCurrent,
                fontSize = fontSize,
                fontWeight = fontWeight,
                activeColor = activeColor,
                pendingColor = pendingColor,
                widthPx = wrapWidthPx,
            )
        } else {
            LineFillLyricText(
                line = line,
                nextTimeMs = nextTimeMs,
                positionMs = positionMs,
                isCurrent = isCurrent,
                fontSize = fontSize,
                fontWeight = fontWeight,
                activeColor = activeColor,
                pendingColor = pendingColor,
                widthPx = wrapWidthPx,
            )
        }
        line.translation?.takeIf { it.isNotBlank() }?.let { translation ->
            // 翻译行以更小字号静置展示，主歌词高亮时翻译同步使用完整高亮色与发光
            Text(
                text = translation,
                fontSize = (fontSize.value * 0.68f).sp,
                fontWeight = FontWeight.Normal,
                color = if (isCurrent) activeColor else pendingColor.copy(alpha = 0.55f),
                style = if (isCurrent) {
                    TextStyle(shadow = Shadow(activeColor.copy(alpha = 0.65f), blurRadius = 5f))
                } else {
                    TextStyle()
                },
                textAlign = TextAlign.Center,
                softWrap = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = with(LocalDensity.current) { reserveWidthPx.toDp() })
                    .padding(top = 1.dp),
            )
        }
    }
}

// 歌词行动画缩放：普通行微缩，当前行高亮放大至 max；分行按预留上限 max 计算宽度
internal const val LYRIC_ROW_SCALE_BASE = 0.98f
internal const val LYRIC_ROW_SCALE_AMPLITUDE = 0.16f
internal const val LYRIC_ROW_SCALE_MAX = 1.35f

// 关闭逐字渲染：整行统一着色，不逐字填充也不跳动
@Composable
private fun WholeLineLyricText(
    line: LyricLine,
    isCurrent: Boolean,
    fontSize: TextUnit,
    fontWeight: FontWeight,
    activeColor: Color,
    pendingColor: Color,
    reserveWidthPx: Int,
    modifier: Modifier = Modifier,
) {
    Text(
        text = line.text,
        fontSize = fontSize,
        fontWeight = fontWeight,
        color = if (isCurrent) activeColor else pendingColor,
        textAlign = TextAlign.Center,
        softWrap = true,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = with(LocalDensity.current) { reserveWidthPx.toDp() }),
    )
}

// 普通歌词（无逐字时序）：按字均分时间整行顺序点亮，正在演唱的字高亮从左到右扫过并叠加弹簧跳动
@Composable
internal fun LineFillLyricText(
    line: LyricLine,
    nextTimeMs: Long,
    positionMs: Long,
    isCurrent: Boolean,
    fontSize: TextUnit,
    fontWeight: FontWeight,
    activeColor: Color,
    pendingColor: Color,
    widthPx: Int,
    modifier: Modifier = Modifier,
) {
    val duration = (nextTimeMs - line.timeMs).coerceAtLeast(1L)
    val totalLen = line.text.length.coerceAtLeast(1)
    val perCharMs = duration / totalLen.toFloat()

    // 当前正在演唱的字下标：仅当前行且已开唱才计算，唱完时停在末字
    val currentCharIdx = if (isCurrent && positionMs > line.timeMs) {
        ((positionMs - line.timeMs).toFloat() / perCharMs).toInt().coerceIn(0, totalLen - 1)
    } else {
        -1
    }
    // 行内已播放时长折算成字符级进度：当前字的亮起比例由其与整数的差值决定
    val charProgress = if (isCurrent && positionMs > line.timeMs) {
        (positionMs - line.timeMs).toFloat() / perCharMs
    } else {
        -1f
    }

    // 逐字独立渲染无法借助 Text 软换行，按传入的可用宽度手动分行
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val textMeasurer = rememberTextMeasurer()
        val style = LocalTextStyle.current.merge(TextStyle(fontSize = fontSize, fontWeight = fontWeight))
        val rows = remember(line.text, widthPx, style) {
            if (widthPx <= 0) listOf(line.text) else wrapLyricText(line.text, widthPx, textMeasurer, style)
        }
        var globalIdx = 0
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                row.forEach { ch ->
                    val idx = globalIdx++
                    LyricChar(
                        text = ch.toString(),
                        fillFraction = if (charProgress < 0f) 0f else (charProgress - idx).coerceIn(0f, 1f),
                        filling = isCurrent && idx == currentCharIdx,
                        fontSize = fontSize,
                        fontWeight = fontWeight,
                        activeColor = activeColor,
                        pendingColor = pendingColor,
                        shadowBlurRadius = LINE_CHAR_SHADOW_BLUR,
                    )
                }
            }
        }
    }
}

// 单个歌词字：未唱为待唱色，已唱为高亮色；正在演唱的字高亮按 fillFraction 从左到右
// 逐渐亮起。待唱层与高亮层共用同一文本布局绘制，逐像素对齐，避免缩放跳起时错位
@Composable
internal fun LyricChar(
    text: String,
    fillFraction: Float,
    filling: Boolean,
    fontSize: TextUnit,
    fontWeight: FontWeight,
    activeColor: Color,
    pendingColor: Color,
    shadowBlurRadius: Float,
) {
    // 跳起与落下均渐进过渡：新字柔和弹起的同时旧字缓缓回落，
    // 两者在时间上重叠，形成连续流动感，避免瞬间落下/瞬间跳起的突兀
    val emphasis = remember { Animatable(0f) }
    LaunchedEffect(filling) {
        emphasis.animateTo(
            targetValue = if (filling) 1f else 0f,
            animationSpec = if (filling) {
                spring(
                    dampingRatio = 0.55f,
                    stiffness = 420f,
                )
            } else {
                tween(
                    durationMillis = LYRIC_JUMP_DOWN_MS,
                    easing = LinearOutSlowInEasing,
                )
            },
        )
    }
    // 位置进度按采样周期跳跃推进，用线性 tween 平滑成连续亮起动画
    val highlightFraction by animateFloatAsState(
        targetValue = fillFraction,
        animationSpec = tween(
            durationMillis = LYRIC_FILL_SMOOTH_MS,
            easing = LinearEasing,
        ),
        label = "lyric_char_fill",
    )
    val density = LocalDensity.current
    val floatPx = with(density) { 0.05f * fontSize.toPx() }
    // 文本样式与 Text 组件默认行为一致（沿用 LocalTextStyle），保证布局高度准确
    val textMeasurer = rememberTextMeasurer()
    val currentTextStyle = LocalTextStyle.current
    val layout = remember(text, fontSize, fontWeight, currentTextStyle) {
        textMeasurer.measure(
            AnnotatedString(text),
            currentTextStyle.merge(
                TextStyle(fontSize = fontSize, fontWeight = fontWeight)
            ),
        )
    }
    val glowShadow = Shadow(activeColor.copy(alpha = 0.65f), blurRadius = shadowBlurRadius)
    Box(
        modifier = Modifier
            .graphicsLayer {
                // 跳起效果：弹簧放大带过冲 + 轻微上浮
                scaleX = 1f + 0.14f * emphasis.value
                scaleY = 1f + 0.14f * emphasis.value
                translationY = -floatPx * emphasis.value
            }
            .size(
                width = with(density) { layout.size.width.toDp() },
                height = with(density) { layout.size.height.toDp() },
            )
            .drawWithContent {
                // 待唱层：整字待唱色
                drawText(layout, color = pendingColor, topLeft = Offset.Zero)
                // 高亮层：按已亮起比例从左到右裁剪露出，发光随高亮区域显示
                clipRect(right = size.width * highlightFraction.coerceIn(0f, 1f)) {
                    drawText(layout, color = activeColor, shadow = glowShadow, topLeft = Offset.Zero)
                }
            }
    )
}

private const val LINE_CHAR_SHADOW_BLUR = 5f

private const val LYRIC_FILL_SMOOTH_MS = 60

// 演唱结束的字回落用时：与下一字跳起重叠渐变，形成渐落衔接，不宜过短
private const val LYRIC_JUMP_DOWN_MS = 320

// 逐字歌词：严格按每个词自身的起止时间做卡拉OK式点亮——已唱完的词整词高亮，
// 正在演唱的词内逐字从左到右亮起并叠加弹簧跳动。词时序来自歌词源，起点与时长都不均匀
// （词间可能存在空隙），故不做「整行时长按词数均分」的近似
@Composable
internal fun WordSplitLyricText(
    line: LyricLine,
    nextTimeMs: Long,
    positionMs: Long,
    isCurrent: Boolean,
    fontSize: TextUnit,
    fontWeight: FontWeight,
    activeColor: Color,
    pendingColor: Color,
    widthPx: Int,
    modifier: Modifier = Modifier,
) {
    // 词终点：优先取词自身的时长；增强 LRC 只提供起点（duration 为 0）时用下一个词的起点兜底，
    // 末词用下一行起点兜底，保证每个词都有可用的起止区间
    val wordEnds = remember(line.words, nextTimeMs) {
        line.words.mapIndexed { index, word ->
            if (word.durationMs > 0) {
                word.startMs + word.durationMs
            } else {
                line.words.getOrNull(index + 1)?.startMs ?: nextTimeMs
            }
        }
    }

    // 词独立渲染无法借助 Text 软换行，按传入的可用宽度将整行词分成多行：英文词保持完整不截断
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val textMeasurer = rememberTextMeasurer()
        val style = LocalTextStyle.current.merge(TextStyle(fontSize = fontSize, fontWeight = fontWeight))
        val rows = remember(line.words, widthPx, style) {
            if (widthPx <= 0) listOf(line.words) else wrapLyricWords(line.words, widthPx, textMeasurer, style)
        }
        var globalIdx = 0
        rows.forEach { rowWords ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                rowWords.forEach { word ->
                    val index = globalIdx++
                    val wordStart = word.startMs
                    // 至少留 1ms 区间：零时长词不能除零，也不能整词瞬间跳过
                    val wordEnd = wordEnds[index].coerceAtLeast(wordStart + 1)
                    val isWordSung = isCurrent && positionMs >= wordEnd
                    val isWordCurrent = isCurrent && positionMs >= wordStart && positionMs < wordEnd
                    val charCount = word.text.length.coerceAtLeast(1)
                    // 当前词内已演唱的字符级进度：按词内已过时长占词时长比例折算到字数
                    val wordCharProgress = if (isWordCurrent) {
                        (positionMs - wordStart).toFloat() / (wordEnd - wordStart) * charCount
                    } else {
                        0f
                    }
                    val currentCharIdx = if (isWordCurrent) {
                        wordCharProgress.toInt().coerceIn(0, charCount - 1)
                    } else {
                        -1
                    }
                    // 词保持整体排版，词内逐字渲染以支持单字亮起与跳动；词间间距由词文本自带空格保留
                    Row(
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        word.text.forEachIndexed { charIdx, ch ->
                            // 仅当前演唱词中当前正在演唱的字触发跳动，其余字保持静态
                            val isFilling = isWordCurrent && charIdx == currentCharIdx
                            LyricChar(
                                text = ch.toString(),
                                fillFraction = when {
                                    isWordSung -> 1f
                                    isWordCurrent -> (wordCharProgress - charIdx).coerceIn(0f, 1f)
                                    else -> 0f
                                },
                                filling = isFilling,
                                fontSize = fontSize,
                                fontWeight = fontWeight,
                                activeColor = activeColor,
                                pendingColor = pendingColor,
                                shadowBlurRadius = WORD_CHAR_SHADOW_BLUR,
                            )
                        }
                    }
                }
            }
        }
    }
}

private const val WORD_CHAR_SHADOW_BLUR = 7f

// 按可用宽度分行：行宽超过可用宽度即换行，行尾截断完整单词时回退到最近空格
internal fun wrapLyricText(
    text: String,
    maxWidthPx: Int,
    textMeasurer: TextMeasurer,
    style: TextStyle,
): List<String> {
    if (text.isBlank() || maxWidthPx <= 0) return listOf(text)
    // 整行放得下时免去分行测量
    if (textMeasurer.measure(AnnotatedString(text), style).size.width <= maxWidthPx) return listOf(text)
    val rows = mutableListOf<String>()
    var lineStart = 0
    while (lineStart < text.length) {
        // 逐字扩展行前缀，找到首个超过可用宽度的位置
        var end = lineStart
        while (end < text.length && textMeasurer.measure(
                AnnotatedString(text.substring(lineStart, end + 1)), style,
            ).size.width <= maxWidthPx
        ) {
            end++
        }
        if (end >= text.length) {
            rows.add(text.substring(lineStart))
            break
        }
        // 行尾落在单词中间时回退到最近的空格断行，避免截断完整单词
        var breakAt = end
        var j = end
        while (j > lineStart) {
            if (text[j - 1].isWhitespace()) {
                breakAt = j
                break
            }
            j--
        }
        // 窄屏下至少放入一个字符，避免出现空行
        if (breakAt <= lineStart) breakAt = lineStart + 1
        rows.add(text.substring(lineStart, breakAt))
        lineStart = breakAt
        // 跳过下一行行首空白
        while (lineStart < text.length && text[lineStart].isWhitespace()) lineStart++
    }
    return rows
}

// 按可用宽度分配逐字歌词的词：累加词宽超过宽度上限时换行，单个词保持完整不截断
private fun wrapLyricWords(
    words: List<LyricWord>,
    maxWidthPx: Int,
    textMeasurer: TextMeasurer,
    style: TextStyle,
): List<List<LyricWord>> {
    val rows = mutableListOf<List<LyricWord>>()
    val row = mutableListOf<LyricWord>()
    var rowWidth = 0
    words.forEach { word ->
        val wordWidth = textMeasurer.measure(AnnotatedString(word.text), style).size.width
        // 当前行加不下下一个词时提前换行；词本身超宽时强制独占一行
        if (row.isNotEmpty() && rowWidth + wordWidth > maxWidthPx) {
            rows.add(row.toList())
            row.clear()
            rowWidth = 0
        }
        row.add(word)
        rowWidth += wordWidth
    }
    if (row.isNotEmpty()) rows.add(row)
    return rows
}

// 播放中位置回退容差：小于该值视为控制器位置抖动，大于视为手动拖动进度条
private const val LYRIC_SEEK_TOLERANCE_MS = 1500L

// 歌词面板默认可见行数：保持奇数使当前行垂直居中（上下各 (n-1)/2 行）
private const val DEFAULT_VISIBLE_LINES = 5

// 歌词行间距：相邻歌词行之间的纵向间距，独立于行内上下内边距（4.dp）单独可调，
// 避免行间距偏大导致歌词过于松散；用于视口高度、兜底行距与纵向布局三处一致换算
private val LYRIC_LINE_SPACING = 1.dp

// 窗口上下各多渲染的行数：滚动时新行已在窗口内、被移除的行已完全移出视口，
// 两者在同一坐标系整体平移，因此只会连续上移，不会出现边缘闪现或整块替换
private const val LYRIC_WINDOW_BUFFER = 2

// 换行滚动：时长与缓动参照自然滚动（先快后慢）；跨度大于该行数（拖动进度/切歌）直接定位
private const val LYRIC_SCROLL_DURATION_MS = 400
private const val LYRIC_SCROLL_MAX_STEP = 2

// 非当前行不透明度：弱化视觉存在感，随高亮进度平滑过渡
private const val LYRIC_INACTIVE_ALPHA = 0.55f

// 拖拽调进度：释放时与基准标识的对齐容差（行），超出则不跳转并回弹到拖拽前位置
private const val LYRIC_SCRUB_SNAP_ROWS = 0.35f

// 对齐释放后吸附到目标行的动画时长
private const val LYRIC_SCRUB_SNAP_MS = 220

// 对齐跳转后的短时保护窗：窗内本地进度自走、忽略控制器的旧位置，
// 避免跟随 Effect 在 seek 回报前把内容拉回拖拽前的行
private const val LYRIC_SCRUB_SEEK_GUARD_MS = 1000L

// 基准标识（两侧短横线）尺寸与淡入淡出时长，以及切换到对齐确认色的过渡时长
private const val LYRIC_SCRUB_MARKER_FADE_MS = 160
private const val LYRIC_SCRUB_MARKER_CONFIRM_MS = 120
private val LYRIC_SCRUB_DASH_WIDTH = 22.dp
private val LYRIC_SCRUB_DASH_HEIGHT = 2.dp

// 拖拽对齐确认色：与中线对齐、松手即会从该行起播的提示色
private val LYRIC_SCRUB_CONFIRM_COLOR = Color(0xFF3FB950)

// 拖拽位置当前对齐到的行下标；偏差超出吸附容差视为未对齐（null）。
// 释放时的吸附判定与拖拽中的标识确认态共用本函数，两侧口径不会漂移
private fun alignedScrubRow(position: Float, lastIndex: Int): Int? {
    if (lastIndex < 0) return null
    val candidate = position.roundToInt().coerceIn(0, lastIndex)
    return candidate.takeIf { abs(position - candidate) <= LYRIC_SCRUB_SNAP_ROWS }
}

// 上下边缘行透明度衰减强度：音乐面板以「离视口中线越远越透明」替代渐隐蒙层，
// 强度为最外行相对中线的透明度降幅
private const val EDGE_LINE_FADE_STRENGTH = 0.55f

// 边缘行透明度下限：避免最外行过淡而看不清，保证上下边缘歌词仍可读
private const val EDGE_LINE_MIN_ALPHA = 0.4f

// 上下边缘渐隐蒙层覆盖的总行数（上下各半）：随可见行数换算比例，行数增减时淡出区间保持一致。
// 取 ≥1.5 行：当容器高度不足以容纳设定行数时，顶部溢出行被裁成一薄片，仅靠窄渐变/单点透明
// 仍会残留纯色细线，需让渐变区覆盖一整个溢出行，使薄片深陷透明区
private const val FADE_TOTAL_LINES = 1.6f

// 边缘行透明度系数：以视口中线（浮点行号）为基准，行越靠上下边缘透明度越低；中线行为 1。
// 仅用于不使用渐隐蒙层的场景（音乐面板）
private fun edgeLineAlpha(index: Int, centerPosition: Float, halfWindow: Int): Float {
    if (halfWindow <= 0) return 1f
    val ratio = (abs(index - centerPosition) / halfWindow).coerceIn(0f, 1f)
    return (1f - EDGE_LINE_FADE_STRENGTH * ratio).coerceAtLeast(EDGE_LINE_MIN_ALPHA)
}

// 上下边缘淡出：按纵向透明度梯度对内容做 DstIn 蒙层，使上下行渐变消失。
// 边缘两端各保留一段完全透明区间，杜绝子像素级残影——行带 graphicsLayer 缩放、以小数 y 放置时，
// 字形/光晕可能仅以 ≤1dp 的细线越过边缘，单点透明 stop 只掩到精确边界，仍会残留原色细线
private fun Modifier.verticalFadeMask(fadeFraction: Float = 0.25f): Modifier = drawWithCache {
    // 完全透明保护带：fadeFraction 随可见行数缩放，覆盖边缘残影的像素宽度
    val edgeGuard = fadeFraction * 0.12f
    val brush = Brush.verticalGradient(
        colorStops = arrayOf(
            0.0f to Color.Transparent,
            edgeGuard to Color.Transparent,
            fadeFraction to Color.Black,
            1f - fadeFraction to Color.Black,
            1f - edgeGuard to Color.Transparent,
            1f to Color.Transparent,
        )
    )
    onDrawWithContent {
        val bounds = Rect(Offset.Zero, size)
        // 绘制级裁剪与蒙层放在同一图层：子层包边像素在进入图层前即被裁除，
        // 避免英文光晕/字形越过窗口边缘后以未蒙层原色残留
        drawIntoCanvas { canvas ->
            canvas.save()
            canvas.clipRect(bounds)
            canvas.saveLayer(bounds, Paint())
        }
        drawContent()
        drawRect(brush = brush, size = size, blendMode = BlendMode.DstIn)
        drawIntoCanvas { canvas ->
            canvas.restore()
            canvas.restore()
        }
    }
}