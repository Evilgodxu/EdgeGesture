package com.byss.jh.di

import com.byss.jh.feature.gesture.GestureSettingsViewModel
import com.byss.jh.feature.privacy.PrivacyViewModel
import com.byss.jh.feature.settings.SettingsViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

// Koin 依赖注入模块，用于管理应用级别的依赖
val appModule = module {
    viewModel { SettingsViewModel(androidApplication()) }
    viewModel { GestureSettingsViewModel(androidApplication()) }
    viewModel { PrivacyViewModel(androidApplication()) }
}
