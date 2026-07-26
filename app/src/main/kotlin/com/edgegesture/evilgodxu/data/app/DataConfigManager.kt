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
import com.edgegesture.evilgodxu.data.launchblock.launchBlockDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

enum class ManagedDataType {
    MUSIC_COVERS, MUSIC_LYRICS
}

data class ManagedDataItem(val type: ManagedDataType, val size: Long)

object DataConfigManager {

    suspend fun listData(context: Context): List<ManagedDataItem> = withContext(Dispatchers.IO) {
        listOf(
            ManagedDataItem(ManagedDataType.MUSIC_COVERS, directorySize(File(context.filesDir, "music_metadata/covers_v2")) + directorySize(File(context.filesDir, "music_metadata/covers_original"))),
            ManagedDataItem(ManagedDataType.MUSIC_LYRICS, directorySize(File(context.filesDir, "music_metadata/lyrics")))
        )
    }

    suspend fun clear(context: Context, selected: Set<ManagedDataType>, stopMusic: suspend () -> Unit) = withContext(Dispatchers.IO) {
        if (selected.any { it in setOf(ManagedDataType.MUSIC_COVERS, ManagedDataType.MUSIC_LYRICS) }) {
            stopMusic()
        }
        if (ManagedDataType.MUSIC_COVERS in selected) {
            File(context.filesDir, "music_metadata/covers_v2").deleteRecursively()
            File(context.filesDir, "music_metadata/covers_original").deleteRecursively()
        }
        if (ManagedDataType.MUSIC_LYRICS in selected) {
            File(context.filesDir, "music_metadata/lyrics").deleteRecursively()
        }
        if (selected.any { it in setOf(ManagedDataType.MUSIC_COVERS, ManagedDataType.MUSIC_LYRICS) }) {
            File(context.filesDir, "music_metadata").mkdirs()
        }
    }

    private fun directorySize(file: File?): Long = file?.takeIf { it.exists() }?.walkTopDown()?.filter { it.isFile }?.sumOf { it.length() } ?: 0L

    // 配置导出（手势配置 + 启动拦截配置）
    private const val FORMAT_VERSION = 1
    private val excludedKeys = setOf(
        "music_saved_uri",
        "music_saved_position",
        "app_switch_blacklist",
        "blacklist_initialized"
    )

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
}
