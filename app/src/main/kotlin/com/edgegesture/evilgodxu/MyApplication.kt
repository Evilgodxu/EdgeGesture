package com.edgegesture.evilgodxu

import android.app.Application
import com.edgegesture.evilgodxu.data.app.AppRepository
import com.edgegesture.evilgodxu.data.gesture.GestureStatsManager
import com.edgegesture.evilgodxu.log.CrashLogManager
import com.edgegesture.evilgodxu.update.UpdateViewModel
import com.edgegesture.evilgodxu.utils.localization.LocalizationManager

// 应用入口类，初始化崩溃日志、全局依赖单例与后台任务
class MyApplication : Application() {

    // 语言管理器单例，驱动 Compose 层语言热切换
    val localizationManager: LocalizationManager by lazy { LocalizationManager(this) }

    // 更新检查以单例共享，主界面自动检查与设置页手动检查读写同一状态
    val updateViewModel: UpdateViewModel by lazy { UpdateViewModel(this, localizationManager) }

    override fun onCreate() {
        super.onCreate()

        // 最先初始化崩溃日志系统，捕获启动阶段及后续所有未捕获异常
        CrashLogManager.init(this)

        // 应用列表在需要时通过 EdgeGestureAccessibilityService 中的 initializeWithScan() 触发扫描
        val repository = AppRepository.getInstance(this)

        // 尽早注册应用变更监听，确保安装/卸载事件被捕获
        // 广播接收器内部会处理权限检查，无权限时只会延迟刷新
        repository.registerAppChangeReceiver()

        // 初始化手势统计数据管理器
        GestureStatsManager.init(this)
    }
}