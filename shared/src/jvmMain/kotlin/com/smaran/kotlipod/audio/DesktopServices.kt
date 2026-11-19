package com.smaran.kotlipod.audio

import com.smaran.kotlipod.core.FolderPicker
import com.smaran.kotlipod.core.PickedFolder
import com.smaran.kotlipod.core.PlatformServices
import com.smaran.kotlipod.core.TaskRunner
import com.smaran.kotlipod.library.FolderScanner
import com.smaran.kotlipod.library.JaudiotaggerTagReader
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.util.concurrent.Executors
import javax.swing.JFileChooser
import javax.swing.SwingUtilities

fun jvmPlatformServices() = PlatformServices(
    folderPicker = SwingFolderPicker(),
    libraryScanner = FolderScanner(JaudiotaggerTagReader()),
    audioPlayer = JavaSoundAudioPlayer(),
    taskRunner = ThreadTaskRunner(),
    report = { message -> System.err.println(message) },
)

// Work runs on daemon threads so a long scan never keeps the app alive after its window closes,
// and results return on the Swing thread.
class ThreadTaskRunner : TaskRunner {
    private val executor = Executors.newCachedThreadPool { runnable ->
        Thread(runnable, "kotlipod-worker").apply { isDaemon = true }
    }

    override fun <T> submit(work: () -> T, onResult: (Result<T>) -> Unit) {
        executor.execute {
            val result = runCatching(work)
            SwingUtilities.invokeLater { onResult(result) }
        }
    }
}

class SwingFolderPicker : FolderPicker {
    // The desktop dialogs block, so the answer is ready before this returns.
    override fun pickFolder(initialLocation: String?, onResult: (PickedFolder?) -> Unit) {
        val path = if (System.getProperty("os.name").orEmpty().contains("mac", ignoreCase = true)) {
            pickWithNativeMacDialog(initialLocation)
        } else {
            pickWithChooser(initialLocation)
        }
        onResult(path?.let { PickedFolder(it, File(it).name.ifEmpty { it }) })
    }

    // AWT's FileDialog for macOS does not work great with Java's JFileChooser well;
    // this property is what switches it from choosing files to choosing folders.
    private fun pickWithNativeMacDialog(initialPath: String?): String? {
        System.setProperty("apple.awt.fileDialogForDirectories", "true")
        try {
            val dialog = FileDialog(null as Frame?, "Choose Music Folder", FileDialog.LOAD)
            initialPath?.let { dialog.directory = it }
            dialog.isVisible = true
            val name = dialog.file ?: return null
            return File(dialog.directory, name).path
        } finally {
            System.setProperty("apple.awt.fileDialogForDirectories", "false")
        }
    }

    private fun pickWithChooser(initialPath: String?): String? {
        val chooser = JFileChooser(initialPath?.let { File(it) }).apply {
            dialogTitle = "Choose Music Folder"
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        }
        return if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) chooser.selectedFile.path else null
    }
}
