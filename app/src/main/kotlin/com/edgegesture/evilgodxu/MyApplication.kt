package com.edgegesture.evilgodxu

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.edgegesture.evilgodxu.data.app.AppRepository
import com.edgegesture.evilgodxu.data.gesture.GestureStatsManager
import com.edgegesture.evilgodxu.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import java.util.concurrent.TimeUnit

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

        // 初始化手势统计数据管理器
        GestureStatsManager.init(this)

        // 创建更新通知渠道
        createUpdateNotificationChannel()

        // 调度周期性更新检查（最小间隔 15 分钟，内部有 1 小时冷却）
        scheduleUpdateCheck()
    }

    private fun createUpdateNotificationChannel() {
        val channel = NotificationChannel(
            UpdateCheckWorker.CHANNEL_ID,
            getString(R.string.update_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = getString(R.string.update_channel_desc)
        }
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    private fun scheduleUpdateCheck() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<UpdateCheckWorker>(
            15, TimeUnit.MINUTES  // WorkManager 最小周期为 15 分钟
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            UpdateCheckWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
