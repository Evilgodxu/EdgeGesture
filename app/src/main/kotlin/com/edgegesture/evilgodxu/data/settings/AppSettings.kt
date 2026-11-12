package com.edgegesture.evilgodxu.data.settings

import androidx.datastore.preferences.core.stringPreferencesKey

object SettingsKeys {
    val THEME_MODE = stringPreferencesKey("theme_mode")

    // 语言统一落 DataStore，由 Compose 层驱动热切换
    val LANGUAGE = stringPreferencesKey("language")
}

// 应用主题模式
enum class ThemeMode(val value: String) {
    SYSTEM("system"),
    DARK("dark"),
    LIGHT("light");

    companion object {
        // 未知值回落到跟随系统，避免旧数据或手改文件导致崩溃
        fun fromValue(value: String): ThemeMode = entries.find { it.value == value } ?: SYSTEM
    }
}

// 应用语言：中文/英文/跟随系统
enum class AppLanguage(val languageTag: String?) {
    SYSTEM(null),
    CHINESE("zh"),
    ENGLISH("en");

    companion object {
        fun fromTag(tag: String?): AppLanguage =
            entries.find { it.languageTag == tag } ?: SYSTEM
    }
}

// 设置状态数据类
data class SettingsState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
)
