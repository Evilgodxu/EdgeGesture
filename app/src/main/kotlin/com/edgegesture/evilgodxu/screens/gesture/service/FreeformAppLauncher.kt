package com.edgegesture.evilgodxu.screens.gesture.service

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Rect
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min
import org.lsposed.hiddenapibypass.HiddenApiBypass

class FreeformAppLauncher(private val context: Context) {
    fun launch(packageName: String, useFreeform: Boolean = false): Boolean {
        return try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent == null) return false

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
                    computeFreeformBounds(launchIntent)?.let(options::setLaunchBounds)
                    context.startActivity(launchIntent, options.toBundle())
                } catch (_: Throwable) {
                    context.startActivity(launchIntent)
                }
            } else {
                context.startActivity(launchIntent)
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun computeFreeformBounds(launchIntent: Intent): Rect? {
        val metrics = context.resources.displayMetrics
        val displayWidth = metrics.widthPixels
        val displayHeight = metrics.heightPixels
        val isDeviceLandscape = displayWidth > displayHeight
        val declaredOrientation = context.packageManager.resolveActivity(
            launchIntent,
            PackageManager.MATCH_DEFAULT_ONLY
        )?.activityInfo?.screenOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

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
        if (isLandscape) return null

        val minSide = min(displayWidth, displayHeight)
        val maxWidth = displayWidth
        val maxHeight = displayHeight
        var width = minSide
        var height = (width * 1.6f).toInt()
        if (width > maxWidth || height > maxHeight) {
            val scale = min(maxWidth / width.toFloat(), maxHeight / height.toFloat())
            width = (width * scale).toInt()
            height = (height * scale).toInt()
        }
        val left = (displayWidth - width) / 2
        val top = (displayHeight - height) / 5
        return Rect(left, top, left + width, top + height)
    }

    companion object {
        private const val WINDOWING_MODE_FREEFORM = 5
        private val hiddenApiInitialized = AtomicBoolean(false)

        private fun ensureHiddenApiExempt() {
            if (hiddenApiInitialized.compareAndSet(false, true)) {
                try {
                    HiddenApiBypass.addHiddenApiExemptions("Landroid.app.ActivityOptions;")
                } catch (_: Throwable) {
                }
            }
        }
    }
}
