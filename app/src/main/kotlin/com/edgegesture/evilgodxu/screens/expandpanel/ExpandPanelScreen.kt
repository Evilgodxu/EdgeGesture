package com.edgegesture.evilgodxu.screens.expandpanel

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edgegesture.evilgodxu.R
import com.edgegesture.evilgodxu.data.gesture.expandPanelShortcutsFlow
import com.edgegesture.evilgodxu.data.gesture.saveExpandPanelShortcut
import com.edgegesture.evilgodxu.data.gesture.saveExpandPanelShortcutFreeform
import com.edgegesture.evilgodxu.screens.gesture.service.expandpanel.AppPickerScreen
import com.edgegesture.evilgodxu.screens.gesture.service.expandpanel.ShortcutsGrid
import com.edgegesture.evilgodxu.screens.gesture.service.expandpanel.VerticalSlidersSection
import kotlinx.coroutines.launch

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
                            imageVector = Icons.Default.ArrowBack,
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // 系统控制卡片标题
            Text(
                text = stringResource(R.string.expand_panel_controls_title),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.2.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
            )

            // 系统控制卡片（亮度 + 音量）
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    VerticalSlidersSection()
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 快捷方式卡片标题
            Text(
                text = stringResource(R.string.expand_panel_shortcuts_title),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.2.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
            )

            // 快捷方式卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    val state = shortcutsState
                    if (state != null) {
                        ShortcutsGrid(
                            shortcuts = state.shortcuts,
                            freeformFlags = state.freeformFlags,
                            showTitle = false,
                            onShortcutSet = { index, packageName ->
                                if (packageName == null) {
                                    // 空槽位点击 或 长按已有快捷方式 → 打开应用选择器
                                    selectedIndex = index
                                    showAppPicker = true
                                } else {
                                    // 直接传入包名保存（来自外部恢复等场景）
                                    scope.launch {
                                        context.saveExpandPanelShortcut(index, packageName)
                                    }
                                }
                            },
                            onLaunchApp = { packageName, index ->
                                // 设置页点击已有快捷方式 → 打开应用选择器供用户更换
                                selectedIndex = index
                                showAppPicker = true
                            },
                            onFreeformToggle = { index, enabled ->
                                scope.launch {
                                    context.saveExpandPanelShortcutFreeform(index, enabled)
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // 应用选择器对话框
    if (showAppPicker && selectedIndex >= 0) {
        AlertDialog(
            onDismissRequest = {
                showAppPicker = false
                selectedIndex = -1
            },
            title = {
                Text(
                    text = stringResource(R.string.expand_panel_app_picker_title),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                AppPickerScreen(
                    onAppSelected = { packageName ->
                        scope.launch {
                            context.saveExpandPanelShortcut(selectedIndex, packageName)
                        }
                        showAppPicker = false
                        selectedIndex = -1
                    },
                    onCancel = {
                        showAppPicker = false
                        selectedIndex = -1
                    }
                )
            },
            confirmButton = {}
        )
    }
}
