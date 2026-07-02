package com.edgegesture.evilgodxu.screens.settings.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edgegesture.evilgodxu.R

private data class OpenSourceLibrary(
    val name: String,
    val license: String
)

private val libraries = listOf(
    OpenSourceLibrary("AndroidX Core KTX", "Apache License 2.0"),
    OpenSourceLibrary("AndroidX Lifecycle", "Apache License 2.0"),
    OpenSourceLibrary("AndroidX Activity Compose", "Apache License 2.0"),
    OpenSourceLibrary("Jetpack Compose", "Apache License 2.0"),
    OpenSourceLibrary("AndroidX DataStore", "Apache License 2.0"),
    OpenSourceLibrary("AndroidX Navigation", "Apache License 2.0"),
    OpenSourceLibrary("AndroidX WorkManager", "Apache License 2.0"),
    OpenSourceLibrary("AndroidX Window", "Apache License 2.0"),
    OpenSourceLibrary("Material3 Adaptive", "Apache License 2.0"),
    OpenSourceLibrary("Koin", "Apache License 2.0"),
    OpenSourceLibrary("Kotlinx Serialization JSON", "Apache License 2.0"),
    OpenSourceLibrary("Shizuku", "Apache License 2.0"),
    OpenSourceLibrary("Hidden API Bypass", "Apache License 2.0")
)

// 开源许可对话框，展示项目使用的开源库列表
@Composable
fun OpenSourceLicensesDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.settings_open_source_title),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        text = {
            val maxHeight = LocalConfiguration.current.screenHeightDp.dp / 2
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxHeight)
                    .verticalScroll(rememberScrollState())
            ) {
                libraries.forEach { library ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = library.name,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = library.license,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {}
    )
}
