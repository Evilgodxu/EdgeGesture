package com.byss.jh.data.shizuku

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuProvider
import java.io.BufferedReader
import java.io.InputStreamReader

// Shizuku 状态
sealed class ShizukuState {
    data object NotInstalled : ShizukuState()
    data object NotRunning : ShizukuState()
    data object Waiting : ShizukuState()
    data object Granted : ShizukuState()
    data object Denied : ShizukuState()
}

object ShizukuManager {

    private val _state = MutableStateFlow<ShizukuState>(ShizukuState.NotRunning)
    val state: StateFlow<ShizukuState> = _state.asStateFlow()

    private var permissionListener: Shizuku.OnRequestPermissionResultListener? = null

    fun init(context: Context) {
        // 检查 Shizuku 是否安装
        if (!isShizukuInstalled(context)) {
            _state.value = ShizukuState.NotInstalled
            return
        }

        // 添加 Binder 接收监听
        Shizuku.addBinderReceivedListener {
            updateState()
        }

        Shizuku.addBinderDeadListener {
            _state.value = ShizukuState.NotRunning
        }

        // 初始状态检查
        updateState()
    }

    fun addPermissionListener(listener: Shizuku.OnRequestPermissionResultListener) {
        permissionListener = listener
        Shizuku.addRequestPermissionResultListener(listener)
    }

    fun removePermissionListener() {
        permissionListener?.let {
            Shizuku.removeRequestPermissionResultListener(it)
        }
        permissionListener = null
    }

    fun requestPermission(requestCode: Int) {
        if (Shizuku.isPreV11()) {
            _state.value = ShizukuState.Denied
            return
        }

        when {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED -> {
                _state.value = ShizukuState.Granted
            }
            Shizuku.shouldShowRequestPermissionRationale() -> {
                _state.value = ShizukuState.Denied
            }
            else -> {
                _state.value = ShizukuState.Waiting
                Shizuku.requestPermission(requestCode)
            }
        }
    }

    private fun updateState() {
        if (!Shizuku.pingBinder()) {
            _state.value = ShizukuState.NotRunning
            return
        }

        _state.value = if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            ShizukuState.Granted
        } else {
            ShizukuState.Denied
        }
    }

    fun isAvailable(): Boolean {
        return Shizuku.pingBinder() &&
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    }

    // 通过 Shizuku 执行 shell 命令
    fun executeCommand(command: String): Result<String> {
        return try {
            val process = Shizuku.newProcess(
                arrayOf("sh", "-c", command),
                null,
                null
            )

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
                Result.success(output.toString())
            } else {
                Result.failure(Exception("Exit code: $exitCode, Error: $error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 强制停止应用
    fun forceStopPackage(packageName: String): Result<String> {
        return executeCommand("am force-stop $packageName")
    }

    private fun isShizukuInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(ShizukuProvider.MANAGER_APPLICATION_ID, 0)
            true
        } catch (_: Exception) {
            false
        }
    }
}
