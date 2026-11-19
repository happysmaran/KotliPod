package com.smaran.kotlipod.core

import com.smaran.kotlipod.core.audio.AudioLoadException
import com.smaran.kotlipod.core.audio.AudioPlayer
import com.smaran.kotlipod.core.audio.PlaybackState

// `location` is whatever LibraryScanner understands on that platform (a path on desktop and iOS,
// a document-tree URI on Android); `name` is what the screen shows, since URIs are unreadable.
class PickedFolder(val location: String, val name: String)

interface FolderPicker {
    // Mobile pickers are separate screens that answer separately, so the result arrives by callback on the
    // UI thread; null means the user cancelled.
    fun pickFolder(initialLocation: String?, onResult: (PickedFolder?) -> Unit)
}

interface LibraryScanner {
    // Reads every playable file below the folder; slow on large libraries, so callers run it in the background.
    fun scan(folderPath: String): List<Song>
}

interface TaskRunner {
    fun <T> submit(work: () -> T, onResult: (Result<T>) -> Unit)
}

class PlatformServices(
    val folderPicker: FolderPicker,
    val libraryScanner: LibraryScanner,
    val audioPlayer: AudioPlayer,
    val taskRunner: TaskRunner,
    val report: (String) -> Unit = { println(it) },
) {
    companion object {
        val Unavailable = PlatformServices(
            folderPicker = UnavailableFolderPicker,
            libraryScanner = UnavailableLibraryScanner,
            audioPlayer = UnavailableAudioPlayer,
            taskRunner = ImmediateTaskRunner,
        )
    }
}

object ImmediateTaskRunner : TaskRunner {
    override fun <T> submit(work: () -> T, onResult: (Result<T>) -> Unit) {
        onResult(runCatching(work))
    }
}

private object UnavailableFolderPicker : FolderPicker {
    override fun pickFolder(initialLocation: String?, onResult: (PickedFolder?) -> Unit) = onResult(null)
}

private object UnavailableLibraryScanner : LibraryScanner {
    override fun scan(folderPath: String): List<Song> = emptyList()
}

private object UnavailableAudioPlayer : AudioPlayer {
    override val state = PlaybackState.Stopped
    override val positionMs = 0L
    override val durationMs = 0L
    override var onTrackEnded: (() -> Unit)? = null

    override fun load(path: String) = throw AudioLoadException("Audio playback is not available on this platform")
    override fun play() = throw IllegalStateException("No track loaded")
    override fun pause() = Unit
    override fun stop() = Unit
    override fun seekTo(positionMs: Long) = Unit
    override fun close() = Unit
}
