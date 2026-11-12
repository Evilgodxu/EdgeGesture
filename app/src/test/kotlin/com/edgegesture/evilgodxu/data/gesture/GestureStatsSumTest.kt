package com.edgegesture.evilgodxu.data.gesture

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

// 统计周期求和：只累加周期覆盖的日期，边界日计入
class GestureStatsSumTest {

    private val today = LocalDate.of(2026, 11, 12)

    private fun gestureCounts(vararg days: Pair<String, Int>): (LocalDate) -> Int {
        val map = days.associate { LocalDate.parse(it.first) to it.second }
        return { date -> map[date] ?: 0 }
    }

    @Test
    fun `单日周期只统计当天`() {
        val gestures = gestureCounts(
            "2026-11-12" to 3,
            "2026-11-11" to 5,
        )

        val stats = sumRecentDays(StatsPeriod.DAY_1, today, gestures) { 0 }

        assertEquals(3, stats.gestureCount)
    }

    @Test
    fun `七日周期包含第七天且排除更早的数据`() {
        val gestures = gestureCounts(
            "2026-11-12" to 1,
            "2026-11-06" to 2, // 第 7 天（含）
            "2026-11-05" to 4, // 第 8 天（排除）
        )

        val stats = sumRecentDays(StatsPeriod.DAY_7, today, gestures) { 0 }

        assertEquals(3, stats.gestureCount)
    }

    @Test
    fun `手势与拦截分别累加`() {
        val stats = sumRecentDays(
            period = StatsPeriod.DAY_7,
            today = today,
            gestureAt = gestureCounts("2026-11-12" to 2, "2026-11-10" to 3),
            blockAt = gestureCounts("2026-11-11" to 7),
        )

        assertEquals(GestureStats(gestureCount = 5, blockCount = 7), stats)
    }
}
