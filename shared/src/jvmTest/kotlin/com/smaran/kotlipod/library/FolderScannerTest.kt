package com.smaran.kotlipod.library

import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FolderScannerTest {
    private val folders = mutableListOf<File>()

    @AfterTest
    fun cleanUp() = folders.forEach { it.deleteRecursively() }

    private fun library(vararg relativePaths: String): File {
        val root = Files.createTempDirectory("kotlipod-scan").toFile().also { folders += it }
        relativePaths.forEach { path ->
            File(root, path).apply {
                parentFile.mkdirs()
                writeText("x")
            }
        }
        return root
    }

    private val noTags = TagReader { null }

    @Test
    fun findsSupportedFilesInSubfoldersAndSkipsEverythingElse() {
        val root = library("a.mp3", "b.M4A", "notes.txt", "sub/c.wav", "sub/deeper/d.aiff", "cover.jpg")
        val titles = FolderScanner(noTags).scan(root.path).map { it.title }
        assertEquals(listOf("a", "b", "c", "d"), titles)
    }

    @Test
    fun skipsHiddenFilesAndFolders() {
        val root = library("a.mp3", "._a.mp3", ".Trashes/b.mp3", "sub/.hidden.mp3")
        assertEquals(listOf("a"), FolderScanner(noTags).scan(root.path).map { it.title })
    }

    @Test
    fun fallsBackToTheFileNameWhenTagsAreMissing() {
        val root = library("Some Song.mp3")
        val song = FolderScanner(noTags).scan(root.path).single()
        assertEquals("Some Song", song.title)
        assertEquals("Unknown Artist", song.artist)
        assertEquals("Unknown Album", song.album)
        assertEquals("Unknown", song.genre)
        assertEquals(File(root, "Some Song.mp3").absolutePath, song.path)
    }

    @Test
    fun usesTagsWhenPresentAndFallsBackPerField() {
        val root = library("x.m4a")
        val reader = TagReader { SongTags("Real Title", "  ", "Real Album", null, 215) }
        val song = FolderScanner(reader).scan(root.path).single()
        assertEquals("Real Title", song.title)
        assertEquals("Unknown Artist", song.artist)
        assertEquals("Real Album", song.album)
        assertEquals(215, song.durationSeconds)
    }

    @Test
    fun aFileWhoseTagsCannotBeReadIsStillListed() {
        val root = library("broken.mp3")
        val reader = TagReader { throw IllegalStateException("corrupt tag") }
        assertEquals(listOf("broken"), FolderScanner(reader).scan(root.path).map { it.title })
    }

    @Test
    fun scanningNeverModifiesTheFolder() {
        val root = library("a.mp3", "sub/b.m4a")
        fun snap() = root.walkTopDown().map { "${it.path}|${it.length()}|${it.lastModified()}" }.toList()
        val before = snap()
        FolderScanner(noTags).scan(root.path)
        assertEquals(before, snap())
    }

    @Test
    fun aMissingFolderIsAnError() {
        assertFailsWith<IllegalArgumentException> { FolderScanner(noTags).scan("/definitely/not/here") }
    }
}