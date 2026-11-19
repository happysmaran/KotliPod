package com.smaran.kotlipod.audio

import com.smaran.kotlipod.core.audio.AudioLoadException
import com.smaran.kotlipod.core.audio.AudioPlayer
import com.smaran.kotlipod.core.audio.PlaybackState
import java.io.File
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.LineEvent
import javax.sound.sampled.LineUnavailableException

class JavaSoundAudioPlayer : AudioPlayer {
    private var clip: Clip? = null

    @Volatile
    override var state: PlaybackState = PlaybackState.Stopped
        private set

    override var onTrackEnded: (() -> Unit)? = null

    override val positionMs: Long
        get() = (clip?.microsecondPosition ?: 0L) / 1000

    override val durationMs: Long
        get() = (clip?.microsecondLength ?: 0L) / 1000

    override fun load(path: String) {
        release()
        val decoded = decodeToPcm(File(path))
        val newClip = try {
            AudioSystem.getClip().also { it.open(decoded.format, decoded.bytes, 0, decoded.bytes.size) }
        } catch (e: LineUnavailableException) {
            throw AudioLoadException("No audio output available", e)
        } catch (e: IllegalArgumentException) {
            throw AudioLoadException("No audio output supports this format", e)
        }
        newClip.addLineListener { event -> if (event.type == LineEvent.Type.STOP) onLineStopped(newClip) }
        clip = newClip
    }

    override fun play() {
        val current = requireClip()
        if (state == PlaybackState.Playing) return
        if (current.getFramePosition() >= current.frameLength) current.setFramePosition(0)
        state = PlaybackState.Playing
        current.start()
    }

    override fun pause() {
        val current = clip ?: return
        if (state != PlaybackState.Playing) return
        // State changes before the line stops so the stop event is not mistaken for the track ending.
        state = PlaybackState.Paused
        current.stop()
    }

    override fun stop() {
        val current = clip ?: return
        state = PlaybackState.Stopped
        current.stop()
        current.setFramePosition(0)
    }

    override fun seekTo(positionMs: Long) {
        val current = clip ?: return
        current.setMicrosecondPosition(positionMs.coerceIn(0L, durationMs) * 1000)
    }

    override fun close() = release()

    private fun onLineStopped(stopped: Clip) {
        // A stop event can arrive after the user has already resumed, so choke it
        if (stopped !== clip || state != PlaybackState.Playing || stopped.isRunning) return
        state = PlaybackState.Stopped
        stopped.setFramePosition(0)
        onTrackEnded?.invoke()
    }

    private fun requireClip(): Clip = clip ?: throw IllegalStateException("No track loaded")

    private fun release() {
        val current = clip ?: return
        clip = null
        state = PlaybackState.Stopped
        current.stop()
        current.close()
    }
}
