package com.edgegesture.evilgodxu.service.signal

import kotlin.math.PI

/**
 * 一阶 IIR 低通滤波器。
 *
 * 用于平滑加速度计信号，提取能量包络或低频运动特征。
 * 截止频率约为 cutofHz Hz，时间常数 τ = 1 / (2π · fc)。
 *
 * @param cutoffHz 截止频率
 */
class LowPassFilter(private val cutoffHz: Float) {
    private var lastOutput = 0f
    private var lastTime = 0L
    private var initialized = false

    val isInitialized get() = initialized

    fun init(value: Float, t: Long) {
        lastOutput = value
        lastTime = t
        initialized = true
    }

    fun reset() {
        initialized = false
        lastTime = 0L
        lastOutput = 0f
    }

    fun update(value: Float, t: Long): Float {
        val dt = ((t - lastTime) / 1_000_000_000f).coerceIn(0.0005f, 0.1f)
        lastTime = t
        val rc = 1f / (2f * PI.toFloat() * cutoffHz)
        val alpha = dt / (rc + dt)
        lastOutput = alpha * value + (1f - alpha) * lastOutput
        return lastOutput
    }
}
