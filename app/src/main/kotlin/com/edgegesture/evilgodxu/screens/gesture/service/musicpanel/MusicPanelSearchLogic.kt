package com.edgegesture.evilgodxu.screens.gesture.service.musicpanel

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.edgegesture.evilgodxu.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

internal suspend fun searchCoverCandidates(
    playbackState: MusicPlaybackState,
    track: MusicTrack,
) {
    playbackState.isCoverSearching = true
    playbackState.coverCandidates = emptyList()
    try {
        val query = listOf(track.title, track.artist).filter { it.isNotBlank() }.joinToString(" ")
        playbackState.coverCandidates = NeteaseMusicApi.searchSongs(query)
            .filter { !it.coverUrl.isNullOrBlank() }
            .take(5)
    } catch (_: Exception) {
        playbackState.coverCandidates = emptyList()
    } finally {
        playbackState.isCoverSearching = false
    }
}

internal suspend fun applyCoverCandidate(
    context: Context,
    playbackState: MusicPlaybackState,
    track: MusicTrack,
    candidate: NeteaseSongSearchResult,
): Boolean {
    return try {
        val updated = withContext(Dispatchers.IO) {
            val bytes = NeteaseMusicApi.loadCoverBytes(candidate.coverUrl.orEmpty()) ?: return@withContext null
            val path = MusicMetadataCache.saveCover(context, candidate.id, bytes).orEmpty()
            val bitmap = MusicMetadataCache.loadCover(path)
            if (path.isBlank() && bitmap == null) return@withContext null
            track.copy(
                albumArt = bitmap,
                neteaseId = candidate.id,
                neteaseCoverUrl = candidate.coverUrl.orEmpty(),
                coverCachePath = path
            )
        } ?: return false
        withContext(Dispatchers.Main) {
            playbackState.updateTrack(updated)
            playbackState.coverCandidates = emptyList()
        }
        true
    } catch (_: Exception) {
        false
    }
}

internal suspend fun performSearch(
    playbackState: MusicPlaybackState,
    context: Context,
) {
    val query = playbackState.searchQuery.trim()
    if (query.isBlank()) return
    playbackState.isSearching = true
    playbackState.searchResults = emptyList()
    playbackState.errorMsg = null
    try {
        val results = NeteaseMusicApi.searchSongs(query)
        playbackState.searchResults = results
        if (results.isNotEmpty()) playbackState.addSearchHistory(query)
        playbackState.showSearchResults = true
    } catch (_: Exception) {
        playbackState.searchResults = emptyList()
    } finally {
        playbackState.isSearching = false
    }
}

internal suspend fun downloadAndPlay(
    context: Context,
    playbackState: MusicPlaybackState,
    result: NeteaseSongSearchResult,
    url: String,
) {
    val trackId = result.id + 1000000L
    val track = MusicTrack(
        id = trackId,
        path = "",
        audioUri = url,
        title = result.title,
        artist = result.artist,
        duration = result.duration,
        albumId = 0L,
        neteaseId = result.id,
        neteaseCoverUrl = result.coverUrl.orEmpty()
    )

    withContext(Dispatchers.Main) {
        val existingIndex = playbackState.playlist.indexOfFirst { it.id == trackId }
        val targetIndex = if (existingIndex >= 0) {
            existingIndex
        } else {
            playbackState.playlist = playbackState.playlist + track
            playbackState.playlist.size - 1
        }
        playbackState.currentIndex = targetIndex
        playbackState.currentTrack = playbackState.playlist[targetIndex]
        playbackState.persistPlaylist()
        playTrackAt(context, playbackState, targetIndex)
    }

    playbackState.playbackScope.launch(Dispatchers.IO) {
        try {
            val lyric = NeteaseMusicApi.lyric(result.id)
            if (lyric.lines.isNotEmpty()) {
                val lyricPath = MusicMetadataCache.saveLyrics(context, result.id, lyric.lines).orEmpty()
                withContext(Dispatchers.Main) {
                    val idx = playbackState.playlist.indexOfFirst { it.id == trackId }
                    if (idx >= 0) {
                        val updated = playbackState.playlist[idx].copy(
                            lyricCachePath = lyricPath,
                            lyricLines = lyric.lines
                        )
                        val list = playbackState.playlist.toMutableList()
                        list[idx] = updated
                        playbackState.playlist = list
                        if (playbackState.currentTrack?.id == trackId) {
                            playbackState.currentTrack = updated
                        }
                    }
                }
            }
        } catch (_: Exception) { }
    }

    playbackState.playbackScope.launch(Dispatchers.IO) {
        cacheToDownloads(context, result, url, trackId, playbackState)
    }
}

internal suspend fun cacheToDownloads(
    context: Context,
    result: NeteaseSongSearchResult,
    url: String,
    trackId: Long,
    playbackState: MusicPlaybackState,
) {
    try {
        val fileName = "${sanitizeFileName(result.title)} - ${sanitizeFileName(result.artist)}.mp3"

        val existingUri = findExistingDownload(context, fileName)
        if (existingUri != null) {
            withContext(Dispatchers.Main) {
                updateTrackAudioUri(playbackState, trackId, existingUri)
            }
            return
        }

        val connection = URL(url).openConnection()
        connection.connectTimeout = 15000
        connection.readTimeout = 60000
        val bytes = (connection as java.net.HttpURLConnection).inputStream.use { it.readBytes() }

        val audioUri: String

        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "audio/mpeg")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/EdgeGesture")
        }
        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.use { os -> os.write(bytes) }
            audioUri = uri.toString()
        } else {
            return
        }

        withContext(Dispatchers.Main) {
            updateTrackAudioUri(playbackState, trackId, audioUri)
        }
    } catch (_: Exception) {
    }
}

internal fun sanitizeFileName(name: String): String {
    return name.replace(Regex("[/\\\\:*?\"<>|]"), "_")
        .take(80)
        .trim()
}

internal fun normalizeTitle(value: String): String {
    return value.lowercase()
        .replace(Regex("""[\s　（）()\[\]【】「」『』《》〈〉、，。！？"'""'']+"""), "")
        .trim()
}

internal suspend fun enrichOnlineMetadata(
    context: Context,
    playbackState: MusicPlaybackState,
    track: MusicTrack,
    result: NeteaseSongSearchResult,
) {
    if (track.neteaseId != 0L && track.lyricLines.isNotEmpty()) return
    try {
        val lyric = NeteaseMusicApi.lyric(result.id)
        val lyricPath = if (lyric.lines.isNotEmpty()) {
            MusicMetadataCache.saveLyrics(context, result.id, lyric.lines).orEmpty()
        } else ""
        withContext(Dispatchers.Main) {
            val idx = playbackState.playlist.indexOfFirst { it.id == track.id }
            if (idx < 0) return@withContext
            val updated = playbackState.playlist[idx].copy(
                neteaseId = result.id,
                neteaseCoverUrl = result.coverUrl.orEmpty(),
                lyricCachePath = lyricPath,
                lyricLines = lyric.lines
            )
            val list = playbackState.playlist.toMutableList()
            list[idx] = updated
            playbackState.playlist = list
            if (playbackState.currentTrack?.id == track.id) {
                playbackState.currentTrack = updated
            }
        }
    } catch (_: Exception) { }
}

internal suspend fun findExistingDownload(
    context: Context,
    fileName: String,
): String? = withContext(Dispatchers.IO) {
    try {
        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Downloads._ID)
        val selection = "${MediaStore.Downloads.DISPLAY_NAME} = ? AND " +
                "${MediaStore.Downloads.RELATIVE_PATH} = ?"
        val args = arrayOf(fileName, Environment.DIRECTORY_DOWNLOADS + "/EdgeGesture/")
        context.contentResolver.query(collection, projection, selection, args, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(0)
                return@withContext Uri.withAppendedPath(collection, id.toString()).toString()
            }
        }
    } catch (_: Exception) { }
    null
}

internal fun updateTrackAudioUri(
    playbackState: MusicPlaybackState,
    trackId: Long,
    audioUri: String,
) {
    val idx = playbackState.playlist.indexOfFirst { it.id == trackId }
    if (idx < 0) return
    val updated = playbackState.playlist[idx].copy(audioUri = audioUri)
    val list = playbackState.playlist.toMutableList()
    list[idx] = updated
    playbackState.playlist = list
    if (playbackState.currentTrack?.id == trackId) {
        playbackState.currentTrack = updated
    }
    playbackState.persistPlaylist()
}

internal suspend fun playSearchResult(
    target: NeteaseSongSearchResult,
    playbackState: MusicPlaybackState,
    context: Context,
    scope: kotlinx.coroutines.CoroutineScope,
) {
    val normalizedTitle = normalizeTitle(target.title)
    val normalizedArtist = normalizeTitle(target.artist)
    val localMatch = playbackState.playlist.firstOrNull { t ->
        t.path.isNotBlank() &&
        normalizeTitle(t.title) == normalizedTitle &&
        (normalizedArtist.isBlank() || normalizeTitle(t.artist) == normalizedArtist)
    }
    if (localMatch != null) {
        val idx = playbackState.playlist.indexOfFirst { it.id == localMatch.id }
        if (idx >= 0) {
            scope.launch {
                enrichOnlineMetadata(context, playbackState, localMatch, target)
            }
            playbackState.errorMsg = null
            playbackState.currentIndex = idx
            playbackState.currentTrack = playbackState.playlist[idx]
            playbackState.isSearchMode = false
            playbackState.showSearchResults = false
            playbackState.searchQuery = ""
            playbackState.searchResults = emptyList()
            playTrackAt(context, playbackState, idx)
            return
        }
    }

    val clickedIndex = playbackState.searchResults.indexOfFirst { it.id == target.id }
    playbackState.pendingSearchResults = if (clickedIndex >= 0) {
        playbackState.searchResults.drop(clickedIndex + 1)
    } else emptyList()

    val fullResult = if (target.coverUrl.isNullOrBlank() || target.duration <= 0L) {
        withContext(Dispatchers.IO) {
            NeteaseMusicApi.songDetail(target.id) ?: target
        }
    } else target

    val url = withContext(Dispatchers.IO) {
        NeteaseMusicApi.getSongUrlWithFallback(fullResult.id)
    }
    if (url != null) {
        playbackState.errorMsg = null
        downloadAndPlay(context, playbackState, fullResult, url)
        playbackState.isSearchMode = false
        playbackState.showSearchResults = false
        playbackState.searchQuery = ""
        playbackState.searchResults = emptyList()
    } else {
        playbackState.errorMsg = context.getString(R.string.music_panel_play_error)
        val pending = playbackState.pendingSearchResults
        if (pending.isNotEmpty()) {
            playbackState.pendingSearchResults = pending.drop(1)
            playSearchResult(pending.first(), playbackState, context, scope)
        } else {
            playbackState.pendingSearchResults = emptyList()
        }
    }
}