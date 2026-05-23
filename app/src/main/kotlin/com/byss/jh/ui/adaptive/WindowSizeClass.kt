package com.byss.jh.ui.adaptive

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.window.core.layout.WindowSizeClass

val LocalWindowSizeClass = compositionLocalOf<WindowSizeClass> {
    error("WindowSizeClass not provided")
}

@Composable
fun ProvideWindowSizeClass(content: @Composable () -> Unit) {
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    CompositionLocalProvider(LocalWindowSizeClass provides windowSizeClass) {
        content()
    }
}

@Composable
fun rememberWindowSizeClass(): WindowSizeClass {
    return LocalWindowSizeClass.current
}
