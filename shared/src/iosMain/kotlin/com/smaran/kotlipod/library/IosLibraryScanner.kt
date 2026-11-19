package com.smaran.kotlipod.library

import com.smaran.kotlipod.core.LibraryScanner
import com.smaran.kotlipod.core.Song
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVMetadataCommonKeyAlbumName
import platform.AVFoundation.AVMetadataCommonKeyArtist
import platform.AVFoundation.AVMetadataCommonKeyTitle
import platform.AVFoundation.AVMetadataItem
import platform.AVFoundation.AVURLAsset
import platform.CoreMedia.CMTimeGetSeconds
import platform.Foundation.NSDirectoryEnumerationSkipsHiddenFiles
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL

// Works on the folder URL the picker granted; tag reading uses AVFoundation so no third-party
// library is needed on iOS.
@OptIn(ExperimentalForeignApi::class)
class IosLibraryScanner : LibraryScanner {
    override fun scan(folderPath: String): List<Song> {
        val root = NSURL.fileURLWithPath(folderPath, isDirectory = true)
        val enumerator = NSFileManager.defaultManager.enumeratorAtURL(
            root,
            includingPropertiesForKeys = null,
            options = NSDirectoryEnumerationSkipsHiddenFiles,
            errorHandler = null,
        ) ?: throw IllegalArgumentException("Cannot read folder")

        val files = mutableListOf<NSURL>()
        while (true) {
            val item = enumerator.nextObject() as? NSURL ?: break
            val name = item.lastPathComponent ?: continue
            if (isAudioFileName(name)) files += item
        }

        return files
            .sortedBy { it.path ?: "" }
            .map { url -> buildSong(url.lastPathComponent ?: "", url.path ?: "", readTags(url)) }
    }

    private fun readTags(url: NSURL): SongTags? = try {
        val asset = AVURLAsset(url, null)
        var title: String? = null
        var artist: String? = null
        var album: String? = null
        asset.commonMetadata.forEach { entry ->
            val item = entry as? AVMetadataItem ?: return@forEach
            when (item.commonKey) {
                AVMetadataCommonKeyTitle -> title = item.stringValue
                AVMetadataCommonKeyArtist -> artist = item.stringValue
                AVMetadataCommonKeyAlbumName -> album = item.stringValue
            }
        }
        val seconds = CMTimeGetSeconds(asset.duration)
        // AVFoundation has no common-key genre, so iOS libraries list under "Unknown" genre.
        SongTags(title, artist, album, null, if (seconds.isFinite() && seconds > 0) seconds.toInt() else null)
    } catch (e: Throwable) {
        null
    }
}
