package com.edgegesture.evilgodxu.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

// 路由键：导航图中的每个目的地对应一个 NavKey
@Serializable
data object GestureRoute : NavKey

@Serializable
data object SettingsRoute : NavKey

@Serializable
data object BlacklistRoute : NavKey

@Serializable
data object LaunchBlockRoute : NavKey

@Serializable
data object BackTapRoute : NavKey

@Serializable
data object LeftEdgeConfigRoute : NavKey

@Serializable
data object RightEdgeConfigRoute : NavKey

@Serializable
data object BottomEdgeConfigRoute : NavKey

@Serializable
data object ExpandPanelRoute : NavKey
