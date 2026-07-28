package com.edgegesture.evilgodxu.screens.gesture.service.musicpanel

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// USB 音频设备监听器，当检测到外接 USB DAC/声卡时自动启用音频路由独占
class UsbAudioMonitor(
    private val context: Context,
    private val onUsbDeviceAttached: (deviceName: String) -> Unit,
    private val onUsbDeviceDetached: () -> Unit,
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var receiverRegistered = false

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    val device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                    if (device != null && isAudioDevice(device)) {
                        handleDeviceAttached()
                    }
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    val device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                    if (device != null && isAudioDevice(device)) {
                        handleDeviceDetached()
                    }
                }
                "android.media.action.USB_AUDIO_ACCESSORY_PLUG",
                "android.media.action.USB_AUDIO_DEVICE_PLUG" -> {
                    when (intent.getIntExtra("state", 0)) {
                        1 -> handleDeviceAttached()
                        0 -> handleDeviceDetached()
                    }
                }
            }
        }
    }

    // 注册广播监听并检测当前是否已有 USB 音频设备连接
    fun register() {
        if (receiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            addAction("android.media.action.USB_AUDIO_ACCESSORY_PLUG")
            addAction("android.media.action.USB_AUDIO_DEVICE_PLUG")
        }
        context.registerReceiver(usbReceiver, filter)
        receiverRegistered = true
        // 检查当前是否已有 USB 音频设备连接
        checkExistingUsbAudioDevice()
    }

    fun unregister() {
        if (receiverRegistered) {
            try {
                context.unregisterReceiver(usbReceiver)
            } catch (_: IllegalArgumentException) {
            }
            receiverRegistered = false
        }
    }

    private fun isAudioDevice(device: UsbDevice): Boolean {
        for (i in 0 until device.interfaceCount) {
            val usbInterface = device.getInterface(i)
            if (usbInterface.interfaceClass == UsbConstants.USB_CLASS_AUDIO) {
                return true
            }
        }
        return false
    }

    private fun checkExistingUsbAudioDevice() {
        scope.launch {
            // 通过 UsbManager 检查
            val usbDevices = usbManager.deviceList
            if (usbDevices.values.any { isAudioDevice(it) }) {
                handleDeviceAttached()
                return@launch
            }
            // 通过 AudioManager 检查已连接的音频设备
            withContext(Dispatchers.Main) {
                val audioDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                if (audioDevices.any { isUsbAudioDeviceType(it.type) }) {
                    handleDeviceAttached()
                }
            }
        }
    }

    private fun handleDeviceAttached() {
        val deviceName = findUsbAudioDeviceName()
        onUsbDeviceAttached(deviceName)
    }

    private fun handleDeviceDetached() {
        onUsbDeviceDetached()
    }

    private fun findUsbAudioDeviceName(): String {
        // 优先从 AudioManager 获取设备名称（更准确）
        val audioDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        val usbAudioDevice = audioDevices.firstOrNull { isUsbAudioDeviceType(it.type) }
        if (usbAudioDevice != null) {
            val productName = usbAudioDevice.productName?.toString()
            if (!productName.isNullOrBlank()) return productName

            // 部分设备 address 包含厂商信息
            val addr = usbAudioDevice.address
            if (addr.isNotBlank() && addr != "0") return addr
        }

        // 回退到 UsbManager 获取产品名
        val devices = usbManager.deviceList.values.filter { isAudioDevice(it) }
        devices.firstOrNull()?.let { device ->
            device.productName?.takeIf { it.isNotBlank() }?.let { return it }
            device.manufacturerName?.takeIf { it.isNotBlank() }?.let { return it }
        }

        return "USB 音频设备"
    }

    companion object {
        // USB 音频输出设备类型（AudioDeviceInfo.TYPE_* 常量，API 31+）
        private val usbAudioDeviceTypes by lazy {
            val types = mutableSetOf<Int>()
            // AudioDeviceInfo.TYPE_USB_DAC = 22, TYPE_USB_DEVICE = 11,
            // TYPE_USB_ACCESSORY = 12, TYPE_USB_HEADSET = 3
            if (Build.VERSION.SDK_INT >= 31) {
                types.addAll(
                    setOf(
                        AudioDeviceInfo.TYPE_USB_DEVICE,
                        AudioDeviceInfo.TYPE_USB_ACCESSORY,
                        AudioDeviceInfo.TYPE_USB_HEADSET,
                    )
                )
                // TYPE_USB_DAC 可能在某些 SDK 版本中未定义
                types.add(22) // AudioDeviceInfo.TYPE_USB_DAC
            } else {
                types.addAll(setOf(3, 11, 12, 22))
            }
            types
        }

        fun isUsbAudioDeviceType(type: Int): Boolean = type in usbAudioDeviceTypes

        // 设置 USB 设备为首选音频输出（API 31+），使用反射兼容不同 SDK
        fun setPreferredUsbDevice(context: Context, enable: Boolean): Boolean {
            if (Build.VERSION.SDK_INT < 31) return false

            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            try {
                if (enable) {
                    val audioDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                    val usbDevice = audioDevices.firstOrNull { isUsbAudioDeviceType(it.type) }
                    if (usbDevice != null) {
                        audioManager::class.java
                            .getMethod("setPreferredDeviceForStrategy", Int::class.java, AudioDeviceInfo::class.java)
                            .invoke(audioManager, 1 /* STRATEGY_MEDIA */, usbDevice)
                        return true
                    }
                    return false
                } else {
                    audioManager::class.java
                        .getMethod("removePreferredDeviceForStrategy", Int::class.java)
                        .invoke(audioManager, 1 /* STRATEGY_MEDIA */)
                    return true
                }
            } catch (_: Exception) {
                return false
            }
        }
    }
}
