package com.edgegesture.evilgodxu.screens.gesture.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// 避让输入法：输入法可见性与开关共同决定边缘触发区的触摸状态
class ImeTouchGateTest {

    private val touchEvents = mutableListOf<Boolean>()
    private val gate = ImeTouchGate { touchEvents.add(it) }

    @Test
    fun `开关开启时输入法弹出禁用触发区触摸`() {
        gate.update(imeVisible = true, avoidOverlap = true)

        assertEquals(listOf(false), touchEvents)
        assertTrue(gate.isImeVisible)
    }

    @Test
    fun `输入法收起后恢复触发区触摸`() {
        gate.update(imeVisible = true, avoidOverlap = true)

        gate.update(imeVisible = false, avoidOverlap = true)

        assertEquals(listOf(false, true), touchEvents)
        assertFalse(gate.isImeVisible)
    }

    @Test
    fun `开关关闭时输入法弹出仍保持触发区可用`() {
        gate.update(imeVisible = true, avoidOverlap = false)

        assertEquals(emptyList<Boolean>(), touchEvents)
        assertTrue(gate.isImeVisible)
    }

    @Test
    fun `输入法显示中关闭开关立即恢复触摸`() {
        gate.update(imeVisible = true, avoidOverlap = true)

        gate.update(imeVisible = true, avoidOverlap = false)

        assertEquals(listOf(false, true), touchEvents)
    }

    @Test
    fun `输入法可见性未变化时不重复下发触摸状态`() {
        gate.update(imeVisible = true, avoidOverlap = true)

        gate.update(imeVisible = true, avoidOverlap = true)

        assertEquals(listOf(false), touchEvents)
    }

    @Test
    fun `视图重建后重新下发触摸状态`() {
        gate.update(imeVisible = true, avoidOverlap = true)

        gate.invalidate()
        gate.update(imeVisible = true, avoidOverlap = true)

        assertEquals(listOf(false, false), touchEvents)
    }
}
