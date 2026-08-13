package com.edgegesture.evilgodxu.data.app

import android.content.Context
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

    // 配置导出范围：边缘手势、背面双击、触发区设置、启动拦截
    private const val FORMAT_VERSION = 1
    // 触发区设置中的开关（尺寸/分段通过下方正则匹配）
    private val TRIGGER_AREA_SWITCH_KEYS = setOf(
        "hide_overlay",
        "hide_from_recents",
        "avoid_keyboard_overlap",
        "vibration_enabled",
        "double_swipe_enabled"
    )
    // 匹配 gestureDataStore 中属于导出范围的键名：
    // - 边缘手势：left/right/bottom 开头，可带段号，后接 _swipe_
    // - 背面双击：back_tap_ 开头
    // - 触发区尺寸：left/right/bottom 的 edge_ 或 segment_count
    private val GESTURE_EXPORT_PATTERN = Regex(
        "^(left|right|bottom)(_\\d)?_swipe_|^back_tap_|^(left|right|bottom)_(edge_|segment_count)"
    )

    private fun isGestureExportKey(name: String): Boolean =
        name in TRIGGER_AREA_SWITCH_KEYS || GESTURE_EXPORT_PATTERN.containsMatchIn(name)

    suspend fun export(context: Context): ByteArray = withContext(Dispatchers.IO) {
        val root = JSONObject().put("formatVersion", FORMAT_VERSION)
        val gesture = JSONObject()
        val prefs = context.gestureDataStore.data.first()
        prefs.asMap().forEach { (key, value) ->
            if (isGestureExportKey(key.name)) gesture.put(key.name, encodeValue(value))
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

        // 先完整解析校验所有键值，全部通过后再写入，避免写入中途异常导致配置被清空
        validateObject(gesture)
        validateObject(launch)

        context.gestureDataStore.edit { prefs ->
            // 仅覆盖导出范围内的键，其余数据保持不变
            putObject(prefs, gesture) { isGestureExportKey(it) }
        }
        context.launchBlockDataStore.edit { prefs ->
            prefs.clear()
            putObject(prefs, launch)
        }
    }

    // 校验导入对象中的每个键值类型，非法数据在此抛出，不会触发清空写入
    private fun validateObject(obj: JSONObject) {
        obj.keys().forEach { name ->
            val encoded = obj.getJSONObject(name)
            when (encoded.getString("type")) {
                "boolean" -> encoded.getBoolean("value")
                "int" -> encoded.getInt("value")
                "long" -> encoded.getLong("value")
                "string" -> encoded.getString("value")
                "stringSet" -> {
                    val array = encoded.getJSONArray("value")
                    for (i in 0 until array.length()) array.getString(i)
                }
                else -> throw IllegalArgumentException("未知配置类型: $name")
            }
        }
    }

    private fun encodeValue(value: Any): JSONObject = JSONObject().apply {
        when (value) {
            is Boolean -> put("type", "boolean").put("value", value)
            is Int -> put("type", "int").put("value", value)
            is Long -> put("type", "long").put("value", value)
            is String -> put("type", "string").put("value", value)
            is Set<*> -> put("type", "stringSet").put("value", JSONArray(value.map { it.toString() }))
        }
    }

    private fun putObject(prefs: MutablePreferences, obj: JSONObject, include: (String) -> Boolean = { true }) {
        obj.keys().forEach { name ->
            if (!include(name)) return@forEach
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
