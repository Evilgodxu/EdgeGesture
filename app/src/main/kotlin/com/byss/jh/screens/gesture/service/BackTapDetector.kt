package com.byss.jh.screens.gesture.service

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import java.util.ArrayDeque

// 背面双击检测器，基于 TapTap Columbus 启发式算法移植
// 通过加速度计 Z 轴信号的峰值检测识别背面敲击，再通过时间间隔判定双击
class BackTapDetector(
    context: Context,
    private val onDoubleTap: () -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val handler = Handler(Looper.getMainLooper())

    // 信号处理组件
    private val resample = Resample()
    private val slope = Slope()
    private val lowpassKey = Lowpass1C()
    private val highpassKey = Highpass1C()
    private val peakPositive = PeakDetector()
    private val peakNegative = PeakDetector()

    // Z 轴信号缓冲区
    private val zBuffer = ArrayDeque<Float>()
    private var syncTime = 0L
    private var result = 0
    private val tapTimestamps = ArrayDeque<Long>()

    // 双击时间窗口参数（纳秒）
    private var minTimeGapNs = 150_000_000L  // 150ms
    private var maxTimeGapNs = 400_000_000L  // 400ms

    // 采样间隔（纳秒），约 2.5ms → 400Hz
    private val samplingIntervalNs = 2500_000L

    private var isListening = false

    // 设置灵敏度：1-10，越高越灵敏（噪声容忍阈值越低）
    fun setSensitivity(value: Int) {
        val noiseTolerate = 0.11f - value * 0.01f  // 0.01 ~ 0.10
        peakPositive.setMinNoiseTolerate(noiseTolerate)
        peakNegative.setMinNoiseTolerate(noiseTolerate)
    }

    // 设置检测范围：1-10，越高检测窗口越大（峰值检测窗口越大，越容易匹配）
    fun setRange(value: Int) {
        val windowSize = value * 4 + 20  // 24 ~ 60
        peakPositive.setWindowSize(windowSize)
        peakNegative.setWindowSize(windowSize)
        // 同时调整双击时间窗口，范围越大允许的时间间隔越宽
        maxTimeGapNs = (300_000_000L + value * 30_000_000L)  // 330ms ~ 600ms
    }

    fun start() {
        start(5, 5)
    }

    fun start(sensitivity: Int, range: Int) {
        if (isListening || accelerometer == null) return
        // 初始化滤波器参数（与 TapTap 启发式模式一致）
        lowpassKey.setPara(0.2f)
        highpassKey.setPara(0.2f)
        setSensitivity(sensitivity)
        setRange(range)
        reset()
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST)
        isListening = true
    }

    fun stop() {
        if (!isListening) return
        sensorManager.unregisterListener(this)
        isListening = false
        reset()
    }

    private fun reset() {
        zBuffer.clear()
        syncTime = 0L
        result = 0
        tapTimestamps.clear()
        resample.reset()
        slope.reset()
        lowpassKey.init(0f)
        highpassKey.init(0f)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val t = event.timestamp

        if (syncTime == 0L) {
            syncTime = t
            resample.init(x, y, z, t, samplingIntervalNs)
            slope.init(resample.point)
            lowpassKey.init(0f)
            highpassKey.init(0f)
            return
        }

        while (resample.update(x, y, z, t)) {
            processSignal(t)
        }

        val timing = checkDoubleTapTiming(t)
        if (timing == 2) {
            handler.post { onDoubleTap() }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun processSignal(timestamp: Long) {
        val point = resample.point
        val scaledInterval = 2500_000f / resample.interval
        val slopeZ = slope.updateZ(point, scaledInterval)
        val lowpassZ = lowpassKey.update(slopeZ)
        val highpassZ = highpassKey.update(lowpassZ)
        peakPositive.update(highpassZ)
        peakNegative.update(-highpassZ)
        zBuffer.add(highpassZ)

        val windowSize = (160_000_000L / resample.interval).toInt()
        while (zBuffer.size > windowSize) {
            zBuffer.removeFirst()
        }

        if (zBuffer.size == windowSize) {
            recognizeTap()
        }

        if (result == 1) {  // 1 = Back tap
            tapTimestamps.addLast(timestamp)
        }
    }

    // 启发式背面敲击识别：正峰在位置 4，负峰在其后 1~2 个位置
    private fun recognizeTap() {
        val posId = peakPositive.idMajorPeak
        val negId = peakNegative.idMajorPeak - posId
        result = if (posId == 4 && negId in 1..2) 1 else 0
    }

    // 双击判定：两次敲击间隔在 [minGap, maxGap] 之间
    private fun checkDoubleTapTiming(timestamp: Long): Int {
        // 清除过期时间戳
        val iter = tapTimestamps.iterator()
        while (iter.hasNext()) {
            if (timestamp - iter.next() > maxTimeGapNs) iter.remove() else break
        }
        if (tapTimestamps.isEmpty()) return 0

        // 检查是否有两次敲击满足间隔要求
        val last = tapTimestamps.last
        val checkIter = tapTimestamps.iterator()
        while (checkIter.hasNext()) {
            val t = checkIter.next()
            if (last - t > minTimeGapNs) {
                tapTimestamps.clear()
                return 2  // 双击
            }
        }
        return 1  // 单击
    }

    // --- 信号处理内部类 ---

    // 一维重采样器
    private class Resample {
        var interval = 0L; private set
        private var tInterval = 0L
        private var xRaw = 0f; private var yRaw = 0f; private var zRaw = 0f
        private var tRaw = 0L
        private var xOut = 0f; private var yOut = 0f; private var zOut = 0f
        private var tOut = 0L

        val point get() = floatArrayOf(xOut, yOut, zOut)

        fun init(x: Float, y: Float, z: Float, t: Long, interval: Long) {
            xRaw = x; yRaw = y; zRaw = z; tRaw = t
            xOut = x; yOut = y; zOut = z; tOut = t
            tInterval = interval
            this.interval = interval
        }

        fun update(x: Float, y: Float, z: Float, t: Long): Boolean {
            if (t.compareTo(tRaw) == 0) return false
            val target = (if (tInterval > 0) tInterval else t - tRaw) + tOut
            if (t < target) { tRaw = t; xRaw = x; yRaw = y; zRaw = z; return false }
            val s = (target - tRaw).toFloat() / (t - tRaw).toFloat()
            xOut = xRaw + (x - xRaw) * s
            yOut = yRaw + (y - yRaw) * s
            zOut = zRaw + (z - zRaw) * s
            tOut = target
            if (tRaw < target) { tRaw = t; xRaw = x; yRaw = y; zRaw = z }
            return true
        }

        fun reset() { tInterval = 0; tRaw = 0; tOut = 0 }
    }

    // 斜率计算器（只计算 Z 轴）
    private class Slope {
        private var last = 0f
        fun init(point: FloatArray) { last = point[2] }
        fun updateZ(point: FloatArray, scale: Float): Float {
            val v = point[2] * scale
            val delta = v - last
            last = v
            return delta
        }
        fun reset() { last = 0f }
    }

    // 低通滤波器
    private class Lowpass1C {
        private var para = 1f
        private var last = 0f

        fun setPara(p: Float) { para = p }
        fun init(v: Float) { last = v }

        fun update(value: Float): Float {
            last = value * para + (1f - para) * last
            return last
        }
    }

    // 高通滤波器
    private class Highpass1C {
        private var para = 1f
        private var lastIn = 0f
        private var lastOut = 0f

        fun setPara(p: Float) { para = p }
        fun init(v: Float) { lastIn = v; lastOut = v }

        fun update(value: Float): Float {
            lastOut = (value - lastIn) * para + lastOut * para
            lastIn = value
            return lastOut
        }
    }

    // 峰值检测器
    private class PeakDetector {
        private var amplitude = 0f
        private var reference = 0f
        private var idPeak = -1
        private var minNoise = 0f
        private var window = 0

        fun setMinNoiseTolerate(v: Float) { minNoise = v }
        fun setWindowSize(s: Int) { window = s }
        val idMajorPeak get() = idPeak

        fun update(value: Float) {
            idPeak--
            if (idPeak < 0) amplitude = 0f
            var noiseTol = minNoise
            val ref = amplitude / 5f
            if (ref > minNoise) noiseTol = ref
            val diff = reference - value
            if (diff >= noiseTol) {
                reference = value
            } else if (diff < 0f && value > noiseTol) {
                reference = value
                if (value > amplitude) {
                    idPeak = window - 1
                    amplitude = value
                }
            }
        }
    }
}
