package com.edgegesture.evilgodxu.screens.gesture.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class RemindAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val minutes = intent.getIntExtra(RemindAlarmService.EXTRA_MINUTES, 1)
        // 启动前台服务播放铃声并显示可关闭的通知
        val serviceIntent = Intent(context, RemindAlarmService::class.java).apply {
            putExtra(RemindAlarmService.EXTRA_MINUTES, minutes)
        }
        context.startForegroundService(serviceIntent)
    }

    companion object {
        const val EXTRA_MINUTES = RemindAlarmService.EXTRA_MINUTES
        const val ACTION_REMIND = "com.edgegesture.action.REMIND"
    }
}
