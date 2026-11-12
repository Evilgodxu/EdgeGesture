package com.edgegesture.evilgodxu.data.gesture

// 手势与拦截的计数
data class GestureStats(
    val gestureCount: Int = 0,
    val blockCount: Int = 0,
)

// 统计周期：1 / 7 / 30 天
enum class StatsPeriod(val days: Int) {
    DAY_1(1),
    DAY_7(7),
    DAY_30(30);

    companion object {
        fun fromDays(days: Int): StatsPeriod = entries.find { it.days == days } ?: DAY_1
    }
}
