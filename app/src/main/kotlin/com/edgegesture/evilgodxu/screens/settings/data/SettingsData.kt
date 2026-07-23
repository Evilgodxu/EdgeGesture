package com.edgegesture.evilgodxu.screens.settings.data

import android.app.LocaleManager
import android.content.Context
import android.os.LocaleList
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.edgegesture.evilgodxu.data.gesture.gestureSettingsFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

// 应用设置 DataStore 实例
val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

// 设置存储键名定义
object SettingsKeys {
    val THEME_MODE = stringPreferencesKey("theme_mode")
}

// 应用主题模式
enum class ThemeMode(val value: String) {
    SYSTEM("system"),
    DARK("dark"),
    LIGHT("light");

    companion object {
        fun fromValue(value: String): ThemeMode = entries.find { it.value == value } ?: SYSTEM
    }
}

// 应用语言设置，通过 LocaleManager 管理应用内语言偏好
enum class AppLanguage(val languageTag: String?) {
    SYSTEM(null),
    CHINESE("zh"),
    ENGLISH("en"),
    JAPANESE("ja"),
    KOREAN("ko"),
    RUSSIAN("ru"),
    GERMAN("de");

    companion object {
        fun fromLocaleList(localeList: LocaleList): AppLanguage {
            if (localeList.isEmpty) return SYSTEM
            val tag = localeList[0].toLanguageTag()
            return entries.find { it.languageTag == tag } ?: SYSTEM
        }
    }
}

// 设置状态数据类
data class SettingsState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val vibrationEnabled: Boolean = false
)

// 获取设置状态流，合并主题和震动设置
fun Context.settingsFlow(): Flow<SettingsState> = settingsDataStore.data.map { preferences ->
    SettingsState(
        themeMode = ThemeMode.fromValue(preferences[SettingsKeys.THEME_MODE] ?: ThemeMode.SYSTEM.value)
    )
}.combine(gestureSettingsFlow()) { settings, gestureSettings ->
    settings.copy(vibrationEnabled = gestureSettings.vibrationEnabled)
}

// 获取主题模式流
fun Context.themeModeFlow(): Flow<ThemeMode> = settingsDataStore.data.map { preferences ->
    ThemeMode.fromValue(preferences[SettingsKeys.THEME_MODE] ?: ThemeMode.SYSTEM.value)
}

// 保存主题模式设置
suspend fun Context.saveThemeMode(mode: ThemeMode) = withContext(Dispatchers.IO) {
    settingsDataStore.edit { preferences ->
        preferences[SettingsKeys.THEME_MODE] = mode.value
    }
}

// 读取当前应用语言
fun Context.getAppLanguage(): AppLanguage {
    val locales = getSystemService(LocaleManager::class.java).applicationLocales
    return AppLanguage.fromLocaleList(locales)
}

// 设置应用语言，系统自动持久化并触发配置变更
fun Context.setAppLanguage(language: AppLanguage) {
    val localeManager = getSystemService(LocaleManager::class.java)
    localeManager.applicationLocales = if (language.languageTag != null) {
        LocaleList.forLanguageTags(language.languageTag)
    } else {
        LocaleList.getEmptyLocaleList()
    }
}
