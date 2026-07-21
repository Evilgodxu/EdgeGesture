package com.edgegesture.evilgodxu.data.gesture

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class GestureStats(
    val gestureCount: Int = 0,
    val blockCount: Int = 0
)

enum class StatsPeriod(val days: Int) {
    DAY_1(1),
    DAY_7(7),
    DAY_30(30);

    companion object {
        fun fromDays(days: Int): StatsPeriod =
            entries.find { it.days == days } ?: DAY_1
    }
}

object GestureStatsManager {

    private val _stats = MutableStateFlow(GestureStats())
    val stats: StateFlow<GestureStats> = _stats.asStateFlow()

    private val _period = MutableStateFlow(StatsPeriod.DAY_1)
    val period: StateFlow<StatsPeriod> = _period.asStateFlow()

    private var scope: CoroutineScope? = null
    private var _initialized = false

    private val STATS_PERIOD_KEY = intPreferencesKey("stats_period")

    fun init(context: Context) {
        if (_initialized) return
        _initialized = true
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        scope?.launch {
            loadPeriod(context)
            loadStats(context)
        }
    }

    private suspend fun loadPeriod(context: Context) {
        val prefs = context.gestureDataStore.data.first()
        val savedPeriod = prefs[STATS_PERIOD_KEY] ?: 1
        _period.value = StatsPeriod.fromDays(savedPeriod)
    }

    suspend fun setPeriod(context: Context, period: StatsPeriod) {
        _period.value = period
        context.gestureDataStore.edit { prefs ->
            prefs[STATS_PERIOD_KEY] = period.days
        }
        loadStats(context)
    }

    private suspend fun loadStats(context: Context) {
        val prefs = context.gestureDataStore.data.first()
        val periodDays = _period.value.days
        var totalGesture = 0
        var totalBlock = 0

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()

        for (i in 0 until periodDays) {
            val dateStr = dateFormat.format(cal.time)
            totalGesture += prefs[intPreferencesKey("daily_gesture_$dateStr")] ?: 0
            totalBlock += prefs[intPreferencesKey("daily_block_$dateStr")] ?: 0
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }

        _stats.value = GestureStats(totalGesture, totalBlock)
    }

    // 手势操作计数 +1
    fun incrementGestureCount(context: Context) {
        scope?.launch {
            val today = getTodayDateString()
            val key = intPreferencesKey("daily_gesture_$today")
            val current = context.gestureDataStore.data.first()[key] ?: 0
            val latest = current + 1
            context.gestureDataStore.edit { prefs ->
                prefs[key] = latest
            }
            // 仅在周期为1天时直接更新内存计数，否则重新加载
            if (_period.value == StatsPeriod.DAY_1) {
                _stats.value = _stats.value.copy(gestureCount = latest)
            } else {
                loadStats(context)
            }
        }
    }

    // 拦截计数 +1
    fun incrementBlockCount(context: Context) {
        scope?.launch {
            val today = getTodayDateString()
            val key = intPreferencesKey("daily_block_$today")
            val current = context.gestureDataStore.data.first()[key] ?: 0
            val latest = current + 1
            context.gestureDataStore.edit { prefs ->
                prefs[key] = latest
            }
            if (_period.value == StatsPeriod.DAY_1) {
                _stats.value = _stats.value.copy(blockCount = latest)
            } else {
                loadStats(context)
            }
        }
    }

    // 统计数据重置
    suspend fun resetStats(context: Context) {
        context.gestureDataStore.edit { prefs ->
            // 清除最近30天的数据
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val cal = Calendar.getInstance()
            for (i in 0 until 30) {
                val dateStr = dateFormat.format(cal.time)
                prefs.remove(intPreferencesKey("daily_gesture_$dateStr"))
                prefs.remove(intPreferencesKey("daily_block_$dateStr"))
                cal.add(Calendar.DAY_OF_YEAR, -1)
            }
        }
        loadStats(context)
    }

    private fun getTodayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }
}
