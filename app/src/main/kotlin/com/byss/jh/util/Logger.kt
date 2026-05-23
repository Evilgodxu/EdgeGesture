package com.byss.jh.util

import android.content.Context
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * 日志系统
 * - 异步写入外部私有目录
 * - 3天保留
 * - 每日轮换
 * - 日志头显示系统信息
 * - 只保留最近50条记录
 */
object Logger {
    private const val LOG_DIR = "logs"
    private const val RETENTION_DAYS = 3
    private const val MAX_LOG_ENTRIES = 50
    private const val DATE_FORMAT = "yyyyMMdd"
    private const val TIME_FORMAT = "HH:mm:ss"
    
    private val scope = CoroutineScope(Dispatchers.IO)
    private val headerMutex = Mutex()

    // 使用固定 Locale 创建 formatter，避免 ConstantLocale 警告
    // 日志日期格式是技术性的，不需要随用户语言变化
    private fun createDateFormatter() = SimpleDateFormat(DATE_FORMAT, Locale.US)
    private fun createTimeFormatter() = SimpleDateFormat(TIME_FORMAT, Locale.US)
    private val writtenHeaders = mutableSetOf<String>()
    
    /**
     * 记录日志（异步）
     */
    fun log(context: Context, level: LogLevel, tag: String, message: String) {
        scope.launch {
            writeLog(context, level, tag, message)
        }
    }
    
    /**
     * 记录调试日志
     */
    fun d(context: Context, tag: String, message: String) {
        log(context, LogLevel.DEBUG, tag, message)
    }
    
    /**
     * 记录信息日志
     */
    fun i(context: Context, tag: String, message: String) {
        log(context, LogLevel.INFO, tag, message)
    }
    
    /**
     * 记录警告日志
     */
    fun w(context: Context, tag: String, message: String) {
        log(context, LogLevel.WARNING, tag, message)
    }
    
    /**
     * 记录错误日志
     */
    fun e(context: Context, tag: String, message: String) {
        log(context, LogLevel.ERROR, tag, message)
    }
    
    /**
     * 清理过期日志
     */
    fun cleanupOldLogs(context: Context) {
        scope.launch {
            cleanOldLogsInternal(context)
        }
    }
    
    private suspend fun writeLog(
        context: Context,
        level: LogLevel,
        tag: String,
        message: String
    ) = withContext(Dispatchers.IO) {
        try {
            val logDir = File(context.getExternalFilesDir(null), LOG_DIR)
            if (!logDir.exists()) {
                logDir.mkdirs()
            }
            
            val currentDate = createDateFormatter().format(Date())
            val logFile = File(logDir, "$currentDate.log")

            // 检查并写入日志头（每个日志文件只写一次）
            writeHeaderIfNeeded(context, logFile, currentDate)

            val timestamp = createTimeFormatter().format(Date())
            val logLine = "[$timestamp] [${level.name}] [$tag] $message"
            
            FileWriter(logFile, true).use { writer ->
                writer.appendLine(logLine)
            }
            
            // 限制日志文件只保留最近50条记录
            trimLogFile(logFile)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * 限制日志文件只保留最近MAX_LOG_ENTRIES条记录
     * 注意：保留日志头（以"["开头的为正式日志，之前的都是头部信息）
     */
    private fun trimLogFile(logFile: File) {
        try {
            if (!logFile.exists() || logFile.length() == 0L) {
                return
            }

            val lines = logFile.readLines()

            // 找到第一个正式日志行（以"["开头的）
            val firstLogIndex = lines.indexOfFirst { it.startsWith("[") }
            if (firstLogIndex == -1) {
                // 没有正式日志，不需要裁剪
                return
            }

            val headerLines = lines.subList(0, firstLogIndex)
            val logLines = lines.subList(firstLogIndex, lines.size)

            // 如果日志行数超过限制，只保留最后MAX_LOG_ENTRIES条日志
            if (logLines.size > MAX_LOG_ENTRIES) {
                val startIndex = logLines.size - MAX_LOG_ENTRIES
                val trimmedLogLines = logLines.subList(startIndex, logLines.size)
                val result = headerLines + trimmedLogLines
                logFile.writeText(result.joinToString("\n") + "\n")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private suspend fun writeHeaderIfNeeded(context: Context, logFile: File, dateKey: String) {
        headerMutex.withLock {
            if (writtenHeaders.contains(dateKey) || logFile.exists()) {
                return
            }
            
            // 获取屏幕信息
            val displayMetrics = context.resources.displayMetrics
            
            val header = buildString {
                appendLine("设备信息:")
                appendLine("品牌: ${Build.BRAND}")
                appendLine("型号: ${Build.MODEL}")
                appendLine("Android版本: ${Build.VERSION.RELEASE}")
                appendLine("SDK版本: ${Build.VERSION.SDK_INT}")
                appendLine()
                appendLine("屏幕信息:")
                appendLine("宽度: ${displayMetrics.widthPixels}px")
                appendLine("高度: ${displayMetrics.heightPixels}px")
                appendLine("密度: ${displayMetrics.densityDpi}dpi")
                appendLine("密度比例: ${displayMetrics.density}")
                appendLine()
            }
            
            try {
                FileWriter(logFile, true).use { writer ->
                    writer.append(header)
                }
                writtenHeaders.add(dateKey)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    private suspend fun cleanOldLogsInternal(context: Context) = withContext(Dispatchers.IO) {
        try {
            val logDir = File(context.getExternalFilesDir(null), LOG_DIR)
            if (!logDir.exists() || !logDir.isDirectory) {
                return@withContext
            }
            
            val cutoffTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(RETENTION_DAYS.toLong())
            
            logDir.listFiles { file ->
                file.isFile && file.name.matches(Regex("\\d{8}\\.log"))
            }?.forEach { file ->
                if (file.lastModified() < cutoffTime) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    enum class LogLevel {
        DEBUG,
        INFO,
        WARNING,
        ERROR
    }
}
