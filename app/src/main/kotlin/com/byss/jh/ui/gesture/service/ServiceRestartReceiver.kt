package com.byss.jh.ui.gesture.service

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
        try {
            EdgeGestureAccessibilityService.startService(context)
        } catch (_: Exception) {
        }
    }

    companion object {
        private const val TAG = "ServiceRestartReceiver"
    }
}
