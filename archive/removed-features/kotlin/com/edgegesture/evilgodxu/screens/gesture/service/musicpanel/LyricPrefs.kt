package com.edgegesture.evilgodxu.screens.gesture.service.musicpanel

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.edgegesture.evilgodxu.data.gesture.gestureDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

private val wordByWordRenderingKey = booleanPreferencesKey("word_by_word_rendering")

// 歌词逐字渲染默认开启：关闭后整行高亮，不再逐字点亮与跳动
fun Context.wordByWordRenderingFlow(): Flow<Boolean> =
    gestureDataStore.data.map { it[wordByWordRenderingKey] ?: true }

suspend fun Context.saveWordByWordRendering(enabled: Boolean) = withContext(Dispatchers.IO) {
    gestureDataStore.edit { it[wordByWordRenderingKey] = enabled }
}