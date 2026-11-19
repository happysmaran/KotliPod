package com.smaran.kotlipod.audio

import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PcmDecoderTest {
    private val workDirs = mutableListOf<File>()

    @AfterTest
    fun cleanUp() = workDirs.forEach { it.deleteRecursively() }

    private fun musicFolderWith(fixture: String): File {
        val folder = Files.createTempDirectory("kotlipod-music").toFile().also { workDirs += it }
        val file = File(folder, fixture)
        checkNotNull(javaClass.getResourceAsStream("/audio/$fixture")) { "missing fixture $fixture" }.use { input ->
            file.outputStream().use { input.copyTo(it) }
        }
        return file
    }

    private fun fingerprint(folder: File): List<String> =
        folder.walkTopDown().sortedBy { it.path }.map { entry ->
            val digest = if (entry.isFile) {
                MessageDigest.getInstance("SHA-256").digest(entry.readBytes()).joinToString("") { "%02x".format(it) }
            } else {
                "dir"
            }
            "${entry.relativeTo(folder).path}|${entry.length()}|${entry.lastModified()}|$digest"
        }.toList()

    private fun assertAboutOneSecond(decoded: DecodedAudio) {
        val frames = decoded.bytes.size / decoded.format.frameSize
        val seconds = frames / decoded.format.sampleRate
        assertTrue(seconds in 0.9f..1.2f, "decoded $seconds seconds")
        assertEquals(16, decoded.format.sampleSizeInBits)
    }

    @Test
    fun mp3DecodesToPcm() {
        assertAboutOneSecond(decodeToPcm(musicFolderWith("silence.mp3")))
    }

    @Test
    fun m4aDecodesToPcm() {
        assertAboutOneSecond(decodeToPcm(musicFolderWith("silence.m4a")))
    }

    @Test
    fun decodingMp3LeavesTheSourceFolderUntouched() {
        val file = musicFolderWith("silence.mp3")
        val before = fingerprint(file.parentFile)
        decodeToPcm(file)
        assertEquals(before, fingerprint(file.parentFile))
    }

    @Test
    fun decodingM4aLeavesTheSourceFolderUntouched() {
        val file = musicFolderWith("silence.m4a")
        val before = fingerprint(file.parentFile)
        decodeToPcm(file)
        assertEquals(before, fingerprint(file.parentFile))
    }
}