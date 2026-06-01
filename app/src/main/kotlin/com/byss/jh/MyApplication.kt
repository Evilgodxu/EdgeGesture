package com.byss.jh

import android.app.Application
import com.byss.jh.data.app.AppRepository
import com.byss.jh.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

// 应用入口类，初始化 Koin 依赖注入框架和应用缓存
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@MyApplication)
            modules(appModule)
        }

        // 延迟初始化：仅从缓存加载应用列表，不触发扫描
        // 实际扫描在用户同意隐私政策后通过 initializeWithScan() 触发
        AppRepository.getInstance(this).initializeFromCache()
    }
}
