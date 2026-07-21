package com.edgegesture.evilgodxu.screens.gesture.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.IBinder
import android.os.VibrationEffect
import android.os.VibratorManager
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.edgegesture.evilgodxu.R

class RemindAlarmService : Service() {

    private var ringtone: Ringtone? = null
    private var hasStopped = false
    private var alarmOverlayView: View? = null

    // 监听电源键熄灭屏幕
    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_OFF) {
                stopAlarm()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_ALARM -> stopAlarm()
            else -> {
                val minutes = intent?.getIntExtra(EXTRA_MINUTES, 1) ?: 1
                startAlarm(minutes)
            }
        }
        return START_NOT_STICKY
    }

    private fun startAlarm(minutes: Int) {
        if (hasStopped) return

        // 1. 前台通知（低调，仅保持服务存活 + 状态栏可见）
        val channelId = "gesture_remind_ring"
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(NotificationChannel(channelId, getString(R.string.gesture_remind_channel_name), NotificationManager.IMPORTANCE_LOW))

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.gesture_remind_notify_title))
            .setContentText(getString(R.string.gesture_remind_notify_body, minutes))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setSilent(true)
            .build()

        startForeground(NOTIFY_ID_BASE + minutes, notification)

        // 2. 注册熄屏监听（电源键关闭）
        registerReceiver(screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF), RECEIVER_NOT_EXPORTED)

        // 3. 弹出系统级悬浮窗（类似闹钟）
        showAlarmOverlay(minutes)

        // 3. 播放系统闹钟铃声（循环）
        playAlarmSound()

        // 4. 震动
        vibrate()

        // 5. 自动超时停止（5 分钟后）
        HandlerExecutor.handler.postDelayed({ stopAlarm() }, 5 * 60 * 1000L)
    }

    // ============================================================
    // 悬浮窗弹窗
    // ============================================================

    private fun showAlarmOverlay(minutes: Int) {
        try {
            val wm = getSystemService(WINDOW_SERVICE) as WindowManager
            val density = resources.displayMetrics.density
            val dp = { n: Int -> (n * density).toInt() }

            // 检测当前主题
            val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                    Configuration.UI_MODE_NIGHT_YES
            val bgCard = if (isDark) 0xFF161B22.toInt() else 0xFFFFFFFF.toInt()
            val textTitle = if (isDark) 0xFFF0F6FC.toInt() else 0xFF1F2328.toInt()
            val textBody = if (isDark) 0xFF8B949E.toInt() else 0xFF656D76.toInt()

            // 窗口参数：全屏、覆盖锁屏、点亮屏幕（不加 KEEP_SCREEN_ON 让电源键可关屏）
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            )

            // 根布局：半透明遮罩，点击任意位置关闭
            val maskColor = if (isDark) Color.argb(200, 0, 0, 0) else Color.argb(180, 0, 0, 0)
            val root = LinearLayout(this).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                gravity = Gravity.CENTER
                setBackgroundColor(maskColor)
                setOnClickListener { stopAlarm() }
            }

            // 卡片
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                val p = dp(24)
                setPadding(p, dp(32), p, dp(24))
                val cardWidth = (density * 300).toInt()
                layoutParams = LinearLayout.LayoutParams(cardWidth, ViewGroup.LayoutParams.WRAP_CONTENT)
                elevation = dp(8).toFloat()
                background = GradientDrawable().apply {
                    setColor(bgCard)
                    cornerRadius = dp(16).toFloat()
                }
            }

            // 标题
            card.addView(TextView(this).apply {
                text = getString(R.string.gesture_remind_notify_title)
                textSize = 22f
                setTextColor(textTitle)
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
            })

            // 消息
            card.addView(TextView(this).apply {
                text = getString(R.string.gesture_remind_notify_body, minutes)
                textSize = 16f
                setTextColor(textBody)
                gravity = Gravity.CENTER
                val p = dp(20)
                setPadding(0, p, 0, 0)
            })

            // 关闭后自动停止
            card.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) {}
                override fun onViewDetachedFromWindow(v: View) { stopAlarm() }
            })

            root.addView(card)
            wm.addView(root, params)
            alarmOverlayView = root
        } catch (_: SecurityException) {
            // 没有悬浮窗权限，仅保留通知和铃声
        } catch (_: Exception) {}
    }

    // ============================================================
    // 铃声 & 震动
    // ============================================================

    private fun playAlarmSound() {
        try {
            val alarmUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ringtone = RingtoneManager.getRingtone(this, alarmUri)
            ringtone?.apply {
                audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                isLooping = true
                play()
            }
        } catch (_: Exception) {}
    }

    private fun vibrate() {
        try {
            val vm = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator.vibrate(
                VibrationEffect.createWaveform(longArrayOf(0, 1000, 500), intArrayOf(1, 0), 2)
            )
        } catch (_: Exception) {}
    }

    // ============================================================
    // 停止
    // ============================================================

    private fun stopAlarm() {
        if (hasStopped) return
        hasStopped = true
        ringtone?.stop()
        ringtone = null

        // 移除熄屏监听
        try { unregisterReceiver(screenOffReceiver) } catch (_: Exception) {}

        // 移除悬浮窗
        alarmOverlayView?.let { view ->
            try { (getSystemService(WINDOW_SERVICE) as WindowManager).removeView(view) } catch (_: Exception) {}
            alarmOverlayView = null
        }

        HandlerExecutor.handler.removeCallbacksAndMessages(null)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopAlarm()
        try { unregisterReceiver(screenOffReceiver) } catch (_: Exception) {}
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val EXTRA_MINUTES = "extra_minutes"
        const val ACTION_STOP_ALARM = "com.edgegesture.action.STOP_REMIND_ALARM"
        const val NOTIFY_ID_BASE = 3000
    }
}

internal object HandlerExecutor {
    val handler = android.os.Handler(android.os.Looper.getMainLooper())
}
