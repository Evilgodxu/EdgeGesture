package com.edgegesture.evilgodxu.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.edgegesture.evilgodxu.data.settings.AppLanguage
import com.edgegesture.evilgodxu.data.settings.SettingsKeys
import com.edgegesture.evilgodxu.data.settings.SettingsState
import com.edgegesture.evilgodxu.data.settings.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// 设置仓库契约：数据层入口，由 AppContainer 注入实现，测试注入内存 fake
interface SettingsRepository {

    // 设置状态流：主题模式驱动全局配色
    val settings: Flow<SettingsState>

    // 应用语言流：统一读 DataStore
    val appLanguage: Flow<AppLanguage>

    suspend fun saveThemeMode(mode: ThemeMode)

    suspend fun setAppLanguage(language: AppLanguage)
}

// DataStore 实现：以 DataStore 为单一事实源，数据源经构造注入便于单测
class DataStoreSettingsRepository(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override val settings: Flow<SettingsState> = dataStore.data.map { preferences ->
        SettingsState(
            themeMode = ThemeMode.fromValue(preferences[SettingsKeys.THEME_MODE] ?: ThemeMode.SYSTEM.value),
        )
    }

    override val appLanguage: Flow<AppLanguage> = dataStore.data.map { preferences ->
        AppLanguage.fromTag(preferences[SettingsKeys.LANGUAGE])
    }

    override suspend fun saveThemeMode(mode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[SettingsKeys.THEME_MODE] = mode.value
        }
    }

    override suspend fun setAppLanguage(language: AppLanguage) {
        dataStore.edit { preferences ->
            preferences[SettingsKeys.LANGUAGE] = language.languageTag.orEmpty()
        }
    }
}
