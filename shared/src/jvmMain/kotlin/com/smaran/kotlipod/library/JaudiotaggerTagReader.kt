package com.smaran.kotlipod.library

import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.io.File
import java.util.logging.Level
import java.util.logging.Logger

class JaudiotaggerTagReader : TagReader {
    // jaudiotagger reports every oddity through java.util.logging, which buries the console when
    // scanning a large library.
    private val logger = Logger.getLogger("org.jaudiotagger").also { it.level = Level.OFF }

    override fun read(file: File): SongTags {
        val audio = AudioFileIO.read(file)
        val tag = audio.tag
        return SongTags(
            title = tag?.getFirst(FieldKey.TITLE),
            artist = tag?.getFirst(FieldKey.ARTIST),
            album = tag?.getFirst(FieldKey.ALBUM),
            genre = tag?.getFirst(FieldKey.GENRE),
            durationSeconds = audio.audioHeader.trackLength,
        )
    }
}