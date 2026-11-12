package com.edgegesture.evilgodxu.screens.gesture.assembly

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.window.core.layout.WindowSizeClass
import com.edgegesture.evilgodxu.R
import com.edgegesture.evilgodxu.data.gesture.GestureSettingsState
import com.edgegesture.evilgodxu.screens.gesture.EdgeType
import com.edgegesture.evilgodxu.screens.gesture.component.action.GestureActionsSection
import com.edgegesture.evilgodxu.screens.gesture.component.dimension.BottomDimensionSliders
import com.edgegesture.evilgodxu.screens.gesture.component.dimension.EdgeDimensionSliders
import com.edgegesture.evilgodxu.screens.gesture.component.dimension.SegmentCountSlider
import com.edgegesture.evilgodxu.screens.gesture.component.preview.EdgePreview
import com.edgegesture.evilgodxu.ui.adaptive.contentMaxWidth
import com.edgegesture.evilgodxu.ui.component.AppTopBar
import com.edgegesture.evilgodxu.ui.component.ContentPane
import com.edgegesture.evilgodxu.ui.component.SectionCard

// 单边缘配置页组装器：触发区预览与滑杆 + 手势操作列表，行宽由 ContentPane 收窄
@Composable
fun EdgeGestureConfigAssembly(
    edgeType: EdgeType,
    title: String,
    settings: GestureSettingsState?,
    windowSizeClass: WindowSizeClass,
    selectedSegment: Int,
    localWidth: Int,
    localHeightPercent: Int,
    localPositionPercent: Int,
    localSegmentCount: Int,
    maxSegments: Int,
    appNameByPackage: Map<String, String>,
    onBack: () -> Unit,
    onSegmentSelected: (Int) -> Unit,
    onWidthChange: (Int) -> Unit,
    onHeightPercentChange: (Int) -> Unit,
    onPositionPercentChange: (Int) -> Unit,
    onWidthChangeFinished: () -> Unit,
    onHeightPercentChangeFinished: () -> Unit,
    onPositionPercentChangeFinished: () -> Unit,
    onSegmentCountChange: (Int) -> Unit,
    onSegmentCountChangeFinished: () -> Unit,
    onActionClick: (Preferences.Key<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            AppTopBar(title = title, onBack = onBack)
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        ContentPane(
            maxContentWidth = windowSizeClass.contentMaxWidth,
            modifier = Modifier
                .fillMaxSize()
                .consumeWindowInsets(innerPadding)
                .padding(innerPadding),
        ) {
            SectionCard(title = stringResource(R.string.gesture_config_section_title)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    EdgePreview(
                        edgeType = edgeType,
                        width = localWidth,
                        heightPercent = localHeightPercent,
                        positionPercent = localPositionPercent,
                        segmentCount = localSegmentCount,
                        selectedSegment = selectedSegment,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (edgeType == EdgeType.BOTTOM) {
                        BottomDimensionSliders(
                            height = localWidth,
                            widthPercent = localHeightPercent,
                            onHeightChange = onWidthChange,
                            onWidthPercentChange = onHeightPercentChange,
                            onHeightChangeFinished = onWidthChangeFinished,
                            onWidthPercentChangeFinished = onHeightPercentChangeFinished,
                        )
                    } else {
                        EdgeDimensionSliders(
                            width = localWidth,
                            heightPercent = localHeightPercent,
                            positionPercent = localPositionPercent,
                            onWidthChange = onWidthChange,
                            onHeightPercentChange = onHeightPercentChange,
                            onPositionPercentChange = onPositionPercentChange,
                            onWidthChangeFinished = onWidthChangeFinished,
                            onHeightPercentChangeFinished = onHeightPercentChangeFinished,
                            onPositionPercentChangeFinished = onPositionPercentChangeFinished,
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    SegmentCountSlider(
                        segmentCount = localSegmentCount,
                        onSegmentCountChange = onSegmentCountChange,
                        onSegmentCountChangeFinished = onSegmentCountChangeFinished,
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            GestureActionsSection(
                edgeType = edgeType,
                settings = settings,
                selectedSegment = selectedSegment,
                maxSegments = maxSegments,
                appNameByPackage = appNameByPackage,
                onSegmentSelected = onSegmentSelected,
                onActionClick = onActionClick,
            )

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
