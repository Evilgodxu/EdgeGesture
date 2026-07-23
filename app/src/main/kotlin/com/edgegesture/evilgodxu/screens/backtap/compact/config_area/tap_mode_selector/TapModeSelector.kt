package com.edgegesture.evilgodxu.screens.backtap.compact.config_area.tap_mode_selector

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edgegesture.evilgodxu.R
import com.edgegesture.evilgodxu.data.gesture.BackTapMode

// 背面双击模式选择器职责组件
@Composable
fun TapModeSelector(
    currentMode: BackTapMode,
    onModeSelected: (BackTapMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BackTapMode.entries.forEach { mode ->
            val isSelected = currentMode == mode
            val label = when (mode) {
                BackTapMode.ALWAYS -> stringResource(R.string.back_tap_mode_always)
                BackTapMode.SCREEN_OFF -> stringResource(R.string.back_tap_mode_screen_off)
                BackTapMode.SCREEN_ON -> stringResource(R.string.back_tap_mode_screen_on)
            }
            FilledTonalButton(
                onClick = { onModeSelected(mode) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}
