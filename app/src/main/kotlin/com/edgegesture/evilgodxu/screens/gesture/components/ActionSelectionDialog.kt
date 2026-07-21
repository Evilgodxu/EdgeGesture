package com.edgegesture.evilgodxu.screens.gesture.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.edgegesture.evilgodxu.R
import com.edgegesture.evilgodxu.data.gesture.GestureAction

// 动作选择对话框
@Composable
fun ActionSelectionDialog(
    currentAction: GestureAction,
    onDismiss: () -> Unit,
    onActionSelected: (GestureAction) -> Unit,
    getActionDisplayName: @Composable (GestureAction) -> String
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(R.string.gesture_select_action_title),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                GestureAction.entries.forEach { action ->
                    val isSelected = action == currentAction
                    Text(
                        text = getActionDisplayName(action),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected)
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                else
                                    MaterialTheme.colorScheme.surface
                            )
                            .clickable { onActionSelected(action) }
                            .padding(vertical = 14.dp),
                        textAlign = TextAlign.Center,
                        color = if (isSelected)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        },
        confirmButton = {}
    )
}
