package com.edgegesture.evilgodxu.screens.gesture.service.musicpanel

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// 蓝牙耳机监听器，检测蓝牙耳机连接并自动降低媒体音量
class BluetoothHeadsetMonitor(
    private val context: Context,
    private val onHeadsetConnected: (deviceName: String) -> Unit,
    private val onHeadsetDisconnected: () -> Unit,
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var registered = false

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
            }
        }
    }

    private fun handleConnected(device: AudioDeviceInfo) {
        val deviceName = device.productName?.toString()
            ?.takeIf { it.isNotBlank() }
            ?: "蓝牙耳机"
        onHeadsetConnected(deviceName)
    }

    private fun handleDisconnected() {
        onHeadsetDisconnected()
    }

    companion object {
        private val btA2dpTypes by lazy {
            // AudioDeviceInfo.TYPE_BLUETOOTH_A2DP = 8, TYPE_BLUETOOTH_SCO = 7
            if (Build.VERSION.SDK_INT >= 31) {
                setOf(
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                    AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
                )
            } else {
                setOf(7, 8)
            }
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
