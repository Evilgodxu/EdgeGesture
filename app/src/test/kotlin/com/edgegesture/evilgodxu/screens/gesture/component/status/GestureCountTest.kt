package com.edgegesture.evilgodxu.screens.gesture.component.status

import com.edgegesture.evilgodxu.data.gesture.GestureAction
import com.edgegesture.evilgodxu.data.gesture.GestureSettingsState
import org.junit.Assert.assertEquals
import org.junit.Test

// 已配置手势数的统计口径：只统计非 NONE 动作，且只计入已启用的分段
class GestureCountTest {

    @Test
    fun `默认配置只统计第一段的已配置动作`() {
        // 左 6 + 右 6 + 底 4：默认只有滑动类动作，点击类与两个底部滑动手势为 NONE
        assertEquals(16, countNonNoneGestures(GestureSettingsState()))
    }

    @Test
    fun `未启用的第二段不计入统计`() {
        val state = GestureSettingsState(
            leftEdgeSegment2 = GestureSettingsState().leftEdgeSegment2.copy(tap = GestureAction.HOME),
        )

        assertEquals(16, countNonNoneGestures(state))
    }

    @Test
    fun `启用第二段后新增动作用于统计`() {
        val state = GestureSettingsState(
            leftSegmentCount = 2,
            leftEdgeSegment2 = GestureSettingsState().leftEdgeSegment2.copy(tap = GestureAction.HOME),
        )

        assertEquals(17, countNonNoneGestures(state))
    }

    @Test
    fun `新增点击类动作计入统计`() {
        val base = GestureSettingsState()
        val state = base.copy(leftEdge = base.leftEdge.copy(longPress = GestureAction.HOME))

        assertEquals(17, countNonNoneGestures(state))
    }
}
