package com.edgegesture.evilgodxu.screens.gesture.service

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.core.graphics.toColorInt
import com.edgegesture.evilgodxu.data.gesture.GestureSettingsState
import com.edgegesture.evilgodxu.data.gesture.EdgePosition

class AccessibilityEdgeViewManager(
    private val context: Context,
    private val gestureDetector: AccessibilityGestureDetector,
    private val onAttachFailed: ((WindowManager.BadTokenException) -> Unit)? = null
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private var leftEdgeViews: MutableList<View> = mutableListOf()
    private var rightEdgeViews: MutableList<View> = mutableListOf()
    private var bottomEdgeViews: MutableList<View> = mutableListOf()

    private var leftParamsList: MutableList<WindowManager.LayoutParams> = mutableListOf()
    private var rightParamsList: MutableList<WindowManager.LayoutParams> = mutableListOf()
    private var bottomParamsList: MutableList<WindowManager.LayoutParams> = mutableListOf()

    private var screenWidth: Int = 0
    private var screenHeight: Int = 0
    private var density: Float = 1f

    fun updateScreenDimensions() {
        val metrics = windowManager.currentWindowMetrics
        val bounds = metrics.bounds
        screenWidth = bounds.width()
        screenHeight = bounds.height()
        density = context.resources.displayMetrics.density
    }

    fun createEdgeViews(settings: GestureSettingsState, settingsProvider: () -> GestureSettingsState) {
        updateScreenDimensions()

        // 先移除旧视图避免重复添加
        removeEdgeViews()

        // 创建左侧边缘触摸区域视图（根据段数动态创建）
        val leftWidthPx = (settings.leftEdgeWidth * density).toInt()
        val leftTotalHeightPx = (screenHeight * settings.leftEdgeHeightPercent / 100)
        val leftBaseY = ((screenHeight - leftTotalHeightPx) * settings.leftEdgePositionPercent / 100)
        val leftGapPx = if (settings.leftSegmentCount > 1) (1 * density).toInt() else 0
        val leftSegmentHeight = if (settings.leftSegmentCount > 1) {
            (leftTotalHeightPx - leftGapPx * (settings.leftSegmentCount - 1)) / settings.leftSegmentCount
        } else {
            leftTotalHeightPx
        }

        for (i in 0 until settings.leftSegmentCount) {
            val y = leftBaseY + i * leftSegmentHeight + i * leftGapPx
            val height = if (i == settings.leftSegmentCount - 1) {
                leftTotalHeightPx - i * leftSegmentHeight - i * leftGapPx
            } else {
                leftSegmentHeight
            }
            val params = createLayoutParams(
                width = leftWidthPx,
                height = height,
                gravity = Gravity.START or Gravity.TOP,
                x = 0,
                y = y
            )
            leftParamsList.add(params)
            leftEdgeViews.add(createEdgeView(EdgePosition.LEFT, i, settingsProvider))
        }

        // 创建右侧边缘触摸区域视图（根据段数动态创建）
        val rightWidthPx = (settings.rightEdgeWidth * density).toInt()
        val rightTotalHeightPx = (screenHeight * settings.rightEdgeHeightPercent / 100)
        val rightBaseY = ((screenHeight - rightTotalHeightPx) * settings.rightEdgePositionPercent / 100)
        val rightGapPx = if (settings.rightSegmentCount > 1) (1 * density).toInt() else 0
        val rightSegmentHeight = if (settings.rightSegmentCount > 1) {
            (rightTotalHeightPx - rightGapPx * (settings.rightSegmentCount - 1)) / settings.rightSegmentCount
        } else {
            rightTotalHeightPx
        }

        for (i in 0 until settings.rightSegmentCount) {
            val y = rightBaseY + i * rightSegmentHeight + i * rightGapPx
            val height = if (i == settings.rightSegmentCount - 1) {
                rightTotalHeightPx - i * rightSegmentHeight - i * rightGapPx
            } else {
                rightSegmentHeight
            }
            val params = createLayoutParams(
                width = rightWidthPx,
                height = height,
                gravity = Gravity.END or Gravity.TOP,
                x = 0,
                y = y
            )
            rightParamsList.add(params)
            rightEdgeViews.add(createEdgeView(EdgePosition.RIGHT, i, settingsProvider))
        }

        // 创建底部边缘触摸区域视图（根据段数动态创建）
        val bottomHeightPx = (settings.bottomEdgeHeight * density).toInt()
        val bottomTotalWidthPx = (screenWidth * settings.bottomEdgeWidthPercent / 100)
        val bottomGapPx = if (settings.bottomSegmentCount > 1) (1 * density).toInt() else 0
        val bottomSegmentWidth = if (settings.bottomSegmentCount > 1) {
            (bottomTotalWidthPx - bottomGapPx * (settings.bottomSegmentCount - 1)) / settings.bottomSegmentCount
        } else {
            bottomTotalWidthPx
        }
        val bottomBaseX = (screenWidth - bottomTotalWidthPx) / 2

        for (i in 0 until settings.bottomSegmentCount) {
            val x = bottomBaseX + i * bottomSegmentWidth + i * bottomGapPx
            val width = if (i == settings.bottomSegmentCount - 1) {
                bottomTotalWidthPx - i * bottomSegmentWidth - i * bottomGapPx
            } else {
                bottomSegmentWidth
            }
            val params = createLayoutParams(
                width = width,
                height = bottomHeightPx,
                gravity = Gravity.BOTTOM or Gravity.START,
                x = x,
                y = 0
            )
            bottomParamsList.add(params)
            bottomEdgeViews.add(createEdgeView(EdgePosition.BOTTOM, i, settingsProvider))
        }
    }

    private fun createLayoutParams(
        width: Int,
        height: Int,
        gravity: Int,
        x: Int,
        y: Int
    ): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            width,
            height,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSPARENT
        ).apply {
            this.gravity = gravity
            this.x = x
            this.y = y
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createEdgeView(position: EdgePosition, segmentIndex: Int, settingsProvider: () -> GestureSettingsState): View {
        return View(context).apply {
            setBackgroundColor("#40FF0000".toColorInt())
            gestureDetector.setupGestureDetection(this, position, segmentIndex, settingsProvider)
        }
    }

    fun showEdgeViews(settings: GestureSettingsState) {
        val alpha = if (settings.hideOverlay) 0f else 0.6f

        leftEdgeViews.forEachIndexed { index, view ->
            view.alpha = alpha
            if (view.windowToken == null) {
                val params = leftParamsList.getOrNull(index)
                if (params != null) {
                    safeAddView(view, params)
                }
            }
        }
        rightEdgeViews.forEachIndexed { index, view ->
            view.alpha = alpha
            if (view.windowToken == null) {
                val params = rightParamsList.getOrNull(index)
                if (params != null) {
                    safeAddView(view, params)
                }
            }
        }
        bottomEdgeViews.forEachIndexed { index, view ->
            view.alpha = alpha
            if (view.windowToken == null) {
                val params = bottomParamsList.getOrNull(index)
                if (params != null) {
                    safeAddView(view, params)
                }
            }
        }
    }

    private fun safeAddView(view: View, params: WindowManager.LayoutParams) {
        try {
            windowManager.addView(view, params)
        } catch (e: WindowManager.BadTokenException) {
            android.util.Log.w(TAG, "BadTokenException adding edge view, service token may be invalid", e)
            onAttachFailed?.invoke(e)
        } catch (e: IllegalStateException) {
            android.util.Log.w(TAG, "IllegalStateException adding edge view", e)
        }
    }

    fun removeEdgeViews() {
        leftEdgeViews.forEach { view ->
            try {
                if (view.windowToken != null) windowManager.removeView(view)
            } catch (_: Exception) {}
        }
        rightEdgeViews.forEach { view ->
            try {
                if (view.windowToken != null) windowManager.removeView(view)
            } catch (_: Exception) {}
        }
        bottomEdgeViews.forEach { view ->
            try {
                if (view.windowToken != null) windowManager.removeView(view)
            } catch (_: Exception) {}
        }
        leftEdgeViews.clear()
        rightEdgeViews.clear()
        bottomEdgeViews.clear()
        leftParamsList.clear()
        rightParamsList.clear()
        bottomParamsList.clear()
    }

    fun updateEdgeViewsAlpha(settings: GestureSettingsState) {
        val alpha = if (settings.hideOverlay) 0f else 0.6f

        val allViews = leftEdgeViews + rightEdgeViews + bottomEdgeViews
        allViews.forEach { view ->
            view.alpha = alpha
            if (settings.hideOverlay) {
                view.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            } else {
                view.setBackgroundColor("#40FF0000".toColorInt())
            }
        }
    }

    fun updateEdgeViewsLayout(settings: GestureSettingsState) {
        updateScreenDimensions()

        // 更新左侧边缘视图位置和尺寸
        val leftWidthPx = (settings.leftEdgeWidth * density).toInt()
        val leftTotalHeightPx = (screenHeight * settings.leftEdgeHeightPercent / 100)
        val leftBaseY = ((screenHeight - leftTotalHeightPx) * settings.leftEdgePositionPercent / 100)
        val leftGapPx = if (settings.leftSegmentCount > 1) (1 * density).toInt() else 0
        val leftSegmentHeight = if (settings.leftSegmentCount > 1) {
            (leftTotalHeightPx - leftGapPx * (settings.leftSegmentCount - 1)) / settings.leftSegmentCount
        } else {
            leftTotalHeightPx
        }

        leftEdgeViews.forEachIndexed { index, view ->
            val params = leftParamsList.getOrNull(index) ?: return@forEachIndexed
            val y = leftBaseY + index * leftSegmentHeight + index * leftGapPx
            val height = if (index == settings.leftSegmentCount - 1) {
                leftTotalHeightPx - index * leftSegmentHeight - index * leftGapPx
            } else {
                leftSegmentHeight
            }
            params.width = leftWidthPx
            params.height = height
            params.y = y
            if (settings.hideOverlay) {
                view.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            } else {
                view.setBackgroundColor("#40FF0000".toColorInt())
            }
            try {
                windowManager.updateViewLayout(view, params)
            } catch (_: Exception) {
            }
        }

        // 更新右侧边缘视图位置和尺寸
        val rightWidthPx = (settings.rightEdgeWidth * density).toInt()
        val rightTotalHeightPx = (screenHeight * settings.rightEdgeHeightPercent / 100)
        val rightBaseY = ((screenHeight - rightTotalHeightPx) * settings.rightEdgePositionPercent / 100)
        val rightGapPx = if (settings.rightSegmentCount > 1) (1 * density).toInt() else 0
        val rightSegmentHeight = if (settings.rightSegmentCount > 1) {
            (rightTotalHeightPx - rightGapPx * (settings.rightSegmentCount - 1)) / settings.rightSegmentCount
        } else {
            rightTotalHeightPx
        }

        rightEdgeViews.forEachIndexed { index, view ->
            val params = rightParamsList.getOrNull(index) ?: return@forEachIndexed
            val y = rightBaseY + index * rightSegmentHeight + index * rightGapPx
            val height = if (index == settings.rightSegmentCount - 1) {
                rightTotalHeightPx - index * rightSegmentHeight - index * rightGapPx
            } else {
                rightSegmentHeight
            }
            params.width = rightWidthPx
            params.height = height
            params.y = y
            if (settings.hideOverlay) {
                view.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            } else {
                view.setBackgroundColor("#40FF0000".toColorInt())
            }
            try {
                windowManager.updateViewLayout(view, params)
            } catch (_: Exception) {
            }
        }

        // 更新底部边缘视图位置和尺寸
        val bottomHeightPx = (settings.bottomEdgeHeight * density).toInt()
        val bottomTotalWidthPx = (screenWidth * settings.bottomEdgeWidthPercent / 100)
        val bottomGapPx = if (settings.bottomSegmentCount > 1) (1 * density).toInt() else 0
        val bottomSegmentWidth = if (settings.bottomSegmentCount > 1) {
            (bottomTotalWidthPx - bottomGapPx * (settings.bottomSegmentCount - 1)) / settings.bottomSegmentCount
        } else {
            bottomTotalWidthPx
        }
        val bottomBaseX = (screenWidth - bottomTotalWidthPx) / 2

        bottomEdgeViews.forEachIndexed { index, view ->
            val params = bottomParamsList.getOrNull(index) ?: return@forEachIndexed
            val x = bottomBaseX + index * bottomSegmentWidth + index * bottomGapPx
            val width = if (index == settings.bottomSegmentCount - 1) {
                bottomTotalWidthPx - index * bottomSegmentWidth - index * bottomGapPx
            } else {
                bottomSegmentWidth
            }
            params.width = width
            params.height = bottomHeightPx
            params.x = x
            if (settings.hideOverlay) {
                view.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            } else {
                view.setBackgroundColor("#40FF0000".toColorInt())
            }
            try {
                windowManager.updateViewLayout(view, params)
            } catch (_: Exception) {
            }
        }
    }

    fun isViewAttached(): Boolean = leftEdgeViews.isNotEmpty() && leftEdgeViews.firstOrNull()?.windowToken != null

    // 获取第一个已附着的边缘视图，用于查询 WindowInsets 等信息
    fun getFirstAttachedView(): View? {
        return (leftEdgeViews + rightEdgeViews + bottomEdgeViews).firstOrNull { it.isAttachedToWindow }
    }

    // 禁用边缘视图的触摸事件，触摸会穿透到下层窗口
    fun disableEdgeViewsTouch() {
        val allViews = leftEdgeViews + rightEdgeViews + bottomEdgeViews
        val allParams = leftParamsList + rightParamsList + bottomParamsList

        allViews.forEachIndexed { index, view ->
            val params = allParams.getOrNull(index) ?: return@forEachIndexed
            // 添加 FLAG_NOT_TOUCHABLE 使触摸事件穿透到下层
            params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            try {
                windowManager.updateViewLayout(view, params)
            } catch (_: Exception) {}
        }
    }

    // 恢复边缘视图的触摸事件
    fun enableEdgeViewsTouch() {
        val allViews = leftEdgeViews + rightEdgeViews + bottomEdgeViews
        val allParams = leftParamsList + rightParamsList + bottomParamsList

        allViews.forEachIndexed { index, view ->
            val params = allParams.getOrNull(index) ?: return@forEachIndexed
            // 移除 FLAG_NOT_TOUCHABLE 恢复触摸检测
            params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
            try {
                windowManager.updateViewLayout(view, params)
            } catch (_: Exception) {}
        }
    }

    companion object {
        private const val TAG = "EdgeViewManager"
    }
}
