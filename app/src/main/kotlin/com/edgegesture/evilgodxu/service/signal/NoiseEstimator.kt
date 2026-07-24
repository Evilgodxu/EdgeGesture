package com.edgegesture.evilgodxu.service.signal

/**
 * 指数移动平均（EMA）噪声基底估计器。
 *
 * 仅在系统处于空闲状态（无敲击脉冲）时更新估计值，避免将有效敲击能量纳入噪声基底。
 * 用于自适应阈值计算：threshold = baseThreshold + ADAPTIVE_FACTOR * noiseFloor
 *
 * @param alpha EMA 平滑系数，取值范围 (0, 1)，值越小收敛越慢但越平滑
 */
class NoiseEstimator(private val alpha: Float) {
    private var estimate = 0f
    private var initialized = false

    val currentEstimate get() = estimate

    fun init(value: Float) {
        estimate = value
        initialized = true
    }

    fun reset() {
        initialized = false
        estimate = 0f
    }

    fun update(value: Float, adopt: Boolean): Float {
        if (adopt) {
            estimate = if (initialized) {
                estimate * (1f - alpha) + value * alpha
            } else {
                value.also { initialized = true }
            }
        }
        return estimate
    }
}
