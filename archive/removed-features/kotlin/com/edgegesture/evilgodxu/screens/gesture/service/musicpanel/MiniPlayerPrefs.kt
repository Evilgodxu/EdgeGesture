package com.edgegesture.evilgodxu.screens.gesture.service.musicpanel

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.edgegesture.evilgodxu.data.gesture.gestureDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

private val miniPlayerEnabledKey = booleanPreferencesKey("mini_player_enabled")

// 迷你模式默认开启，与规格说明书一致
fun Context.miniPlayerEnabledFlow(): Flow<Boolean> =
    gestureDataStore.data.map { it[miniPlayerEnabledKey] ?: true }

suspend fun Context.saveMiniPlayerEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
    gestureDataStore.edit { it[miniPlayerEnabledKey] = enabled }
}