package com.edgegesture.evilgodxu.data.repository

import com.edgegesture.evilgodxu.data.settings.AppLanguage
import com.edgegesture.evilgodxu.data.settings.SettingsState
import com.edgegesture.evilgodxu.data.settings.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

// 设置仓库的内存实现：替代 mock 框架，充当状态唯一事实源
class FakeSettingsRepository : SettingsRepository {

    private val settingsState = MutableStateFlow(SettingsState())
    private val languageState = MutableStateFlow(AppLanguage.SYSTEM)

    val savedThemeModes = mutableListOf<ThemeMode>()
    val savedLanguages = mutableListOf<AppLanguage>()

    override val settings: Flow<SettingsState> = settingsState

    override val appLanguage: Flow<AppLanguage> = languageState

    override suspend fun saveThemeMode(mode: ThemeMode) {
        savedThemeModes += mode
        settingsState.value = SettingsState(themeMode = mode)
    }

    override suspend fun setAppLanguage(language: AppLanguage) {
        savedLanguages += language
        languageState.value = language
    }
}
