package com.edgegesture.evilgodxu.screens.backtap

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.edgegesture.evilgodxu.R
import com.edgegesture.evilgodxu.data.gesture.BackTapMode
import com.edgegesture.evilgodxu.data.gesture.gestureSettingsFlow
import com.edgegesture.evilgodxu.data.gesture.saveBackTapAction
import com.edgegesture.evilgodxu.data.gesture.saveBackTapEnabled
import com.edgegesture.evilgodxu.data.gesture.saveBackTapMode
import com.edgegesture.evilgodxu.data.gesture.saveBackTapPauseOnCharging
import com.edgegesture.evilgodxu.data.gesture.saveBackTapRange
import com.edgegesture.evilgodxu.data.gesture.saveBackTapSensitivity
import com.edgegesture.evilgodxu.screens.backtap.compact.CompactAssembly
import com.edgegesture.evilgodxu.screens.gesture.components.ActionSelectionDialog
import com.edgegesture.evilgodxu.screens.gesture.components.getActionDisplayName
import kotlinx.coroutines.launch

// 背面双击设置页 — 页面入口 Composable
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackTapScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val gestureSettings by context.gestureSettingsFlow().collectAsState(initial = null)

    var showActionDialog by remember { mutableStateOf(false) }
    val settings = gestureSettings

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.back_tap_title),
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
            settings = settings,
            onSaveBackTapEnabled = { enabled ->
                scope.launch { context.saveBackTapEnabled(enabled) }
            },
            onSaveBackTapSensitivity = { value ->
                scope.launch { context.saveBackTapSensitivity(value) }
            },
            onSaveBackTapRange = { value ->
                scope.launch { context.saveBackTapRange(value) }
            },
            onSaveBackTapMode = { mode ->
                scope.launch { context.saveBackTapMode(mode) }
            },
            onSaveBackTapPauseOnCharging = { pause ->
                scope.launch { context.saveBackTapPauseOnCharging(pause) }
            },
            onActionClick = { showActionDialog = true },
        )
    }

    // 动作选择对话框
    if (showActionDialog && settings != null) {
        ActionSelectionDialog(
            currentAction = settings.backTapAction,
            onDismiss = { showActionDialog = false },
            onActionSelected = { action ->
                scope.launch { context.saveBackTapAction(action) }
                showActionDialog = false
            },
            getActionDisplayName = { getActionDisplayName(it) }
        )
    }
}
