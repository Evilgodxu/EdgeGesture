package com.byss.jh.ui.gesture.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import com.byss.jh.R
import com.byss.jh.data.gesture.GestureAction

/**
 * 边缘手势区域组件
 */
@Composable
fun EdgeGestureSection(
    title: String,
    gestures: List<Triple<String, GestureAction, Preferences.Key<String>>>,
    disabledGestures: Set<String> = emptySet(),
    onGestureClick: (String, GestureAction, Preferences.Key<String>) -> Unit,
    getActionDisplayName: @Composable (GestureAction) -> String
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    gestures.forEach { (name, action, key) ->
                        val isDisabled = disabledGestures.contains(name)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !isDisabled) { onGestureClick(name, action, key) }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isDisabled) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isDisabled) stringResource(R.string.gesture_disabled) else getActionDisplayName(action),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isDisabled) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary
                            )
                        }
                        if (gestures.last() != Triple(name, action, key)) {
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}
