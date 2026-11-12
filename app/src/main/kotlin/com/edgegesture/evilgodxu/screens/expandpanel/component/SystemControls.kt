package com.edgegesture.evilgodxu.screens.expandpanel.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.edgegesture.evilgodxu.R
import com.edgegesture.evilgodxu.screens.gesture.service.expandpanel.VerticalSlidersSection
import com.edgegesture.evilgodxu.ui.component.SectionCard

// 系统控制区块：亮度与音量滑杆，与悬浮面板共用同一套滑块实现
@Composable
fun SystemControls() {
    SectionCard(title = stringResource(R.string.expand_panel_controls_title)) {
        Column(modifier = Modifier.padding(20.dp)) {
            VerticalSlidersSection()
        }
    }
}
