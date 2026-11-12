package com.edgegesture.evilgodxu

import android.content.Context
import android.content.pm.PackageManager
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.edgegesture.evilgodxu.data.repository.DataStoreSettingsRepository
import com.edgegesture.evilgodxu.data.repository.DataStoreUpdateCheckRepository
import com.edgegesture.evilgodxu.data.repository.SettingsRepository
import com.edgegesture.evilgodxu.data.repository.UpdateCheckRepository
import com.edgegesture.evilgodxu.utils.localization.LocalizationManager

// 手动 DI 容器：Application 启动时构造并持有全部依赖
// 数据源经构造注入仓库，便于单元测试替换实现
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val settingsRepository: SettingsRepository by lazy {
        DataStoreSettingsRepository(settingsDataStore)
    }

    val updateCheckRepository: UpdateCheckRepository by lazy {
        DataStoreUpdateCheckRepository(settingsDataStore, legacyUpdatePreferences)
    }

    val localizationManager: LocalizationManager by lazy {
        LocalizationManager(appContext)
    }

    // 应用版本号：冷启动读取一次，供应用级状态与更新检查共用
    val appVersion: String = readAppVersion()

    // 设置与更新检查记录共用一份 DataStore，写入天然串行
    private val settingsDataStore: DataStore<Preferences> by lazy {
        PreferenceDataStoreFactory.create {
            appContext.preferencesDataStoreFile("settings")
        }
    }

    // 旧版本更新记录存放位置，仅用于一次性迁移
    private val legacyUpdatePreferences by lazy {
        appContext.getSharedPreferences(LEGACY_UPDATE_PREFERENCES, Context.MODE_PRIVATE)
    }

    private fun readAppVersion(): String =
        appContext.packageManager
            .getPackageInfo(appContext.packageName, PackageManager.PackageInfoFlags.of(0L))
            .versionName.orEmpty()

    private companion object {
        const val LEGACY_UPDATE_PREFERENCES = "update_prefs"
    }
}
