package com.edgegesture.evilgodxu.ui.adaptive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.window.core.layout.WindowSizeClass

// CompositionLocal 用于在 Compose 树中传递窗口尺寸类
val LocalWindowSizeClass = compositionLocalOf<WindowSizeClass> {
    error("WindowSizeClass not provided")
}

// 提供窗口尺寸类给子组件，用于响应式布局适配
// 直接依据 Configuration 的窗口尺寸同步计算，免除 WindowInfoTracker 订阅。
// 该订阅在安卓 16 上要求上下文必须是带 display 关联的 UI Context（Activity/WindowContext 等），
// 非 UI 上下文场景会抛 IllegalArgumentException 导致闪退，故此处不再使用 currentWindowAdaptiveInfo。
@Composable
fun ProvideWindowSizeClass(content: @Composable () -> Unit) {
    // 读取 Configuration 以在配置（含窗口尺寸/旋转）变化时自动重组并重算尺寸类
    val configuration = LocalConfiguration.current
    val windowSizeClass = WindowSizeClass.compute(
        configuration.screenWidthDp.toFloat(),
        configuration.screenHeightDp.toFloat(),
    )
    CompositionLocalProvider(LocalWindowSizeClass provides windowSizeClass) {
        content()
    }
}

// 获取当前窗口尺寸类，用于判断设备类型（手机/平板/折叠屏）和屏幕方向。
// 仅读取 CompositionLocal，无记忆/缓存效果，故不命名为 remember 前缀
@Composable
fun currentWindowSizeClass(): WindowSizeClass {
    return LocalWindowSizeClass.current
}
