package com.smaran.kotlipod.library

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.DocumentsContract
import com.smaran.kotlipod.core.LibraryScanner
import com.smaran.kotlipod.core.Song

// Reads the folder the user granted through the system picker. DocumentsContract queries are used
// instead of DocumentFile because a library of thousands of files needs one query per folder,
// not one provider round-trip per file.
class DocumentTreeScanner(private val context: Context) : LibraryScanner {
    private class Entry(val path: String, val name: String, val uri: Uri)

    override fun scan(folderPath: String): List<Song> {
        val tree = Uri.parse(folderPath)
        val entries = mutableListOf<Entry>()
        collect(tree, DocumentsContract.getTreeDocumentId(tree), "", entries)
        return entries.sortedBy { it.path }.map { entry ->
            buildSong(entry.name, entry.uri.toString(), readTags(entry.uri))
        }
    }

    private fun collect(tree: Uri, parentId: String, parentPath: String, out: MutableList<Entry>) {
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, parentId)
        val columns = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
        )
        val folders = mutableListOf<Pair<String, String>>()

        val cursor = context.contentResolver.query(children, columns, null, null, null)
            ?: throw IllegalArgumentException("Cannot read folder")
        cursor.use {
            while (it.moveToNext()) {
                val id = it.getString(0)
                val name = it.getString(1) ?: continue
                if (name.startsWith(".")) continue
                val path = "$parentPath/$name"
                if (it.getString(2) == DocumentsContract.Document.MIME_TYPE_DIR) {
                    folders += id to path
                } else if (isAudioFileName(name)) {
                    out += Entry(path, name, DocumentsContract.buildDocumentUriUsingTree(tree, id))
                }
            }
        }
        // Recursing after the cursor closes keeps at most one provider query open at a time.
        folders.forEach { (id, path) -> collect(tree, id, path, out) }
    }

    private fun readTags(uri: Uri): SongTags? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            SongTags(
                title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE),
                artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST),
                album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM),
                genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE),
                durationSeconds = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull()?.let { (it / 1000).toInt() },
            )
        } catch (e: Exception) {
            null
        } finally {
            retriever.release()
        }
    }
}
