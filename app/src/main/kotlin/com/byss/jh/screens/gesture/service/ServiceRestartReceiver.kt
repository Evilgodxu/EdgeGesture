package com.byss.jh.screens.gesture.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ServiceRestartReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return

        when (action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                handleBootCompleted(context)
            }
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                handlePackageReplaced(context)
            }
        }
    }

    // 设备启动完成后延迟5秒启动服务，延迟确保系统完成初始化，避免启动失败
    private fun handleBootCompleted(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            delay(5000)
            startGestureService(context)
        }
    }

    // 应用更新完成后立即重启服务
    private fun handlePackageReplaced(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            startGestureService(context)
        }
    }

    private fun startGestureService(context: Context) {
        // 设备启动或应用更新后，尝试恢复手势视图
        // 注意：AccessibilityService 只能由用户手动开启，无法自动启动
        // 如果服务已在运行，则恢复手势触发区域
        try {
            if (EdgeGestureAccessibilityService.isAvailable()) {
                EdgeGestureAccessibilityService.startGesture(context)
            }
        } catch (_: Exception) {
        }
    }

    companion object {
        private const val TAG = "ServiceRestartReceiver"
    }
}
