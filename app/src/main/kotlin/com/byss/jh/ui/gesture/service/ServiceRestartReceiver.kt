package com.byss.jh.ui.gesture.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.byss.jh.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ServiceRestartReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Logger.i(context, TAG, "收到广播: $action")

        when (action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                handleBootCompleted(context)
            }
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                handlePackageReplaced(context)
            }
        }
    }

    /**
     * 设备启动完成后延迟5秒启动服务
     * 延迟确保系统完成初始化，避免启动失败
     */
    private fun handleBootCompleted(context: Context) {
        Logger.i(context, TAG, "设备启动完成，启动服务")
        CoroutineScope(Dispatchers.IO).launch {
            delay(5000)
            startGestureService(context)
        }
    }

    /**
     * 应用更新完成后立即重启服务
     */
    private fun handlePackageReplaced(context: Context) {
        Logger.i(context, TAG, "应用更新完成，重启服务")
        CoroutineScope(Dispatchers.IO).launch {
            startGestureService(context)
        }
    }

    private fun startGestureService(context: Context) {
        try {
            EdgeGestureAccessibilityService.startService(context)
            Logger.i(context, TAG, "EdgeGestureAccessibilityService 已启动")
        } catch (e: Exception) {
            Logger.e(context, TAG, "启动 EdgeGestureAccessibilityService 失败: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "ServiceRestartReceiver"
    }
}
