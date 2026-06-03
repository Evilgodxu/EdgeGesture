package com.byss.jh.screens.gesture.service.expandpanel

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun ExpandPanelContent(
    shortcuts: List<String?>,
    onShortcutSet: (index: Int, packageName: String?) -> Unit,
    onDismiss: () -> Unit
) {
    var isAppPickerMode by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableIntStateOf(-1) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        AnimatedContent(
            targetState = isAppPickerMode,
            transitionSpec = {
                if (targetState) {
                    // 进入应用选择器：从右向左滑入 + 淡入
                    (slideInHorizontally(
                        initialOffsetX = { fullWidth -> fullWidth },
                        animationSpec = tween(durationMillis = 300)
                    ) + fadeIn(animationSpec = tween(durationMillis = 300))).togetherWith(
                        slideOutHorizontally(
                            targetOffsetX = { fullWidth -> -fullWidth / 3 },
                            animationSpec = tween(durationMillis = 300)
                        ) + fadeOut(animationSpec = tween(durationMillis = 250))
                    )
                } else {
                    // 返回主界面：从左向右滑入 + 淡入
                    (slideInHorizontally(
                        initialOffsetX = { fullWidth -> -fullWidth / 3 },
                        animationSpec = tween(durationMillis = 300)
                    ) + fadeIn(animationSpec = tween(durationMillis = 300))).togetherWith(
                        slideOutHorizontally(
                            targetOffsetX = { fullWidth -> fullWidth },
                            animationSpec = tween(durationMillis = 300)
                        ) + fadeOut(animationSpec = tween(durationMillis = 250))
                    )
                }
            },
            label = "AppPickerTransition"
        ) { pickerMode ->
            if (pickerMode) {
                AppPickerScreen(
                    onAppSelected = { packageName ->
                        onShortcutSet(selectedIndex, packageName)
                        isAppPickerMode = false
                        selectedIndex = -1
                    },
                    onCancel = {
                        isAppPickerMode = false
                        selectedIndex = -1
                    }
                )
            } else {
                Column {
                    VerticalSlidersSection()

                    Spacer(modifier = Modifier.height(12.dp))

                    ShortcutsGrid(
                        shortcuts = shortcuts,
                        onShortcutSet = { index, packageName ->
                            if (packageName == null) {
                                selectedIndex = index
                                isAppPickerMode = true
                            } else {
                                onShortcutSet(index, packageName)
                            }
                        },
                        onLaunchApp = { packageName, index ->
                            val launched = launchApp(context, packageName)
                            if (launched) {
                                onDismiss()
                            } else {
                                selectedIndex = index
                                isAppPickerMode = true
                            }
                        }
                    )
                }
            }
        }
    }
}
