package com.edgegesture.evilgodxu.screens.backtap.compact.action_area

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.edgegesture.evilgodxu.data.gesture.GestureAction
import com.edgegesture.evilgodxu.screens.backtap.compact.action_area.action_card.ActionCard

// 触发操作 Area — 仅负责组件排列
@Composable
fun ActionArea(
    currentAction: GestureAction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        ActionCard(currentAction = currentAction, onClick = onClick)
        Spacer(modifier = Modifier.height(80.dp))
    }
}
