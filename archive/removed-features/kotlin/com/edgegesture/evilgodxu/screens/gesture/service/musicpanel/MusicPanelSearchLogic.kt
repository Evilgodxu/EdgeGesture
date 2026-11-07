package com.edgegesture.evilgodxu.screens.gesture.service.musicpanel

import android.content.Context

// 归一化歌名/歌手：去除大小写、空白与常见括号内容，便于宽松比较
internal fun normalizeTitle(value: String): String {
    return value.lowercase()
        .replace(Regex("""[\s　（）()\[\]【】「」『』《》〈〉、，。！？"'""'']+"""), "")
        .trim()
}

// 本地歌曲匹配：标题+歌手、标题、歌手包含查询词，或标题/歌手首字命中查询词首字
internal fun matchesLocalQuery(track: MusicTrack, query: String): Boolean {
    val q = normalizeTitle(query)
    if (q.isBlank()) return false
    val title = normalizeTitle(track.title)
    val artist = normalizeTitle(track.artist)
    if ((title + artist).contains(q)) return true
    if (title.contains(q) || artist.contains(q)) return true
    val firstChar = q.first()
    return title.startsWith(firstChar) || artist.startsWith(firstChar)
}

// 在本地歌单中检索，命中结果写入搜索结果状态，命中时记录搜索历史
internal fun performLocalSearch(playbackState: MusicPlaybackState, query: String) {
    val trimmed = query.trim()
    if (trimmed.isBlank()) return
    val results = playbackState.playlist.filter { matchesLocalQuery(it, trimmed) }
    playbackState.searchResults = results
    playbackState.showSearchResults = true
    if (results.isNotEmpty()) playbackState.addSearchHistory(trimmed)
}

// 播放本地搜索结果并退出搜索态（保留搜索历史）
internal suspend fun playLocalSearchResult(
    track: MusicTrack,
    playbackState: MusicPlaybackState,
    context: Context,
) {
    val index = playbackState.playlist.indexOfFirst { it.id == track.id }
    if (index < 0) return
    playbackState.errorMsg = null
    playbackState.currentIndex = index
    playbackState.currentTrack = playbackState.playlist[index]
    playbackState.isSearchMode = false
    playbackState.showSearchResults = false
    playbackState.searchQuery = ""
    playbackState.searchResults = emptyList()
    playTrackAt(context, playbackState, index)
}