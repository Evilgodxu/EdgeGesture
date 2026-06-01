package com.byss.jh.ui.gesture.service

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.media.AudioManager
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.byss.jh.R
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.byss.jh.data.gesture.ExpandPanelShortcutsState
import com.byss.jh.ui.settings.ThemeMode
import com.byss.jh.ui.theme.DarkColorScheme
import com.byss.jh.ui.theme.LightColorScheme
import kotlinx.coroutines.flow.Flow

// 扩展面板悬浮窗管理器
class ExpandPanelViewManager(
    private val context: Context,
    private val shortcutsFlow: Flow<ExpandPanelShortcutsState>,
    private val themeModeFlow: Flow<ThemeMode>,
    private val onShortcutSet: (index: Int, packageName: String?) -> Unit,
    private val onDismiss: () -> Unit
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var composeView: ComposeView? = null

    private val lifecycleOwner = object : LifecycleOwner {
        private val lifecycleRegistry = LifecycleRegistry(this)
        override val lifecycle: Lifecycle get() = lifecycleRegistry
        fun handleLifecycleEvent(event: Lifecycle.Event) = lifecycleRegistry.handleLifecycleEvent(event)
    }

    private val viewModelStoreOwner = object : ViewModelStoreOwner {
        private val store = ViewModelStore()
        override val viewModelStore: ViewModelStore get() = store
    }

    private val savedStateRegistryOwner = object : SavedStateRegistryOwner {
        private val controller = SavedStateRegistryController.create(this)
        override val savedStateRegistry: SavedStateRegistry get() = controller.savedStateRegistry
        override val lifecycle: Lifecycle get() = lifecycleOwner.lifecycle
        fun performAttach() = controller.performAttach()
        fun performRestore() = controller.performRestore(null)
    }

    // 显示扩展面板悬浮窗，需要 SYSTEM_ALERT_WINDOW 权限和 WRITE_SETTINGS 权限
    @SuppressLint("ClickableViewAccessibility")
    fun show(): Boolean {
        if (composeView != null) return true

        // 检查是否有修改系统设置权限（用于调节亮度/音量）
        if (!Settings.System.canWrite(context)) {
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                data = android.net.Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            return false
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                    WindowManager.LayoutParams.FLAG_DIM_BEHIND,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = 0
            dimAmount = 0.3f
        }

        val view = ComposeView(context).apply {
            setContent {
                ExpandPanelOverlay(
                    shortcutsFlow = shortcutsFlow,
                    themeModeFlow = themeModeFlow,
                    onShortcutSet = onShortcutSet,
                    onDismiss = { dismiss() }
                )
            }
        }

        savedStateRegistryOwner.performAttach()
        savedStateRegistryOwner.performRestore()
        view.setViewTreeLifecycleOwner(lifecycleOwner)
        view.setViewTreeViewModelStoreOwner(viewModelStoreOwner)
        view.setViewTreeSavedStateRegistryOwner(savedStateRegistryOwner)

        view.setOnKeyListener { _, keyCode, event ->
            if (keyCode == android.view.KeyEvent.KEYCODE_BACK && event.action == android.view.KeyEvent.ACTION_UP) {
                dismiss()
                true
            } else {
                false
            }
        }
        view.isFocusableInTouchMode = true
        view.requestFocus()

        composeView = view
        windowManager.addView(view, params)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        return true
    }

    // 关闭扩展面板并清理资源
    fun dismiss() {
        val view = composeView ?: return
        // 按正确顺序触发生命周期事件
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        try {
            if (view.windowToken != null) {
                windowManager.removeView(view)
            }
        } catch (_: Exception) {}
        composeView = null
        onDismiss()
    }

    companion object {
        private const val TAG = "ExpandPanelViewManager"
    }
}

@Composable
private fun ExpandPanelOverlay(
    shortcutsFlow: Flow<ExpandPanelShortcutsState>,
    themeModeFlow: Flow<ThemeMode>,
    onShortcutSet: (index: Int, packageName: String?) -> Unit,
    onDismiss: () -> Unit
) {
    var currentShortcuts by remember { mutableStateOf(ExpandPanelShortcutsState()) }
    var themeMode by remember { mutableStateOf(ThemeMode.SYSTEM) }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isSystemInDarkTheme = (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    LaunchedEffect(shortcutsFlow) {
        shortcutsFlow.collect { state ->
            currentShortcuts = state
        }
    }

    LaunchedEffect(themeModeFlow) {
        themeModeFlow.collect { mode ->
            themeMode = mode
        }
    }

    val isDarkTheme = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme
    }

    val colorScheme = if (isDarkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(colorScheme = colorScheme) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = if (isLandscape) {
                    Modifier
                        .widthIn(max = 480.dp)
                        .fillMaxWidth(0.7f)
                        .padding(horizontal = 12.dp, vertical = 16.dp)
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null,
                            onClick = { /* 阻止点击穿透 */ }
                        )
                } else {
                    Modifier
                        .fillMaxWidth(0.92f)
                        .padding(horizontal = 12.dp, vertical = 16.dp)
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null,
                            onClick = { /* 阻止点击穿透 */ }
                        )
                },
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                shadowElevation = 8.dp
            ) {
                ExpandPanelContent(
                    shortcuts = currentShortcuts.shortcuts,
                    onShortcutSet = onShortcutSet,
                    onDismiss = onDismiss
                )
            }
        }
    }
}

@Composable
private fun ExpandPanelContent(
    shortcuts: List<String?>,
    onShortcutSet: (index: Int, packageName: String?) -> Unit,
    onDismiss: () -> Unit
) {
    var isAppPickerMode by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableIntStateOf(-1) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        if (isAppPickerMode) {
            AppPickerScreen(
                onAppSelected = { packageName ->
                    onShortcutSet(selectedIndex, packageName)
                    isAppPickerMode = false
                    selectedIndex = -1
                },
                onCancel = {
                    isAppPickerMode = false
                    selectedIndex = -1
                }
            )
        } else {
            VerticalSlidersSection()

            Spacer(modifier = Modifier.height(12.dp))

            ShortcutsGrid(
                shortcuts = shortcuts,
                onShortcutSet = { index, packageName ->
                    if (packageName == null) {
                        selectedIndex = index
                        isAppPickerMode = true
                    } else {
                        onShortcutSet(index, packageName)
                    }
                },
                onLaunchApp = { packageName, index ->
                    val launched = launchApp(context, packageName)
                    if (launched) {
                        onDismiss()
                    } else {
                        selectedIndex = index
                        isAppPickerMode = true
                    }
                }
            )
        }
    }
}

@Composable
private fun VerticalSlidersSection() {
    val context = LocalContext.current
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    var brightness by remember { mutableFloatStateOf(getCurrentBrightnessPercent(context)) }

    val maxAlarmVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
    var alarmVolume by remember {
        mutableFloatStateOf(audioManager.getStreamVolume(AudioManager.STREAM_ALARM).toFloat() / maxAlarmVolume)
    }

    val maxRingVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
    var ringVolume by remember {
        mutableFloatStateOf(audioManager.getStreamVolume(AudioManager.STREAM_RING).toFloat() / maxRingVolume)
    }

    val maxMusicVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    var mediaVolume by remember {
        mutableFloatStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxMusicVolume)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        VerticalSliderItem(
            label = stringResource(R.string.expand_panel_brightness),
            icon = androidx.compose.material.icons.Icons.Default.LightMode,
            value = brightness,
            onValueChange = {
                brightness = it
                setBrightness(context, it)
            },
            onValueChangeFinished = {}
        )

        VerticalSliderItem(
            label = stringResource(R.string.expand_panel_alarm_volume),
            icon = androidx.compose.material.icons.Icons.Default.Alarm,
            value = alarmVolume,
            onValueChange = {
                alarmVolume = it
                val newVolume = (it * maxAlarmVolume).toInt()
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, newVolume, 0)
            },
            onValueChangeFinished = {}
        )

        VerticalSliderItem(
            label = stringResource(R.string.expand_panel_ring_volume),
            icon = androidx.compose.material.icons.Icons.Default.Notifications,
            value = ringVolume,
            onValueChange = {
                ringVolume = it
                val newVolume = (it * maxRingVolume).toInt()
                audioManager.setStreamVolume(AudioManager.STREAM_RING, newVolume, 0)
            },
            onValueChangeFinished = {}
        )

        VerticalSliderItem(
            label = stringResource(R.string.expand_panel_media_volume),
            icon = androidx.compose.material.icons.Icons.Default.MusicNote,
            value = mediaVolume,
            onValueChange = {
                mediaVolume = it
                val newVolume = (it * maxMusicVolume).toInt()
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
            },
            onValueChangeFinished = {}
        )
    }
}

@Composable
private fun VerticalSliderItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit
) {
    val density = LocalDensity.current
    val sliderHeightPx = remember(density) { with(density) { 140.dp.toPx() } }
    var currentValue by remember { mutableFloatStateOf(value) }
    var isDragging by remember { mutableStateOf(false) }
    var showValue by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(value) {
        currentValue = value
    }

    val colorScheme = MaterialTheme.colorScheme

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val fillColor = colorScheme.primary
        val backgroundColor = colorScheme.surfaceVariant

        Box(
            modifier = Modifier
                .width(56.dp)
                .height(140.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(backgroundColor)
                .clickable {
                    showValue = true
                    scope.launch {
                        delay(1000)
                        showValue = false
                    }
                },
            contentAlignment = Alignment.BottomCenter
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val fillHeight = size.height * currentValue

                drawRoundRect(
                    color = fillColor,
                    topLeft = Offset(0f, size.height - fillHeight),
                    size = Size(size.width, fillHeight),
                    cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragStart = { offset ->
                                isDragging = true
                                showValue = true
                                val newValue = (1f - (offset.y / sliderHeightPx)).coerceIn(0f, 1f)
                                currentValue = newValue
                                onValueChange(newValue)
                            },
                            onVerticalDrag = { change, _ ->
                                change.consume()
                                val position = change.position
                                val newValue = (1f - (position.y / sliderHeightPx)).coerceIn(0f, 1f)
                                currentValue = newValue
                                onValueChange(newValue)
                            },
                            onDragEnd = {
                                isDragging = false
                                showValue = false
                            }
                        )
                    }
            )

            // 计算数值文本和图标的颜色，基于它们在滑块上的实际位置
            // 中心点位置对应的颜色（数值文本在中心）
            val centerFillRatio = currentValue.coerceIn(0f, 1f)
            val centerTextColor = if (centerFillRatio > 0.5f) {
                androidx.compose.ui.graphics.Color.White
            } else {
                fillColor
            }

            // 底部位置对应的颜色（图标在底部，约占总高度的 15%）
            val iconPositionRatio = (currentValue - 0.15f).coerceIn(0f, 1f)
            val iconTintColor = if (iconPositionRatio > 0f) {
                androidx.compose.ui.graphics.Color.White
            } else {
                fillColor.copy(alpha = 0.6f)
            }

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (showValue) {
                    Text(
                        text = "${(currentValue * 100).toInt()}",
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = centerTextColor
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .align(Alignment.BottomCenter),
                contentAlignment = Alignment.BottomCenter
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = iconTintColor
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ShortcutsGrid(
    shortcuts: List<String?>,
    onShortcutSet: (index: Int, packageName: String?) -> Unit,
    onLaunchApp: (String, Int) -> Unit
) {
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ShortcutItem(
    packageName: String?,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    var appIcon by remember { mutableStateOf<android.graphics.drawable.Drawable?>(null) }

    LaunchedEffect(packageName) {
        appIcon = if (packageName != null) {
            try {
                context.packageManager.getApplicationIcon(packageName)
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }
    }

    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (packageName != null) {
                    androidx.compose.ui.graphics.Color.Transparent
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (appIcon != null) {
            Image(
                painter = BitmapPainter(
                    appIcon!!.toBitmap().asImageBitmap()
                ),
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

@Composable
private fun AppPickerScreen(
    onAppSelected: (String) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var apps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var showSystemApps by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        apps = loadInstalledApps(context)
    }

    val filteredApps = apps.filter { app ->
        val matchesSearch = app.label.contains(searchQuery, ignoreCase = true) ||
                app.packageName.contains(searchQuery, ignoreCase = true)
        val matchesSystemFilter = showSystemApps || !app.isSystemApp
        matchesSearch && matchesSystemFilter
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.expand_panel_app_picker_title),
                style = MaterialTheme.typography.titleMedium,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.expand_panel_cancel), color = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text(stringResource(R.string.expand_panel_search_hint), color = MaterialTheme.colorScheme.onSurfaceVariant) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                imeAction = androidx.compose.ui.text.input.ImeAction.Done
            ),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                onDone = { focusManager.clearFocus() }
            ),
            textStyle = androidx.compose.ui.text.TextStyle(color = MaterialTheme.colorScheme.onSurface)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.expand_panel_show_system_apps),
                style = MaterialTheme.typography.bodySmall,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(4.dp))
            androidx.compose.material3.Switch(
                checked = showSystemApps,
                onCheckedChange = { showSystemApps = it }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .verticalScroll(rememberScrollState())
        ) {
            filteredApps.forEach { app ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAppSelected(app.packageName) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val icon = remember(app.packageName) {
                        try {
                            context.packageManager.getApplicationIcon(app.packageName)
                        } catch (_: Exception) {
                            null
                        }
                    }
                    if (icon != null) {
                        Image(
                            painter = BitmapPainter(
                                icon.toBitmap().asImageBitmap()
                            ),
                            contentDescription = null,
                            modifier = Modifier.size(40.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = app.label,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = app.packageName,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private data class AppInfo(
    val label: String,
    val packageName: String,
    val isSystemApp: Boolean
)

private fun sendMediaKeyEvent(context: Context, keyCode: Int) {
    try {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.dispatchMediaKeyEvent(android.view.KeyEvent(
            android.view.KeyEvent.ACTION_DOWN, keyCode
        ))
        audioManager.dispatchMediaKeyEvent(android.view.KeyEvent(
            android.view.KeyEvent.ACTION_UP, keyCode
        ))
    } catch (_: Exception) {}
}

private fun getCurrentBrightnessPercent(context: Context): Float {
    return try {
        val brightness = Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        brightnessToPercent(brightness)
    } catch (_: Exception) {
        0.5f
    }
}

private fun setBrightness(context: Context, percent: Float) {
    try {
        val brightness = (percent * 50).toInt().coerceIn(0, 50)
        Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, brightness)
    } catch (_: Exception) {}
}

private fun brightnessToPercent(brightness: Int): Float {
    return (brightness / 50f).coerceIn(0f, 1f)
}

private fun launchApp(context: Context, packageName: String): Boolean {
    return try {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            context.startActivity(launchIntent)
            true
        } else {
            false
        }
    } catch (_: Exception) {
        false
    }
}

private fun loadInstalledApps(context: Context): List<AppInfo> {
    val pm = context.packageManager
    return pm.getInstalledApplications(0)
        .filter { app ->
            pm.getLaunchIntentForPackage(app.packageName) != null
        }
        .map { app ->
            val label = pm.getApplicationLabel(app).toString()
            val isSystem = (app.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
            AppInfo(label, app.packageName, isSystem)
        }
        .sortedBy { it.label }
}

private fun android.graphics.drawable.Drawable.toBitmap(): android.graphics.Bitmap {
    val bitmap = android.graphics.Bitmap.createBitmap(
        intrinsicWidth.coerceAtLeast(1),
        intrinsicHeight.coerceAtLeast(1),
        android.graphics.Bitmap.Config.ARGB_8888
    )
    val canvas = android.graphics.Canvas(bitmap)
    setBounds(0, 0, canvas.width, canvas.height)
    draw(canvas)
    return bitmap
}
