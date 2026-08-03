package com.edgegesture.evilgodxu.screens.gesture.service.taskpanel

import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
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
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var singleTapJob by remember { androidx.compose.runtime.mutableStateOf<Job?>(null) }
    val listState = rememberLazyListState()
    val thresholdPx = with(LocalDensity.current) { 48.dp.toPx() }
    val touchSlopPx = with(LocalDensity.current) { 8.dp.toPx() }
    val hintTextColor = if (isSystemInDarkTheme()) Color.White else Color.LightGray
    val virtualCenter = Int.MAX_VALUE / 2
    var virtualSelected by remember(apps, selectedPackageName) {
        mutableIntStateOf(virtualCenter - (virtualCenter % apps.size.coerceAtLeast(1)) + selected)
    }
    var settleRequest by remember { mutableIntStateOf(0) }

    LaunchedEffect(apps.size, selectedPackageName) {
        if (apps.isNotEmpty()) {
            selected = apps.indexOfFirst { it.packageName == selectedPackageName }.coerceAtLeast(0)
            virtualSelected = virtualCenter - (virtualCenter % apps.size) + selected
            listState.scrollToItem(virtualSelected)
        }
    }
    LaunchedEffect(virtualSelected, apps.size, settleRequest) {
        if (apps.isNotEmpty()) {
            listState.animateScrollToItem(virtualSelected)
            val target = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == virtualSelected }
            val viewportCenter = (listState.layoutInfo.viewportStartOffset + listState.layoutInfo.viewportEndOffset) / 2
            if (target != null) {
                listState.animateScrollToItem(
                    virtualSelected,
                    target.offset - viewportCenter + target.size / 2
                )
            }
        }
    }

    fun moveByDrag(total: Float) {
        if (apps.isNotEmpty() && abs(total) >= thresholdPx) {
            virtualSelected += if (total < 0) 1 else -1
            selected = Math.floorMod(virtualSelected, apps.size)
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
                }
                if (hasMoved && abs(total) >= thresholdPx) {
                    moveByDrag(total)
                } else if (hasMoved) {
                    settleRequest++
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
                // 应用图标：在面板内居中显示
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    LazyRow(
                        state = listState,
                        modifier = Modifier.taskGesture(),
                        contentPadding = PaddingValues(horizontal = 48.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        userScrollEnabled = false
                    ) {
                        items(count = if (apps.isEmpty()) 0 else Int.MAX_VALUE) { index ->
                            val app = apps[index % apps.size]
                            val targetScale = if (index == virtualSelected) 1f else 0.8f
                            val scale by animateFloatAsState(
                                targetValue = targetScale,
                                animationSpec = tween(durationMillis = 180),
                                label = "task_panel_icon_scale"
                            )
                            AndroidView(
                                factory = { ImageView(it) },
                                update = { it.setImageDrawable(app.icon) },
                                modifier = Modifier
                                    .padding(horizontal = 8.dp)
                                    .size(80.dp)
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                    }
                            )
                        }
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
