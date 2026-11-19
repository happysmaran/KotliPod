package com.smaran.kotlipod.core

interface MenuActions {
    fun chooseMusicFolder()
    fun play(song: Song)

    companion object {
        val None: MenuActions = object : MenuActions {
            override fun chooseMusicFolder() = Unit
            override fun play(song: Song) = Unit
        }
    }
}

// Mirrors the first-generation nano menu hierarchy. Entries without a screen behind
// them yet are leaves, so selecting one does nothing instead of opening an empty list.
object MenuTree {
    // The library and folder label are read when a menu is opened, so a rescan shows up
    // without rebuilding the tree.
    fun build(
        songs: () -> List<Song> = { emptyList() },
        musicFolderLabel: () -> String = { "Select Music Folder" },
        actions: MenuActions = MenuActions.None,
    ): MenuNode = MenuNode.branch("iPod") {
        listOf(
            MenuNode.branch("Music") { musicMenu(songs, actions) },
            MenuNode.leaf("Photos"),
            MenuNode.branch("Extras") { extrasMenu() },
            MenuNode.branch("Settings") { settingsMenu(musicFolderLabel, actions) },
            MenuNode.leaf("Shuffle Songs"),
            MenuNode.leaf("Backlight"),
        )
    }

    private fun musicMenu(songs: () -> List<Song>, actions: MenuActions) = listOf(
        MenuNode.leaf("Playlists"),
        MenuNode.branch("Artists") { artistNodes(songs(), actions) },
        MenuNode.branch("Albums") { albumNodes(songs(), actions) },
        MenuNode.branch("Songs") { songNodes(songs().sortedBy { it.title }, actions) },
        MenuNode.branch("Genres") { genreNodes(songs(), actions) },
    )

    private fun extrasMenu() = listOf(
        MenuNode.leaf("Clock"),
        MenuNode.leaf("Contacts"),
        MenuNode.leaf("Calendar"),
        MenuNode.leaf("Notes"),
        MenuNode.branch("Games") {
            listOf("Brick", "Music Quiz", "Parachute", "Solitaire").map { MenuNode.leaf(it) }
        },
        MenuNode.leaf("Stopwatch"),
        MenuNode.leaf("Screen Lock"),
    )

    private fun settingsMenu(musicFolderLabel: () -> String, actions: MenuActions) =
        listOf(MenuNode.action(musicFolderLabel()) { actions.chooseMusicFolder() }) +
            listOf("About", "Shuffle", "Repeat", "Backlight", "Brightness").map { MenuNode.leaf(it) }

    private fun songNodes(songs: List<Song>, actions: MenuActions) =
        songs.map { song -> MenuNode.action(song.title) { actions.play(song) } }

    private fun artistNodes(songs: List<Song>, actions: MenuActions) =
        songs.groupBy { it.artist }.entries.sortedBy { it.key }.map { (artist, own) ->
            MenuNode.branch(artist) {
                own.groupBy { it.album }.entries.sortedBy { it.key }.map { (album, tracks) ->
                    MenuNode.branch(album) { songNodes(tracks, actions) }
                }
            }
        }

    // Keyed by artist and album because different artists can share an album title.
    private fun albumNodes(songs: List<Song>, actions: MenuActions) =
        songs.groupBy { it.artist to it.album }.entries.sortedBy { it.key.second }.map { (key, tracks) ->
            MenuNode.branch(key.second) { songNodes(tracks, actions) }
        }

    private fun genreNodes(songs: List<Song>, actions: MenuActions) =
        songs.groupBy { it.genre }.entries.sortedBy { it.key }.map { (genre, tracks) ->
            MenuNode.branch(genre) { songNodes(tracks.sortedBy { it.title }, actions) }
        }
}
