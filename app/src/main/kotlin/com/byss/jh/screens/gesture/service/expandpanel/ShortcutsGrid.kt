package com.byss.jh.screens.gesture.service.expandpanel

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.byss.jh.R
import com.byss.jh.data.app.AppRepository
import org.koin.compose.koinInject

@Composable
fun ShortcutsGrid(
    shortcuts: List<String?>,
    onShortcutSet: (index: Int, packageName: String?) -> Unit,
    onLaunchApp: (String, Int) -> Unit,
    appRepository: AppRepository = koinInject()
) {
    // 从缓存仓库获取应用列表，实现图标预加载
    val apps by appRepository.appsFlow.collectAsState()
    
    // 构建包名到图标的映射，避免重复查询
    val iconMap = remember(apps) {
        apps.associateBy({ it.packageName }, { it })
    }

    Column {
        Text(
            text = stringResource(R.string.expand_panel_shortcuts_title),
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        repeat(2) { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                repeat(4) { col ->
                    val index = row * 4 + col
                    val packageName = shortcuts.getOrNull(index)
                    ShortcutItem(
                        packageName = packageName,
                        appInfo = packageName?.let { iconMap[it] },
                        onClick = {
                            if (packageName != null) {
                                onLaunchApp(packageName, index)
                            } else {
                                onShortcutSet(index, null)
                            }
                        },
                        onLongClick = {
                            onShortcutSet(index, null)
                        }
                    )
                }
            }
            if (row == 0) {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ShortcutItem(
    packageName: String?,
    appInfo: com.byss.jh.data.app.AppInfo?,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current

    // 从缓存数据异步加载图标，避免阻塞主线程
    val appIcon = remember(packageName, appInfo) {
        if (packageName != null) {
            try {
                context.packageManager.getApplicationIcon(packageName)
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }
    }

    // 长按按压状态管理
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "press_scale"
    )

    Box(
        modifier = Modifier
            .size(56.dp)
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    packageName != null -> androidx.compose.ui.graphics.Color.Transparent
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        // 按下时触发按压效果
                        isPressed = true
                        tryAwaitRelease()
                        // 释放时取消按压效果
                        isPressed = false
                    },
                    onTap = { onClick() },
                    onLongPress = {
                        isPressed = false
                        onLongClick()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        if (appIcon != null) {
            Image(
                painter = BitmapPainter(appIcon.toBitmap().asImageBitmap()),
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(R.string.expand_panel_add_shortcut_desc),
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
