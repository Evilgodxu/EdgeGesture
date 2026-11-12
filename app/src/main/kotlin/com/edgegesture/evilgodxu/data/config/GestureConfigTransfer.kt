package com.edgegesture.evilgodxu.data.config

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

// 手势与启动拦截配置的导入导出：导出范围由键名规则决定，导入先全量校验再写入
class GestureConfigTransfer(
    private val gestureDataStore: DataStore<Preferences>,
    private val launchBlockDataStore: DataStore<Preferences>,
) {

    suspend fun export(): ByteArray = withContext(Dispatchers.IO) {
        val root = JSONObject().put("formatVersion", FORMAT_VERSION)

        val gesture = JSONObject()
        gestureDataStore.data.first().asMap().forEach { (key, value) ->
            if (isGestureExportKey(key.name)) gesture.put(key.name, encodeValue(value))
        }
        root.put("gesture", gesture)

        root.put("launchBlock", JSONObject().apply {
            launchBlockDataStore.data.first().asMap().forEach { (key, value) ->
                put(key.name, encodeValue(value))
            }
        })

        root.toString(2).toByteArray(Charsets.UTF_8)
    }

    suspend fun import(bytes: ByteArray) = withContext(Dispatchers.IO) {
        val root = JSONObject(bytes.toString(Charsets.UTF_8))
        require(root.optInt("formatVersion") == FORMAT_VERSION) { "不支持的配置版本" }
        val gesture = root.getJSONObject("gesture")
        val launch = root.getJSONObject("launchBlock")

        // 先完整解析校验所有键值，全部通过后再写入，避免写入中途异常导致配置被清空
        validateObject(gesture)
        validateObject(launch)

        gestureDataStore.edit { preferences ->
            // 仅覆盖导出范围内的键，其余数据保持不变
            putObject(preferences, gesture) { isGestureExportKey(it) }
        }
        launchBlockDataStore.edit { preferences ->
            preferences.clear()
            putObject(preferences, launch)
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

    private fun putObject(
        preferences: MutablePreferences,
        obj: JSONObject,
        include: (String) -> Boolean = { true },
    ) {
        obj.keys().forEach { name ->
            if (!include(name)) return@forEach
            val encoded = obj.getJSONObject(name)
            when (encoded.getString("type")) {
                "boolean" -> preferences[booleanPreferencesKey(name)] = encoded.getBoolean("value")
                "int" -> preferences[intPreferencesKey(name)] = encoded.getInt("value")
                "long" -> preferences[longPreferencesKey(name)] = encoded.getLong("value")
                "string" -> preferences[stringPreferencesKey(name)] = encoded.getString("value")
                "stringSet" -> preferences[stringSetPreferencesKey(name)] = buildSet(encoded.getJSONArray("value"))
            }
        }
    }

    private fun buildSet(array: JSONArray): Set<String> = buildSet {
        for (i in 0 until array.length()) add(array.getString(i))
    }

    private companion object {
        // 配置导出范围：边缘手势、背面双击、触发区设置、启动拦截
        const val FORMAT_VERSION = 1

        // 触发区设置中的开关（尺寸/分段通过键名正则匹配）
        val TRIGGER_AREA_SWITCH_KEYS = setOf(
            "hide_overlay",
            "hide_from_recents",
            "avoid_keyboard_overlap",
            "vibration_enabled",
            "double_swipe_enabled",
        )

        // 匹配 gestureDataStore 中属于导出范围的键名：
        // - 边缘手势：left/right/bottom 开头，可带段号，后接 _swipe_ 或 _tap/_double_tap/_long_press
        // - 背面双击：back_tap_ 开头
        // - 触发区尺寸：left/right/bottom 的 edge_ 或 segment_count
        val GESTURE_EXPORT_PATTERN = Regex(
            "^(left|right|bottom)(_\\d)?_(swipe_|tap|double_tap|long_press)|^back_tap_|^(left|right|bottom)_(edge_|segment_count)",
        )

        fun isGestureExportKey(name: String): Boolean =
            name in TRIGGER_AREA_SWITCH_KEYS || GESTURE_EXPORT_PATTERN.containsMatchIn(name)
    }
}
