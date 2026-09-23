package com.edgegesture.evilgodxu.screens.gesture.service

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import com.edgegesture.evilgodxu.data.gesture.BottomEdgeConfig
import com.edgegesture.evilgodxu.data.gesture.EdgePosition
import com.edgegesture.evilgodxu.data.gesture.EdgeTapGestureConfig
import com.edgegesture.evilgodxu.data.gesture.GestureAction
import com.edgegesture.evilgodxu.data.gesture.GestureSettingsKeys
import com.edgegesture.evilgodxu.data.gesture.GestureSettingsState
import com.edgegesture.evilgodxu.data.gesture.LeftEdgeConfig
import com.edgegesture.evilgodxu.data.gesture.RightEdgeConfig
import kotlin.math.abs

class AccessibilityGestureDetector(
    private val callback: GestureCallback
) {

    interface GestureCallback {
        fun onGestureAction(action: GestureAction, launchAppTarget: String?)
    }

    // 手势解析结果：动作 + 启动应用动作绑定的目标包名
    private data class ResolvedGesture(val action: GestureAction, val launchAppTarget: String?)

    enum class SwipeDirection {
        UP, DOWN, LEFT, RIGHT
    }

    // 点击类手势类型，对应配置页槽位 6（单击）/7（双击）/8（长按）
    private enum class TapType { SINGLE, DOUBLE, LONG_PRESS }

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
        // 二次滑动模式下的待确认方向，跨触摸会话持久保持
        var pendingDirection: SwipeDirection? = null
        // 长按（不滑动）动作是否已触发，避免松手时再次按点击或滑动处理
        var isLongPressActionTriggered = false
        // 本次触摸是否为双击的第二次点击
        var isSecondTap = false
        val swipeThreshold = 80f // 滑动触发阈值（像素）
        val longPressThreshold = 200L // 长按触发阈值（毫秒）
        val doubleTapTimeout = ViewConfiguration.getDoubleTapTimeout().toLong() // 双击判定窗口（毫秒）
        val tapSlop = ViewConfiguration.get(view.context).scaledTouchSlop.toFloat() // 点击允许的最大位移

        val handler = Handler(Looper.getMainLooper())
        var longPressRunnable: Runnable? = null
        // 已配置双击时，单击动作延迟到双击窗口结束后才触发
        var pendingSingleTap: Runnable? = null

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // 双击窗口内再次按下：取消待触发的单击，本次触摸按双击的第二次点击处理
                    val pendingSingle = pendingSingleTap
                    if (pendingSingle != null) {
                        handler.removeCallbacks(pendingSingle)
                        pendingSingleTap = null
                        isSecondTap = true
                    } else {
                        isSecondTap = false
                    }
                    startX = event.rawX
                    startY = event.rawY
                    isLongPressTriggered = false
                    isSwipeStarted = false
                    swipeDirection = null
                    isLongPressActionTriggered = false

                    val runnable = Runnable {
                        isLongPressTriggered = true
                        val settings = settingsProvider()
                        if (settings.vibrationEnabled) {
                            view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                        }
                        val direction = swipeDirection
                        if (direction != null) {
                            // 已产生方向：按「长按+滑动」处理
                            if (settings.doubleSwipeEnabled) {
                                // 二次滑动模式：只有待确认方向匹配时才触发长按操作
                                if (pendingDirection == direction) {
                                    dispatchSwipe(position, segmentIndex, direction, true, settings)
                                    pendingDirection = null
                                }
                                // 方向不匹配时静默消耗，不设置待确认方向
                            } else {
                                dispatchSwipe(position, segmentIndex, direction, true, settings)
                            }
                        } else {
                            // 未产生方向：按长按（不滑动）处理
                            val gesture = resolveTapGesture(position, segmentIndex, TapType.LONG_PRESS, settings)
                            if (gesture.action != GestureAction.NONE) {
                                isLongPressActionTriggered = true
                                callback.onGestureAction(gesture.action, gesture.launchAppTarget)
                            }
                        }
                    }
                    longPressRunnable = runnable
                    handler.postDelayed(runnable, longPressThreshold)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.rawX - startX
                    val deltaY = event.rawY - startY

                    if (!isLongPressTriggered) {
                        if (isSwipeStarted) {
                            // 越过阈值后按当前主方向刷新，避免初始抖动锁定错误方向
                            swipeDirection = resolveSwipeDirection(deltaX, deltaY)
                        } else if (abs(deltaX) > swipeThreshold || abs(deltaY) > swipeThreshold) {
                            isSwipeStarted = true
                            swipeDirection = resolveSwipeDirection(deltaX, deltaY)
                        }
                    }
                    isSwipeStarted
                }
                MotionEvent.ACTION_UP -> {
                    val deltaX = event.rawX - startX
                    val deltaY = event.rawY - startY
                    val absDeltaX = abs(deltaX)
                    val absDeltaY = abs(deltaY)

                    longPressRunnable?.let { handler.removeCallbacks(it) }

                    if (isLongPressTriggered && isSwipeStarted) {
                        return@setOnTouchListener true
                    }
                    // 长按动作已触发，松手不再处理
                    if (isLongPressActionTriggered) {
                        return@setOnTouchListener true
                    }

                    val isSwipe = absDeltaX > swipeThreshold || absDeltaY > swipeThreshold

                    if (isSwipe) {
                        // 以起止点总位移裁定向，与 MOVE 阶段共用同一判向基准
                        val direction = resolveSwipeDirection(deltaX, deltaY)
                        val settings = settingsProvider()
                        if (settings.doubleSwipeEnabled) {
                            if (pendingDirection == direction) {
                                // 第二次滑动相同方向：触发操作并清除待确认状态
                                dispatchSwipe(position, segmentIndex, direction, false, settings)
                                pendingDirection = null
                            } else {
                                // 第一次滑动（或方向变更）：设为待确认，不触发操作
                                pendingDirection = direction
                            }
                        } else {
                            dispatchSwipe(position, segmentIndex, direction, false, settings)
                        }
                    } else if (absDeltaX <= tapSlop && absDeltaY <= tapSlop) {
                        // 位移在点击容差内：按点击类手势处理
                        val settings = settingsProvider()
                        if (isSecondTap) {
                            // 第二次点击：触发双击动作（单击已在按下阶段取消）
                            val gesture = resolveTapGesture(position, segmentIndex, TapType.DOUBLE, settings)
                            if (gesture.action != GestureAction.NONE) {
                                callback.onGestureAction(gesture.action, gesture.launchAppTarget)
                            }
                        } else {
                            val doubleGesture = resolveTapGesture(position, segmentIndex, TapType.DOUBLE, settings)
                            val singleGesture = resolveTapGesture(position, segmentIndex, TapType.SINGLE, settings)
                            if (doubleGesture.action != GestureAction.NONE) {
                                // 已配置双击：单击延迟到双击窗口结束，避免与双击冲突
                                val runnable = Runnable {
                                    pendingSingleTap = null
                                    if (singleGesture.action != GestureAction.NONE) {
                                        callback.onGestureAction(singleGesture.action, singleGesture.launchAppTarget)
                                    }
                                }
                                pendingSingleTap = runnable
                                handler.postDelayed(runnable, doubleTapTimeout)
                            } else if (singleGesture.action != GestureAction.NONE) {
                                // 未配置双击：单击立即触发
                                callback.onGestureAction(singleGesture.action, singleGesture.launchAppTarget)
                            }
                        }
                    }
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    longPressRunnable?.let { handler.removeCallbacks(it) }
                    pendingDirection = null
                    false
                }
                else -> false
            }
        }
    }

    // 依据起止点位移判定主方向，纵向/横向互斥
    private fun resolveSwipeDirection(deltaX: Float, deltaY: Float): SwipeDirection {
        return if (abs(deltaY) > abs(deltaX)) {
            if (deltaY > 0) SwipeDirection.DOWN else SwipeDirection.UP
        } else {
            if (deltaX > 0) SwipeDirection.RIGHT else SwipeDirection.LEFT
        }
    }

    // 解析并派发滑动类手势（含长按+滑动）
    private fun dispatchSwipe(
        position: EdgePosition,
        segmentIndex: Int,
        direction: SwipeDirection,
        isLongPress: Boolean,
        settings: GestureSettingsState
    ) {
        val gesture = resolveGesture(position, segmentIndex, direction, isLongPress, settings)
        if (gesture.action != GestureAction.NONE) {
            callback.onGestureAction(gesture.action, gesture.launchAppTarget)
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

    // 解析手势对应的动作及其启动应用目标
    private fun resolveGesture(
        position: EdgePosition,
        segmentIndex: Int,
        direction: SwipeDirection,
        isLongPress: Boolean,
        settings: GestureSettingsState
    ): ResolvedGesture {
        val action = resolveAction(position, segmentIndex, direction, isLongPress, settings)
        val target = if (action == GestureAction.LAUNCH_APP) {
            slotOf(position, direction, isLongPress)?.let { slot ->
                settings.launchAppTargets[GestureSettingsKeys.keyFor(position, segmentIndex, slot).name]
            }
        } else {
            null
        }
        return ResolvedGesture(action, target)
    }

    // 解析点击类手势对应的动作及其启动应用目标
    private fun resolveTapGesture(
        position: EdgePosition,
        segmentIndex: Int,
        tapType: TapType,
        settings: GestureSettingsState
    ): ResolvedGesture {
        val config = tapConfig(position, segmentIndex, settings)
        val action = when (tapType) {
            TapType.SINGLE -> config.tap
            TapType.DOUBLE -> config.doubleTap
            TapType.LONG_PRESS -> config.longPress
        }
        val target = if (action == GestureAction.LAUNCH_APP) {
            settings.launchAppTargets[GestureSettingsKeys.keyFor(position, segmentIndex, slotOfTap(tapType)).name]
        } else {
            null
        }
        return ResolvedGesture(action, target)
    }

    // 取指定边缘分段的点击类手势配置
    private fun tapConfig(
        position: EdgePosition,
        segmentIndex: Int,
        settings: GestureSettingsState
    ): EdgeTapGestureConfig = when (position) {
        EdgePosition.LEFT -> leftEdgeConfig(segmentIndex, settings)
        EdgePosition.RIGHT -> rightEdgeConfig(segmentIndex, settings)
        EdgePosition.BOTTOM -> bottomEdgeConfig(segmentIndex, settings)
    }

    // 方向与长按标识映射到配置页的槽位序号（0/1 主方向、2/3 次方向、4/5 第三方向），方向不属于该边缘时返回 null
    private fun slotOf(position: EdgePosition, direction: SwipeDirection, isLongPress: Boolean): Int? {
        val base = when (position) {
            EdgePosition.LEFT -> when (direction) {
                SwipeDirection.RIGHT -> 0
                SwipeDirection.UP -> 2
                SwipeDirection.DOWN -> 4
                else -> null
            }
            EdgePosition.RIGHT -> when (direction) {
                SwipeDirection.LEFT -> 0
                SwipeDirection.UP -> 2
                SwipeDirection.DOWN -> 4
                else -> null
            }
            EdgePosition.BOTTOM -> when (direction) {
                SwipeDirection.UP -> 0
                SwipeDirection.LEFT -> 2
                SwipeDirection.RIGHT -> 4
                else -> null
            }
        }
        return base?.let { it + if (isLongPress) 1 else 0 }
    }

    // 点击类手势映射到配置页的槽位序号（单击 6、双击 7、长按 8）
    private fun slotOfTap(tapType: TapType): Int = when (tapType) {
        TapType.SINGLE -> 6
        TapType.DOUBLE -> 7
        TapType.LONG_PRESS -> 8
    }

    private fun leftEdgeConfig(segmentIndex: Int, settings: GestureSettingsState): LeftEdgeConfig =
        when (segmentIndex) {
            1 -> settings.leftEdgeSegment2
            2 -> settings.leftEdgeSegment3
            else -> settings.leftEdge
        }

    private fun rightEdgeConfig(segmentIndex: Int, settings: GestureSettingsState): RightEdgeConfig =
        when (segmentIndex) {
            1 -> settings.rightEdgeSegment2
            2 -> settings.rightEdgeSegment3
            else -> settings.rightEdge
        }

    private fun bottomEdgeConfig(segmentIndex: Int, settings: GestureSettingsState): BottomEdgeConfig =
        when (segmentIndex) {
            1 -> settings.bottomEdgeSegment2
            2 -> settings.bottomEdgeSegment3
            else -> settings.bottomEdge
        }

    private fun resolveLeftEdgeAction(
        segmentIndex: Int,
        direction: SwipeDirection,
        isLongPress: Boolean,
        settings: GestureSettingsState
    ): GestureAction {
        val config = leftEdgeConfig(segmentIndex, settings)
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
        val config = rightEdgeConfig(segmentIndex, settings)
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
        val config = bottomEdgeConfig(segmentIndex, settings)
        return when (direction) {
            SwipeDirection.UP -> if (isLongPress) config.swipeUpLong else config.swipeUp
            SwipeDirection.LEFT -> if (isLongPress) config.swipeLeftLong else config.swipeLeft
            SwipeDirection.RIGHT -> if (isLongPress) config.swipeRightLong else config.swipeRight
            else -> GestureAction.NONE
        }
    }

}
