package com.edgegesture.evilgodxu.screens.gesture.service.compassclock

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.KeyEvent
import android.view.WindowManager
import com.edgegesture.evilgodxu.log.CrashLogManager

/**
 * 罗盘时钟悬浮窗管理器。
 * 全屏无障碍悬浮窗，点击或按返回键关闭。
 */
class CompassClockViewManager(
    private val context: Context,
    private val onDismiss: () -> Unit
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var view: CompassClockView? = null
    private var isDismissing = false

    fun show(): Boolean {
        if (view != null) return true
        val clockView = CompassClockView(context).apply {
            setOnClickListener { dismiss() }
            isFocusableInTouchMode = true
            setOnKeyListener { _, code, event ->
                if (code == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                    dismiss()
                    true
                } else {
                    false
                }
            }
            requestFocus()
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                WindowManager.LayoutParams.FLAG_BLUR_BEHIND,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            blurBehindRadius = 80
            // 不避让系统栏插入边，让窗口延伸到状态栏及挖孔区域，保证全屏遮罩颜色一致
            setFitInsetsTypes(0)
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        }
        return try {
            view = clockView
            windowManager.addView(clockView, params)
            clockView.start()
            true
        } catch (e: Exception) {
            CrashLogManager.logException("CompassClockViewManager", "显示罗盘时钟失败", e)
            view = null
            false
        }
    }

    fun dismiss() {
        val current = view ?: return
        if (isDismissing) return
        isDismissing = true
        // 先播放反向关闭动画，动画结束后再移除视图
        current.startClose {
            current.stop()
            try {
                if (current.windowToken != null) {
                    windowManager.removeView(current)
                }
            } catch (e: Exception) {
                CrashLogManager.logException("CompassClockViewManager", "移除罗盘时钟失败", e)
            }
            view = null
            isDismissing = false
            onDismiss()
        }
    }
}
