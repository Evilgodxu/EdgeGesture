package com.edgegesture.evilgodxu

import com.edgegesture.evilgodxu.data.repository.FakeSettingsRepository
import com.edgegesture.evilgodxu.data.settings.AppLanguage
import com.edgegesture.evilgodxu.data.settings.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

// 应用级状态持有者：写入经仓库回流更新对外状态
@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `初始状态携带应用版本`() = runTest(dispatcher) {
        val viewModel = MainViewModel(FakeSettingsRepository(), AppUiState(version = "5.3.0"))

        assertEquals("5.3.0", viewModel.uiState.value.version)
        assertEquals(ThemeMode.SYSTEM, viewModel.uiState.value.themeMode)
    }

    @Test
    fun `写入主题经仓库回流更新状态`() = runTest(dispatcher) {
        val repository = FakeSettingsRepository()
        val viewModel = MainViewModel(repository, AppUiState(version = "5.3.0"))
        advanceUntilIdle()

        viewModel.setThemeMode(ThemeMode.DARK)
        advanceUntilIdle()

        assertEquals(listOf(ThemeMode.DARK), repository.savedThemeModes)
        assertEquals(ThemeMode.DARK, viewModel.uiState.value.themeMode)
    }

    @Test
    fun `写入语言经仓库回流更新状态`() = runTest(dispatcher) {
        val repository = FakeSettingsRepository()
        val viewModel = MainViewModel(repository, AppUiState(version = "5.3.0"))
        advanceUntilIdle()

        viewModel.setLanguage(AppLanguage.ENGLISH)
        advanceUntilIdle()

        assertEquals(listOf(AppLanguage.ENGLISH), repository.savedLanguages)
        assertEquals(AppLanguage.ENGLISH, viewModel.uiState.value.language)
    }
}
