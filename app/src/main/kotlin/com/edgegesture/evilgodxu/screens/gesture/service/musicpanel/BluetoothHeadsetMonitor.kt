package com.edgegesture.evilgodxu.screens.gesture.service.musicpanel

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import com.edgegesture.evilgodxu.R
import com.edgegesture.evilgodxu.log.CrashLogManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// 蓝牙耳机监听器，检测蓝牙耳机连接并自动降低媒体音量
class BluetoothHeadsetMonitor(
    private val context: Context,
    private val onHeadsetConnected: (deviceName: String, isNewConnection: Boolean) -> Unit,
    private val onHeadsetDisconnected: () -> Unit,
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var registered = false
    private var isHeadsetConnected = false

    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<AudioDeviceInfo>) {
            val btDevice = addedDevices.firstOrNull { isBluetoothA2dp(it) }
            if (btDevice != null) {
                handleConnected(btDevice)
            }
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<AudioDeviceInfo>) {
            if (removedDevices.any { isBluetoothA2dp(it) }) {
                // 确认是否还有其他蓝牙设备连接
                val remaining = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                if (remaining.none { isBluetoothA2dp(it) }) {
                    handleDisconnected()
                }
            }
        }
    }

    fun register() {
        if (registered) return
        audioManager.registerAudioDeviceCallback(audioDeviceCallback, Handler(Looper.getMainLooper()))
        registered = true
        // 异步兜底检查：注册回调后可能遗漏已在连接中的设备
        checkExisting()
    }

    /**
     * 同步检查并更新当前已连接的蓝牙设备状态。
     * 应在 UI 渲染前调用，确保首次显示时状态正确。
     */
    fun checkExistingSync() {
        val btDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            .filter { isBluetoothA2dp(it) }
        btDevices.firstOrNull()?.let { device ->
            handleConnected(device)
        } ?: run {
            handleDisconnected()
        }
    }

    fun unregister() {
        if (registered) {
            audioManager.unregisterAudioDeviceCallback(audioDeviceCallback)
            registered = false
        }
    }

    private fun checkExisting() {
        scope.launch {
            val btDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                .filter { isBluetoothA2dp(it) }
            btDevices.firstOrNull()?.let { device ->
                handleConnected(device)
            } ?: run {
                handleDisconnected()
            }
        }
    }

    private fun handleConnected(device: AudioDeviceInfo) {
        val deviceName = resolveBluetoothDeviceName(device)
            ?: context.getString(R.string.bluetooth_headset_default_name)
        if (isHeadsetConnected) {
            onHeadsetConnected(deviceName, false)
            return
        }
        isHeadsetConnected = true
        onHeadsetConnected(deviceName, true)
    }

    /**
     * 通过 Bluetooth API 获取远程蓝牙设备的真实名称。
     *
     * AudioDeviceInfo.productName 在部分设备上返回的是本机蓝牙名称而非远程设备名称，
     * 因此优先使用 BluetoothDevice.getName() 获取蓝牙耳机/音箱的实际名称。
     */
    private fun resolveBluetoothDeviceName(audioDevice: AudioDeviceInfo): String? {
        // 在调用蓝牙 API 前先检查 BLUETOOTH_CONNECT 运行时权限，
        // 避免触发系统权限弹窗（Android 12+ 为危险权限）
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return audioDevice.productName?.toString()?.takeIf { it.isNotBlank() }
        }
        // 优先使用 BluetoothDevice.getName() 获取远程蓝牙设备名称
        try {
            val bluetoothAdapter = context.getSystemService(BluetoothManager::class.java).adapter
            if (bluetoothAdapter != null) {
                val address = audioDevice.address
                if (address.isNotBlank()) {
                    val btDevice = bluetoothAdapter.getRemoteDevice(address)
                    // getName() 返回远程设备在其广播或配对过程中声明的名称
                    btDevice.name?.takeIf { it.isNotBlank() }?.let { return it }
                }
            }
        } catch (e: SecurityException) {
            CrashLogManager.logException("BluetoothHeadsetMonitor", "获取蓝牙设备名称失败（蓝牙权限不足）", e)
            // BLUETOOTH_CONNECT 权限不足，回退到 productName
        } catch (e: IllegalArgumentException) {
            CrashLogManager.logException("BluetoothHeadsetMonitor", "获取蓝牙设备名称失败（设备地址无效）", e)
            // address 格式无效，回退到 productName
        } catch (e: Exception) {
            CrashLogManager.logException("BluetoothHeadsetMonitor", "获取蓝牙设备名称失败", e)
            // 其他异常，回退到 productName
        }
        // 回退到 AudioDeviceInfo.productName
        return audioDevice.productName?.toString()?.takeIf { it.isNotBlank() }
    }

    private fun handleDisconnected() {
        if (!isHeadsetConnected) return
        isHeadsetConnected = false
        onHeadsetDisconnected()
    }

    companion object {
        private val btA2dpTypes by lazy {
            // AudioDeviceInfo.TYPE_BLUETOOTH_A2DP = 8, TYPE_BLUETOOTH_SCO = 7
            setOf(
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
            )
        }

        fun isBluetoothA2dp(device: AudioDeviceInfo): Boolean = device.type in btA2dpTypes

        /** 降低媒体音量到指定百分比（0f ~ 1f） */
        fun reduceMediaVolume(context: Context, percentage: Float) {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val targetVolume = (maxVolume * percentage.coerceIn(0f, 1f)).toInt()
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, 0)
        }
    }
}
