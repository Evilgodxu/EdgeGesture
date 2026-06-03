package com.byss.jh.data.app

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.Intent
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// 应用列表缓存 DataStore
val Context.appCacheDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_cache")

object AppCacheKeys {
    val CACHED_APPS_JSON = stringPreferencesKey("cached_apps_json")
    val LAST_CACHE_TIME = longPreferencesKey("last_cache_time")
    val CACHE_VERSION = longPreferencesKey("cache_version")
}

// 缓存版本，用于强制刷新
private const val CURRENT_CACHE_VERSION = 1L

// 缓存有效期：7天
private const val CACHE_VALIDITY_MS = 7 * 24 * 60 * 60 * 1000L

// 应用缓存管理器，提供持久化缓存和快速读取
class AppCacheManager(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    // 获取缓存的应用列表流
    fun getCachedAppsFlow(): Flow<List<AppInfo>> {
        return context.appCacheDataStore.data.map { prefs ->
            val cachedJson = prefs[AppCacheKeys.CACHED_APPS_JSON] ?: "[]"
            try {
                val cachedList = json.decodeFromString<List<SerializableAppInfo>>(cachedJson)
                cachedList.map { it.toAppInfo() }
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    // 检查缓存是否有效
    suspend fun isCacheValid(): Boolean {
        val prefs = context.appCacheDataStore.data.first()
        val lastCacheTime = prefs[AppCacheKeys.LAST_CACHE_TIME] ?: 0L
        val cacheVersion = prefs[AppCacheKeys.CACHE_VERSION] ?: 0L
        val currentTime = System.currentTimeMillis()

        return cacheVersion == CURRENT_CACHE_VERSION &&
               lastCacheTime > 0 &&
               (currentTime - lastCacheTime) < CACHE_VALIDITY_MS
    }

    // 保存应用列表到缓存
    suspend fun saveAppsToCache(apps: List<AppInfo>) = withContext(Dispatchers.IO) {
        val serializableList = apps.map { it.toSerializable() }
        val jsonString = json.encodeToString(serializableList)

        context.appCacheDataStore.edit { prefs ->
            prefs[AppCacheKeys.CACHED_APPS_JSON] = jsonString
            prefs[AppCacheKeys.LAST_CACHE_TIME] = System.currentTimeMillis()
            prefs[AppCacheKeys.CACHE_VERSION] = CURRENT_CACHE_VERSION
        }
    }

    // 清空缓存
    suspend fun clearCache() {
        context.appCacheDataStore.edit { prefs ->
            prefs.remove(AppCacheKeys.CACHED_APPS_JSON)
            prefs.remove(AppCacheKeys.LAST_CACHE_TIME)
        }
    }

    // 快速扫描应用列表，queryIntentActivities 比 getInstalledApplications 更快
    suspend fun quickScanApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = pm.queryIntentActivities(launcherIntent, 0)

        resolveInfos.map { resolveInfo ->
            val activityInfo = resolveInfo.activityInfo
            val appInfo = activityInfo.applicationInfo
            val isSystemApp = appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0 ||
                    appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0

            AppInfo(
                packageName = activityInfo.packageName,
                appName = resolveInfo.loadLabel(pm).toString(),
                isSystemApp = isSystemApp
            )
        }
            .distinctBy { it.packageName }
            .sortedWith(compareBy({ !it.isSystemApp }, { it.appName }))
    }
}

// 可序列化的应用信息，用于 DataStore 存储
@Serializable
private data class SerializableAppInfo(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean
)

private fun AppInfo.toSerializable(): SerializableAppInfo {
    return SerializableAppInfo(
        packageName = packageName,
        appName = appName,
        isSystemApp = isSystemApp
    )
}

private fun SerializableAppInfo.toAppInfo(): AppInfo {
    return AppInfo(
        packageName = packageName,
        appName = appName,
        isSystemApp = isSystemApp
    )
}
