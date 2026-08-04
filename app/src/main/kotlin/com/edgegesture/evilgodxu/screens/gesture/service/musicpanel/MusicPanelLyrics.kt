package com.edgegesture.evilgodxu.screens.gesture.service.musicpanel

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle

import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit
import com.edgegesture.evilgodxu.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
internal fun LyricsPanel(
    playbackState: MusicPlaybackState,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    var lyricPosition by remember { mutableLongStateOf(playbackState.currentPosition) }
    LaunchedEffect(playbackState.isPlaying, playbackState.currentTrack?.id) {
        while (isActive) {
            lyricPosition = playbackState.mediaController?.currentPosition
                ?.takeIf { it >= 0L }
                ?: playbackState.currentPosition
            delay(if (playbackState.isPlaying) 50L else 200L)
        }
    }

    val lines = playbackState.currentTrack?.lyricLines.orEmpty()
    val activeIndex = lines.indexOfLast { it.timeMs <= lyricPosition }.coerceAtLeast(0)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(top = 4.dp, bottom = 0.dp),
        contentAlignment = Alignment.Center
    ) {
        if (lines.isEmpty()) {
            Text(stringResource(R.string.music_panel_no_lyrics), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        } else {
            AnimatedContent(
                targetState = activeIndex,
                transitionSpec = {
                    val movingForward = targetState > initialState
                    val distance = { height: Int -> (height / 4).coerceAtLeast(1) }
                    if (movingForward) {
                        (slideInVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { it }
                            + fadeIn(animationSpec = tween(180))) togetherWith
                            (slideOutVertically(animationSpec = tween(260)) { -distance(it) }
                                + fadeOut(animationSpec = tween(180)))
                    } else {
                        (slideInVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { -distance(it) }
                            + fadeIn(animationSpec = tween(180))) togetherWith
                            (slideOutVertically(animationSpec = tween(260)) { it }
                                + fadeOut(animationSpec = tween(180)))
                    }
                },
                label = "lyric_column_scroll"
            ) { renderedActiveIndex ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    repeat(5) { row ->
                        val index = renderedActiveIndex - 2 + row
                        val line = lines.getOrNull(index)
                        if (line == null) {
                            LyricSpacer()
                            return@repeat
                        }
                        val isCurrent = index == activeIndex
                        val emphasis by animateFloatAsState(
                            targetValue = if (isCurrent) 1f else 0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            label = "lyric_emphasis"
                        )
                        val scale = 0.98f + 0.16f * emphasis
                        val nextTimeMs = lines.getOrNull(index + 1)?.timeMs ?: line.timeMs + 3000L
                        LyricText(
                            line = line,
                            nextTimeMs = nextTimeMs,
                            positionMs = lyricPosition,
                            isCurrent = isCurrent,
                            text = buildLyricText(line, nextTimeMs, lyricPosition, isCurrent),
                            fontSize = 12.sp,
                            fontWeight = if (isCurrent) FontWeight.Medium else FontWeight.Normal,
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun LyricSpacer() {
    Spacer(modifier = Modifier.height(18.dp))
}

@Composable
internal fun LyricText(
    line: LyricLine,
    nextTimeMs: Long,
    positionMs: Long,
    isCurrent: Boolean,
    text: AnnotatedString,
    fontSize: TextUnit,
    fontWeight: FontWeight,
    modifier: Modifier = Modifier,
) {
    val pendingColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
    val activeColor = MaterialTheme.colorScheme.primary
    val duration = (nextTimeMs - line.timeMs).coerceAtLeast(1L)
    val progress = when {
        !isCurrent || positionMs <= line.timeMs -> 0f
        positionMs >= nextTimeMs -> 1f
        else -> ((positionMs - line.timeMs).toFloat() / duration).coerceIn(0f, 1f)
    }
    val lyricBrush = when {
        progress <= 0f -> Brush.horizontalGradient(listOf(pendingColor, pendingColor))
        progress >= 1f -> Brush.horizontalGradient(listOf(activeColor, activeColor))
        else -> Brush.horizontalGradient(
            colorStops = arrayOf(
                0f to activeColor,
                progress to activeColor,
                progress to pendingColor,
                1f to pendingColor
            )
        )
    }

    Text(
        text = if (line.words.isNotEmpty()) text else AnnotatedString(line.text),
        style = TextStyle(
            brush = if (line.words.isNotEmpty()) null else lyricBrush,
            shadow = if (progress > 0f) Shadow(
                activeColor.copy(alpha = 0.65f),
                blurRadius = 7f
            ) else null
        ),
        fontSize = fontSize,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        fontWeight = fontWeight,
        modifier = modifier
    )
}

@Composable
internal fun buildLyricText(
    line: LyricLine,
    nextTimeMs: Long,
    positionMs: Long,
    isCurrent: Boolean,
): AnnotatedString {
    val primary = MaterialTheme.colorScheme.primary
    val idle = MaterialTheme.colorScheme.onSurfaceVariant
    val pendingColor = idle.copy(alpha = 0.72f)
    val activeColor = primary.copy(alpha = 1f)
    val highlightShadow = Shadow(
        primary.copy(alpha = 0.65f),
        blurRadius = 7f
    )

    val tokens = if (line.words.isNotEmpty()) {
        line.words
    } else {
        val parts = splitLyricText(line.text)
        val duration = (nextTimeMs - line.timeMs).coerceAtLeast(1L)
        val partDuration = (duration / parts.size.coerceAtLeast(1)).coerceAtLeast(1L)
        parts.mapIndexed { index, part ->
            LyricWord(
                startMs = line.timeMs + index * partDuration,
                durationMs = if (index == parts.lastIndex) {
                    (nextTimeMs - (line.timeMs + index * partDuration)).coerceAtLeast(1L)
                } else partDuration,
                text = part
            )
        }
    }

    return buildAnnotatedString {
        tokens.forEachIndexed { tokenIndex, token ->
            val tokenEndMs = if (tokenIndex + 1 < tokens.size) {
                tokens[tokenIndex + 1].startMs.coerceAtLeast(token.startMs + 1L)
            } else {
                token.startMs + token.durationMs.coerceAtLeast(1L)
            }
            val progress = if (isCurrent) {
                ((positionMs - token.startMs).toFloat() / (tokenEndMs - token.startMs)).coerceIn(0f, 1f)
            } else 0f
            val split = (token.text.length * progress).toInt()
            withStyle(
                SpanStyle(
                    color = activeColor,
                    shadow = if (split > 0) highlightShadow else null
                )
            ) { append(token.text.take(split)) }
            withStyle(SpanStyle(color = pendingColor)) { append(token.text.drop(split)) }
        }
    }
}

internal fun splitLyricText(text: String): List<String> {
    if (text.isBlank()) return listOf(text)
    val result = mutableListOf<String>()
    var index = 0
    while (index < text.length) {
        val start = index
        val isSpace = text[index].isWhitespace()
        if (isSpace) {
            while (index < text.length && text[index].isWhitespace()) index++
        } else if (text[index].isLetterOrDigit() && text[index].code < 128) {
            while (index < text.length && text[index].isLetterOrDigit() && text[index].code < 128) index++
        } else {
            index++
        }
        result += text.substring(start, index)
    }
    return result
}

