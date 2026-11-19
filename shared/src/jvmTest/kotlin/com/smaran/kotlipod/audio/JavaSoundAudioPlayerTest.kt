package com.smaran.kotlipod.audio

import com.smaran.kotlipod.core.audio.AudioLoadException
import com.smaran.kotlipod.core.audio.PlaybackState
import java.io.ByteArrayInputStream
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.LineUnavailableException
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class JavaSoundAudioPlayerTest {
    private val tempFiles = mutableListOf<File>()

    @AfterTest
    fun cleanUp() = tempFiles.forEach { it.delete() }

    private fun silentWav(milliseconds: Int, channels: Int = 1): File {
        val format = AudioFormat(44100f, 16, channels, true, false)
        val frames = 44100 * milliseconds / 1000
        val bytes = ByteArray(frames * format.frameSize)
        val file = File.createTempFile("kotlipod-test", ".wav").also { tempFiles += it }
        AudioInputStream(ByteArrayInputStream(bytes), format, frames.toLong()).use {
            AudioSystem.write(it, AudioFileFormat.Type.WAVE, file)
        }
        return file
    }

    private fun playerOrNull(file: File): JavaSoundAudioPlayer? {
        val player = JavaSoundAudioPlayer()
        return try {
            player.load(file.path)
            player
        } catch (e: AudioLoadException) {
            if (e.cause is LineUnavailableException || e.cause is IllegalArgumentException) null else throw e
        }
    }

    @Test
    fun decodingKeepsEveryFrame() {
        val decoded = decodeToPcm(silentWav(500))
        assertEquals(22050 * 2, decoded.bytes.size)
        assertEquals(16, decoded.format.sampleSizeInBits)
    }

    @Test
    fun decodingPreservesChannelCount() {
        val decoded = decodeToPcm(silentWav(250, channels = 2))
        assertEquals(2, decoded.format.channels)
        assertEquals(11025 * 4, decoded.bytes.size)
    }

    @Test
    fun missingFileIsReportedAsLoadFailure() {
        assertFailsWith<AudioLoadException> { decodeToPcm(File("does-not-exist.wav")) }
    }

    @Test
    fun nonAudioFileIsReportedAsUnsupported() {
        val text = File.createTempFile("kotlipod-test", ".txt").also { tempFiles += it }
        text.writeText("not audio")
        assertFailsWith<AudioLoadException> { decodeToPcm(text) }
    }

    @Test
    fun playingWithoutATrackIsAnError() {
        assertFailsWith<IllegalStateException> { JavaSoundAudioPlayer().play() }
    }

    @Test
    fun reportsDurationAndTransitionsBetweenStates() {
        val player = playerOrNull(silentWav(2000)) ?: return
        try {
            assertEquals(PlaybackState.Stopped, player.state)
            assertTrue(player.durationMs in 1990L..2010L, "duration=${player.durationMs}")

            player.play()
            assertEquals(PlaybackState.Playing, player.state)
            player.pause()
            assertEquals(PlaybackState.Paused, player.state)

            val pausedAt = player.positionMs
            Thread.sleep(150)
            assertEquals(pausedAt, player.positionMs)

            player.stop()
            assertEquals(PlaybackState.Stopped, player.state)
            assertEquals(0L, player.positionMs)
        } finally {
            player.close()
        }
    }

    @Test
    fun seekingMovesThePosition() {
        val player = playerOrNull(silentWav(2000)) ?: return
        try {
            player.seekTo(1000)
            assertTrue(player.positionMs in 990L..1010L, "position=${player.positionMs}")
            player.seekTo(99_999)
            assertTrue(player.positionMs <= player.durationMs)
            player.seekTo(-5)
            assertEquals(0L, player.positionMs)
        } finally {
            player.close()
        }
    }

    @Test
    fun finishingATrackNotifiesAndRewinds() {
        val player = playerOrNull(silentWav(300)) ?: return
        val finished = CountDownLatch(1)
        player.onTrackEnded = { finished.countDown() }
        try {
            player.play()
            assertTrue(finished.await(5, TimeUnit.SECONDS), "track end was never reported")
            assertEquals(PlaybackState.Stopped, player.state)
            assertEquals(0L, player.positionMs)
        } finally {
            player.close()
        }
    }

    @Test
    fun stoppingByHandDoesNotReportTrackEnd() {
        val player = playerOrNull(silentWav(2000)) ?: return
        var ended = false
        player.onTrackEnded = { ended = true }
        try {
            player.play()
            player.stop()
            Thread.sleep(200)
            assertEquals(false, ended)
        } finally {
            player.close()
        }
    }
}
