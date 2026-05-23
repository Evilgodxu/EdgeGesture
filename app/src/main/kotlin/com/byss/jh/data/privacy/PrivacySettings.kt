package com.byss.jh.data.privacy

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

val Context.privacyDataStore: DataStore<Preferences> by preferencesDataStore(name = "privacy_settings")

object PrivacySettingsKeys {
    val PRIVACY_AGREED = booleanPreferencesKey("privacy_agreed")
}

/**
 * 检查用户是否已同意隐私政策
 */
fun Context.isPrivacyAgreed(): Flow<Boolean> {
    return privacyDataStore.data.map { preferences ->
        preferences[PrivacySettingsKeys.PRIVACY_AGREED] ?: false
    }
}

/**
 * 保存隐私政策同意状态
 */
suspend fun Context.savePrivacyAgreed(agreed: Boolean) {
    withContext(Dispatchers.IO) {
        privacyDataStore.edit { preferences ->
            preferences[PrivacySettingsKeys.PRIVACY_AGREED] = agreed
        }
    }
}
