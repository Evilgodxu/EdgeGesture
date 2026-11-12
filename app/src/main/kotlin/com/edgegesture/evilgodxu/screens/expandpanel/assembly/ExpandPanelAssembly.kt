package com.edgegesture.evilgodxu.screens.expandpanel.assembly

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.edgegesture.evilgodxu.R
import com.edgegesture.evilgodxu.data.gesture.ExpandPanelShortcutsState
import com.edgegesture.evilgodxu.screens.expandpanel.component.ShortcutSection
import com.edgegesture.evilgodxu.screens.expandpanel.component.SystemControls
import com.edgegesture.evilgodxu.ui.component.AppTopBar

// 扩展面板设置页组装器：系统控制 + 快捷方式两个区块，滚动容器沿用重构前的内边距与顺序
@Composable
fun ExpandPanelAssembly(
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
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(modifier = Modifier.height(4.dp))

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
