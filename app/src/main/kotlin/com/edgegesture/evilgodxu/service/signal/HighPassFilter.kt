package com.edgegesture.evilgodxu.service.signal

import kotlin.math.PI

/**
 * 一阶 IIR 高通滤波器。
 *
 * 用于从加速度计信号中滤除重力分量（直流偏置），保留冲击/敲击产生的高频分量。
 * 截止频率约为 cutofHz Hz，时间常数 τ = 1 / (2π · fc)。
 *
 * @param cutoffHz 截止频率，推荐 5 Hz（可有效滤除 ~1.6 Hz 的手持晃动同时保留敲击瞬态）
 */
class HighPassFilter(private val cutoffHz: Float) {
    private var lastInput = 0f
    private var lastOutput = 0f
    private var lastTime = 0L
    private var initialized = false

    val isInitialized get() = initialized

    fun init(value: Float, t: Long) {
        lastInput = value
        lastOutput = 0f
        lastTime = t
        initialized = true
    }

    fun reset() {
        initialized = false
        lastTime = 0L
        lastInput = 0f
        lastOutput = 0f
    }

    fun update(value: Float, t: Long): Float {
        val dt = ((t - lastTime) / 1_000_000_000f).coerceIn(0.0005f, 0.1f)
        lastTime = t
        val rc = 1f / (2f * PI.toFloat() * cutoffHz)
        val alpha = rc / (rc + dt)
        lastOutput = alpha * (lastOutput + value - lastInput)
        lastInput = value
        return lastOutput
    }
}
