package com.edgegesture.evilgodxu.screens.backtap.assembly

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
import com.edgegesture.evilgodxu.data.gesture.BackTapMode
import com.edgegesture.evilgodxu.data.gesture.GestureSettingsState
import com.edgegesture.evilgodxu.screens.backtap.component.action.BackTapAction
import com.edgegesture.evilgodxu.screens.backtap.component.config.BackTapConfig
import com.edgegesture.evilgodxu.ui.component.AppTopBar

// 背面双击页组装器：配置卡片 + 触发操作卡片，滚动容器沿用重构前的内边距与顺序
@Composable
fun BackTapAssembly(
    settings: GestureSettingsState?,
    appNameByPackage: Map<String, String>,
    onBack: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onSensitivityChange: (Int) -> Unit,
    onRangeChange: (Int) -> Unit,
    onModeChange: (BackTapMode) -> Unit,
    onPauseOnChargingChange: (Boolean) -> Unit,
    onShowActionDialog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            AppTopBar(
                title = stringResource(R.string.back_tap_title),
                onBack = onBack,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            BackTapConfig(
                settings = settings,
                onEnabledChange = onEnabledChange,
                onSensitivityChange = onSensitivityChange,
                onRangeChange = onRangeChange,
                onModeChange = onModeChange,
                onPauseOnChargingChange = onPauseOnChargingChange,
            )

            Spacer(modifier = Modifier.height(12.dp))

            BackTapAction(
                settings = settings,
                appNameByPackage = appNameByPackage,
                onClick = onShowActionDialog,
            )

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
