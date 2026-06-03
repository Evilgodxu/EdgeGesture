package com.byss.jh.service

import android.content.ComponentName
import android.os.IBinder
import android.os.Process
import android.util.Log
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Shizuku UserService 实现类
 * 在独立进程中运行，具有 shell/root 权限
 */
class CommandUserService : ICommandService.Stub() {

    companion object {
        private const val TAG = "CommandUserService"

        // UserService 组件名
        private val COMPONENT_NAME = ComponentName(
            "com.byss.jh",
            "com.byss.jh.service.CommandUserService"
        )

        /**
         * 构建 UserService 启动参数
         */
        fun createServiceArgs(): Shizuku.UserServiceArgs {
            return Shizuku.UserServiceArgs(COMPONENT_NAME)
        }
    }

    init {
        Log.i(TAG, "CommandUserService created, uid=${Process.myUid()}")
    }

    override fun executeCommand(command: String): String {
        return try {
            val process = Runtime.getRuntime().exec(command)
            val output = StringBuilder()
            val error = StringBuilder()

            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                reader.lineSequence().forEach { output.appendLine(it) }
            }

            BufferedReader(InputStreamReader(process.errorStream)).use { reader ->
                reader.lineSequence().forEach { error.appendLine(it) }
            }

            val exitCode = process.waitFor()

            if (exitCode == 0) {
                output.toString()
            } else {
                "Error (exit code $exitCode): $error"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to execute command: $command", e)
            "Exception: ${e.message}"
        }
    }

    override fun forceStopPackage(packageName: String): Boolean {
        return try {
            val result = executeCommand("am force-stop $packageName")
            !result.startsWith("Error") && !result.startsWith("Exception")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to force stop package: $packageName", e)
            false
        }
    }

    override fun isAlive(): Boolean {
        return true
    }

    // 服务销毁方法，由 Shizuku 调用
    fun destroy() {
        Log.i(TAG, "CommandUserService destroyed")
    }
}
