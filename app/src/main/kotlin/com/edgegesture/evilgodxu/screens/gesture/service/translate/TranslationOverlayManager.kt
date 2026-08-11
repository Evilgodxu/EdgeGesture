package com.edgegesture.evilgodxu.screens.gesture.service.translate

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Looper
import android.provider.Settings
import android.text.Layout
import android.text.StaticLayout
import android.util.TypedValue
import android.view.Display
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import com.edgegesture.evilgodxu.R
import com.edgegesture.evilgodxu.data.translate.TranslationService
import com.edgegesture.evilgodxu.log.CrashLogManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * 屏幕翻译悬浮窗：
 * 从无障碍树取当前窗口的文本及屏幕坐标，翻译后在原文位置原位覆盖译文。
 * 悬浮窗整体不可触摸，点击、滑动会穿透到下方应用，不影响原应用交互。
 * 开关式交互：首次触发显示译文，再次触发关闭。
 */
class TranslationOverlayManager(
    private val service: AccessibilityService,
    private val onDismiss: () -> Unit,
) {
    private val windowManager = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null
    private var rootView: FrameLayout? = null
    @Volatile private var active = false

    val isActive: Boolean get() = active

    fun show() {
        if (active) return
        active = true
        toast(service.getString(R.string.translate_started))
        job = scope.launch {
            val items = runCatching { buildDisplayItems() }.getOrElse { e ->
                if (e is CancellationException) throw e
                CrashLogManager.logException("TranslationOverlayManager", "屏幕翻译失败", e)
                null
            }
            withContext(Dispatchers.Main) {
                if (!active) return@withContext
                when {
                    items == null -> finishWithToast(R.string.translate_failed)
                    items.isEmpty() -> finishWithToast(R.string.translate_no_text)
                    else -> showOverlay(items)
                }
            }
        }
    }

    fun dismiss() {
        active = false
        job?.cancel()
        job = null
        val root = rootView ?: return
        rootView = null
        if (Looper.myLooper() == Looper.getMainLooper()) {
            removeWindow(root)
        } else {
            service.mainExecutor.execute { removeWindow(root) }
        }
    }

    private fun removeWindow(root: View) {
        try {
            if (root.windowToken != null) {
                windowManager.removeView(root)
            }
        } catch (e: Exception) {
            CrashLogManager.logException("TranslationOverlayManager", "移除翻译悬浮窗失败", e)
        }
    }

    private fun finishWithToast(resId: Int) {
        active = false
        toast(service.getString(resId))
        onDismiss()
    }

    private fun toast(message: String) {
        service.mainExecutor.execute {
            Toast.makeText(service, message, Toast.LENGTH_SHORT).show()
        }
    }

    private data class TextSegment(val bounds: Rect, val text: String)

    private data class DisplayItem(
        val bounds: Rect,
        val translated: String,
        val bgColor: Int,
        val textColor: Int,
        val singleLine: Boolean,
    )

    // 截图 → 取文本 → 翻译 → 计算每个文本块的背景色，返回待显示条目
    private suspend fun buildDisplayItems(): List<DisplayItem> {
        val root = service.rootInActiveWindow ?: return emptyList()
        if (root.packageName == service.packageName) return emptyList()
        val segments = collectTextSegments(root)
        if (segments.isEmpty()) return emptyList()

        val screenshot = takeScreenshot()
        val darkMode = if (screenshot == null) isAppDarkMode(root.packageName?.toString()) else null
        val deviceId =
            Settings.Secure.getString(service.contentResolver, Settings.Secure.ANDROID_ID) ?: ""

        val translated = TranslationService.get().translateWithFallback(
            texts = segments.map { it.text },
            deviceId = deviceId,
        )

        val items = mutableListOf<DisplayItem>()
        val density = service.resources.displayMetrics.density
        for (i in segments.indices) {
            val text = translated.getOrNull(i)?.trim().orEmpty()
            // 译文为空或与原文相同（本就在目标语言）时无需覆盖
            if (text.isEmpty() || text == segments[i].text) continue
            val bounds = segments[i].bounds
            // 外扩覆盖框，避免原文字形超出上报 bounds 导致边缘露出
            val marginH = maxOf((2 * density).toInt(), bounds.width() / 30)
            val marginV = maxOf((2 * density).toInt(), bounds.height() / 20)
            val cover = Rect(
                (bounds.left - marginH).coerceAtLeast(0),
                (bounds.top - marginV).coerceAtLeast(0),
                bounds.right + marginH,
                bounds.bottom + marginV
            )
            val bg = screenshot?.let { sampleBorderColor(it, cover) }
                ?: if (darkMode == true) DARK_BG_COLOR else Color.WHITE
            val fg = if (isLightColor(bg)) Color.BLACK else Color.WHITE
            items.add(DisplayItem(cover, text, bg, fg, isLikelySingleLine(segments[i].text)))
        }
        return items
    }

    // 遍历无障碍树收集可见文本及其屏幕坐标
    private fun collectTextSegments(root: AccessibilityNodeInfo): List<TextSegment> {
        val density = service.resources.displayMetrics.density
        val minWidth = (18 * density).toInt()
        val minHeight = (10 * density).toInt()
        val segments = mutableListOf<TextSegment>()

        fun visit(node: AccessibilityNodeInfo) {
            if (segments.size >= MAX_SEGMENTS) return
            val text = node.text?.toString()?.trim()
            if (!text.isNullOrEmpty() && text.length <= MAX_TEXT_LENGTH &&
                node.isVisibleToUser && !node.isEditable && !isNoiseText(node, text)
            ) {
                val bounds = Rect()
                node.getBoundsInScreen(bounds)
                if (!bounds.isEmpty && bounds.width() >= minWidth && bounds.height() >= minHeight) {
                    segments.add(TextSegment(bounds, text))
                }
            }
            for (i in 0 until node.childCount) {
                if (segments.size >= MAX_SEGMENTS) return
                val child = node.getChild(i) ?: continue
                visit(child)
            }
        }
        visit(root)

        // 父容器与子节点常上报相同文本，仅保留其中覆盖范围最小的一条
        val cleaned = mutableListOf<TextSegment>()
        for (segment in segments) {
            val contained = cleaned.any {
                it.bounds.contains(segment.bounds) && it.text == segment.text
            }
            if (contained) continue
            cleaned.removeAll { segment.bounds.contains(it.bounds) && it.text == segment.text }
            cleaned.add(segment)
        }
        return cleaned
    }

    // 过滤旁枝信息（昵称、时间、计数、纯数字等），只保留正文文本
    private fun isNoiseText(node: AccessibilityNodeInfo, text: String): Boolean {
        if (text.length < 2) return true
        if (text.none { it.isLetterOrDigit() }) return true
        // 纯数字/时间戳/计数器等无翻译价值的文本
        if (text.all { it.isDigit() || it.isWhitespace() || it in ":./年月日时分秒-｜" }) return true
        val viewId = node.viewIdResourceName?.substringAfterLast('/')?.lowercase().orEmpty()
        if (viewId.isNotEmpty()) {
            val noiseKeywords = listOf(
                "name", "nick", "author", "user_name", "time", "date", "count",
                "badge", "avatar", "index", "icon"
            )
            if (noiseKeywords.any { viewId.contains(it) }) return true
        }
        return false
    }

    private suspend fun takeScreenshot(): Bitmap? = suspendCancellableCoroutine { cont ->
        try {
            service.takeScreenshot(
                Display.DEFAULT_DISPLAY,
                service.mainExecutor,
                object : AccessibilityService.TakeScreenshotCallback {
                    override fun onSuccess(screenshot: AccessibilityService.ScreenshotResult) {
                        if (cont.isCancelled) return
                        try {
                            val wrapped = Bitmap.wrapHardwareBuffer(
                                screenshot.hardwareBuffer,
                                screenshot.colorSpace
                            )
                            // 复制为软件位图，之后可安全关闭硬件缓冲
                            cont.resume(wrapped?.copy(Bitmap.Config.ARGB_8888, false))
                        } catch (e: Exception) {
                            cont.resume(null)
                        } finally {
                            screenshot.hardwareBuffer.close()
                        }
                    }

                    override fun onFailure(errorCode: Int) {
                        if (!cont.isCancelled) cont.resume(null)
                    }
                }
            )
        } catch (e: Exception) {
            if (!cont.isCancelled) cont.resume(null)
        }
    }

    // 采样文本块四周的像素颜色作为译文背景，使覆盖与原页面背景近似一致
    private fun sampleBorderColor(bitmap: Bitmap, bounds: Rect): Int {
        val left = bounds.left.coerceIn(0, bitmap.width - 1)
        val top = bounds.top.coerceIn(0, bitmap.height - 1)
        val right = (bounds.right - 1).coerceIn(0, bitmap.width - 1)
        val bottom = (bounds.bottom - 1).coerceIn(0, bitmap.height - 1)
        if (right <= left || bottom <= top) return Color.WHITE

        var r = 0L
        var g = 0L
        var b = 0L
        var count = 0L
        fun sample(x: Int, y: Int) {
            val color = bitmap.getPixel(x, y)
            r += Color.red(color)
            g += Color.green(color)
            b += Color.blue(color)
            count++
        }
        val stepX = maxOf(1, (right - left) / 40)
        val stepY = maxOf(1, (bottom - top) / 40)
        var x = left
        while (x <= right) {
            sample(x, top)
            sample(x, bottom)
            x += stepX
        }
        var y = top + 1
        while (y < bottom) {
            sample(left, y)
            sample(right, y)
            y += stepY
        }
        if (count == 0L) return Color.WHITE
        return Color.rgb(
            (r / count).toInt(),
            (g / count).toInt(),
            (b / count).toInt()
        )
    }

    private fun isAppDarkMode(packageName: String?): Boolean {
        if (packageName.isNullOrEmpty()) return false
        return try {
            val appContext = service.createPackageContext(packageName, 0)
            (appContext.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
        } catch (e: Exception) {
            false
        }
    }

    private fun isLightColor(color: Int): Boolean {
        return 0.299f * Color.red(color) + 0.587f * Color.green(color) + 0.114f * Color.blue(color) > 150f
    }

    private fun isLikelySingleLine(text: String): Boolean =
        !text.contains('\n') && text.length <= 12

    private fun showOverlay(items: List<DisplayItem>) {
        val root = FrameLayout(service)
        root.setBackgroundColor(Color.TRANSPARENT)
        for (item in items) {
            val view = createItemView(item)
            val layoutParams = FrameLayout.LayoutParams(item.bounds.width(), item.bounds.height())
            layoutParams.leftMargin = item.bounds.left
            layoutParams.topMargin = item.bounds.top
            root.addView(view, layoutParams)
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 0
        }
        try {
            windowManager.addView(root, params)
            rootView = root
        } catch (e: Exception) {
            CrashLogManager.logException("TranslationOverlayManager", "显示翻译悬浮窗失败", e)
            finishWithToast(R.string.translate_failed)
        }
    }

    private fun createItemView(item: DisplayItem): TextView {
        val density = service.resources.displayMetrics.density
        val paddingH = (item.bounds.width() * 0.03f).toInt().coerceAtLeast((2 * density).toInt())
        val paddingV = (item.bounds.height() * 0.05f).toInt().coerceAtLeast(density.toInt())
        val textView = TextView(service).apply {
            setBackgroundColor(item.bgColor)
            setTextColor(item.textColor)
            text = item.translated
            gravity = if (item.singleLine) Gravity.CENTER else (Gravity.START or Gravity.CENTER_VERTICAL)
            setPadding(paddingH, paddingV, paddingH, paddingV)
            includeFontPadding = false
        }
        fitTextToBounds(
            textView,
            maxWidth = (item.bounds.width() - paddingH * 2).coerceAtLeast(1),
            maxHeight = (item.bounds.height() - paddingV * 2).coerceAtLeast(1),
            singleLine = item.singleLine
        )
        return textView
    }

    // 从预估字号开始逐级缩小，直到译文整体高度能放进原文文本块
    private fun fitTextToBounds(textView: TextView, maxWidth: Int, maxHeight: Int, singleLine: Boolean) {
        val lineCount = textView.text.count { it == '\n' } + 1
        var size = (maxHeight / lineCount.toFloat()).coerceIn(8f, 40f)
        while (size > 6f) {
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, size)
            val layout = StaticLayout.Builder.obtain(
                textView.text,
                0,
                textView.text.length,
                textView.paint,
                maxWidth
            )
                .setAlignment(if (singleLine) Layout.Alignment.ALIGN_CENTER else Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1f)
                .build()
            if (layout.height <= maxHeight) return
            size *= 0.9f
        }
    }

    private companion object {
        const val MAX_SEGMENTS = 150
        const val MAX_TEXT_LENGTH = 1800
        val DARK_BG_COLOR = Color.rgb(0x1e, 0x1e, 0x1e)
    }
}
