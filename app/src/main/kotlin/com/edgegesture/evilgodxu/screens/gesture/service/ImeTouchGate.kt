package com.edgegesture.evilgodxu.screens.gesture.service

// 输入法可见性与避让开关到边缘触发区触摸状态的映射。
// 只在结果变化时下发，避免每次窗口事件都调用 updateViewLayout；
// 视图重建后新窗口不带 FLAG_NOT_TOUCHABLE，需要 invalidate() 强制重新下发。
class ImeTouchGate(
    private val setTouchEnabled: (Boolean) -> Unit
) {
    var isImeVisible: Boolean = false
        private set

    // null 表示当前窗口上的触摸状态未知，下一次 update 必定下发
    private var appliedTouchEnabled: Boolean? = true

    fun update(imeVisible: Boolean, avoidOverlap: Boolean) {
        isImeVisible = imeVisible
        val touchEnabled = !(imeVisible && avoidOverlap)
        if (appliedTouchEnabled == touchEnabled) return
        appliedTouchEnabled = touchEnabled
        setTouchEnabled(touchEnabled)
    }

    fun invalidate() {
        appliedTouchEnabled = null
    }
}
