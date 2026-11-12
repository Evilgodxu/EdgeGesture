package com.edgegesture.evilgodxu.screens.expandpanel.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.edgegesture.evilgodxu.R
import com.edgegesture.evilgodxu.data.gesture.ExpandPanelShortcutsState
import com.edgegesture.evilgodxu.screens.gesture.service.expandpanel.ShortcutsGrid
import com.edgegesture.evilgodxu.ui.component.SectionCard

// 快捷方式区块：槽位网格，空槽位或点击已有项都进入应用选择器
@Composable
fun ShortcutSection(
    state: ExpandPanelShortcutsState?,
    onPickShortcut: (Int) -> Unit,
    onSaveShortcut: (Int, String?) -> Unit,
    onFreeformToggle: (Int, Boolean) -> Unit,
) {
    SectionCard(title = stringResource(R.string.expand_panel_shortcuts_title)) {
        Column(modifier = Modifier.padding(20.dp)) {
            if (state != null) {
                ShortcutsGrid(
                    shortcuts = state.shortcuts,
                    freeformFlags = state.freeformFlags,
                    showTitle = false,
                    onShortcutSet = { index, packageName ->
                        if (packageName == null) {
                            // 空槽位点击或长按已有快捷方式 → 打开应用选择器
                            onPickShortcut(index)
                        } else {
                            // 直接传入包名保存（来自外部恢复等场景）
                            onSaveShortcut(index, packageName)
                        }
                    },
                    onLaunchApp = { _, index ->
                        // 设置页点击已有快捷方式 → 打开应用选择器供用户更换
                        onPickShortcut(index)
                    },
                    onFreeformToggle = onFreeformToggle,
                )
            }
        }
    }
}
