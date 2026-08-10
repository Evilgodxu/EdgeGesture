package com.edgegesture.evilgodxu.di

import com.edgegesture.evilgodxu.data.app.AppRepository
import com.edgegesture.evilgodxu.screens.backtap.BackTapViewModel
import com.edgegesture.evilgodxu.screens.expandpanel.ExpandPanelViewModel
import com.edgegesture.evilgodxu.screens.gesture.EdgeGestureConfigViewModel
import com.edgegesture.evilgodxu.screens.gesture.GestureSettingsViewModel
import com.edgegesture.evilgodxu.screens.launchblock.LaunchBlockViewModel
import com.edgegesture.evilgodxu.screens.settings.DataConfigViewModel
import com.edgegesture.evilgodxu.screens.settings.SettingsViewModel
import com.edgegesture.evilgodxu.update.UpdateViewModel
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
    viewModel { UpdateViewModel(androidApplication()) }
    viewModel { EdgeGestureConfigViewModel(androidApplication()) }
    viewModel { BackTapViewModel(androidApplication()) }
    viewModel { LaunchBlockViewModel(androidApplication()) }
    viewModel { ExpandPanelViewModel(androidApplication()) }
    viewModel { DataConfigViewModel(androidApplication()) }
}
