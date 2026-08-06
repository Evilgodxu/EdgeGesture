package com.edgegesture.evilgodxu.screens.gesture.service.taskpanel

import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    currentPackageName: String?,
    onLaunch: (String) -> Unit,
    onLaunchInFreeform: (String) -> Unit,
    onSwipeAway: (String) -> Unit,
    onDismiss: () -> Unit
) {
    // 使用可变的快照列表，支持动态删除
    val appList = remember(apps) { mutableStateListOf(*apps.toTypedArray()) }
    var selectedIndex by remember(apps, selectedPackageName) {
        mutableIntStateOf(appList.indexOfFirst { it.packageName == selectedPackageName }.coerceAtLeast(0))
    }
    // 正在执行删除动画的包名集合
    val deletingPackages = remember { mutableStateOf(setOf<String>()) }
    val scope = rememberCoroutineScope()
    var singleTapJob by remember { mutableStateOf<Job?>(null) }
    val thresholdPx = with(LocalDensity.current) { 48.dp.toPx() }
    val touchSlopPx = with(LocalDensity.current) { 8.dp.toPx() }
    val hintTextColor = Color.White
    val hintBackground = Color.Black.copy(alpha = 0.45f)
    val controlColor = Color.Black.copy(alpha = 0.9f)
    val itemStepPx = with(LocalDensity.current) { 104.dp.toPx() }
    val carouselAnim = remember { Animatable(0f) }
    // 是否正在执行清理（批量删除）动画
    var isCleaning by remember { mutableStateOf(false) }

    LaunchedEffect(apps.size, selectedPackageName) {
        if (appList.isNotEmpty()) {
            selectedIndex = appList.indexOfFirst { it.packageName == selectedPackageName }.coerceAtLeast(0)
        }
    }

    // 从列表中移除指定包名，并调整选中索引
    fun removeApp(packageName: String) {
        val removeIdx = appList.indexOfFirst { it.packageName == packageName }
        if (removeIdx < 0) return
        appList.removeAt(removeIdx)
        if (appList.isEmpty()) {
            selectedIndex = 0
        } else if (selectedIndex >= appList.size) {
            selectedIndex = appList.size - 1
        } else if (removeIdx < selectedIndex) {
            selectedIndex--
        }
    }

    fun Modifier.taskGesture(): Modifier = pointerInput(appList.size, deletingPackages.value, isCleaning) {
        awaitPointerEventScope {
            while (true) {
                var totalX = 0f
                var totalY = 0f
                var up = false
                var hasMoved = false
                while (!up) {
                    val event = awaitPointerEvent()
                    event.changes.forEach { change ->
                        totalX += change.position.x - change.previousPosition.x
                        totalY += change.position.y - change.previousPosition.y
                        hasMoved = hasMoved || abs(totalX) >= touchSlopPx || abs(totalY) >= touchSlopPx
                        if (!change.pressed) up = true
                        change.consume()
                    }
                    if (hasMoved) {
                        scope.launch {
                            carouselAnim.snapTo(
                                if (abs(totalY) > abs(totalX))
                                    (totalY / itemStepPx).coerceIn(-1f, 1f)
                                else
                                    (totalX / itemStepPx).coerceIn(-1f, 1f)
                            )
                        }
                    }
                }
                if (hasMoved) {
                    if (abs(totalY) > abs(totalX) && abs(totalY) >= thresholdPx && totalY < 0 && appList.isNotEmpty() && selectedIndex < appList.size) {
                        // 上滑删除
                        val pkg = appList[selectedIndex].packageName
                        if (pkg !in deletingPackages.value) {
                            deletingPackages.value = deletingPackages.value + pkg
                            scope.launch {
                                delay(300)
                                removeApp(pkg)
                                deletingPackages.value = deletingPackages.value - pkg
                                onSwipeAway(pkg)
                            }
                        }
                        scope.launch { carouselAnim.animateTo(0f, animationSpec = tween(200)) }
                    } else if (abs(totalX) >= thresholdPx && appList.size > 1) {
                        // 水平滑动切换
                        val target = if (totalX < 0) -1f else 1f
                        scope.launch {
                            carouselAnim.animateTo(target, animationSpec = tween(durationMillis = 200))
                            selectedIndex = if (totalX < 0)
                                Math.floorMod(selectedIndex + 1, appList.size)
                            else
                                Math.floorMod(selectedIndex - 1, appList.size)
                            carouselAnim.snapTo(0f)
                        }
                    } else {
                        scope.launch {
                            carouselAnim.animateTo(0f, animationSpec = tween(durationMillis = 200))
                        }
                    }
                } else if (!isCleaning && appList.isNotEmpty() && selectedIndex < appList.size) {
                    // 点击处理
                    val packageName = appList[selectedIndex].packageName
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
                } else if (appList.isEmpty()) {
                    onDismiss()
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
        Column(
            modifier = Modifier
                .width(320.dp)
                .background(Color.Transparent)
                .padding(bottom = 48.dp)
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
                if (appList.isNotEmpty()) {
                    if (appList.size == 1) {
                        // 只有一个应用时，只显示中间图标，不重复
                        CarouselIcon(
                            app = appList[0],
                            offset = 0,
                            scale = 1f,
                            isDeleting = appList[0].packageName in deletingPackages.value
                        )
                    } else {
                        val animOffset = carouselAnim.value
                        val prevIndex = Math.floorMod(selectedIndex - 1, appList.size)
                        val nextIndex = Math.floorMod(selectedIndex + 1, appList.size)

                        CarouselIcon(
                            app = appList[prevIndex],
                            offset = (-itemStepPx + animOffset * itemStepPx).roundToInt(),
                            scale = scaleForDistance(abs(-1f - animOffset)),
                            isDeleting = appList[prevIndex].packageName in deletingPackages.value
                        )
                        CarouselIcon(
                            app = appList[selectedIndex],
                            offset = (animOffset * itemStepPx).roundToInt(),
                            scale = scaleForDistance(abs(animOffset)),
                            isDeleting = appList[selectedIndex].packageName in deletingPackages.value
                        )
                        // 当只有2个应用时，prev和next指向同一个，跳过重复
                        if (prevIndex != nextIndex) {
                            CarouselIcon(
                                app = appList[nextIndex],
                                offset = (itemStepPx + animOffset * itemStepPx).roundToInt(),
                                scale = scaleForDistance(abs(1f - animOffset)),
                                isDeleting = appList[nextIndex].packageName in deletingPackages.value
                            )
                        }
                    }
                }
            }

            // 控制区域
            Box(
                Modifier.fillMaxWidth().height(300.dp).taskGesture(),
                contentAlignment = Alignment.Center
            ) {
                if (appList.isEmpty()) {
                    Text(
                        stringResource(R.string.task_panel_empty),
                        color = Color.White
                    )
                }
            }
            // 清理按钮，水平居中，位于控制区域与提示信息之间
            val cleanProgress = remember { Animatable(0f) }
            val cleanDurationMs = remember { mutableIntStateOf(0) }
            LaunchedEffect(isCleaning) {
                if (isCleaning) {
                    cleanProgress.snapTo(0f)
                    cleanProgress.animateTo(1f, animationSpec = tween(durationMillis = cleanDurationMs.intValue))
                } else {
                    cleanProgress.snapTo(0f)
                }
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = Color.White.copy(alpha = 0.24f),
                        shape = CircleShape
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (appList.isEmpty()) { onDismiss(); return@clickable }
                        if (isCleaning) return@clickable
                        val toDelete = if (currentPackageName != null) {
                            appList.filter { it.packageName != currentPackageName }
                        } else {
                            appList
                        }
                        cleanDurationMs.intValue = toDelete.size * 100 + 300
                        isCleaning = true
                        scope.launch {
                            for (app in toDelete) {
                                deletingPackages.value = deletingPackages.value + app.packageName
                                delay(100)
                            }
                            delay(300)
                            toDelete.forEach { app ->
                                removeApp(app.packageName)
                                onSwipeAway(app.packageName)
                            }
                            deletingPackages.value = emptySet()
                            isCleaning = false
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                // 进度环
                val progressRingColor = Color.White
                Canvas(modifier = Modifier.size(44.dp)) {
                    val sweepAngle = cleanProgress.value * 360f
                    val strokeWidth = 3.dp.toPx()
                    drawArc(
                        color = progressRingColor,
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round),
                        topLeft = androidx.compose.ui.geometry.Offset(strokeWidth / 2, strokeWidth / 2),
                        size = androidx.compose.ui.geometry.Size(size.width - strokeWidth, size.height - strokeWidth)
                    )
                }
                Text(
                    text = "✕",
                    color = Color.White,
                    fontSize = 18.sp
                )
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
    scale: Float,
    isDeleting: Boolean
) {
    val animScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = tween(durationMillis = 180),
        label = "carousel_icon_scale"
    )
    val deleteAnim = remember { Animatable(0f) }

    LaunchedEffect(isDeleting) {
        if (isDeleting) {
            deleteAnim.animateTo(1f, animationSpec = tween(durationMillis = 300))
        } else {
            deleteAnim.snapTo(0f)
        }
    }

    val deleteOffset = if (isDeleting) {
        with(LocalDensity.current) { -200.dp.toPx() * deleteAnim.value }
    } else 0f
    val deleteAlpha = (1f - deleteAnim.value).coerceIn(0f, 1f)

    AndroidView(
        factory = { ImageView(it) },
        update = { it.setImageDrawable(app.icon) },
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .size(80.dp)
            .graphicsLayer {
                translationX = offset.toFloat()
                translationY = deleteOffset
                scaleX = animScale
                scaleY = animScale
                alpha = deleteAlpha
            }
    )
}