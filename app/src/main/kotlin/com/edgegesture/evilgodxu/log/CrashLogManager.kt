package com.edgegesture.evilgodxu.log

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.io.StringWriter
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.system.exitProcess

/**
 * 日志管理器：捕获未捕获异常并记录所有 catch 到的异常。
 *
 * - 按天生成日志文件，实现每日刷新
 * - 只保留最近 [KEEP_DAYS] 天的日志文件，超出自动清理
 * - 日志写入应用专属外部目录（getExternalFilesDir），无需存储权限
 * - 链式调用系统默认处理器，不干扰原有崩溃处理流程
 */
object CrashLogManager : Thread.UncaughtExceptionHandler {

    private const val TAG = "CrashLogManager"

    /** 日志文件存放目录名（位于应用专属外部目录下） */
    private const val LOG_DIR_NAME = "logs"

    /** 日志文件名前缀 */
    private const val LOG_FILE_PREFIX = "crash_"

    /** 保留天数：最多保留最近 3 天的日志 */
    private const val KEEP_DAYS = 3L

    private val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val timeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

    private var logDir: File? = null
    private var appVersion = "unknown"
    private var previousHandler: Thread.UncaughtExceptionHandler? = null

    /** 初始化日志系统，应在 Application.onCreate 最前面调用 */
    fun init(context: Context) {
        logDir = File(context.getExternalFilesDir(null), LOG_DIR_NAME).apply { mkdirs() }
        appVersion = runCatching {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            "${info.versionName} (${info.versionCode})"
        }.getOrDefault("unknown")

        // 链式接管默认处理器，保留系统默认崩溃流程
        previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)

        cleanOldLogs()
    }

    /**
     * 记录一般异常日志，标题格式：类名 + 中文描述
     *
     * @param className   发生异常的类名
     * @param description 中文描述该操作失败原因
     * @param throwable   捕获到的异常，可为空
     */
    fun logException(className: String, description: String, throwable: Throwable? = null) {
        if (logDir == null) {
            // 未初始化（如 Shizuku 独立进程）时降级到系统日志
            Log.e(TAG, "$className: $description", throwable)
            return
        }
        writeLog(title = "$className: $description", throwable = throwable)
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        writeLog(title = "未捕获异常（线程 ${thread.name}）", thread = thread, throwable = throwable, withDeviceInfo = true)
        // 交给原处理器；没有原处理器时主动结束进程，保持系统默认行为
        previousHandler?.uncaughtException(thread, throwable) ?: exitProcess(2)
    }

    @Synchronized
    private fun writeLog(title: String, thread: Thread? = null, throwable: Throwable?, withDeviceInfo: Boolean = false) {
        cleanOldLogs()
        val dir = logDir ?: return
        val logFile = File(dir, "$LOG_FILE_PREFIX${LocalDate.now().format(dateFormat)}.log")
        try {
            FileWriter(logFile, true).use { writer ->
                writer.appendLine("================ $title ================")
                writer.appendLine("时间: ${LocalDateTime.now().format(timeFormat)}")
                if (withDeviceInfo) {
                    writer.appendLine("线程: ${thread?.name}")
                    writer.appendLine("进程: ${android.os.Process.myPid()}")
                    writer.appendLine("设备: ${Build.MANUFACTURER} ${Build.MODEL}")
                    writer.appendLine("系统: Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                    writer.appendLine("版本: $appVersion")
                }
                if (throwable != null) {
                    writer.appendLine("异常: ${throwable.javaClass.name}: ${throwable.message}")
                    writer.appendLine("堆栈:")
                    StringWriter().use { sw ->
                        throwable.printStackTrace(PrintWriter(sw))
                        writer.append(sw.toString())
                    }
                }
                writer.appendLine()
            }
        } catch (e: Exception) {
            // 写日志本身失败时降级到系统日志，避免递归崩溃
            Log.e(TAG, "写入日志失败", e)
        }
    }

    /** 清理超过保留天数的旧日志文件 */
    private fun cleanOldLogs() {
        val dir = logDir ?: return
        val deadline = System.currentTimeMillis() - KEEP_DAYS * 24 * 60 * 60 * 1000L
        dir.listFiles { f -> f.isFile && f.name.startsWith(LOG_FILE_PREFIX) }
            ?.filter { it.lastModified() < deadline }
            ?.forEach { it.delete() }
    }
}
