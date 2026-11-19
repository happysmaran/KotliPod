package com.smaran.kotlipod.core.audio

enum class PlaybackState { Stopped, Playing, Paused }

class AudioLoadException(message: String, cause: Throwable? = null) : Exception(message, cause)

// Playback differs per platform (JavaSound on desktop, MediaPlayer on Android, AVFoundation on iOS),
// so the pod's logic talks to this interface and never to a platform audio API.
interface AudioPlayer {
    val state: PlaybackState
    val positionMs: Long
    val durationMs: Long

    // Invoked from an audio thread when a track plays to its end, not when it is stopped by the user.
    var onTrackEnded: (() -> Unit)?

    // Replaces any previously loaded track; throws AudioLoadException if the file cannot be played.
    // Decoding can take seconds for a long track, so callers keep this off the UI thread.
    fun load(path: String)

    // Throws IllegalStateException when no track has been loaded.
    fun play()
    fun pause()
    fun stop()
    fun seekTo(positionMs: Long)

    fun close()
}
