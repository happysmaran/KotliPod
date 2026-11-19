package com.smaran.kotlipod.core

import com.smaran.kotlipod.core.audio.AudioLoadException
import com.smaran.kotlipod.core.audio.AudioPlayer
import com.smaran.kotlipod.core.audio.PlaybackState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PodTest {
    private class FakePicker(vararg answers: String?) : FolderPicker {
        private val remaining = answers.toMutableList()
        val requestedStarts = mutableListOf<String?>()

        override fun pickFolder(initialLocation: String?, onResult: (PickedFolder?) -> Unit) {
            requestedStarts += initialLocation
            val answer = if (remaining.isEmpty()) null else remaining.removeAt(0)
            onResult(answer?.let { PickedFolder(it, it.substringAfterLast('/')) })
        }
    }

    private class FakeScanner(private val libraries: Map<String, List<Song>>) : LibraryScanner {
        override fun scan(folderPath: String): List<Song> =
            libraries[folderPath] ?: throw IllegalArgumentException("Not a folder: $folderPath")
    }

    private class FakePlayer : AudioPlayer {
        override var state = PlaybackState.Stopped
        override val positionMs = 0L
        override val durationMs = 0L
        override var onTrackEnded: (() -> Unit)? = null
        var loadedPath: String? = null
        var failLoadsWith: String? = null

        override fun load(path: String) {
            failLoadsWith?.let { throw AudioLoadException(it) }
            loadedPath = path
            state = PlaybackState.Stopped
        }

        override fun play() {
            if (loadedPath == null) throw IllegalStateException("No track loaded")
            state = PlaybackState.Playing
        }

        override fun pause() {
            if (state == PlaybackState.Playing) state = PlaybackState.Paused
        }

        override fun stop() {
            state = PlaybackState.Stopped
        }

        override fun seekTo(positionMs: Long) = Unit
        override fun close() = Unit
    }

    // Holds results until the test releases them, to reproduce a slow scan finishing late.
    private class DeferredRunner : TaskRunner {
        private val pending = mutableListOf<() -> Unit>()

        override fun <T> submit(work: () -> T, onResult: (Result<T>) -> Unit) {
            pending += { onResult(runCatching(work)) }
        }

        fun finish(index: Int) = pending[index]()
    }

    private fun song(title: String, path: String) = Song(title, "Artist", "Album", "Rock", 100, path)

    private val rock = listOf(song("Beta", "/rock/b.mp3"), song("Alpha", "/rock/a.m4a"))
    private val jazz = listOf(song("Coda", "/jazz/c.mp3"))

    private class Harness(
        val picker: FakePicker,
        val player: FakePlayer,
        val reports: MutableList<String>,
        val pod: Pod,
    )

    private fun harness(
        picker: FakePicker,
        libraries: Map<String, List<Song>>,
        runner: TaskRunner = ImmediateTaskRunner,
    ): Harness {
        val player = FakePlayer()
        val reports = mutableListOf<String>()
        val pod = Pod(
            PlatformServices(
                folderPicker = picker,
                libraryScanner = FakeScanner(libraries),
                audioPlayer = player,
                taskRunner = runner,
                report = { reports += it },
            ),
        )
        return Harness(picker, player, reports, pod)
    }

    private fun Pod.openSettings() {
        handle(Input.Scroll(3))
        handle(Input.Select)
    }

    private fun Pod.selectFolderRow() = handle(Input.Select)

    private fun Pod.openSongs() {
        handle(Input.Back)
        handle(Input.Scroll(-10))
        handle(Input.Select)
        handle(Input.Scroll(3))
        handle(Input.Select)
    }

    @Test
    fun settingsOffersToSelectAFolderUntilOneIsChosen() {
        val h = harness(FakePicker(), emptyMap())
        h.pod.openSettings()
        assertEquals("Settings", h.pod.screen.title)
        assertEquals("Select Music Folder", h.pod.screen.rows[0].text)
    }

    @Test
    fun choosingAFolderLoadsItsSongsIntoTheMusicMenus() {
        val h = harness(FakePicker("/music/Rock"), mapOf("/music/Rock" to rock))
        h.pod.openSettings()
        h.pod.selectFolderRow()

        assertEquals("Folder: Rock (2)", h.pod.screen.rows[0].text)
        h.pod.openSongs()
        assertEquals("Songs", h.pod.screen.title)
        assertEquals(listOf("Alpha", "Beta"), h.pod.screen.rows.map { it.text })
    }

    @Test
    fun cancellingThePickerChangesNothing() {
        val h = harness(FakePicker(null), emptyMap())
        h.pod.openSettings()
        h.pod.selectFolderRow()
        assertEquals("Select Music Folder", h.pod.screen.rows[0].text)
    }

    @Test
    fun theDialogOpensAtTheCurrentFolder() {
        val h = harness(FakePicker("/music/Rock", "/music/Jazz"), mapOf("/music/Rock" to rock, "/music/Jazz" to jazz))
        h.pod.openSettings()
        h.pod.selectFolderRow()
        h.pod.selectFolderRow()
        assertEquals(listOf(null, "/music/Rock"), h.picker.requestedStarts)
        assertEquals("Folder: Jazz (1)", h.pod.screen.rows[0].text)
    }

    @Test
    fun selectingASongLoadsItsFileAndStartsPlayback() {
        val h = harness(FakePicker("/music/Rock"), mapOf("/music/Rock" to rock))
        h.pod.openSettings()
        h.pod.selectFolderRow()
        h.pod.openSongs()
        h.pod.handle(Input.Select)

        assertEquals("/rock/a.m4a", h.player.loadedPath)
        assertEquals(PlaybackState.Playing, h.player.state)
    }

    @Test
    fun playPauseTogglesPlayback() {
        val h = harness(FakePicker("/music/Rock"), mapOf("/music/Rock" to rock))
        h.pod.openSettings()
        h.pod.selectFolderRow()
        h.pod.openSongs()
        h.pod.handle(Input.Select)

        h.pod.handle(Input.PlayPause)
        assertEquals(PlaybackState.Paused, h.player.state)
        h.pod.handle(Input.PlayPause)
        assertEquals(PlaybackState.Playing, h.player.state)
    }

    @Test
    fun playPauseBeforeAnySongIsChosenIsHarmless() {
        val h = harness(FakePicker(), emptyMap())
        h.pod.handle(Input.PlayPause)
        assertEquals(PlaybackState.Stopped, h.player.state)
    }

    @Test
    fun aSongThatCannotBeDecodedIsReportedAndNotPlayed() {
        val h = harness(FakePicker("/music/Rock"), mapOf("/music/Rock" to rock))
        h.player.failLoadsWith = "Unsupported audio format"
        h.pod.openSettings()
        h.pod.selectFolderRow()
        h.pod.openSongs()
        h.pod.handle(Input.Select)

        assertEquals(PlaybackState.Stopped, h.player.state)
        assertEquals(1, h.reports.size)
        assertTrue(h.reports[0].contains("Alpha") && h.reports[0].contains("Unsupported audio format"))
    }

    @Test
    fun aFolderThatCannotBeScannedIsReportedAndLeavesTheLibraryEmpty() {
        val h = harness(FakePicker("/missing"), emptyMap())
        h.pod.openSettings()
        h.pod.selectFolderRow()
        assertEquals("Folder: missing (0)", h.pod.screen.rows[0].text)
        assertEquals(1, h.reports.size)
    }

    @Test
    fun anOlderScanFinishingLateDoesNotOverwriteANewerOne() {
        val runner = DeferredRunner()
        val h = harness(
            FakePicker("/music/Rock", "/music/Jazz"),
            mapOf("/music/Rock" to rock, "/music/Jazz" to jazz),
            runner,
        )
        h.pod.openSettings()
        h.pod.selectFolderRow()
        assertEquals("Scanning…", h.pod.screen.rows[0].text)
        h.pod.selectFolderRow()

        runner.finish(1)
        runner.finish(0)
        assertEquals("Folder: Jazz (1)", h.pod.screen.rows[0].text)
    }

    @Test
    fun theRendererIsNotifiedWhenAScanCompletes() {
        val runner = DeferredRunner()
        val h = harness(FakePicker("/music/Rock"), mapOf("/music/Rock" to rock), runner)
        var notifications = 0
        h.pod.onChanged = { notifications++ }
        h.pod.openSettings()
        h.pod.selectFolderRow()
        val before = notifications
        runner.finish(0)
        assertEquals(before + 1, notifications)
    }
}
