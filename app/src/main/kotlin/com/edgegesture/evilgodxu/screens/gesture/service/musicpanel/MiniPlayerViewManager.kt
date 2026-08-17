package com.edgegesture.evilgodxu.screens.gesture.service.musicpanel

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.center
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
import com.edgegesture.evilgodxu.R
import com.edgegesture.evilgodxu.log.CrashLogManager
import com.edgegesture.evilgodxu.ui.theme.LightColorScheme
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

// 迷你播放器浮动窗管理器：状态栏下方的紧凑播放条，支持展开完整面板与下拉播放列表
class MiniPlayerViewManager(
    private val context: Context,
    private val onExpandPanel: () -> Unit,
    private val onSwipedDismiss: () -> Unit,
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var composeView: ComposeView? = null
    private var isDismissing = false
    private val playbackState = MusicPanelStateHolder.state

    // 播放列表展开状态（Compose 状态 + 窗口布局共用）
    private val playlistExpanded = mutableStateOf(false)
    private var statusBarHeight = getStatusBarHeight()

    private val mainHandler = Handler(Looper.getMainLooper())

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) collapsePlaylist()
        }
    }

    val isShowing: Boolean get() = composeView != null

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

    @SuppressLint("ClickableViewAccessibility", "InflateParams")
    fun show() {
        if (composeView != null) return
        statusBarHeight = currentTopInset()
        playlistExpanded.value = false

        val barH = barHeightPx()
        val flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE

        val params = WindowManager.LayoutParams(
            barWidthPx(),
            barH,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = topOffsetPx()
        }

        val view = ComposeView(context).apply {
            translationY = (-barH).toFloat()
            setContent {
                MiniPlayerOverlay(
                    playbackState = playbackState,
                    barHeightPx = barH,
                    playlistExpanded = playlistExpanded.value,
                    onPlaylistExpandedChange = { expanded -> setPlaylistExpanded(expanded) },
                    onLayoutChanged = { applyWindowLayout() },
                    onExpandPanel = onExpandPanel,
                    onSwipeDismiss = { temporaryDismiss() }
                )
            }
        }

        savedStateRegistryOwner.performAttach()
        savedStateRegistryOwner.performRestore()
        view.setViewTreeLifecycleOwner(lifecycleOwner)
        view.setViewTreeViewModelStoreOwner(viewModelStoreOwner)
        view.setViewTreeSavedStateRegistryOwner(savedStateRegistryOwner)

        // 实时监听系统状态栏高度变化（刘海屏/分屏/折叠屏等动态调整）
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            if (top != statusBarHeight) {
                statusBarHeight = top
                applyWindowLayout()
            }
            ViewCompat.dispatchApplyWindowInsets(v, insets)
        }

        // 点击迷你播放器窗口以外的区域：收起播放列表
        view.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_OUTSIDE) {
                collapsePlaylist()
                true
            } else false
        }
        // 系统返回键：收起播放列表
        view.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                collapsePlaylist()
                true
            } else false
        }
        view.isFocusableInTouchMode = true

        composeView = view
        try {
            windowManager.addView(view, params)
        } catch (e: WindowManager.BadTokenException) {
            CrashLogManager.logException("MiniPlayerViewManager", "添加迷你播放器失败（窗口令牌失效）", e)
            composeView = null
            return
        }
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        view.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(260)
            .setInterpolator(DecelerateInterpolator())
            .start()

        ContextCompat.registerReceiver(
            context,
            screenOffReceiver,
            IntentFilter(Intent.ACTION_SCREEN_OFF),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    private fun setPlaylistExpanded(expanded: Boolean) {
        if (playlistExpanded.value == expanded) return
        playlistExpanded.value = expanded
        // 展开播放列表时移除 NOT_FOCUSABLE，使系统返回键可收起列表
        val view = composeView
        val params = view?.layoutParams as? WindowManager.LayoutParams
        if (params != null) {
            params.flags = if (expanded) {
                params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
            } else {
                params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            }
            runCatching { windowManager.updateViewLayout(view!!, params) }
        }
        applyWindowLayout()
        if (expanded) {
            view?.let {
                it.isFocusableInTouchMode = true
                it.requestFocus()
            }
        }
    }

    private fun collapsePlaylist() {
        if (!playlistExpanded.value) return
        setPlaylistExpanded(false)
    }

    // 重新计算并平滑过渡窗口的宽度、高度与纵向位置
    private fun applyWindowLayout() {
        val view = composeView ?: return
        val params = view.layoutParams as? WindowManager.LayoutParams ?: return
        val barH = barHeightPx()
        val targetWidth = barWidthPx()
        val targetHeight: Int
        val targetY: Int
        if (playlistExpanded.value) {
            val rows = playbackState.playlist.size.coerceIn(0, MAX_VISIBLE_ROWS)
            val contentH = dpToPx(PLAYLIST_HEADER_DP) + rows * dpToPx(PLAYLIST_ROW_DP)
            val total = barH + contentH
            val screenH = context.resources.displayMetrics.heightPixels
            targetHeight = total.coerceAtMost(screenH)
            targetY = max(0, (screenH - targetHeight) / 2)
        } else {
            targetHeight = barH
            targetY = topOffsetPx()
        }
        if (targetWidth != params.width) {
            params.width = targetWidth
            runCatching { windowManager.updateViewLayout(view, params) }
        }
        if (targetHeight == params.height && targetY == params.y) return
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 260
            interpolator = LinearInterpolator()
            val startH = params.height
            val startY = params.y
            addUpdateListener { anim ->
                val f = anim.animatedValue as Float
                params.height = (startH + (targetHeight - startH) * f).roundToInt()
                params.y = (startY + (targetY - startY) * f).roundToInt()
                runCatching { windowManager.updateViewLayout(view, params) }
            }
        }.start()
    }

    private fun barHeightPx(): Int = dpToPx(BAR_HEIGHT_DP)

    // 横屏时限制窗口宽度并水平居中，竖屏保持全宽
    private fun barWidthPx(): Int {
        if (!isLandscape()) return WindowManager.LayoutParams.MATCH_PARENT
        return minOf(context.resources.displayMetrics.widthPixels, dpToPx(MAX_WIDTH_DP))
    }

    @SuppressLint("DiscouragedApi")
    private fun getStatusBarHeight(): Int {
        val res = context.resources
        val id = res.getIdentifier("status_bar_height", "dimen", "android")
        return if (id > 0) res.getDimensionPixelSize(id) else 0
    }

    // 横屏时状态栏位于屏幕侧边，顶部偏移为 0
    private fun isLandscape(): Boolean =
        context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // 实时获取当前窗口顶部状态栏 inset（横屏时状态栏位于侧边，顶部为 0）
    private fun currentTopInset(): Int = runCatching {
        WindowInsetsCompat.toWindowInsetsCompat(windowManager.currentWindowMetrics.windowInsets)
            .getInsets(WindowInsetsCompat.Type.statusBars()).top
    }.getOrElse { if (isLandscape()) 0 else getStatusBarHeight() }

    // 迷你播放器纵向位置：横屏状态栏在侧边，顶部仅保留 1dp 间距；
    // 竖屏位于状态栏下方，状态栏高度未刷新（横屏遗留）时回退到系统标准高度，避免嵌入状态栏
    private fun topOffsetPx(): Int =
        if (isLandscape()) dpToPx(LANDSCAPE_TOP_GAP_DP)
        else max(statusBarHeight, getStatusBarHeight())

    private fun dpToPx(value: Int): Int = (value * context.resources.displayMetrics.density).roundToInt()

    // 滑动临时关闭：仅收起并回调给上层记录临时隐藏状态
    private fun temporaryDismiss() {
        dismiss(notifySwiped = true)
    }

    fun dismiss(notifySwiped: Boolean = false) {
        val view = composeView ?: return
        if (isDismissing) return
        isDismissing = true

        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        view.animate()
            .translationY((-view.height).toFloat())
            .alpha(0f)
            .setDuration(240)
            .setInterpolator(AccelerateInterpolator())
            .withEndAction {
                lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
                lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
                try {
                    if (view.windowToken != null) windowManager.removeView(view)
                } catch (e: Exception) {
                    CrashLogManager.logException("MiniPlayerViewManager", "移除迷你播放器失败", e)
                }
                mainHandler.post {
                    try {
                        context.unregisterReceiver(screenOffReceiver)
                    } catch (e: Exception) {
                        CrashLogManager.logException("MiniPlayerViewManager", "注销熄屏监听失败", e)
                    }
                }
                composeView = null
                isDismissing = false
                if (notifySwiped) onSwipedDismiss()
            }
            .start()
    }

    companion object {
        // 迷你播放器条高度：紧凑容纳两行文本与 48dp 触控热区
        private const val BAR_HEIGHT_DP = 48
        // 横屏时的最大宽度，超出则水平居中，避免全宽拉伸
        private const val MAX_WIDTH_DP = 400
        // 横屏时顶部保留的间距
        private const val LANDSCAPE_TOP_GAP_DP = 1
        private const val PLAYLIST_HEADER_DP = 36
        private const val PLAYLIST_ROW_DP = 44
        const val MAX_VISIBLE_ROWS = 5
    }
}

@Composable
private fun MiniPlayerOverlay(
    playbackState: MusicPlaybackState,
    barHeightPx: Int,
    playlistExpanded: Boolean,
    onPlaylistExpandedChange: (Boolean) -> Unit,
    onLayoutChanged: () -> Unit,
    onExpandPanel: () -> Unit,
    onSwipeDismiss: () -> Unit,
) {
    val density = LocalDensity.current
    val context = LocalContext.current
    val barHeight = with(density) { barHeightPx.toDp() }

    // 迷你播放器固定浅色外观，不随系统主题变化
    val colorScheme = LightColorScheme
    val cardBackground = Color(0xFFF5F5F7).copy(alpha = 0.82f)

    // 歌曲切换 / 播放列表数量变化时，重新计算窗口高度
    LaunchedEffect(playlistExpanded, playbackState.playlist.size) {
        if (playlistExpanded) onLayoutChanged()
    }

    // 屏幕旋转时自动收起播放列表并重新布局
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    LaunchedEffect(configuration.orientation) {
        if (playlistExpanded) onPlaylistExpandedChange(false)
        onLayoutChanged()
    }

    MaterialTheme(colorScheme = colorScheme) {
        Column(
            modifier = Modifier
                // 胶囊圆角：半径取条高（48dp）一半，呈椭圆轮廓
                .background(cardBackground, RoundedCornerShape(24.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { /* 阻止点击穿透到状态栏区域 */ }
                )
        ) {
            MiniPlayerBar(
                playbackState = playbackState,
                barHeight = barHeight,
                playlistExpanded = playlistExpanded,
                onPlaylistExpandedChange = onPlaylistExpandedChange,
                onExpandPanel = onExpandPanel,
                onSwipeDismiss = onSwipeDismiss
            )
            if (playlistExpanded) {
                MiniPlaylistPanel(
                    playbackState = playbackState,
                    context = context,
                    onClose = { onPlaylistExpandedChange(false) }
                )
            }
        }
    }
}

@Composable
private fun MiniPlayerBar(
    playbackState: MusicPlaybackState,
    barHeight: androidx.compose.ui.unit.Dp,
    playlistExpanded: Boolean,
    onPlaylistExpandedChange: (Boolean) -> Unit,
    onExpandPanel: () -> Unit,
    onSwipeDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val current = playbackState.currentTrack
    val coverDesc = stringResource(R.string.mini_player_cover)

    // 左右滑动关闭迷你播放器（播放列表展开时不响应滑动）
    var totalDx by remember { mutableStateOf(0f) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(barHeight)
            .pointerInput(playlistExpanded) {
                totalDx = 0f
                detectHorizontalDragGestures(
                    onDragStart = { totalDx = 0f },
                    onDragEnd = {
                        if (!playlistExpanded && kotlin.math.abs(totalDx) > 60f) {
                            onSwipeDismiss()
                        }
                        totalDx = 0f
                    }
                ) { change, drag ->
                    change.consume()
                    totalDx += drag
                }
            }
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(0.dp, Alignment.CenterHorizontally)
    ) {
        // 专辑封面：旋转 + 黑胶质感
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onExpandPanel
                )
                .semantics { contentDescription = coverDesc }
        ) {
            MiniDiscArt(
                track = current,
                isPlaying = playbackState.isPlaying,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 循环模式
        MiniControlButton(
            icon = when (playbackState.playMode) {
                PlayMode.RepeatAll -> Icons.Default.Repeat
                PlayMode.RepeatOne -> Icons.Default.RepeatOne
                PlayMode.Shuffle -> Icons.Default.Shuffle
            },
            contentDescription = stringResource(R.string.music_panel_play_mode),
            onClick = {
                playbackState.setPlayMode(
                    when (playbackState.playMode) {
                        PlayMode.RepeatAll -> PlayMode.RepeatOne
                        PlayMode.RepeatOne -> PlayMode.Shuffle
                        PlayMode.Shuffle -> PlayMode.RepeatAll
                    }
                )
                playbackState.mediaController?.let { controller ->
                    applyPlaybackMode(controller, playbackState.playMode)
                }
                playbackState.persistState()
            }
        )
        // 上一曲
        MiniControlButton(
            icon = Icons.Default.SkipPrevious,
            contentDescription = stringResource(R.string.mini_player_previous),
            enabled = playbackState.playlist.isNotEmpty(),
            onClick = {
                val prev = playbackState.previousIndex()
                if (prev >= 0) scope.launch { playTrackAt(context, playbackState, prev) }
            }
        )
        // 暂停 / 播放（无背景色）
        MiniControlButton(
            icon = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = stringResource(
                if (playbackState.isPlaying) R.string.music_panel_pause else R.string.music_panel_play
            ),
            onClick = { togglePlayPause(playbackState) }
        )
        // 下一曲
        MiniControlButton(
            icon = Icons.Default.SkipNext,
            contentDescription = stringResource(R.string.mini_player_next),
            enabled = playbackState.playlist.isNotEmpty(),
            onClick = {
                val next = playbackState.nextIndex()
                if (next >= 0) scope.launch { playTrackAt(context, playbackState, next) }
            }
        )
        // 播放列表
        MiniControlButton(
            icon = Icons.AutoMirrored.Outlined.QueueMusic,
            contentDescription = stringResource(R.string.mini_player_playlist),
            onClick = { onPlaylistExpandedChange(!playlistExpanded) }
        )
    }
}

@Composable
private fun MiniDiscArt(
    track: MusicTrack?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
) {
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isActive) {
                rotation.animateTo(
                    targetValue = rotation.value + 360f,
                    animationSpec = tween(durationMillis = 12_000, easing = LinearEasing)
                )
            }
        }
    }
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = rotation.value }
                .clip(CircleShape)
        ) {
            // 专辑封面仅覆盖中间区域，外圈边缘与中心留出透明材质
            AlbumArt(
                track = track,
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxSize(0.85f)
                    .clip(CircleShape)
            )
            Canvas(modifier = Modifier.fillMaxSize()) {
                val r = size.minDimension / 2f
                val center = this.center
                // 绘制环形区域（外圆减内圆）
                val ring: (Float, Float, Color) -> Unit = { outer, inner, color ->
                    val path = Path().apply {
                        addOval(Rect(center = center, radius = outer))
                        addOval(Rect(center = center, radius = inner), Path.Direction.CounterClockwise)
                    }
                    drawPath(path, color)
                }

                // 外圈透明边缘
                ring(r, r * 0.85f, Color.White.copy(alpha = 0.16f))
                drawCircle(
                    color = Color.Black.copy(alpha = 0.15f),
                    radius = r,
                    center = center,
                    style = Stroke(width = 0.8f)
                )

                // 中心空洞 + 一圈透明材质
                ring(r * 0.32f, r * 0.18f, Color.White.copy(alpha = 0.16f))
                drawCircle(Color(0xFF141414), r * 0.18f, center)

                // 碟面高光：左上角径向光泽，模拟光碟反光（仅作用于塑料边缘，避免浅色遮罩影响封面）
                val highlightPath = Path().apply {
                    // 外圈塑料边缘
                    addOval(Rect(center = center, radius = r))
                    addOval(Rect(center = center, radius = r * 0.85f), Path.Direction.CounterClockwise)
                    // 内圈塑料白环
                    addOval(Rect(center = center, radius = r * 0.32f))
                    addOval(Rect(center = center, radius = r * 0.18f), Path.Direction.CounterClockwise)
                }
                clipPath(highlightPath) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.18f),
                                Color.White.copy(alpha = 0.05f),
                                Color.Transparent
                            ),
                            center = Offset(center.x - r * 0.35f, center.y - r * 0.35f),
                            radius = r * 1.3f
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(40.dp)
    ) {
        // 视觉圆环小于触控热区，避免在紧凑高度下贴满上下边缘
        Box(
            modifier = Modifier.size(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(20.dp),
                tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
private fun MiniPlaylistPanel(
    playbackState: MusicPlaybackState,
    context: android.content.Context,
    onClose: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val visibleCount = playbackState.playlist.size.coerceIn(0, MiniPlayerViewManager.MAX_VISIBLE_ROWS)
    val listState = rememberLazyListState()

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.mini_player_playlist_title),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.music_panel_track_count, playbackState.playlist.size),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
        }
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .height((visibleCount * 44).dp)
        ) {
            itemsIndexed(
                items = playbackState.playlist,
                key = { _, track -> track.audioUri }
            ) { index, track ->
                val isActive = index == playbackState.currentIndex
                MiniPlaylistRow(
                    track = track,
                    isActive = isActive,
                    isPlaying = isActive && playbackState.isPlaying,
                    onClick = {
                        scope.launch { playTrackAt(context, playbackState, index) }
                    },
                    onFavoriteClick = { playbackState.toggleFavorite(track.id) }
                )
            }
        }
        // 当前曲目不在可视区域内时，滚动定位到对应位置
        LaunchedEffect(playbackState.currentIndex) {
            if (playbackState.currentIndex >= 0 && playbackState.playlist.isNotEmpty()) {
                listState.animateScrollToItem(
                    playbackState.currentIndex.coerceIn(0, playbackState.playlist.size - 1)
                )
            }
        }
    }
}

@Composable
private fun MiniPlaylistRow(
    track: MusicTrack,
    isActive: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
) {
    val bg by animateColorAsState(
        targetValue = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
        else Color.Transparent,
        label = "mini_playlist_bg"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 与完整面板一致：显示专辑封面，播放中叠加动态指示
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(6.dp))
        ) {
            PlaylistArt(track = track, modifier = Modifier.fillMaxSize())
            if (isActive && isPlaying) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        repeat(3) { i ->
                            val height by animateFloatAsState(
                                targetValue = 0.4f + kotlin.random.Random.nextFloat() * 0.5f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
                                label = "mini_wave_$i"
                            )
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .height((height * 10).dp)
                                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(1.dp))
                            )
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = track.title,
                color = if (isActive) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal,
                modifier = if (track.title.length > 12) Modifier.basicMarquee(iterations = Int.MAX_VALUE) else Modifier
            )
            Text(
                text = track.artist,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(
            onClick = onFavoriteClick,
            modifier = Modifier.size(26.dp)
        ) {
            Icon(
                imageVector = if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = null,
                tint = if (track.isFavorite) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}