package com.edgegesture.evilgodxu.screens.expandpanel.assembly

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import com.edgegesture.evilgodxu.R
import com.edgegesture.evilgodxu.data.gesture.ExpandPanelShortcutsState
import com.edgegesture.evilgodxu.screens.expandpanel.component.ShortcutSection
import com.edgegesture.evilgodxu.screens.expandpanel.component.SystemControls
import com.edgegesture.evilgodxu.ui.adaptive.contentMaxWidth
import com.edgegesture.evilgodxu.ui.component.AppTopBar
import com.edgegesture.evilgodxu.ui.component.ContentPane

// 扩展面板设置页组装器：系统控制 + 快捷方式两个区块，行宽由 ContentPane 收窄
@Composable
fun ExpandPanelAssembly(
    windowSizeClass: WindowSizeClass,
    state: ExpandPanelShortcutsState?,
    onBack: () -> Unit,
    onPickShortcut: (Int) -> Unit,
    onSaveShortcut: (Int, String?) -> Unit,
    onFreeformToggle: (Int, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            AppTopBar(
                title = stringResource(R.string.more_expand_panel),
                onBack = onBack,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        ContentPane(
            maxContentWidth = windowSizeClass.contentMaxWidth,
            modifier = Modifier
                .fillMaxSize()
                .consumeWindowInsets(innerPadding)
                .padding(innerPadding),
        ) {
            SystemControls()

            Spacer(modifier = Modifier.height(16.dp))

            ShortcutSection(
                state = state,
                onPickShortcut = onPickShortcut,
                onSaveShortcut = onSaveShortcut,
                onFreeformToggle = onFreeformToggle,
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
