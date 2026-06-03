package com.byss.jh.screens.gesture.service

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import com.byss.jh.data.gesture.GestureAction
import com.byss.jh.data.gesture.GestureSettingsState
import com.byss.jh.data.gesture.EdgePosition
import kotlin.math.abs

class AccessibilityGestureDetector(
    private val context: Context,
    private val callback: GestureCallback
) {

    interface GestureCallback {
        fun onSwipeAction(action: GestureAction)
    }

    enum class SwipeDirection {
        UP, DOWN, LEFT, RIGHT
    }

    @SuppressLint("ClickableViewAccessibility")
    fun setupGestureDetection(
        view: View,
        position: EdgePosition,
        segmentIndex: Int,
        settingsProvider: () -> GestureSettingsState
    ) {
        var startX = 0f
        var startY = 0f
        var isLongPressTriggered = false
        var isSwipeStarted = false
        var swipeDirection: SwipeDirection? = null
        val swipeThreshold = 80f // 滑动触发阈值（像素）
        val longPressThreshold = 200L // 长按触发阈值（毫秒）

        val handler = Handler(Looper.getMainLooper())
        var longPressRunnable: Runnable? = null

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.rawX
                    startY = event.rawY
                    isLongPressTriggered = false
                    isSwipeStarted = false
                    swipeDirection = null

                    val runnable = Runnable {
                        isLongPressTriggered = true
                        if (settingsProvider().vibrationEnabled) {
                            view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                        }
                        swipeDirection?.let { direction ->
                            val action = resolveAction(position, segmentIndex, direction, true, settingsProvider())
                            if (action != GestureAction.NONE) {
                                callback.onSwipeAction(action)
                            }
                        }
                    }
                    longPressRunnable = runnable
                    handler.postDelayed(runnable, longPressThreshold)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val currentX = event.rawX
                    val currentY = event.rawY
                    val deltaX = currentX - startX
                    val deltaY = currentY - startY
                    val absDeltaX = abs(deltaX)
                    val absDeltaY = abs(deltaY)

                    if (!isLongPressTriggered && !isSwipeStarted) {
                        isSwipeStarted = true
                        swipeDirection = when {
                            absDeltaY > absDeltaX -> {
                                if (deltaY > 0) SwipeDirection.DOWN else SwipeDirection.UP
                            }
                            else -> {
                                if (deltaX > 0) SwipeDirection.RIGHT else SwipeDirection.LEFT
                            }
                        }
                    }
                    isSwipeStarted
                }
                MotionEvent.ACTION_UP -> {
                    val endX = event.rawX
                    val endY = event.rawY
                    val deltaX = endX - startX
                    val deltaY = endY - startY
                    val absDeltaX = abs(deltaX)
                    val absDeltaY = abs(deltaY)

                    longPressRunnable?.let { handler.removeCallbacks(it) }

                    if (isLongPressTriggered && isSwipeStarted) {
                        return@setOnTouchListener true
                    }

                    val isSwipe = absDeltaX > swipeThreshold || absDeltaY > swipeThreshold

                    if (isSwipe) {
                        val direction = when {
                            absDeltaY > absDeltaX -> {
                                if (deltaY > 0) SwipeDirection.DOWN else SwipeDirection.UP
                            }
                            else -> {
                                if (deltaX > 0) SwipeDirection.RIGHT else SwipeDirection.LEFT
                            }
                        }
                        val action = resolveAction(position, segmentIndex, direction, false, settingsProvider())
                        if (action != GestureAction.NONE) {
                            callback.onSwipeAction(action)
                        }
                        true
                    } else {
                        true
                    }
                }
                MotionEvent.ACTION_CANCEL -> {
                    longPressRunnable?.let { handler.removeCallbacks(it) }
                    false
                }
                else -> false
            }
        }
    }

    private fun resolveAction(
        position: EdgePosition,
        segmentIndex: Int,
        direction: SwipeDirection,
        isLongPress: Boolean,
        settings: GestureSettingsState
    ): GestureAction {
        return when (position) {
            EdgePosition.LEFT -> resolveLeftEdgeAction(segmentIndex, direction, isLongPress, settings)
            EdgePosition.RIGHT -> resolveRightEdgeAction(segmentIndex, direction, isLongPress, settings)
            EdgePosition.BOTTOM -> resolveBottomEdgeAction(segmentIndex, direction, isLongPress, settings)
        }
    }

    private fun resolveLeftEdgeAction(
        segmentIndex: Int,
        direction: SwipeDirection,
        isLongPress: Boolean,
        settings: GestureSettingsState
    ): GestureAction {
        val config = when (segmentIndex) {
            0 -> settings.leftEdge
            1 -> settings.leftEdgeSegment2
            2 -> settings.leftEdgeSegment3
            else -> settings.leftEdge
        }
        return when (direction) {
            SwipeDirection.RIGHT -> if (isLongPress) config.swipeRightLong else config.swipeRight
            SwipeDirection.UP -> if (isLongPress) config.swipeUpLong else config.swipeUp
            SwipeDirection.DOWN -> if (isLongPress) config.swipeDownLong else config.swipeDown
            else -> GestureAction.NONE
        }
    }

    private fun resolveRightEdgeAction(
        segmentIndex: Int,
        direction: SwipeDirection,
        isLongPress: Boolean,
        settings: GestureSettingsState
    ): GestureAction {
        val config = when (segmentIndex) {
            0 -> settings.rightEdge
            1 -> settings.rightEdgeSegment2
            2 -> settings.rightEdgeSegment3
            else -> settings.rightEdge
        }
        return when (direction) {
            SwipeDirection.LEFT -> if (isLongPress) config.swipeLeftLong else config.swipeLeft
            SwipeDirection.UP -> if (isLongPress) config.swipeUpLong else config.swipeUp
            SwipeDirection.DOWN -> if (isLongPress) config.swipeDownLong else config.swipeDown
            else -> GestureAction.NONE
        }
    }

    private fun resolveBottomEdgeAction(
        segmentIndex: Int,
        direction: SwipeDirection,
        isLongPress: Boolean,
        settings: GestureSettingsState
    ): GestureAction {
        val config = when (segmentIndex) {
            0 -> settings.bottomEdge
            1 -> settings.bottomEdgeSegment2
            2 -> settings.bottomEdgeSegment3
            else -> settings.bottomEdge
        }
        return when (direction) {
            SwipeDirection.UP -> if (isLongPress) config.swipeUpLong else config.swipeUp
            SwipeDirection.LEFT -> if (isLongPress) config.swipeLeftLong else config.swipeLeft
            SwipeDirection.RIGHT -> if (isLongPress) config.swipeRightLong else config.swipeRight
            else -> GestureAction.NONE
        }
    }

    companion object {
        private const val TAG = "GestureDetector"
    }
}
