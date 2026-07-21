package com.edgegesture.evilgodxu.screens.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.edgegesture.evilgodxu.screens.settings.AppLanguage

// 语言选择对话框
@Composable
fun LanguageSelectionDialog(
    currentLanguage: AppLanguage,
    onDismiss: () -> Unit,
    onLanguageSelected: (AppLanguage) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(R.string.settings_language_dialog_title),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AppLanguage.entries.forEach { language ->
                    val isSelected = currentLanguage == language
                    Text(
                        text = when (language) {
                            AppLanguage.SYSTEM -> stringResource(R.string.settings_language_system)
                            AppLanguage.CHINESE -> stringResource(R.string.settings_language_chinese)
                            AppLanguage.ENGLISH -> stringResource(R.string.settings_language_english)
                        },
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
                            .clickable { onLanguageSelected(language) }
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
