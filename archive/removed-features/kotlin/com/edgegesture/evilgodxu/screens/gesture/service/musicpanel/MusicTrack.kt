package com.edgegesture.evilgodxu.screens.gesture.service.musicpanel

// 本地音乐轨道
 data class MusicTrack(
    val id: Long,
    val path: String,
    val audioUri: String,
    val title: String,
    val artist: String,
    val duration: Long,
    val albumId: Long,
    internal val lyricLines: List<LyricLine> = emptyList(),
    /** 是否已尝试过自动补全歌词（无论是否找到），用于避免本次会话内重复扫描 */
    val lyricResolved: Boolean = false,
    val isFavorite: Boolean = false,
)

// 播放模式
enum class PlayMode { RepeatOne, RepeatAll, Shuffle }