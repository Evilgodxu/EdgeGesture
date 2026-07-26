package com.edgegesture.evilgodxu.data.app

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.edgegesture.evilgodxu.data.gesture.gestureDataStore
import com.edgegesture.evilgodxu.data.gesture.GestureStatsManager
import com.edgegesture.evilgodxu.data.launchblock.LaunchBlockKeys
import com.edgegesture.evilgodxu.data.launchblock.launchBlockDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

enum class ManagedDataType {
    APP_ICONS, MUSIC_COVERS, MUSIC_LYRICS, MUSIC_PLAYLIST, MUSIC_POSITION,
    UPDATE_CACHE, STATS, TEMP
}

data class ManagedDataItem(val type: ManagedDataType, val size: Long)

object DataConfigManager {
    private const val FORMAT_VERSION = 1
    private const val UPDATE_PREFS = "update_prefs"
    private val excludedKeys = setOf(
        "music_saved_uri",
        "music_saved_position",
        "app_switch_blacklist",
        "blacklist_initialized"
    )

    suspend fun listData(context: Context): List<ManagedDataItem> = withContext(Dispatchers.IO) {
        listOf(
            ManagedDataItem(ManagedDataType.APP_ICONS, directorySize(File(context.cacheDir, "app-icons"))),
            ManagedDataItem(ManagedDataType.MUSIC_COVERS, directorySize(File(context.filesDir, "music_metadata/covers_v2")) + directorySize(File(context.filesDir, "music_metadata/covers"))),
            ManagedDataItem(ManagedDataType.MUSIC_LYRICS, directorySize(File(context.filesDir, "music_metadata/lyrics"))),
            ManagedDataItem(ManagedDataType.MUSIC_PLAYLIST, sharedPreferencesSize(context, "music_playlist_cache_preferences")),
            ManagedDataItem(ManagedDataType.MUSIC_POSITION, 0L),
            ManagedDataItem(ManagedDataType.UPDATE_CACHE, sharedPreferencesSize(context, UPDATE_PREFS) + directorySize(context.externalCacheDir)),
            ManagedDataItem(ManagedDataType.STATS, statsSize(context)),
            ManagedDataItem(ManagedDataType.TEMP, 0L)
        )
    }

    suspend fun clear(context: Context, selected: Set<ManagedDataType>, stopMusic: suspend () -> Unit) = withContext(Dispatchers.IO) {
        if (selected.any { it in setOf(ManagedDataType.MUSIC_COVERS, ManagedDataType.MUSIC_LYRICS, ManagedDataType.MUSIC_PLAYLIST, ManagedDataType.MUSIC_POSITION) }) {
            stopMusic()
        }
        if (ManagedDataType.APP_ICONS in selected) {
            File(context.cacheDir, "app-icons").deleteRecursively()
            context.appCacheDataStore.edit { prefs ->
                prefs.remove(AppCacheKeys.CACHED_APPS_JSON)
                prefs.remove(AppCacheKeys.LAST_CACHE_TIME)
                prefs.remove(AppCacheKeys.CACHE_VERSION)
            }
        }
        if (ManagedDataType.MUSIC_COVERS in selected) {
            File(context.filesDir, "music_metadata/covers_v2").deleteRecursively()
            File(context.filesDir, "music_metadata/covers").deleteRecursively()
            File(context.filesDir, "music_metadata/covers_original").deleteRecursively()
        }
        if (ManagedDataType.MUSIC_LYRICS in selected) File(context.filesDir, "music_metadata/lyrics").deleteRecursively()
        if (ManagedDataType.MUSIC_PLAYLIST in selected) context.getSharedPreferences("music_playlist_cache_preferences", Context.MODE_PRIVATE).edit().clear().apply()
        if (ManagedDataType.MUSIC_POSITION in selected) context.gestureDataStore.edit { prefs ->
            prefs.remove(stringPreferencesKey("music_saved_uri"))
            prefs.remove(longPreferencesKey("music_saved_position"))
        }
        if (ManagedDataType.UPDATE_CACHE in selected) {
            context.getSharedPreferences(UPDATE_PREFS, Context.MODE_PRIVATE).edit().clear().apply()
            context.externalCacheDir?.deleteRecursively()
        }
        if (ManagedDataType.STATS in selected) GestureStatsManager.resetStats(context)
        if (selected.any { it in setOf(ManagedDataType.APP_ICONS, ManagedDataType.MUSIC_COVERS, ManagedDataType.MUSIC_LYRICS, ManagedDataType.MUSIC_PLAYLIST) }) {
            File(context.cacheDir, "app-icons").mkdirs()
            File(context.filesDir, "music_metadata").mkdirs()
        }
    }

    suspend fun export(context: Context): ByteArray = withContext(Dispatchers.IO) {
        val root = JSONObject().put("formatVersion", FORMAT_VERSION)
        val gesture = JSONObject()
        val prefs = context.gestureDataStore.data.first()
        prefs.asMap().forEach { (key, value) ->
            if (!isExportedKey(key.name)) gesture.put(key.name, encodeValue(value))
        }
        root.put("gesture", gesture)
        root.put("launchBlock", JSONObject().apply {
            val values = context.launchBlockDataStore.data.first().asMap()
            values.forEach { (key, value) -> put(key.name, encodeValue(value)) }
        })
        root.toString(2).toByteArray(Charsets.UTF_8)
    }

    suspend fun import(context: Context, bytes: ByteArray) = withContext(Dispatchers.IO) {
        val root = JSONObject(bytes.toString(Charsets.UTF_8))
        require(root.optInt("formatVersion") == FORMAT_VERSION) { "不支持的配置版本" }
        val gesture = root.getJSONObject("gesture")
        val launch = root.getJSONObject("launchBlock")
        val currentGesture = context.gestureDataStore.data.first().asMap()
        context.gestureDataStore.edit { prefs ->
            val preserved = currentGesture.filter { !isExportedKey(it.key.name) }
            prefs.clear()
            preserved.forEach { (key, value) -> putValue(prefs, key.name, value) }
            putObject(prefs, gesture)
        }
        context.launchBlockDataStore.edit { prefs ->
            prefs.clear()
            putObject(prefs, launch)
        }
    }

    private fun isExportedKey(name: String): Boolean =
        name.startsWith("expand_panel_shortcut_") || name.startsWith("daily_gesture_") ||
                name.startsWith("daily_block_") || name == "stats_period" || name in excludedKeys

    private fun encodeValue(value: Any): JSONObject = JSONObject().apply {
        when (value) {
            is Boolean -> put("type", "boolean").put("value", value)
            is Int -> put("type", "int").put("value", value)
            is Long -> put("type", "long").put("value", value)
            is String -> put("type", "string").put("value", value)
            is Set<*> -> put("type", "stringSet").put("value", JSONArray(value.map { it.toString() }))
        }
    }

    private fun putValue(prefs: MutablePreferences, name: String, value: Any) {
        when (value) {
            is Boolean -> prefs[booleanPreferencesKey(name)] = value
            is Int -> prefs[intPreferencesKey(name)] = value
            is Long -> prefs[longPreferencesKey(name)] = value
            is String -> prefs[stringPreferencesKey(name)] = value
            is Set<*> -> prefs[stringSetPreferencesKey(name)] = value.map { it.toString() }.toSet()
        }
    }

    private fun putObject(prefs: MutablePreferences, obj: JSONObject) {
        obj.keys().forEach { name ->
            val encoded = obj.getJSONObject(name)
            when (encoded.getString("type")) {
                "boolean" -> prefs[booleanPreferencesKey(name)] = encoded.getBoolean("value")
                "int" -> prefs[intPreferencesKey(name)] = encoded.getInt("value")
                "long" -> prefs[longPreferencesKey(name)] = encoded.getLong("value")
                "string" -> prefs[stringPreferencesKey(name)] = encoded.getString("value")
                "stringSet" -> prefs[stringSetPreferencesKey(name)] = buildSet(encoded.getJSONArray("value"))
            }
        }
    }

    private fun buildSet(array: JSONArray): Set<String> = buildSet {
        for (i in 0 until array.length()) add(array.getString(i))
    }

    private fun directorySize(file: File?): Long = file?.takeIf { it.exists() }?.walkTopDown()?.filter { it.isFile }?.sumOf { it.length() } ?: 0L
    private fun sharedPreferencesSize(context: Context, name: String): Long = directorySize(File(context.dataDir, "shared_prefs/$name.xml"))
    private suspend fun statsSize(context: Context): Long = context.gestureDataStore.data.first().asMap().filter { it.key.name.startsWith("daily_") }.size.toLong()
}
