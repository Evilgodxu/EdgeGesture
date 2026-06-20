package com.byss.jh.screens.gesture.service.expandpanel

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.core.graphics.drawable.toBitmap
import com.byss.jh.R
import com.byss.jh.data.app.AppRepository
import org.koin.compose.koinInject

@Composable
fun ShortcutsGrid(
    shortcuts: List<String?>,
    freeformFlags: List<Boolean>,
    onShortcutSet: (index: Int, packageName: String?) -> Unit,
    onLaunchApp: (String, Int) -> Unit,
    onFreeformToggle: (Int, Boolean) -> Unit,
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
                        isFreeform = freeformFlags.getOrElse(index) { false },
                        onClick = {
                            if (packageName != null) {
                                onLaunchApp(packageName, index)
                            } else {
                                onShortcutSet(index, null)
                            }
                        },
                        onLongClick = {
                            onShortcutSet(index, null)
                        },
                        onFreeformToggle = {
                            onFreeformToggle(index, it)
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
    isFreeform: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onFreeformToggle: (Boolean) -> Unit
) {
    val context = LocalContext.current

    // 优先使用扫描时缓存的图标，失败则回退到 PackageManager 实时查询
    val appIconBitmap by androidx.compose.runtime.produceState<android.graphics.Bitmap?>(
        initialValue = null,
        key1 = packageName,
        key2 = appInfo?.iconPath
    ) {
        value = if (packageName != null) {
            loadShortcutIconBitmap(context, packageName, appInfo?.iconPath)
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
        if (appIconBitmap != null) {
            Image(
                painter = BitmapPainter(appIconBitmap!!.asImageBitmap()),
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

        // 已设置应用的快捷方式右上角显示“小”字开关
        if (packageName != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp)
                    .size(18.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (isFreeform) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
                        }
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onFreeformToggle(!isFreeform) }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "小",
                    fontSize = 9.sp,
                    color = if (isFreeform) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

// 加载快捷方式图标：优先读取缓存文件，失败则回退到 PackageManager
private fun loadShortcutIconBitmap(
    context: android.content.Context,
    packageName: String,
    iconPath: String?
): android.graphics.Bitmap? {
    if (!iconPath.isNullOrBlank()) {
        runCatching {
            android.graphics.BitmapFactory.decodeFile(iconPath)
        }.getOrNull()?.let { return it }
    }
    return runCatching {
        context.packageManager.getApplicationIcon(packageName).toBitmap()
    }.getOrNull()
}
