package com.edgegesture.evilgodxu.screens.gesture.service.taskpanel

import android.graphics.PixelFormat
import android.view.Gravity
import android.view.KeyEvent
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
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

class TaskPanelViewManager(
    private val context: android.content.Context,
    private val apps: List<TaskPanelApp>,
    private val selectedPackageName: String?,
    private val onLaunch: (String) -> Unit,
    private val onLaunchInFreeform: (String) -> Unit,
    private val onSwipeAway: (String) -> Unit,
    private val onDismiss: () -> Unit
) {
    private val windowManager = context.getSystemService(android.content.Context.WINDOW_SERVICE) as WindowManager
    private var view: ComposeView? = null
    private var isDismissing = false
    private val lifecycleOwner = object : LifecycleOwner {
        private val registry = LifecycleRegistry(this)
        override val lifecycle: Lifecycle get() = registry
        fun event(event: Lifecycle.Event) = registry.handleLifecycleEvent(event)
    }
    private val storeOwner = object : ViewModelStoreOwner {
        override val viewModelStore = ViewModelStore()
    }
    private val savedOwner = object : SavedStateRegistryOwner {
        private val controller = SavedStateRegistryController.create(this)
        override val savedStateRegistry: SavedStateRegistry get() = controller.savedStateRegistry
        override val lifecycle: Lifecycle get() = lifecycleOwner.lifecycle
        fun attach() = controller.performAttach()
        fun restore() = controller.performRestore(null)
    }

    fun show(): Boolean {
        if (view != null) return true
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                WindowManager.LayoutParams.FLAG_BLUR_BEHIND,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            blurBehindRadius = 80
        }
        val compose = ComposeView(context).apply {
            alpha = 0f
            scaleX = 0.9f
            scaleY = 0.9f
            setContent {
                TaskPanelOverlay(
                    apps = apps,
                    selectedPackageName = selectedPackageName,
                    onLaunch = { onLaunch(it); dismiss() },
                    onLaunchInFreeform = { onLaunchInFreeform(it); dismiss() },
                    onSwipeAway = onSwipeAway,
                    onDismiss = { dismiss() }
                )
            }
            setOnKeyListener { _, code, event ->
                if (code == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) { dismiss(); true } else false
            }
            isFocusableInTouchMode = true
            requestFocus()
        }
        savedOwner.attach(); savedOwner.restore()
        compose.setViewTreeLifecycleOwner(lifecycleOwner)
        compose.setViewTreeViewModelStoreOwner(storeOwner)
        compose.setViewTreeSavedStateRegistryOwner(savedOwner)
        return try {
            view = compose
            windowManager.addView(compose, params)
            lifecycleOwner.event(Lifecycle.Event.ON_CREATE)
            lifecycleOwner.event(Lifecycle.Event.ON_RESUME)
            compose.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(250)
                .setInterpolator(DecelerateInterpolator())
                .start()
            true
        } catch (_: Exception) {
            view = null
            false
        }
    }

    fun dismiss() {
        val current = view ?: return
        if (isDismissing) return
        isDismissing = true
        lifecycleOwner.event(Lifecycle.Event.ON_PAUSE)
        current.animate()
            .alpha(0f)
            .scaleX(0.9f)
            .scaleY(0.9f)
            .setDuration(200)
            .setInterpolator(AccelerateInterpolator())
            .withEndAction {
                lifecycleOwner.event(Lifecycle.Event.ON_STOP)
                lifecycleOwner.event(Lifecycle.Event.ON_DESTROY)
                try {
                    if (current.windowToken != null) {
                        windowManager.removeView(current)
                    }
                } catch (_: Exception) { }
                view = null
                isDismissing = false
                onDismiss()
            }
            .start()
    }
}