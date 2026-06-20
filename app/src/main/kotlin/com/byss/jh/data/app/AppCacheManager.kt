package com.byss.jh.data.app

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
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
import java.io.File

// 应用列表缓存 DataStore
val Context.appCacheDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_cache")

object AppCacheKeys {
    val CACHED_APPS_JSON = stringPreferencesKey("cached_apps_json")
    val LAST_CACHE_TIME = longPreferencesKey("last_cache_time")
    val CACHE_VERSION = longPreferencesKey("cache_version")
}

// 缓存版本，用于强制刷新
private const val CURRENT_CACHE_VERSION = 2L

// 缓存有效期：7天
private const val CACHE_VALIDITY_MS = 7 * 24 * 60 * 60 * 1000L

// 图标缓存尺寸
private const val ICON_CACHE_SIZE = 96

// 应用缓存管理器，提供持久化缓存和快速读取
class AppCacheManager(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val iconCacheDir: File
        get() = File(context.cacheDir, "app-icons").apply { mkdirs() }

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

    // 删除指定应用的图标缓存文件
    fun deleteIconCache(packageName: String) {
        File(iconCacheDir, "$packageName.png").delete()
    }

    // 快速扫描应用列表，queryIntentActivities 比 getInstalledApplications 更快
    // 扫描同时缓存应用图标到本地，减少 UI 层重复查询 PackageManager
    suspend fun quickScanApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(launcherIntent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(launcherIntent, 0)
        }

        resolveInfos.map { resolveInfo ->
            val activityInfo = resolveInfo.activityInfo
            val appInfo = activityInfo.applicationInfo
            val packageName = activityInfo.packageName
            val isSystemApp = appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0 ||
                    appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0

            AppInfo(
                packageName = packageName,
                appName = resolveInfo.loadLabel(pm).toString(),
                isSystemApp = isSystemApp,
                iconPath = cacheAppIcon(appInfo),
                versionName = versionName(pm, packageName),
                sourcePath = appInfo.sourceDir.orEmpty()
            )
        }
            .distinctBy { it.packageName }
            .sortedWith(compareBy({ !it.isSystemApp }, { it.appName }))
    }

    // 缓存应用图标到本地文件，返回缓存路径
    private fun cacheAppIcon(appInfo: ApplicationInfo): String {
        val target = File(iconCacheDir, "${appInfo.packageName}.png")
        if (!target.isFile) {
            runCatching {
                val icon = appInfo.loadIcon(context.packageManager)
                icon.toBitmap(ICON_CACHE_SIZE).saveAsPng(target)
            }
        }
        return if (target.isFile) target.absolutePath else ""
    }

    // 获取应用版本名称
    private fun versionName(pm: PackageManager, packageName: String): String {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0L)).versionName
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(packageName, 0).versionName
            }
        }.getOrNull().orEmpty()
    }

    private fun Drawable.toBitmap(size: Int): Bitmap {
        if (this is BitmapDrawable && bitmap != null) {
            return Bitmap.createScaledBitmap(bitmap, size, size, true)
        }
        return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).also { bitmap ->
            val canvas = Canvas(bitmap)
            setBounds(0, 0, canvas.width, canvas.height)
            draw(canvas)
        }
    }

    private fun Bitmap.saveAsPng(file: File) {
        file.parentFile?.mkdirs()
        file.outputStream().use { output ->
            compress(Bitmap.CompressFormat.PNG, 100, output)
        }
    }
}

// 可序列化的应用信息，用于 DataStore 存储
@Serializable
private data class SerializableAppInfo(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean,
    val iconPath: String = "",
    val versionName: String = "",
    val sourcePath: String = ""
)

private fun AppInfo.toSerializable(): SerializableAppInfo {
    return SerializableAppInfo(
        packageName = packageName,
        appName = appName,
        isSystemApp = isSystemApp,
        iconPath = iconPath,
        versionName = versionName,
        sourcePath = sourcePath
    )
}

private fun SerializableAppInfo.toAppInfo(): AppInfo {
    return AppInfo(
        packageName = packageName,
        appName = appName,
        isSystemApp = isSystemApp,
        iconPath = iconPath,
        versionName = versionName,
        sourcePath = sourcePath
    )
}
