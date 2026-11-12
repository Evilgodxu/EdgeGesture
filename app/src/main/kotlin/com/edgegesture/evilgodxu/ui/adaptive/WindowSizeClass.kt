package com.edgegesture.evilgodxu.ui.adaptive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.computeWindowSizeClass

// CompositionLocal 用于在 Compose 树中传递窗口尺寸类
val LocalWindowSizeClass = compositionLocalOf<WindowSizeClass> {
    error("WindowSizeClass not provided")
}

// 提供窗口尺寸类给子组件，用于响应式布局适配。
// 取窗口容器尺寸而非 Configuration.screenWidthDp：后者与 inset 行为耦合且被取整，
// 不能准确反映实际可用窗口宽度；也不使用 currentWindowAdaptiveInfo，
// 该 API 在安卓 16 上要求带 display 关联的 UI Context，非 UI 上下文会抛异常导致闪退。
@Composable
fun ProvideWindowSizeClass(content: @Composable () -> Unit) {
    // 读取窗口容器尺寸，尺寸（含旋转、分屏、折叠）变化时自动重组并重算
    val density = LocalDensity.current
    val containerSize = LocalWindowInfo.current.containerSize
    val windowSizeClass = WindowSizeClass.BREAKPOINTS_V1.computeWindowSizeClass(
        widthDp = with(density) { containerSize.width.toDp().value },
        heightDp = with(density) { containerSize.height.toDp().value },
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