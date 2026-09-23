package com.edgegesture.evilgodxu.di

import com.edgegesture.evilgodxu.MyApplication
import com.edgegesture.evilgodxu.data.app.AppRepository
import com.edgegesture.evilgodxu.screens.backtap.BackTapViewModel
import com.edgegesture.evilgodxu.screens.expandpanel.ExpandPanelViewModel
import com.edgegesture.evilgodxu.screens.gesture.EdgeGestureConfigViewModel
import com.edgegesture.evilgodxu.screens.gesture.GestureSettingsViewModel
import com.edgegesture.evilgodxu.screens.launchblock.LaunchBlockViewModel
import com.edgegesture.evilgodxu.screens.settings.DataConfigViewModel
import com.edgegesture.evilgodxu.screens.settings.SettingsViewModel
import com.edgegesture.evilgodxu.utils.localization.LocalizationManager
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

// Koin 依赖注入模块，用于管理应用级别的依赖
val appModule = module {
    // 单例模式提供 AppRepository
    single { AppRepository.getInstance(androidContext()) }

    // 语言管理器复用 Application 持有的同一实例，供 Compose 层语言热切换
    single<LocalizationManager> { (androidApplication() as MyApplication).localizationManager }

    viewModel { SettingsViewModel(androidApplication(), get()) }
    viewModel { GestureSettingsViewModel(androidApplication()) }
    viewModel { EdgeGestureConfigViewModel(androidApplication()) }
    viewModel { BackTapViewModel(androidApplication()) }
    viewModel { LaunchBlockViewModel(androidApplication()) }
    viewModel { ExpandPanelViewModel(androidApplication()) }
    viewModel { DataConfigViewModel(androidApplication()) }
}
