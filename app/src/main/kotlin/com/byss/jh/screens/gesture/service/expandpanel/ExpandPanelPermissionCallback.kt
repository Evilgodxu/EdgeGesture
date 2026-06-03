package com.byss.jh.screens.gesture.service.expandpanel

// 扩展面板权限请求回调
interface ExpandPanelPermissionCallback {
    // 当需要修改系统设置权限时调用，返回是否已处理
    fun onRequestWriteSettings(): Boolean
}
