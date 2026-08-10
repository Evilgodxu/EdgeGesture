package com.edgegesture.evilgodxu.data.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import com.edgegesture.evilgodxu.log.CrashLogManager
import com.edgegesture.evilgodxu.service.ICommandService
import com.edgegesture.evilgodxu.service.CommandUserService
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

    private val _state = MutableStateFlow<ShizukuState>(ShizukuState.NotRunning)
    val state: StateFlow<ShizukuState> = _state.asStateFlow()

    private var permissionListener: Shizuku.OnRequestPermissionResultListener? = null

    // UserService 连接
    private var commandService: ICommandService? = null
    private var isServiceBinding = false

    // init 幂等标志：binder 监听器只注册一次，避免重复注册导致监听器累积
    @Volatile
    private var initialized = false

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        updateState()
        // Binder 可用时自动绑定 UserService
        bindUserService()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        _state.value = ShizukuState.NotRunning
        commandService = null
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            commandService = ICommandService.Stub.asInterface(service)
            isServiceBinding = false
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            commandService = null
            isServiceBinding = false
        }
    }

    fun init(context: Context) {
        // 检查 Shizuku 是否安装
        if (!isShizukuInstalled(context)) {
            _state.value = ShizukuState.NotInstalled
            return
        }

        // 只注册一次 binder 监听器，重复 init 仅刷新状态
        if (!initialized) {
            initialized = true
            Shizuku.addBinderReceivedListener(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
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

    fun removePermissionListener(listener: Shizuku.OnRequestPermissionResultListener) {
        Shizuku.removeRequestPermissionResultListener(listener)
        if (permissionListener == listener) {
            permissionListener = null
        }
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
            CrashLogManager.logException("ShizukuManager", "绑定 UserService 失败", e)
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
                CrashLogManager.logException("ShizukuManager", "解绑 UserService 失败", e)
            }
            commandService = null
        }
    }

    // 通过 UserService 执行 shell 命令
    fun executeCommand(command: String): Result<String> {
        val service = commandService
        // commandService 非空即表示 binder 连接有效（断开时会被置空），无需额外存活检查
        return if (service != null) {
            try {
                val result = service.executeCommand(command)
                Result.success(result)
            } catch (e: Exception) {
                CrashLogManager.logException("ShizukuManager", "执行命令失败", e)
                Result.failure(e)
            }
        } else {
            // 服务未连接，尝试绑定
            bindUserService()
            Result.failure(IllegalStateException("CommandService not connected"))
        }
    }

    // 结束指定应用（等效系统最近任务滑掉，需 shell 权限）：
    // am stop-app 即系统 UI 滑掉任务时调用的接口（REASON_USER_REQUESTED），
    // 会终止该应用进程与前台服务；无 dumpsys 输出格式依赖，跨版本稳定
    fun removePackageTasks(packageName: String): Boolean {
        return executeCommand("am stop-app $packageName").isSuccess
    }

    private fun isShizukuInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(ShizukuProvider.MANAGER_APPLICATION_ID, 0)
            true
        } catch (e: Exception) {
            CrashLogManager.logException("ShizukuManager", "检查 Shizuku 安装状态失败", e)
            false
        }
    }
}
