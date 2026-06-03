package com.byss.jh.data.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Log
import com.byss.jh.service.ICommandService
import com.byss.jh.service.CommandUserService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuProvider

// Shizuku 状态
sealed class ShizukuState {
    data object NotInstalled : ShizukuState()
    data object NotRunning : ShizukuState()
    data object Waiting : ShizukuState()
    data object Granted : ShizukuState()
    data object Denied : ShizukuState()
}

object ShizukuManager {

    private const val TAG = "ShizukuManager"

    private val _state = MutableStateFlow<ShizukuState>(ShizukuState.NotRunning)
    val state: StateFlow<ShizukuState> = _state.asStateFlow()

    private var permissionListener: Shizuku.OnRequestPermissionResultListener? = null

    // UserService 连接
    private var commandService: ICommandService? = null
    private var isServiceBinding = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            commandService = ICommandService.Stub.asInterface(service)
            isServiceBinding = false
            Log.i(TAG, "CommandService connected")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            commandService = null
            isServiceBinding = false
            Log.i(TAG, "CommandService disconnected")
        }
    }

    fun init(context: Context) {
        // 检查 Shizuku 是否安装
        if (!isShizukuInstalled(context)) {
            _state.value = ShizukuState.NotInstalled
            return
        }

        // 添加 Binder 接收监听
        Shizuku.addBinderReceivedListener {
            updateState()
            // Binder 可用时自动绑定 UserService
            bindUserService()
        }

        Shizuku.addBinderDeadListener {
            _state.value = ShizukuState.NotRunning
            commandService = null
        }

        // 初始状态检查
        updateState()
        if (Shizuku.pingBinder()) {
            bindUserService()
        }
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

    // 绑定 UserService
    private fun bindUserService() {
        if (commandService != null || isServiceBinding) return
        if (!isAvailable()) return

        isServiceBinding = true
        try {
            val args = CommandUserService.createServiceArgs()
            Shizuku.bindUserService(args, serviceConnection)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to bind UserService", e)
            isServiceBinding = false
        }
    }

    // 解绑 UserService
    fun unbindUserService() {
        if (commandService != null) {
            try {
                Shizuku.unbindUserService(
                    CommandUserService.createServiceArgs(),
                    serviceConnection,
                    true
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to unbind UserService", e)
            }
            commandService = null
        }
    }

    // 通过 UserService 执行 shell 命令
    fun executeCommand(command: String): Result<String> {
        val service = commandService
        return if (service != null && service.isAlive) {
            try {
                val result = service.executeCommand(command)
                Result.success(result)
            } catch (e: Exception) {
                Log.e(TAG, "Command execution failed", e)
                Result.failure(e)
            }
        } else {
            // 服务未连接，尝试绑定
            bindUserService()
            Result.failure(IllegalStateException("CommandService not connected"))
        }
    }

    // 强制停止应用
    fun forceStopPackage(packageName: String): Result<String> {
        val service = commandService
        return if (service != null && service.isAlive) {
            try {
                val success = service.forceStopPackage(packageName)
                if (success) {
                    Result.success("Package $packageName force stopped")
                } else {
                    Result.failure(Exception("Failed to force stop package"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Force stop failed", e)
                Result.failure(e)
            }
        } else {
            bindUserService()
            Result.failure(IllegalStateException("CommandService not connected"))
        }
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
