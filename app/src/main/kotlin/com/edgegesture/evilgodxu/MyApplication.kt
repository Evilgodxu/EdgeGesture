package com.edgegesture.evilgodxu

import android.app.Application
import com.edgegesture.evilgodxu.data.app.AppRepository
import com.edgegesture.evilgodxu.di.appModule
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
        // 实际扫描在获取 QUERY_ALL_PACKAGES 权限后通过 initializeWithScan() 触发
        val repository = AppRepository.getInstance(this)
        repository.initializeFromCache()

        // 尽早注册应用变更监听，确保安装/卸载事件被捕获
        // 广播接收器内部会处理权限检查，无权限时只会延迟刷新
        repository.registerAppChangeReceiver()
    }
}
