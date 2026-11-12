package com.edgegesture.evilgodxu.data.repository

import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first

// 更新检查记录仓库契约：最近一次自动检查日期与用户忽略的版本
interface UpdateCheckRepository {

    suspend fun lastCheckDay(): String?

    suspend fun setLastCheckDay(day: String)

    suspend fun ignoredVersion(): String?

    suspend fun setIgnoredVersion(version: String)
}

// DataStore 实现：与设置共用同一份 DataStore；
// 旧版本把这两项存在 SharedPreferences，首次访问时一次性搬过来，避免升级后重复提示已忽略的版本
class DataStoreUpdateCheckRepository(
    private val dataStore: DataStore<Preferences>,
    private val legacyPreferences: SharedPreferences? = null,
) : UpdateCheckRepository {

    override suspend fun lastCheckDay(): String? = read()[LAST_CHECK_DAY]

    override suspend fun setLastCheckDay(day: String) {
        dataStore.edit { preferences ->
            preferences[LAST_CHECK_DAY] = day
        }
    }

    override suspend fun ignoredVersion(): String? = read()[IGNORED_VERSION]

    override suspend fun setIgnoredVersion(version: String) {
        dataStore.edit { preferences ->
            preferences[IGNORED_VERSION] = version
        }
    }

    private suspend fun read(): Preferences {
        migrateLegacyPreferencesOnce()
        return dataStore.data.first()
    }

    private suspend fun migrateLegacyPreferencesOnce() {
        val legacy = legacyPreferences ?: return
        dataStore.edit { preferences ->
            if (preferences[LEGACY_MIGRATED] == true) return@edit
            legacy.getString(LEGACY_LAST_CHECK_DAY, null)?.let { preferences[LAST_CHECK_DAY] = it }
            legacy.getString(LEGACY_IGNORED_VERSION, null)?.let { preferences[IGNORED_VERSION] = it }
            preferences[LEGACY_MIGRATED] = true
        }
    }

    private companion object {
        val LAST_CHECK_DAY = stringPreferencesKey("last_check_day")
        val IGNORED_VERSION = stringPreferencesKey("ignored_version")
        val LEGACY_MIGRATED = booleanPreferencesKey("update_legacy_migrated")

        // 旧 SharedPreferences（update_prefs）中的键名
        const val LEGACY_LAST_CHECK_DAY = "last_check_day"
        const val LEGACY_IGNORED_VERSION = "ignored_version"
    }
}
