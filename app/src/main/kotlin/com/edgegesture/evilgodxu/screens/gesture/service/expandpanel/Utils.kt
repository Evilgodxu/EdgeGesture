package com.edgegesture.evilgodxu.screens.gesture.service.expandpanel

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Rect
import android.media.AudioManager
import android.provider.Settings
import android.view.KeyEvent
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt
import org.lsposed.hiddenapibypass.HiddenApiBypass

// Android 系统亮度范围
private const val BRIGHTNESS_MIN = 0
private const val BRIGHTNESS_MAX = 255

// HLG (Hybrid Log Gamma) 曲线参数 - 与 Android 9.0+ AOSP 一致
// 来源: frameworks/base/packages/SettingsLib/src/com/android/settingslib/display/BrightnessUtils.java
private const val GAMMA_SPACE_MIN = 0
private const val GAMMA_SPACE_MAX = 1023
private const val R = 0.5f
private const val A = 0.17883277f
private const val B_HLG = 0.28466892f  // 重命名避免与亮度参数 B 冲突
private const val C = 0.55991073f

// 发送媒体按键事件
fun sendMediaKeyEvent(context: Context, keyCode: Int) {
    try {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
    } catch (_: Exception) {}
}

// 获取当前亮度百分比 (0.0 - 1.0)，使用 AOSP 相同的 HLG 伽马曲线转换
fun getCurrentBrightnessPercent(context: Context): Float {
    return try {
        val brightness = Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        convertLinearToGamma(brightness, BRIGHTNESS_MIN, BRIGHTNESS_MAX)
    } catch (_: Exception) {
        0.5f
    }
}

// 设置屏幕亮度，percent 为 0.0 - 1.0 的百分比值
fun setBrightness(context: Context, percent: Float) {
    try {
        val brightness = convertGammaToLinear(
            (percent * GAMMA_SPACE_MAX).roundToInt(),
            BRIGHTNESS_MIN,
            BRIGHTNESS_MAX
        )
        Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, brightness)
    } catch (_: Exception) {}
}

// 将系统线性亮度值转换为伽马空间滑块值 (0.0-1.0)
private fun convertLinearToGamma(value: Int, min: Int, max: Int): Float {
    // HLG normalizes to the range [0, 12] rather than [0, 1]
    val normalizedVal = norm(min, max, value) * 12f
    val ret = if (normalizedVal <= 1f) {
        sqrt(normalizedVal) * R
    } else {
        A * ln(normalizedVal - B_HLG) + C
    }
    return lerp(GAMMA_SPACE_MIN, GAMMA_SPACE_MAX, ret).coerceIn(0f, GAMMA_SPACE_MAX.toFloat()) / GAMMA_SPACE_MAX
}

// 将伽马空间滑块值转换为系统线性亮度值
private fun convertGammaToLinear(valInGamma: Int, min: Int, max: Int): Int {
    val normalizedVal = norm(GAMMA_SPACE_MIN, GAMMA_SPACE_MAX, valInGamma)
    val ret = if (normalizedVal <= R) {
        sq(normalizedVal / R)
    } else {
        exp((normalizedVal - C) / A) + B_HLG
    }
    // HLG is normalized to the range [0, 12], re-normalize to [0, 1]
    return lerp(min, max, ret / 12f).roundToInt().coerceIn(min, max)
}

// AOSP MathUtils 工具函数
private fun norm(start: Int, stop: Int, value: Int): Float {
    return (value - start).toFloat() / (stop - start)
}

private fun lerp(start: Int, stop: Int, amount: Float): Float {
    return start + (stop - start) * amount
}

private fun sq(value: Float): Float {
    return value * value
}

// 窗口模式常量：自由窗口（小窗）
private const val WINDOWING_MODE_FREEFORM = 5

private val hiddenApiInitialized = AtomicBoolean(false)

private fun ensureHiddenApiExempt() {
    if (hiddenApiInitialized.compareAndSet(false, true)) {
        try {
            HiddenApiBypass.addHiddenApiExemptions("Landroid/app/ActivityOptions;")
        } catch (_: Throwable) {
            // 部分系统上该调用可能受限，忽略失败
        }
    }
}

// 计算小窗启动的初始 bounds。
// 经验：首次实现仅设置 windowing mode 时，横屏应用由系统默认处理完全正常；
// 竖屏应用会出现灰色遮罩和异常高度，因此只在竖屏时主动传一个合理的初始大小。
private fun computeFreeformBounds(context: Context, launchIntent: Intent): Rect? {
    val metrics = context.resources.displayMetrics
    val displayWidth = metrics.widthPixels
    val displayHeight = metrics.heightPixels
    val isDeviceLandscape = displayWidth > displayHeight

    val resolveInfo = context.packageManager.resolveActivity(
        launchIntent,
        PackageManager.MATCH_DEFAULT_ONLY
    )
    val declaredOrientation = resolveInfo?.activityInfo?.screenOrientation
        ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

    val isLandscape = when (declaredOrientation) {
        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
        ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE,
        ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE,
        ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE -> true
        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
        ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT,
        ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT,
        ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT -> false
        else -> isDeviceLandscape
    }

    // 横屏交给系统默认处理，避免把原本正常的窗口锁死成小尺寸
    if (isLandscape) {
        return null
    }

    // 竖屏应用：以屏幕短边百分比为宽，按比例计算高。
    // 该 API 的默认尺寸越接近屏幕，系统越允许双向缩放。
    val minSide = min(displayWidth, displayHeight)
    val maxWidth = (displayWidth * 0.85f).toInt()
    val maxHeight = (displayHeight * 0.85f).toInt()

    var width = (minSide * 1f).toInt()
    var height = (width * 1.45f).toInt()
    if (width > maxWidth || height > maxHeight) {
        val scale = min(maxWidth / width.toFloat(), maxHeight / height.toFloat())
        width = (width * scale).toInt()
        height = (height * scale).toInt()
    }

    val left = (displayWidth - width) / 2
    val top = (displayHeight - height) / 5
    return Rect(left, top, left + width, top + height)
}

// 启动指定包名的应用；useFreeform 为 true 时尝试以自由窗口（小窗）模式启动
fun launchApp(context: Context, packageName: String, useFreeform: Boolean = false): Boolean {
    return try {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            if (useFreeform) {
                try {
                    ensureHiddenApiExempt()
                    val options = ActivityOptions.makeBasic()
                    HiddenApiBypass.invoke(
                        ActivityOptions::class.java,
                        options,
                        "setLaunchWindowingMode",
                        WINDOWING_MODE_FREEFORM
                    )
                    computeFreeformBounds(context, launchIntent)?.let { bounds ->
                        options.setLaunchBounds(bounds)
                    }
                    context.startActivity(launchIntent, options.toBundle())
                } catch (_: Throwable) {
                    // 小窗启动失败时降级为普通启动
                    context.startActivity(launchIntent)
                }
            } else {
                context.startActivity(launchIntent)
            }
            true
        } else {
            false
        }
    } catch (_: Exception) {
        false
    }
}

// 将 Drawable 转换为 Bitmap
fun android.graphics.drawable.Drawable.toBitmap(): android.graphics.Bitmap {
    val bitmap = android.graphics.Bitmap.createBitmap(
        intrinsicWidth.coerceAtLeast(1),
        intrinsicHeight.coerceAtLeast(1),
        android.graphics.Bitmap.Config.ARGB_8888
    )
    val canvas = android.graphics.Canvas(bitmap)
    setBounds(0, 0, canvas.width, canvas.height)
    draw(canvas)
    return bitmap
}
