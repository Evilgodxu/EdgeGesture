package com.edgegesture.evilgodxu.screens.gesture.service.musicpanel

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.KeyEvent
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

// 音乐面板悬浮窗管理器
class MusicPanelViewManager(
    private val context: Context,
    private val onDismiss: () -> Unit,
    private val onShowFailed: ((WindowManager.BadTokenException) -> Unit)? = null
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var composeView: ComposeView? = null
    private val managerJob = SupervisorJob()
    private val managerScope = CoroutineScope(managerJob + Dispatchers.IO)

    private val playbackState = MusicPanelStateHolder.state

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

    // 显示音乐面板悬浮窗
    @SuppressLint("ClickableViewAccessibility")
    fun show() {
        if (composeView != null) return

        val flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    WindowManager.LayoutParams.FLAG_BLUR_BEHIND
                } else 0

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        val view = ComposeView(context).apply {
            setContent {
                MusicPanelOverlay(
                    playbackState = playbackState,
                    onScan = { },
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
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                dismiss()
                true
            } else {
                false
            }
        }
        view.isFocusableInTouchMode = true
        view.requestFocus()

        composeView = view
        try {
            windowManager.addView(view, params)
        } catch (e: WindowManager.BadTokenException) {
            composeView = null
            onShowFailed?.invoke(e)
            return
        }
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        // 触发自动扫描并播放第一首
        scanAndPlay()
    }

    private fun scanAndPlay() {
        managerScope.launch {
            playbackState.isScanning = true
            val tracks = MusicScanner.scan(context)
            playbackState.setSortedPlaylist(tracks)
            playbackState.isScanning = false
            // 启动面板时不自动播放，仅恢复上一次的播放状态由 UI 层处理
        }
    }

    // 关闭音乐面板并释放资源
    fun dismiss() {
        val view = composeView ?: return
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        try {
            if (view.windowToken != null) {
                windowManager.removeView(view)
            }
        } catch (_: Exception) {
        }
        composeView = null
        // 若正在播放则保留后台播放与状态，否则释放资源
        if (playbackState.isPlaying) {
            playbackState.updatePosition()
        } else {
            playbackState.release()
        }
        onDismiss()
        managerJob.cancel()
    }

    companion object {
        private const val TAG = "MusicPanelViewManager"
    }
}