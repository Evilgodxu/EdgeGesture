package com.edgegesture.evilgodxu.screens.gesture.service.musicpanel

import android.graphics.Bitmap

// 本地音乐轨道
 data class MusicTrack(
    val id: Long,
    val path: String,
    val title: String,
    val artist: String,
    val duration: Long,
    val albumId: Long,
    val albumArt: Bitmap? = null,
    val isFavorite: Boolean = false,
)

// 播放模式
enum class PlayMode { RepeatOne, RepeatAll, Shuffle }