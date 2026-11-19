package com.smaran.kotlipod.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import com.smaran.kotlipod.core.audio.AudioLoadException
import com.smaran.kotlipod.core.audio.AudioPlayer
import com.smaran.kotlipod.core.audio.PlaybackState

// MediaPlayer decodes mp3 and m4a through the platform codecs and reads document URIs directly,
// so unlike desktop there is no decoder chain and no copy of the file is made.
class MediaPlayerAudioPlayer(private val context: Context) : AudioPlayer {
    private var current: MediaPlayer? = null
    private var paused = false

    override var onTrackEnded: (() -> Unit)? = null

    @get:Synchronized
    override val state: PlaybackState
        get() {
            val player = current ?: return PlaybackState.Stopped
            return when {
                player.isPlaying -> PlaybackState.Playing
                paused -> PlaybackState.Paused
                else -> PlaybackState.Stopped
            }
        }

    override val positionMs: Long
        @Synchronized get() = current?.currentPosition?.toLong() ?: 0L

    override val durationMs: Long
        @Synchronized get() = current?.duration?.toLong()?.coerceAtLeast(0L) ?: 0L

    // Runs on a worker thread; prepare() blocks while the file is opened and its header parsed.
    override fun load(path: String) {
        val next = MediaPlayer()
        try {
            next.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            next.setDataSource(context, if (path.startsWith("/")) Uri.fromFile(java.io.File(path)) else Uri.parse(path))
            next.prepare()
        } catch (e: Exception) {
            next.release()
            throw AudioLoadException("Cannot open audio: ${e.message}", e)
        }
        next.setOnCompletionListener {
            synchronized(this) { paused = false }
            onTrackEnded?.invoke()
        }
        next.setOnErrorListener { _, _, _ -> true }

        val previous: MediaPlayer?
        synchronized(this) {
            previous = current
            current = next
            paused = false
        }
        previous?.release()
    }

    @Synchronized
    override fun play() {
        val player = current ?: throw IllegalStateException("No track loaded")
        player.start()
        paused = false
    }

    @Synchronized
    override fun pause() {
        val player = current ?: return
        if (player.isPlaying) {
            player.pause()
            paused = true
        }
    }

    // MediaPlayer.stop() would force a re-prepare, so stopping rewinds instead; play() then restarts the track.
    @Synchronized
    override fun stop() {
        val player = current ?: return
        if (player.isPlaying) player.pause()
        player.seekTo(0)
        paused = false
    }

    @Synchronized
    override fun seekTo(positionMs: Long) {
        current?.seekTo(positionMs.toInt())
    }

    @Synchronized
    override fun close() {
        current?.release()
        current = null
        paused = false
    }
}
