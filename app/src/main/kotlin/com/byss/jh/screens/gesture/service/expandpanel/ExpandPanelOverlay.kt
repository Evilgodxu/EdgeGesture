package com.byss.jh.screens.gesture.service.expandpanel

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.byss.jh.data.gesture.ExpandPanelShortcutsState
import com.byss.jh.screens.settings.ThemeMode
import com.byss.jh.ui.theme.DarkColorScheme
import com.byss.jh.ui.theme.LightColorScheme
import kotlinx.coroutines.flow.Flow

@Composable
fun ExpandPanelOverlay(
    shortcutsFlow: Flow<ExpandPanelShortcutsState>,
    themeModeFlow: Flow<ThemeMode>,
    onShortcutSet: (index: Int, packageName: String?) -> Unit,
    onDismiss: () -> Unit,
    onDismissAnimationEnd: () -> Unit = onDismiss
) {
    var currentShortcuts by remember { mutableStateOf(ExpandPanelShortcutsState()) }
    var themeMode by remember { mutableStateOf(ThemeMode.SYSTEM) }
    var isVisible by remember { mutableStateOf(false) }
    var isDismissing by remember { mutableStateOf(false) }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isSystemInDarkTheme = (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    // 触发进入动画
    LaunchedEffect(Unit) {
        isVisible = true
    }

    // 处理关闭动画完成后的回调
    LaunchedEffect(isDismissing) {
        if (isDismissing) {
            kotlinx.coroutines.delay(200) // 等待退出动画完成
            onDismissAnimationEnd()
        }
    }

    LaunchedEffect(shortcutsFlow) {
        shortcutsFlow.collect { state ->
            currentShortcuts = state
        }
    }

    LaunchedEffect(themeModeFlow) {
        themeModeFlow.collect { mode ->
            themeMode = mode
        }
    }

    val isDarkTheme = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme
    }

    val colorScheme = if (isDarkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(colorScheme = colorScheme) {
        val dismissWithAnimation = {
            isDismissing = true
            isVisible = false
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { dismissWithAnimation() }
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(
                    initialOffsetY = { fullHeight -> fullHeight },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ),
                exit = slideOutVertically(
                    targetOffsetY = { fullHeight -> fullHeight },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            ) {
                Surface(
                    modifier = if (isLandscape) {
                        Modifier
                            .widthIn(max = 480.dp)
                            .fillMaxWidth(0.7f)
                            .padding(horizontal = 12.dp, vertical = 16.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { /* 阻止点击穿透 */ }
                            )
                    } else {
                        Modifier
                            .fillMaxWidth(0.92f)
                            .padding(horizontal = 12.dp, vertical = 16.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { /* 阻止点击穿透 */ }
                            )
                    },
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    shadowElevation = 8.dp
                ) {
                    ExpandPanelContent(
                        shortcuts = currentShortcuts.shortcuts,
                        onShortcutSet = onShortcutSet,
                        onDismiss = { dismissWithAnimation() }
                    )
                }
            }
        }
    }
}
