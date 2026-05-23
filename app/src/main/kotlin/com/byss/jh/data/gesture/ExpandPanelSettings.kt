package com.byss.jh.data.gesture

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * 扩展面板快捷方式存储键
 */
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

/**
 * 扩展面板快捷方式状态
 */
data class ExpandPanelShortcutsState(
    val shortcuts: List<String?> = List(8) { null }
)

/**
 * 扩展面板快捷方式数据流
 */
fun Context.expandPanelShortcutsFlow(): Flow<ExpandPanelShortcutsState> = gestureDataStore.data.map { prefs ->
    val shortcuts = ExpandPanelSettingsKeys.ALL_KEYS.map { key ->
        prefs[key]
    }
    ExpandPanelShortcutsState(shortcuts = shortcuts)
}

/**
 * 保存单个快捷方式
 */
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

/**
 * 重置所有快捷方式
 */
suspend fun Context.resetExpandPanelShortcuts() = withContext(Dispatchers.IO) {
    gestureDataStore.edit { prefs ->
        ExpandPanelSettingsKeys.ALL_KEYS.forEach { key ->
            prefs.minusAssign(key)
        }
    }
}
