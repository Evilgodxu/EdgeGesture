package com.edgegesture.evilgodxu.data.gesture

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

// 扩展面板快捷方式存储键
object ExpandPanelSettingsKeys {
    private const val PREFIX = "expand_panel_shortcut_"

    val SHORTCUT_1 = stringPreferencesKey("${PREFIX}1")
    val SHORTCUT_2 = stringPreferencesKey("${PREFIX}2")
    val SHORTCUT_3 = stringPreferencesKey("${PREFIX}3")
    val SHORTCUT_4 = stringPreferencesKey("${PREFIX}4")
    val SHORTCUT_5 = stringPreferencesKey("${PREFIX}5")
    val SHORTCUT_6 = stringPreferencesKey("${PREFIX}6")
    val SHORTCUT_7 = stringPreferencesKey("${PREFIX}7")
    val SHORTCUT_8 = stringPreferencesKey("${PREFIX}8")

    val ALL_KEYS = listOf(SHORTCUT_1, SHORTCUT_2, SHORTCUT_3, SHORTCUT_4, SHORTCUT_5, SHORTCUT_6, SHORTCUT_7, SHORTCUT_8)
}

// 扩展面板快捷方式小窗启动开关存储键
object ExpandPanelFreeformKeys {
    private const val PREFIX = "expand_panel_shortcut_freeform_"

    val FREEFORM_1 = booleanPreferencesKey("${PREFIX}1")
    val FREEFORM_2 = booleanPreferencesKey("${PREFIX}2")
    val FREEFORM_3 = booleanPreferencesKey("${PREFIX}3")
    val FREEFORM_4 = booleanPreferencesKey("${PREFIX}4")
    val FREEFORM_5 = booleanPreferencesKey("${PREFIX}5")
    val FREEFORM_6 = booleanPreferencesKey("${PREFIX}6")
    val FREEFORM_7 = booleanPreferencesKey("${PREFIX}7")
    val FREEFORM_8 = booleanPreferencesKey("${PREFIX}8")

    val ALL_KEYS = listOf(FREEFORM_1, FREEFORM_2, FREEFORM_3, FREEFORM_4, FREEFORM_5, FREEFORM_6, FREEFORM_7, FREEFORM_8)
}

// 扩展面板快捷方式状态
data class ExpandPanelShortcutsState(
    val shortcuts: List<String?> = List(8) { null },
    val freeformFlags: List<Boolean> = List(8) { false }
)

// 扩展面板快捷方式数据流
fun Context.expandPanelShortcutsFlow(): Flow<ExpandPanelShortcutsState> = gestureDataStore.data.map { prefs ->
    val shortcuts = ExpandPanelSettingsKeys.ALL_KEYS.map { key ->
        prefs[key]
    }
    val freeformFlags = ExpandPanelFreeformKeys.ALL_KEYS.map { key ->
        prefs[key] ?: false
    }
    ExpandPanelShortcutsState(shortcuts = shortcuts, freeformFlags = freeformFlags)
}

// 保存单个快捷方式
suspend fun Context.saveExpandPanelShortcut(index: Int, packageName: String?) = withContext(Dispatchers.IO) {
    val key = ExpandPanelSettingsKeys.ALL_KEYS.getOrNull(index) ?: return@withContext
    gestureDataStore.edit { prefs ->
        if (packageName == null) {
            prefs.minusAssign(key)
        } else {
            prefs[key] = packageName
        }
    }
}

// 保存单个快捷方式的小窗启动开关
suspend fun Context.saveExpandPanelShortcutFreeform(index: Int, enabled: Boolean) = withContext(Dispatchers.IO) {
    val key = ExpandPanelFreeformKeys.ALL_KEYS.getOrNull(index) ?: return@withContext
    gestureDataStore.edit { prefs ->
        if (enabled) {
            prefs[key] = true
        } else {
            prefs.minusAssign(key)
        }
    }
}

// 重置所有快捷方式
suspend fun Context.resetExpandPanelShortcuts() = withContext(Dispatchers.IO) {
    gestureDataStore.edit { prefs ->
        ExpandPanelSettingsKeys.ALL_KEYS.forEach { key ->
            prefs.minusAssign(key)
        }
        ExpandPanelFreeformKeys.ALL_KEYS.forEach { key ->
            prefs.minusAssign(key)
        }
    }
}

// 清理指定包名的快捷方式（应用卸载时调用）
suspend fun Context.clearExpandPanelShortcut(packageName: String) = withContext(Dispatchers.IO) {
    gestureDataStore.edit { prefs ->
        ExpandPanelSettingsKeys.ALL_KEYS.forEachIndexed { index, key ->
            if (prefs[key] == packageName) {
                prefs.minusAssign(key)
                val freeformKey = ExpandPanelFreeformKeys.ALL_KEYS.getOrNull(index)
                if (freeformKey != null) {
                    prefs.minusAssign(freeformKey)
                }
            }
        }
    }
}
