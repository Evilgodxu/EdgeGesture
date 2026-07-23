package com.edgegesture.evilgodxu.screens.expandpanel

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.edgegesture.evilgodxu.R
import com.edgegesture.evilgodxu.data.gesture.expandPanelShortcutsFlow
import com.edgegesture.evilgodxu.data.gesture.saveExpandPanelShortcut
import com.edgegesture.evilgodxu.data.gesture.saveExpandPanelShortcutFreeform
import com.edgegesture.evilgodxu.screens.expandpanel.compact.CompactAssembly
import com.edgegesture.evilgodxu.screens.expandpanel.overlay.AppPickerDialog
import kotlinx.coroutines.launch

// 扩展面板设置页 — 页面入口 Composable
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpandPanelScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val shortcutsState by context.expandPanelShortcutsFlow().collectAsState(initial = null)

    var showAppPicker by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableIntStateOf(-1) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.more_expand_panel),
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.nav_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        CompactAssembly(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            shortcutsState = shortcutsState,
            onShortcutSet = { index, packageName ->
                if (packageName == null) {
                    selectedIndex = index
                    showAppPicker = true
                } else {
                    scope.launch {
                        context.saveExpandPanelShortcut(index, packageName)
                    }
                }
            },
            onLaunchApp = { _, index ->
                selectedIndex = index
                showAppPicker = true
            },
            onFreeformToggle = { index, enabled ->
                scope.launch {
                    context.saveExpandPanelShortcutFreeform(index, enabled)
                }
            },
        )
    }

    // 应用选择器对话框
    if (showAppPicker && selectedIndex >= 0) {
        AppPickerDialog(
            onAppSelected = { packageName ->
                val targetIndex = selectedIndex
                scope.launch {
                    context.saveExpandPanelShortcut(targetIndex, packageName)
                }
                showAppPicker = false
                selectedIndex = -1
            },
            onDismiss = {
                showAppPicker = false
                selectedIndex = -1
            },
        )
    }
}
