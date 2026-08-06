package com.edgegesture.evilgodxu.screens.gesture.service.musicpanel

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import com.edgegesture.evilgodxu.log.CrashLogManager

internal fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

internal fun formatSignalRate(rate: Int): String = if (rate >= 1000) {
    "${rate / 1000.0} kHz"
} else {
    "$rate Hz"
}

internal fun formatSignalChannels(channels: Int): String = when (channels) {
    1 -> "单声道"
    2 -> "立体声"
    else -> "$channels 声道"
}

internal fun copyToClipboard(context: Context, text: String) {
    try {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("label", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "已复制: $text", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        CrashLogManager.logException("MusicPanelUtils", "复制到剪贴板失败", e)
    }
}