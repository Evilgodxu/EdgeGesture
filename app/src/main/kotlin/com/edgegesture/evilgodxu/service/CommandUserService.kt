package com.edgegesture.evilgodxu.service

import android.content.ComponentName
import android.os.IBinder
import com.edgegesture.evilgodxu.log.CrashLogManager
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * Shizuku UserService 实现类
 * 在独立进程中运行，具有 shell/root 权限
 */
class CommandUserService : ICommandService.Stub() {

    companion object {
        // UserService 组件名
        private val COMPONENT_NAME = ComponentName(
            "com.edgegesture.evilgodxu",
            "com.edgegesture.evilgodxu.service.CommandUserService"
        )

        private const val COMMAND_TIMEOUT_SECONDS = 30L
        private const val READER_JOIN_TIMEOUT_MS = 5000L

        /**
         * 构建 UserService 启动参数
         * 注意：必须设置 processNameSuffix，否则 13.1.5 的 UserServiceArgs 在绑定时会因
         * processName 为 null 抛 "process name suffix must not be null"
         */
        fun createServiceArgs(): Shizuku.UserServiceArgs {
            return Shizuku.UserServiceArgs(COMPONENT_NAME)
                .processNameSuffix("user_service")
        }
    }

    override fun executeCommand(command: String): String {
        return try {
            val process = Runtime.getRuntime().exec(command)
            val output = StringBuilder()
            val error = StringBuilder()

            // 并发读取 stdout/stderr，避免任一管道缓冲写满时子进程阻塞导致死锁
            val stdoutReader = thread(name = "stdout-reader") {
                BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                    reader.lineSequence().forEach { output.appendLine(it) }
                }
            }
            val stderrReader = thread(name = "stderr-reader") {
                BufferedReader(InputStreamReader(process.errorStream)).use { reader ->
                    reader.lineSequence().forEach { error.appendLine(it) }
                }
            }

            // 等待命令完成，超时后强制终止，避免挂起命令长期占住 Shizuku binder 线程
            val completed = process.waitFor(COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            stdoutReader.join(READER_JOIN_TIMEOUT_MS)
            stderrReader.join(READER_JOIN_TIMEOUT_MS)

            if (!completed) {
                process.destroyForcibly()
                return "Error (timeout after $COMMAND_TIMEOUT_SECONDS seconds)"
            }

            val exitCode = process.exitValue()
            if (exitCode == 0) {
                output.toString()
            } else {
                "Error (exit code $exitCode): $error"
            }
        } catch (e: Exception) {
            CrashLogManager.logException("CommandUserService", "执行 shell 命令失败", e)
            "Exception: ${e.message}"
        }
    }

    // UserService 绑定成功后进程即存活；binder 断开时 Shizuku 会回调
    // onServiceDisconnected 置空连接，因此客户端判断 commandService 非空即可
    override fun isAlive(): Boolean {
        return true
    }

    // 服务销毁方法，由 Shizuku 调用
    fun destroy() {
    }
}
