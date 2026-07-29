package com.edgegesture.evilgodxu.screens.gesture.service.musicpanel

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
        scope.coroutineContext.cancelChildren()
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

        return context.getString(R.string.usb_audio_default_name)
    }

    companion object {
        // USB 音频输出设备类型（AudioDeviceInfo.TYPE_* 常量，API 33+）
        private val usbAudioDeviceTypes = setOf(
            AudioDeviceInfo.TYPE_USB_DEVICE,
            AudioDeviceInfo.TYPE_USB_ACCESSORY,
            AudioDeviceInfo.TYPE_USB_HEADSET,
            22, // AudioDeviceInfo.TYPE_USB_DAC
        )

        fun isUsbAudioDeviceType(type: Int): Boolean = type in usbAudioDeviceTypes

        fun directPlaybackSupport(
            context: Context,
            sampleRate: Int = 48000,
            channels: Int = 2,
            encoding: Int = AudioFormat.ENCODING_PCM_16BIT,
        ): Int {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
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
                    return setPreferredStrategy(context, null)
                }
                if (usbDevice == null || directPlaybackSupport(
                        context,
                        currentSampleRate,
                        currentChannels,
                        currentEncoding,
                    ) == AudioManager.DIRECT_PLAYBACK_NOT_SUPPORTED
                ) return false
                audioSinkDeviceSetter?.invoke(usbDevice)
                if (setPreferredStrategy(context, usbDevice)) return true
                audioSinkDeviceSetter?.invoke(null)
                return false
            }
        }

        fun setPreferredUsbDevice(context: Context, enable: Boolean): Boolean {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val usbDevice = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                .firstOrNull { isUsbAudioDeviceType(it.type) }
            return if (enable) {
                if (usbDevice == null) false else setPreferredStrategy(context, usbDevice)
            } else {
                setPreferredStrategy(context, null)
            }
        }

        private fun setPreferredStrategy(context: Context, usbDevice: AudioDeviceInfo?): Boolean {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            return try {
                val strategyClass = Class.forName("android.media.audiopolicy.AudioProductStrategy")
                val strategies = strategyClass.getMethod("getAudioProductStrategies")
                    .invoke(null) as? List<*> ?: return false
                val strategy = strategies.firstOrNull { candidate ->
                    val attributes = candidate?.let {
                        strategyClass.getMethod("getAudioAttributes").invoke(it)
                    } as? AudioAttributes
                    attributes?.usage == AudioAttributes.USAGE_MEDIA
                } ?: return false
                if (usbDevice != null) {
                    val deviceAttributes = Class.forName("android.media.AudioDeviceAttributes")
                        .getConstructor(AudioDeviceInfo::class.java)
                        .newInstance(usbDevice)
                    val setMethod = AudioManager::class.java.methods.firstOrNull {
                        it.name == "setPreferredDeviceForStrategy" && it.parameterTypes.size == 2
                    } ?: return false
                    setMethod.invoke(audioManager, strategy, deviceAttributes) as? Boolean ?: false
                } else {
                    val removeMethod = AudioManager::class.java.methods.firstOrNull {
                        it.name == "removePreferredDeviceForStrategy" && it.parameterTypes.size == 1
                    } ?: return false
                    removeMethod.invoke(audioManager, strategy) as? Boolean ?: false
                }
            } catch (_: ReflectiveOperationException) {
                false
            }
        }
    }
}
