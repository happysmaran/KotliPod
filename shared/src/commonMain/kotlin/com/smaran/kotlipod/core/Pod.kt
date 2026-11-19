package com.smaran.kotlipod.core

import com.smaran.kotlipod.core.audio.PlaybackState

// The device as a whole
class Pod(private val services: PlatformServices) : MenuActions {
    // Renderer redraws wihle something finishes in the background (a scan, a track loading).
    var onChanged: () -> Unit = {}

    private var songs: List<Song> = emptyList()
    private var folder: PickedFolder? = null
    private var scanning = false

    // Choosing a second folder or song before the first finishes must not let the slower,
    // older result overwrite the newer one.
    private var scanGeneration = 0
    private var playGeneration = 0

    private val navigator = Navigator(
        MenuTree.build(songs = { songs }, musicFolderLabel = { folderLabel() }, actions = this),
    )

    val screen: ListScreen get() = navigator.screen

    fun handle(input: Input) {
        if (input == Input.PlayPause) togglePlayback() else navigator.handle(input)
        onChanged()
    }

    override fun chooseMusicFolder() {
        services.folderPicker.pickFolder(folder?.location) { picked ->
            if (picked != null) loadFolder(picked)
        }
    }

    private fun loadFolder(picked: PickedFolder) {
        folder = picked
        scanning = true
        val generation = ++scanGeneration
        navigator.refresh()
        onChanged()

        services.taskRunner.submit({ services.libraryScanner.scan(picked.location) }) { result ->
            if (generation != scanGeneration) return@submit
            scanning = false
            result
                .onSuccess { songs = it }
                .onFailure {
                    songs = emptyList()
                    services.report("Could not read ${picked.name}: ${it.message}")
                }
            navigator.refresh()
            onChanged()
        }
    }

    override fun play(song: Song) {
        val generation = ++playGeneration
        services.taskRunner.submit({ services.audioPlayer.load(song.path) }) { result ->
            if (generation != playGeneration) return@submit
            result
                .onSuccess { services.audioPlayer.play() }
                .onFailure { services.report("Cannot play ${song.title}: ${it.message}") }
            onChanged()
        }
    }

    private fun togglePlayback() {
        val player = services.audioPlayer
        if (player.state == PlaybackState.Playing) {
            player.pause()
        } else {
            try {
                player.play()
            } catch (e: IllegalStateException) {
                // Nothing is loaded until the first song is chosen, so there is nothing to resume.
            }
        }
    }

    private fun folderLabel(): String {
        val current = folder
        return when {
            current == null -> "Select Music Folder"
            scanning -> "Scanning…"
            else -> "Folder: ${current.name} (${songs.size})"
        }
    }
}
