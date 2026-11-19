package com.smaran.kotlipod.library

import com.smaran.kotlipod.core.LibraryScanner
import com.smaran.kotlipod.core.Song
import java.io.File

// Tag parsing is the only part of scanning that needs a third-party library, so it sits behind
// this interface and the folder-walking rules can be tested without audio files.
fun interface TagReader {
    fun read(file: File): SongTags?
}

class FolderScanner(private val tagReader: TagReader) : LibraryScanner {
    override fun scan(folderPath: String): List<Song> {
        val root = File(folderPath)
        require(root.isDirectory) { "Not a folder: $folderPath" }

        // macOS leaves "._name.mp3" companion files next to copied music; they carry the
        // extension of the actual file but contain no audio. For some reason.
        return root.walkTopDown()
            .onEnter { it == root || !it.name.startsWith(".") }
            .filter { it.isFile && isAudioFileName(it.name) }
            .sortedBy { it.path }
            .map { toSong(it) }
            .toList()
    }

    private fun toSong(file: File): Song {
        val tags = try {
            tagReader.read(file)
        } catch (e: Exception) {
            null
        }
        return buildSong(file.name, file.absolutePath, tags)
    }
}
