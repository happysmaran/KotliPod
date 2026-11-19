package com.smaran.kotlipod.library

import com.smaran.kotlipod.core.Song

val AUDIO_EXTENSIONS = setOf("mp3", "m4a", "wav", "aif", "aiff")

class SongTags(
    val title: String?,
    val artist: String?,
    val album: String?,
    val genre: String?,
    val durationSeconds: Int?,
)

// Every platform walks folders differently but must agree on which files are music, otherwise the
// same folder would show different libraries on desktop and phone. macOS leaves "._name.mp3"
// companion files that carry the audio extension without containing audio.
fun isAudioFileName(fileName: String): Boolean =
    !fileName.startsWith(".") && fileName.substringAfterLast('.', "").lowercase() in AUDIO_EXTENSIONS

// A file whose tags are missing or unreadable is still playable, so it is listed under its file name.
fun buildSong(fileName: String, path: String, tags: SongTags?): Song = Song(
    title = tags?.title.orElse(fileName.substringBeforeLast('.')),
    artist = tags?.artist.orElse("Unknown Artist"),
    album = tags?.album.orElse("Unknown Album"),
    genre = tags?.genre.orElse("Unknown"),
    durationSeconds = tags?.durationSeconds ?: 0,
    path = path,
)

private fun String?.orElse(default: String): String = if (isNullOrBlank()) default else this
