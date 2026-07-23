package com.edgegesture.evilgodxu.screens.expandpanel.compact

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.edgegesture.evilgodxu.data.gesture.ExpandPanelShortcutsState
import com.edgegesture.evilgodxu.screens.expandpanel.compact.shortcuts_area.ShortcutsArea
import com.edgegesture.evilgodxu.screens.expandpanel.compact.system_controls_area.SystemControlsArea

// 紧凑视图物理空间组装器
@Composable
fun CompactAssembly(
    shortcutsState: ExpandPanelShortcutsState?,
    onShortcutSet: (Int, String?) -> Unit,
    onLaunchApp: (String, Int) -> Unit,
    onFreeformToggle: (Int, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        SystemControlsArea()

        Spacer(modifier = Modifier.height(16.dp))

        ShortcutsArea(
            shortcutsState = shortcutsState,
            onShortcutSet = onShortcutSet,
            onLaunchApp = onLaunchApp,
            onFreeformToggle = onFreeformToggle,
        )

        Spacer(modifier = Modifier.height(20.dp))
    }
}
