package com.smaran.kotlipod.library

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LibraryRulesTest {
    @Test
    fun recognisesSupportedExtensionsCaseInsensitively() {
        assertTrue(isAudioFileName("a.mp3"))
        assertTrue(isAudioFileName("b.M4A"))
        assertTrue(isAudioFileName("c.aiff"))
        assertFalse(isAudioFileName("cover.jpg"))
        assertFalse(isAudioFileName("mp3"))
    }

    @Test
    fun ignoresMacCompanionFiles() {
        assertFalse(isAudioFileName("._a.mp3"))
    }

    @Test
    fun missingTagsFallBackToTheFileName() {
        val song = buildSong("My Song.mp3", "/m/My Song.mp3", null)
        assertEquals("My Song", song.title)
        assertEquals("Unknown Artist", song.artist)
        assertEquals("/m/My Song.mp3", song.path)
    }

    @Test
    fun blankTagFieldsFallBackIndividually() {
        val song = buildSong("x.m4a", "/x.m4a", SongTags("T", " ", null, "Jazz", 90))
        assertEquals("T", song.title)
        assertEquals("Unknown Artist", song.artist)
        assertEquals("Jazz", song.genre)
        assertEquals(90, song.durationSeconds)
    }
}
