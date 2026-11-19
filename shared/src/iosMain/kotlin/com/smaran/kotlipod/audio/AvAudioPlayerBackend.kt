package com.smaran.kotlipod.audio

import com.smaran.kotlipod.core.audio.AudioLoadException
import com.smaran.kotlipod.core.audio.AudioPlayer
import com.smaran.kotlipod.core.audio.PlaybackState
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.AVFAudio.AVAudioPlayer
import platform.AVFAudio.AVAudioPlayerDelegateProtocol
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.setActive
import platform.Foundation.NSError
import platform.Foundation.NSURL
import platform.darwin.NSObject

// AVAudioPlayer decodes mp3 and m4a natively and streams from the file, so the original is only read.
@OptIn(ExperimentalForeignApi::class)
class AvAudioPlayerBackend : AudioPlayer {
    private var player: AVAudioPlayer? = null
    private var paused = false
    private var sessionReady = false

    override var onTrackEnded: (() -> Unit)? = null

    // AVAudioPlayer holds its delegate weakly.
    private val finishDelegate = FinishDelegate {
        paused = false
        onTrackEnded?.invoke()
    }

    override val state: PlaybackState
        get() {
            val current = player ?: return PlaybackState.Stopped
            return when {
                current.playing -> PlaybackState.Playing
                paused -> PlaybackState.Paused
                else -> PlaybackState.Stopped
            }
        }

    override val positionMs: Long get() = ((player?.currentTime ?: 0.0) * 1000).toLong()
    override val durationMs: Long get() = ((player?.duration ?: 0.0) * 1000).toLong()

    override fun load(path: String) {
        configureSession()
        val url = NSURL.fileURLWithPath(path)
        val created = memScoped {
            val error = alloc<ObjCObjectVar<NSError?>>()
            try {
                AVAudioPlayer(contentsOfURL = url, error = error.ptr)
            } catch (e: Throwable) {
                throw AudioLoadException(error.value?.localizedDescription ?: "Cannot open audio", e)
            }
        }
        created.delegate = finishDelegate
        if (!created.prepareToPlay()) throw AudioLoadException("Cannot prepare audio for playback")
        player?.stop()
        player = created
        paused = false
    }

    override fun play() {
        val current = player ?: throw IllegalStateException("No track loaded")
        current.play()
        paused = false
    }

    override fun pause() {
        val current = player ?: return
        if (current.playing) {
            current.pause()
            paused = true
        }
    }

    override fun stop() {
        val current = player ?: return
        current.stop()
        current.currentTime = 0.0
        paused = false
    }

    override fun seekTo(positionMs: Long) {
        player?.currentTime = positionMs / 1000.0
    }

    override fun close() {
        player?.stop()
        player = null
        paused = false
    }

    // The playback category makes sound audible with the ring/silent switch on, which is how a music player behaves.
    private fun configureSession() {
        if (sessionReady) return
        val session = AVAudioSession.sharedInstance()
        session.setCategory(AVAudioSessionCategoryPlayback, null)
        session.setActive(true, null)
        sessionReady = true
    }

    private class FinishDelegate(private val onFinished: () -> Unit) : NSObject(), AVAudioPlayerDelegateProtocol {
        override fun audioPlayerDidFinishPlaying(player: AVAudioPlayer, successfully: Boolean) {
            onFinished()
        }
    }
}
