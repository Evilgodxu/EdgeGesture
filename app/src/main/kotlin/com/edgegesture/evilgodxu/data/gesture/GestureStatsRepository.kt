package com.edgegesture.evilgodxu.data.gesture

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

// 手势统计仓库契约：按日累计计数，按周期汇总为对外可观察的状态
interface GestureStatsRepository {

    val stats: Flow<GestureStats>

    val period: Flow<StatsPeriod>

    suspend fun setPeriod(period: StatsPeriod)

    suspend fun incrementGestureCount()

    suspend fun incrementBlockCount()
}

// DataStore 实现：按日键累计，统计值由数据派生，不另存副本
class DataStoreGestureStatsRepository(
    private val dataStore: DataStore<Preferences>,
) : GestureStatsRepository {

    override val period: Flow<StatsPeriod> = dataStore.data.map { preferences ->
        StatsPeriod.fromDays(preferences[PERIOD] ?: StatsPeriod.DAY_1.days)
    }

    override val stats: Flow<GestureStats> = combine(dataStore.data, period) { preferences, currentPeriod ->
        countRecentDays(preferences, currentPeriod)
    }

    override suspend fun setPeriod(period: StatsPeriod) {
        dataStore.edit { preferences ->
            preferences[PERIOD] = period.days
        }
    }

    override suspend fun incrementGestureCount() = increment(GESTURE_PREFIX)

    override suspend fun incrementBlockCount() = increment(BLOCK_PREFIX)

    private suspend fun increment(keyPrefix: String) {
        val key = intPreferencesKey(keyPrefix + today())
        dataStore.edit { preferences ->
            preferences[key] = (preferences[key] ?: 0) + 1
        }
    }

    private fun countRecentDays(preferences: Preferences, period: StatsPeriod): GestureStats =
        sumRecentDays(
            period = period,
            gestureAt = { date -> preferences[intPreferencesKey(GESTURE_PREFIX + date)] ?: 0 },
            blockAt = { date -> preferences[intPreferencesKey(BLOCK_PREFIX + date)] ?: 0 },
        )

    private fun today(): String = LocalDate.now().toString()

    private companion object {
        val PERIOD = intPreferencesKey("stats_period")

        // 按日计数键前缀，日期部分为 ISO yyyy-MM-dd，与历史数据格式一致
        const val GESTURE_PREFIX = "daily_gesture_"
        const val BLOCK_PREFIX = "daily_block_"
    }
}

// 按周期累加最近 N 天的计数，日期由调用方提供，便于单测覆盖跨天边界
internal fun sumRecentDays(
    period: StatsPeriod,
    today: LocalDate = LocalDate.now(),
    gestureAt: (LocalDate) -> Int,
    blockAt: (LocalDate) -> Int,
): GestureStats {
    var gestures = 0
    var blocks = 0
    repeat(period.days) { offset ->
        val date = today.minusDays(offset.toLong())
        gestures += gestureAt(date)
        blocks += blockAt(date)
    }
    return GestureStats(gestureCount = gestures, blockCount = blocks)
}
