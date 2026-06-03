package com.byss.jh.screens.gesture.service.expandpanel

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.provider.Settings
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
import com.byss.jh.data.gesture.ExpandPanelShortcutsState
import com.byss.jh.screens.settings.ThemeMode
import kotlinx.coroutines.flow.Flow

// 扩展面板悬浮窗管理器
class ExpandPanelViewManager(
    private val context: Context,
    private val shortcutsFlow: Flow<ExpandPanelShortcutsState>,
    private val themeModeFlow: Flow<ThemeMode>,
    private val onShortcutSet: (index: Int, packageName: String?) -> Unit,
    private val onDismiss: () -> Unit,
    private val permissionCallback: ExpandPanelPermissionCallback? = null
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
            // 优先使用回调处理权限请求，支持授权后自动返回并显示面板
            val handled = permissionCallback?.onRequestWriteSettings() ?: false
            if (!handled) {
                // 回调未处理，使用默认方式跳转设置页
                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                    data = android.net.Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }
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
                    onDismiss = { dismiss() },
                    onDismissAnimationEnd = { dismiss() }
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
