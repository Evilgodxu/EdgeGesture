package com.edgegesture.evilgodxu.screens.gesture.service.taskpanel

import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.edgegesture.evilgodxu.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

 data class TaskPanelApp(val packageName: String, val label: String, val icon: Drawable)

@Composable
fun TaskPanelOverlay(
    apps: List<TaskPanelApp>,
    selectedPackageName: String?,
    hasUsageAccess: Boolean,
    onLaunch: (String) -> Unit,
    onLaunchInFreeform: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selected by remember(apps, selectedPackageName) {
        mutableIntStateOf(apps.indexOfFirst { it.packageName == selectedPackageName }.coerceAtLeast(0))
    }
    val scope = rememberCoroutineScope()
    var singleTapJob by remember { androidx.compose.runtime.mutableStateOf<Job?>(null) }
    val thresholdPx = with(LocalDensity.current) { 48.dp.toPx() }
    val touchSlopPx = with(LocalDensity.current) { 8.dp.toPx() }
    val hintTextColor = if (isSystemInDarkTheme()) Color.White else Color.LightGray
    val itemStepPx = with(LocalDensity.current) { 104.dp.toPx() }
    val carouselAnim = remember { Animatable(0f) }

    LaunchedEffect(apps.size, selectedPackageName) {
        if (apps.isNotEmpty()) {
            selected = apps.indexOfFirst { it.packageName == selectedPackageName }.coerceAtLeast(0)
        }
    }

    fun Modifier.taskGesture(): Modifier = pointerInput(apps) {
        awaitPointerEventScope {
            while (true) {
                var total = 0f
                var up = false
                var hasMoved = false
                while (!up) {
                    val event = awaitPointerEvent()
                    event.changes.forEach { change ->
                        total += change.position.x - change.previousPosition.x
                        hasMoved = hasMoved || abs(total) >= touchSlopPx
                        if (!change.pressed) up = true
                        change.consume()
                    }
                    if (hasMoved) {
                        scope.launch {
                            carouselAnim.snapTo((total / itemStepPx).coerceIn(-1f, 1f))
                        }
                    }
                }
                if (hasMoved && abs(total) >= thresholdPx) {
                    val target = if (total < 0) -1f else 1f
                    scope.launch {
                        carouselAnim.animateTo(target, animationSpec = tween(durationMillis = 200))
                        selected = if (total < 0)
                            Math.floorMod(selected + 1, apps.size)
                        else
                            Math.floorMod(selected - 1, apps.size)
                        carouselAnim.snapTo(0f)
                    }
                } else if (hasMoved) {
                    scope.launch {
                        carouselAnim.animateTo(0f, animationSpec = tween(durationMillis = 200))
                    }
                } else if (apps.isNotEmpty()) {
                    val packageName = apps[selected].packageName
                    if (singleTapJob?.isActive == true) {
                        singleTapJob?.cancel()
                        singleTapJob = null
                        onLaunchInFreeform(packageName)
                        onDismiss()
                    } else {
                        singleTapJob = scope.launch {
                            delay(300L)
                            onLaunch(packageName)
                            singleTapJob = null
                        }
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = true,
            modifier = Modifier.padding(bottom = 48.dp),
            enter = fadeIn() + scaleIn(initialScale = 0.9f)
        ) {
            Column(
                modifier = Modifier
                    .width(320.dp)
                    .background(Color.Transparent)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { }
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 应用图标轮播
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp)
                        .taskGesture(),
                    contentAlignment = Alignment.Center
                ) {
                    if (apps.isNotEmpty()) {
                        val animOffset = carouselAnim.value
                        val prevIndex = Math.floorMod(selected - 1, apps.size)
                        val nextIndex = Math.floorMod(selected + 1, apps.size)

                        // 前一个图标
                        CarouselIcon(
                            app = apps[prevIndex],
                            offset = (-itemStepPx + animOffset * itemStepPx).roundToInt(),
                            scale = scaleForDistance(abs(-1f - animOffset))
                        )
                        // 当前选中图标
                        CarouselIcon(
                            app = apps[selected],
                            offset = (animOffset * itemStepPx).roundToInt(),
                            scale = scaleForDistance(abs(animOffset))
                        )
                        // 后一个图标
                        CarouselIcon(
                            app = apps[nextIndex],
                            offset = (itemStepPx + animOffset * itemStepPx).roundToInt(),
                            scale = scaleForDistance(abs(1f - animOffset))
                        )
                    }
                }
                // 控制区域
                Box(
                    Modifier.fillMaxWidth().height(320.dp).taskGesture(),
                    contentAlignment = Alignment.Center
                ) {
                    if (apps.isEmpty()) {
                        Text(
                            if (hasUsageAccess) stringResource(R.string.task_panel_empty)
                            else stringResource(R.string.task_panel_usage_access_required),
                            color = Color.White
                        )
                    }
                }
                // 提示信息
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onDismiss
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.task_panel_single_tap_hint),
                        color = hintTextColor,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stringResource(R.string.task_panel_double_tap_hint),
                        color = hintTextColor,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}

private fun scaleForDistance(distance: Float): Float {
    return 1f - 0.2f * distance.coerceIn(0f, 1f)
}

@Composable
private fun CarouselIcon(
    app: TaskPanelApp,
    offset: Int,
    scale: Float
) {
    val animScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = tween(durationMillis = 180),
        label = "carousel_icon_scale"
    )
    AndroidView(
        factory = { ImageView(it) },
        update = { it.setImageDrawable(app.icon) },
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .size(80.dp)
            .graphicsLayer {
                translationX = offset.toFloat()
                scaleX = animScale
                scaleY = animScale
            }
    )
}