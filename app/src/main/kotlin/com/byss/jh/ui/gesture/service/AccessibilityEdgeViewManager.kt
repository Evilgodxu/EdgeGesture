package com.byss.jh.ui.gesture.service

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.core.graphics.toColorInt
import com.byss.jh.data.gesture.GestureSettingsState
import com.byss.jh.data.gesture.EdgePosition

class AccessibilityEdgeViewManager(
    private val context: Context,
    private val gestureDetector: AccessibilityGestureDetector
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

    // 缓存边缘视图的原始位置，用于输入法关闭后恢复
    private var originalLeftY: MutableList<Int> = mutableListOf()
    private var originalRightY: MutableList<Int> = mutableListOf()
    private var originalBottomX: MutableList<Int> = mutableListOf()

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
            leftTotalHeightPx / settings.leftSegmentCount
        }

        originalLeftY.clear()
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
            originalLeftY.add(y)
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
            rightTotalHeightPx / settings.rightSegmentCount
        }

        originalRightY.clear()
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
            originalRightY.add(y)
            rightEdgeViews.add(createEdgeView(EdgePosition.RIGHT, i, settingsProvider))
        }

        // 创建底部边缘触摸区域视图（根据段数动态创建）
        val bottomHeightPx = (settings.bottomEdgeHeight * density).toInt()
        val bottomTotalWidthPx = (screenWidth * settings.bottomEdgeWidthPercent / 100)
        val bottomGapPx = if (settings.bottomSegmentCount > 1) (1 * density).toInt() else 0
        val bottomSegmentWidth = if (settings.bottomSegmentCount > 1) {
            (bottomTotalWidthPx - bottomGapPx * (settings.bottomSegmentCount - 1)) / settings.bottomSegmentCount
        } else {
            bottomTotalWidthPx / settings.bottomSegmentCount
        }
        val bottomBaseX = (screenWidth - bottomTotalWidthPx) / 2

        originalBottomX.clear()
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
            originalBottomX.add(x)
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
                    windowManager.addView(view, params)
                }
            }
        }
        rightEdgeViews.forEachIndexed { index, view ->
            view.alpha = alpha
            if (view.windowToken == null) {
                val params = rightParamsList.getOrNull(index)
                if (params != null) {
                    windowManager.addView(view, params)
                }
            }
        }
        bottomEdgeViews.forEachIndexed { index, view ->
            view.alpha = alpha
            if (view.windowToken == null) {
                val params = bottomParamsList.getOrNull(index)
                if (params != null) {
                    windowManager.addView(view, params)
                }
            }
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
            leftTotalHeightPx / settings.leftSegmentCount
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
            // 同步更新原始位置记录，确保输入法关闭后能正确恢复到更新后的位置
            originalLeftY[index] = y
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
            rightTotalHeightPx / settings.rightSegmentCount
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
            // 同步更新原始位置记录，确保输入法关闭后能正确恢复到更新后的位置
            originalRightY[index] = y
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
            bottomTotalWidthPx / settings.bottomSegmentCount
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
            // 同步更新原始位置记录，确保输入法关闭后能正确恢复到更新后的位置
            originalBottomX[index] = x
            try {
                windowManager.updateViewLayout(view, params)
            } catch (_: Exception) {
            }
        }
    }

    fun isViewAttached(): Boolean = leftEdgeViews.isNotEmpty() && leftEdgeViews.firstOrNull()?.windowToken != null

    // 输入法弹出时将边缘视图移到屏幕外，避免遮挡输入区域
    fun hideEdgeViewsForKeyboard() {
        // 左侧边缘移到屏幕下方（y坐标设为屏幕高度+1000）
        leftEdgeViews.forEachIndexed { index, view ->
            val params = leftParamsList.getOrNull(index) ?: return@forEachIndexed
            params.y = screenHeight + 1000
            try {
                windowManager.updateViewLayout(view, params)
            } catch (_: Exception) {}
        }

        // 右侧边缘移到屏幕下方
        rightEdgeViews.forEachIndexed { index, view ->
            val params = rightParamsList.getOrNull(index) ?: return@forEachIndexed
            params.y = screenHeight + 1000
            try {
                windowManager.updateViewLayout(view, params)
            } catch (_: Exception) {}
        }

        // 底部边缘移到屏幕右侧（x坐标设为屏幕宽度+1000）
        bottomEdgeViews.forEachIndexed { index, view ->
            val params = bottomParamsList.getOrNull(index) ?: return@forEachIndexed
            params.x = screenWidth + 1000
            try {
                windowManager.updateViewLayout(view, params)
            } catch (_: Exception) {}
        }
    }

    // 输入法关闭后恢复边缘视图到原始位置
    fun restoreEdgeViewsAfterKeyboard() {
        // 恢复左侧边缘到缓存的原始位置
        leftEdgeViews.forEachIndexed { index, view ->
            val params = leftParamsList.getOrNull(index) ?: return@forEachIndexed
            val originalY = originalLeftY.getOrNull(index) ?: return@forEachIndexed
            params.y = originalY
            try {
                windowManager.updateViewLayout(view, params)
            } catch (_: Exception) {}
        }

        // 恢复右侧边缘到缓存的原始位置
        rightEdgeViews.forEachIndexed { index, view ->
            val params = rightParamsList.getOrNull(index) ?: return@forEachIndexed
            val originalY = originalRightY.getOrNull(index) ?: return@forEachIndexed
            params.y = originalY
            try {
                windowManager.updateViewLayout(view, params)
            } catch (_: Exception) {}
        }

        // 恢复底部边缘到缓存的原始位置
        bottomEdgeViews.forEachIndexed { index, view ->
            val params = bottomParamsList.getOrNull(index) ?: return@forEachIndexed
            val originalX = originalBottomX.getOrNull(index) ?: return@forEachIndexed
            params.x = originalX
            try {
                windowManager.updateViewLayout(view, params)
            } catch (_: Exception) {}
        }
    }

    companion object {
        private const val TAG = "EdgeViewManager"
    }
}
