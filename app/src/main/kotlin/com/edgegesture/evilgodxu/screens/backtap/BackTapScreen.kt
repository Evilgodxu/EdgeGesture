package com.edgegesture.evilgodxu.screens.backtap

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import com.edgegesture.evilgodxu.ui.theme.AppSwitch
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edgegesture.evilgodxu.R
import com.composables.icons.materialsymbols.outlined.R.drawable as MsRDrawable
import com.edgegesture.evilgodxu.data.gesture.BackTapMode
import com.edgegesture.evilgodxu.data.gesture.GestureAction
import com.edgegesture.evilgodxu.data.gesture.gestureSettingsFlow
import com.edgegesture.evilgodxu.data.gesture.saveBackTapAction
import com.edgegesture.evilgodxu.data.gesture.saveBackTapEnabled
import com.edgegesture.evilgodxu.data.gesture.saveBackTapMode
import com.edgegesture.evilgodxu.data.gesture.saveBackTapPauseOnCharging
import com.edgegesture.evilgodxu.data.gesture.saveBackTapRange
import com.edgegesture.evilgodxu.data.gesture.saveBackTapSensitivity
import com.edgegesture.evilgodxu.screens.gesture.components.ActionSelectionDialog
import com.edgegesture.evilgodxu.screens.gesture.components.getActionDisplayName
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

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
                            painter = painterResource(MsRDrawable.materialsymbols_ic_arrow_back_outlined),
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // 设置卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // 卡片头部
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            painter = painterResource(MsRDrawable.materialsymbols_ic_touch_double_outlined),
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.back_tap_title),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 启用开关
                    SettingsToggleRow(
                        title = stringResource(R.string.back_tap_enable_title),
                        description = stringResource(R.string.back_tap_enable_desc),
                        checked = settings?.backTapEnabled ?: false,
                        onCheckedChange = { enabled ->
                            scope.launch { context.saveBackTapEnabled(enabled) }
                        }
                    )

                    if (settings?.backTapEnabled == true) {
                        Spacer(modifier = Modifier.height(16.dp))

                        // 灵敏度滑块
                        SettingsSliderItem(
                            label = stringResource(R.string.back_tap_sensitivity),
                            value = settings.backTapSensitivity,
                            onValueChange = { value ->
                                scope.launch { context.saveBackTapSensitivity(value) }
                            }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // 检测范围滑块
                        SettingsSliderItem(
                            label = stringResource(R.string.back_tap_range),
                            value = settings.backTapRange,
                            onValueChange = { value ->
                                scope.launch { context.saveBackTapRange(value) }
                            }
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // 工作模式选择
                        Text(
                            text = stringResource(R.string.back_tap_mode),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            BackTapMode.entries.forEach { mode ->
                                val isSelected = settings.backTapMode == mode
                                val label = when (mode) {
                                    BackTapMode.ALWAYS -> stringResource(R.string.back_tap_mode_always)
                                    BackTapMode.SCREEN_OFF -> stringResource(R.string.back_tap_mode_screen_off)
                                    BackTapMode.SCREEN_ON -> stringResource(R.string.back_tap_mode_screen_on)
                                }
                                FilledTonalButton(
                                    onClick = { scope.launch { context.saveBackTapMode(mode) } },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = if (isSelected)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = if (isSelected)
                                            MaterialTheme.colorScheme.onPrimary
                                        else
                                            MaterialTheme.colorScheme.onSurface
                                    )
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 充电时暂停
                        SettingsToggleRow(
                            title = stringResource(R.string.back_tap_pause_on_charging_title),
                            description = stringResource(R.string.back_tap_pause_on_charging_desc),
                            checked = settings.backTapPauseOnCharging,
                            onCheckedChange = { pause ->
                                scope.launch { context.saveBackTapPauseOnCharging(pause) }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 触发操作卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showActionDialog = true }
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        painter = painterResource(MsRDrawable.materialsymbols_ic_touch_double_outlined),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.back_tap_action),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = if (settings != null) getActionDisplayName(settings.backTapAction)
                               else "",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Icon(
                        painter = painterResource(MsRDrawable.materialsymbols_ic_chevron_right_outlined),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
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

@Composable
private fun SettingsToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        AppSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SettingsSliderItem(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "$value",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        val fraction = ((value - 1) / 9f).coerceIn(0f, 1f)
        val primary = MaterialTheme.colorScheme.primary
        val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
        val surface = MaterialTheme.colorScheme.surface
        val trackHeight = 6.dp
        val thumbRadius = 9.dp

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val widthPx = size.width.toFloat()
                        val ratio = (offset.x / widthPx).coerceIn(0f, 1f)
                        onValueChange((1 + ratio * 9).roundToInt().coerceIn(1, 10))
                    }
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { change, _ ->
                        change.consume()
                        val widthPx = size.width.toFloat()
                        val ratio = (change.position.x / widthPx).coerceIn(0f, 1f)
                        onValueChange((1 + ratio * 9).roundToInt().coerceIn(1, 10))
                    }
                },
            contentAlignment = Alignment.CenterStart
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val trackY = h / 2f
                val trackH = trackHeight.toPx()
                val thumbR = thumbRadius.toPx()
                val thumbX = w * fraction

                drawRoundRect(
                    color = surfaceVariant,
                    topLeft = Offset(0f, trackY - trackH / 2f),
                    size = Size(w, trackH),
                    cornerRadius = CornerRadius(trackH / 2f)
                )
                drawRoundRect(
                    color = primary,
                    topLeft = Offset(0f, trackY - trackH / 2f),
                    size = Size(thumbX, trackH),
                    cornerRadius = CornerRadius(trackH / 2f)
                )
                drawCircle(
                    color = surface,
                    radius = thumbR + 1.5.dp.toPx(),
                    center = Offset(thumbX, trackY)
                )
                drawCircle(
                    color = primary,
                    radius = thumbR,
                    center = Offset(thumbX, trackY)
                )
            }
        }
    }
}
