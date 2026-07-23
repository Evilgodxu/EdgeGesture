package com.edgegesture.evilgodxu.screens.settings.expanded.footer_area

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.edgegesture.evilgodxu.R

// 宽屏底部信息 Area
@Composable
fun FooterArea(
    versionName: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = "Evilgodxu",
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 2.dp)
    )
    Text(
        text = stringResource(R.string.settings_version, versionName),
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
    )
}
