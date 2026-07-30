package com.edgegesture.evilgodxu.screens.gesture.service.musicpanel

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.os.Build
import com.edgegesture.evilgodxu.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// USB 音频设备监听器，当检测到外接 USB DAC/声卡时自动启用音频路由独占
class UsbAudioMonitor(
    private val context: Context,
    private val onUsbDeviceAttached: (deviceName: String) -> Unit,
    private val onUsbDeviceDetached: () -> Unit,
    private val onError: ((message: String) -> Unit)? = null,
    private val onBeforeRequestPermission: (() -> Unit)? = null,
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var receiverRegistered = false

    // 待请求权限的 USB 设备（系统广播携带的 device 可直接用于 requestPermission，无需先检查 interface）
    private var pendingPermissionDevice: UsbDevice? = null

    // USB 权限结果广播接收器
    private val usbPermissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != ACTION_USB_PERMISSION) return
            val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
            val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
            }
            if (granted && device != null && isAudioDevice(device)) {
                handleDeviceAttached()
            } else if (!granted) {
                onError?.invoke("USB 权限被拒绝: ${device?.deviceName ?: "未知设备"}")
            }
            pendingPermissionDevice = null
        }
    }

    // 设备和音频路由变化的广播接收器
    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    val device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                    if (device != null) {
                        // 先请求 USB 权限，权限授予后再检查是否为音频设备
                        requestUsbPermission(device)
                    }
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    val device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                    if (device != null && hasUsbPermission(device) && isAudioDevice(device)) {
                        handleDeviceDetached()
                    } else {
                        // 即使没有权限，只要设备拔出也触发断开事件
                        handleDeviceDetached()
                    }
                }
                "android.media.action.USB_AUDIO_ACCESSORY_PLUG",
                "android.media.action.USB_AUDIO_DEVICE_PLUG" -> {
                    when (intent.getIntExtra("state", 0)) {
                        1 -> {
                            // USB_AUDIO_DEVICE_PLUG 是音频路由切换广播，
                            // 如果此时正在等待 USB 权限弹窗结果，则跳过避免重复触发，
                            // 授权结果接收器会在权限授予后自行调用 handleDeviceAttached
                            if (pendingPermissionDevice == null) {
                                handleDeviceAttached()
                            }
                        }
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(usbReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            context.registerReceiver(usbReceiver, filter)
        }
        // 注册 USB 权限结果接收器
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(usbPermissionReceiver, IntentFilter(ACTION_USB_PERMISSION), Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            context.registerReceiver(usbPermissionReceiver, IntentFilter(ACTION_USB_PERMISSION))
        }
        receiverRegistered = true
        // 检查当前是否已有 USB 音频设备连接
        checkExistingUsbAudioDevice()
    }

    fun unregister() {
        scope.coroutineContext.cancelChildren()
        if (receiverRegistered) {
            try {
                context.unregisterReceiver(usbReceiver)
                context.unregisterReceiver(usbPermissionReceiver)
            } catch (_: IllegalArgumentException) {
            }
            receiverRegistered = false
        }
    }

    // 请求 USB 设备权限
    private fun requestUsbPermission(device: UsbDevice) {
        if (usbManager.hasPermission(device)) {
            // 已有权限，直接检查是否为音频设备
            if (isAudioDevice(device)) {
                handleDeviceAttached()
            }
            return
        }
        // 先通知关闭面板（面板悬浮窗优先级高于系统弹窗）
        onBeforeRequestPermission?.invoke()
        pendingPermissionDevice = device
        val permissionIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(ACTION_USB_PERMISSION).setPackage(context.packageName),
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        usbManager.requestPermission(device, permissionIntent)
    }

    // 检查是否已有 USB 权限
    private fun hasUsbPermission(device: UsbDevice): Boolean {
        return try {
            usbManager.hasPermission(device)
        } catch (_: Exception) {
            false
        }
    }

    // 安全地检查设备是否为音频设备（捕获 SecurityException）
    private fun isAudioDevice(device: UsbDevice): Boolean {
        return try {
            for (i in 0 until device.interfaceCount) {
                val usbInterface = device.getInterface(i)
                if (usbInterface.interfaceClass == UsbConstants.USB_CLASS_AUDIO) {
                    return true
                }
            }
            false
        } catch (e: SecurityException) {
            onError?.invoke("USB 权限不足: ${e.message}")
            false
        } catch (e: Exception) {
            onError?.invoke("USB 设备检测异常: ${e.message}")
            false
        }
    }

    private fun checkExistingUsbAudioDevice() {
        scope.launch {
            // 通过 AudioManager 检查已连接的音频设备（不需要 USB 权限）
            withContext(Dispatchers.Main) {
                try {
                    val audioDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                    if (audioDevices.any { isUsbAudioDeviceType(it.type) }) {
                        handleDeviceAttached()
                        return@withContext
                    }
                } catch (_: Exception) {
                }
            }
            // 通过 UsbManager 检查（需要 USB 权限，用 try-catch 保护）
            try {
                val usbDevices = usbManager.deviceList
                for (device in usbDevices.values) {
                    if (hasUsbPermission(device) && isAudioDevice(device)) {
                        handleDeviceAttached()
                        return@launch
                    }
                }
            } catch (_: Exception) {
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
        // 优先从 AudioManager 获取设备名称（更准确，不需要 USB 权限）
        val audioDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        val usbAudioDevice = audioDevices.firstOrNull { isUsbAudioDeviceType(it.type) }
        if (usbAudioDevice != null) {
            val productName = usbAudioDevice.productName?.toString()
            if (!productName.isNullOrBlank()) return productName

            // 部分设备 address 包含厂商信息
            val addr = usbAudioDevice.address
            if (addr.isNotBlank() && addr != "0") return addr
        }

        // 回退到 UsbManager 获取产品名（用 try-catch 保护）
        try {
            for (device in usbManager.deviceList.values) {
                if (hasUsbPermission(device) && isAudioDevice(device)) {
                    device.productName?.takeIf { it.isNotBlank() }?.let { return it }
                    device.manufacturerName?.takeIf { it.isNotBlank() }?.let { return it }
                }
            }
        } catch (_: Exception) {
        }

        return context.getString(R.string.usb_audio_default_name)
    }

    companion object {
        private const val ACTION_USB_PERMISSION = "com.edgegesture.evilgodxu.USB_PERMISSION"

        // USB 音频输出设备类型（AudioDeviceInfo.TYPE_* 常量，API 33+）
        private val usbAudioDeviceTypes = setOf(
            AudioDeviceInfo.TYPE_USB_DEVICE,
            AudioDeviceInfo.TYPE_USB_ACCESSORY,
            AudioDeviceInfo.TYPE_USB_HEADSET,
            22, // AudioDeviceInfo.TYPE_USB_DAC (API 33+)
        )

        fun isUsbAudioDeviceType(type: Int): Boolean = type in usbAudioDeviceTypes

        fun directPlaybackSupport(
            context: Context,
            sampleRate: Int = 48000,
            channels: Int = 2,
            encoding: Int = AudioFormat.ENCODING_PCM_16BIT,
        ): Int {
            val format = AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setEncoding(encoding)
                .setChannelMask(
                    if (channels == 1) AudioFormat.CHANNEL_OUT_MONO
                    else AudioFormat.CHANNEL_OUT_STEREO
                )
                .build()
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                return AudioManager.getDirectPlaybackSupport(format, attributes)
            }
            return try {
                AudioManager::class.java
                    .getMethod("getDirectPlaybackSupport", AudioFormat::class.java, AudioAttributes::class.java)
                    .invoke(null, format, attributes) as Int
            } catch (_: ReflectiveOperationException) {
                AudioManager.DIRECT_PLAYBACK_NOT_SUPPORTED
            }
        }

        private val routeLock = Any()
        private var currentSampleRate = 48000
        private var currentChannels = 2
        private var currentEncoding = AudioFormat.ENCODING_PCM_16BIT
        var audioSinkDeviceSetter: ((AudioDeviceInfo?) -> Unit)? = null

        fun updatePlaybackFormat(sampleRate: Int, channels: Int, encoding: Int) {
            synchronized(routeLock) {
                currentSampleRate = sampleRate
                currentChannels = channels
                currentEncoding = encoding
            }
        }

        @JvmStatic
        fun setUsbExclusive(context: Context, enable: Boolean): Boolean {
            synchronized(routeLock) {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                val usbDevice = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                    .firstOrNull { isUsbAudioDeviceType(it.type) }
                if (!enable) {
                    audioSinkDeviceSetter?.invoke(null)
                    return true
                }
                if (usbDevice == null || directPlaybackSupport(
                        context,
                        currentSampleRate,
                        currentChannels,
                        currentEncoding,
                    ) == AudioManager.DIRECT_PLAYBACK_NOT_SUPPORTED
                ) return false
                audioSinkDeviceSetter?.invoke(usbDevice)
                return true
            }
        }
    }
}
