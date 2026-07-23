package com.edgegesture.evilgodxu.screens.backtap.compact

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
import com.edgegesture.evilgodxu.data.gesture.BackTapMode
import com.edgegesture.evilgodxu.data.gesture.GestureAction
import com.edgegesture.evilgodxu.data.gesture.GestureSettingsState
import com.edgegesture.evilgodxu.screens.backtap.compact.action_area.ActionArea
import com.edgegesture.evilgodxu.screens.backtap.compact.config_area.ConfigArea
// 紧凑视图物理空间组装器
@Composable
fun CompactAssembly(
    settings: GestureSettingsState?,
    onSaveBackTapEnabled: (Boolean) -> Unit,
    onSaveBackTapSensitivity: (Int) -> Unit,
    onSaveBackTapRange: (Int) -> Unit,
    onSaveBackTapMode: (BackTapMode) -> Unit,
    onSaveBackTapPauseOnCharging: (Boolean) -> Unit,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        ConfigArea(
            settings = settings,
            onSaveBackTapEnabled = onSaveBackTapEnabled,
            onSaveBackTapSensitivity = onSaveBackTapSensitivity,
            onSaveBackTapRange = onSaveBackTapRange,
            onSaveBackTapMode = onSaveBackTapMode,
            onSaveBackTapPauseOnCharging = onSaveBackTapPauseOnCharging,
        )

        Spacer(modifier = Modifier.height(12.dp))

        ActionArea(
            currentAction = settings?.backTapAction ?: GestureAction.NONE,
            onClick = onActionClick,
        )
    }
}
