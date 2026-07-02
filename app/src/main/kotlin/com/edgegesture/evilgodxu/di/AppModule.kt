package com.edgegesture.evilgodxu.di

import com.edgegesture.evilgodxu.data.app.AppRepository
import com.edgegesture.evilgodxu.screens.gesture.GestureSettingsViewModel
import com.edgegesture.evilgodxu.screens.settings.SettingsViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

// Koin 依赖注入模块，用于管理应用级别的依赖
val appModule = module {
    // 单例模式提供 AppRepository
    single { AppRepository.getInstance(androidContext()) }

    viewModel { SettingsViewModel(androidApplication()) }
    viewModel { GestureSettingsViewModel(androidApplication()) }
}
