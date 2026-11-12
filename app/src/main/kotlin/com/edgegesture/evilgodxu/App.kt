package com.edgegesture.evilgodxu

import android.app.Application
import com.edgegesture.evilgodxu.data.app.AppRepository
import com.edgegesture.evilgodxu.data.gesture.GestureStatsManager
import com.edgegesture.evilgodxu.data.shizuku.ShizukuManager
import com.edgegesture.evilgodxu.log.CrashLogManager

class App : Application() {

    // 手动 DI 容器：Application 级单例，宿主 Activity 启动时取用
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()

        // 最先初始化崩溃日志系统，捕获启动阶段及后续所有未捕获异常
        CrashLogManager.init(this)

        container = AppContainer(this)

        // Shizuku 属应用级能力：提前注册 binder 与授权结果监听，页面只读状态
        ShizukuManager.init(this)

        // 应用列表在需要时通过 EdgeGestureAccessibilityService 中的 initializeWithScan() 触发扫描
        val repository = AppRepository.getInstance(this)

        // 尽早注册应用变更监听，确保安装/卸载事件被捕获
        // 广播接收器内部会处理权限检查，无权限时只会延迟刷新
        repository.registerAppChangeReceiver()

        // 初始化手势统计数据管理器
        GestureStatsManager.init(this)
    }
}
