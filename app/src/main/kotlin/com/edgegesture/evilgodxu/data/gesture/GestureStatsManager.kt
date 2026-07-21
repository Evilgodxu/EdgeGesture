package com.edgegesture.evilgodxu.data.gesture

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
    val todayBlockCount: Int = 0
)

object GestureStatsManager {

    private val _stats = MutableStateFlow(GestureStats())
    val stats: StateFlow<GestureStats> = _stats.asStateFlow()

    private var scope: CoroutineScope? = null
    private var _initialized = false

    private val STATS_DATE_KEY = stringPreferencesKey("stats_date")
    private val TODAY_GESTURE_COUNT_KEY = intPreferencesKey("today_gesture_count")
    private val TODAY_BLOCK_COUNT_KEY = intPreferencesKey("today_block_count")

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
            val gestureCount = prefs[TODAY_GESTURE_COUNT_KEY] ?: 0
            val blockCount = prefs[TODAY_BLOCK_COUNT_KEY] ?: 0
            _stats.value = _stats.value.copy(
                todayGestureCount = gestureCount,
                todayBlockCount = blockCount
            )
        } else {
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
    }

    // 手势操作计数 +1
    fun incrementGestureCount(context: Context) {
        scope?.launch {
            val latest = _stats.value.todayGestureCount + 1
            context.gestureDataStore.edit { prefs ->
                prefs[TODAY_GESTURE_COUNT_KEY] = latest
            }
            _stats.value = _stats.value.copy(todayGestureCount = latest)
        }
    }

    // 拦截计数 +1
    fun incrementBlockCount(context: Context) {
        scope?.launch {
            val latest = _stats.value.todayBlockCount + 1
            context.gestureDataStore.edit { prefs ->
                prefs[TODAY_BLOCK_COUNT_KEY] = latest
            }
            _stats.value = _stats.value.copy(todayBlockCount = latest)
        }
    }

    private fun getTodayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }
}
