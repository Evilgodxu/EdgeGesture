package com.edgegesture.evilgodxu.data.gesture

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class GestureStats(
    val todayGestureCount: Int = 0,
    val todayBlockCount: Int = 0,
    val uptimeMs: Long = 0L
)

object GestureStatsManager {

    private val _stats = MutableStateFlow(GestureStats())
    val stats: StateFlow<GestureStats> = _stats.asStateFlow()

    private var scope: CoroutineScope? = null
    private var uptimeJob: Job? = null
    private var serviceStartTime: Long = 0L
    private var _initialized = false

    private val STATS_DATE_KEY = stringPreferencesKey("stats_date")
    private val TODAY_GESTURE_COUNT_KEY = intPreferencesKey("today_gesture_count")
    private val TODAY_BLOCK_COUNT_KEY = intPreferencesKey("today_block_count")
    private val UPTIME_START_KEY = longPreferencesKey("uptime_start_time")

    fun init(context: Context) {
        if (_initialized) return
        _initialized = true
        scope = CoroutineScope(Dispatchers.IO + kotlinx.coroutines.SupervisorJob())
        scope?.launch {
            loadFromDataStore(context)
        }
    }

    private suspend fun loadFromDataStore(context: Context) {
        val prefs = context.gestureDataStore.data.first()
        val today = getTodayDateString()
        val storedDate = prefs[STATS_DATE_KEY] ?: ""

        if (storedDate == today) {
            // 同日，使用已存储的计数
            val gestureCount = prefs[TODAY_GESTURE_COUNT_KEY] ?: 0
            val blockCount = prefs[TODAY_BLOCK_COUNT_KEY] ?: 0
            _stats.value = _stats.value.copy(
                todayGestureCount = gestureCount,
                todayBlockCount = blockCount
            )
        } else {
            // 新的一天，重置计数
            _stats.value = _stats.value.copy(
                todayGestureCount = 0,
                todayBlockCount = 0
            )
            context.gestureDataStore.edit { prefsEdit ->
                prefsEdit[STATS_DATE_KEY] = today
                prefsEdit[TODAY_GESTURE_COUNT_KEY] = 0
                prefsEdit[TODAY_BLOCK_COUNT_KEY] = 0
            }
        }

        // 恢复运行时长
        val uptimeStart = prefs[UPTIME_START_KEY] ?: 0L
        if (uptimeStart > 0L) {
            serviceStartTime = uptimeStart
            val elapsed = System.currentTimeMillis() - uptimeStart
            _stats.value = _stats.value.copy(uptimeMs = elapsed)
        }
    }

    // 手势操作计数 +1
    fun incrementGestureCount(context: Context) {
        val current = _stats.value.todayGestureCount
        _stats.value = _stats.value.copy(todayGestureCount = current + 1)
        scope?.launch {
            context.gestureDataStore.edit { prefs ->
                prefs[TODAY_GESTURE_COUNT_KEY] = current + 1
            }
        }
    }

    // 拦截计数 +1
    fun incrementBlockCount(context: Context) {
        val current = _stats.value.todayBlockCount
        _stats.value = _stats.value.copy(todayBlockCount = current + 1)
        scope?.launch {
            context.gestureDataStore.edit { prefs ->
                prefs[TODAY_BLOCK_COUNT_KEY] = current + 1
            }
        }
    }

    // 开始统计运行时长
    fun startUptime(context: Context) {
        serviceStartTime = System.currentTimeMillis()
        scope?.launch {
            context.gestureDataStore.edit { prefs ->
                prefs[UPTIME_START_KEY] = serviceStartTime
            }
        }
        uptimeJob?.cancel()
        uptimeJob = scope?.launch {
            while (true) {
                val elapsed = System.currentTimeMillis() - serviceStartTime
                _stats.value = _stats.value.copy(uptimeMs = elapsed)
                delay(1000L)
            }
        }
    }

    // 停止统计运行时长
    fun stopUptime(context: Context) {
        uptimeJob?.cancel()
        uptimeJob = null
        serviceStartTime = 0L
        _stats.value = _stats.value.copy(uptimeMs = 0L)
        scope?.launch {
            context.gestureDataStore.edit { prefs ->
                prefs.remove(UPTIME_START_KEY)
            }
        }
    }

    private fun getTodayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }
}
