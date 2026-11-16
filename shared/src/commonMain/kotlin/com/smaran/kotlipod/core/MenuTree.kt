package com.smaran.kotlipod.core

// Mirrors the first-generation nano menu hierarchy. Entries without a screen behind
// them yet are leaves, so selecting one does nothing instead of opening an empty list.
object MenuTree {
    fun build(songs: List<Song>): MenuNode = MenuNode.branch("iPod") {
        listOf(
            MenuNode.branch("Music") { musicMenu(songs) },
            MenuNode.leaf("Photos"),
            MenuNode.branch("Extras") { extrasMenu() },
            MenuNode.branch("Settings") { settingsMenu() },
            MenuNode.leaf("Shuffle Songs"),
            MenuNode.leaf("Backlight"),
        )
    }

    private fun musicMenu(songs: List<Song>) = listOf(
        MenuNode.leaf("Playlists"),
        MenuNode.branch("Artists") { artistNodes(songs) },
        MenuNode.branch("Albums") { albumNodes(songs) },
        MenuNode.branch("Songs") { songNodes(songs.sortedBy { it.title }) },
        MenuNode.branch("Genres") { genreNodes(songs) },
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

    private fun settingsMenu() =
        listOf("About", "Shuffle", "Repeat", "Backlight", "Brightness").map { MenuNode.leaf(it) }

    private fun songNodes(songs: List<Song>) = songs.map { MenuNode.leaf(it.title) }

    private fun artistNodes(songs: List<Song>) =
        songs.groupBy { it.artist }.entries.sortedBy { it.key }.map { (artist, own) ->
            MenuNode.branch(artist) {
                own.groupBy { it.album }.entries.sortedBy { it.key }.map { (album, tracks) ->
                    MenuNode.branch(album) { songNodes(tracks) }
                }
            }
        }

    // Keyed by artist and album because different artists can share the same album title.
    private fun albumNodes(songs: List<Song>) =
        songs.groupBy { it.artist to it.album }.entries.sortedBy { it.key.second }.map { (key, tracks) ->
            MenuNode.branch(key.second) { songNodes(tracks) }
        }

    private fun genreNodes(songs: List<Song>) =
        songs.groupBy { it.genre }.entries.sortedBy { it.key }.map { (genre, tracks) ->
            MenuNode.branch(genre) { songNodes(tracks.sortedBy { it.title }) }
        }
}