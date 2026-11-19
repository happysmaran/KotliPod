package com.smaran.kotlipod.audio

import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.DocumentsContract
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.smaran.kotlipod.core.FolderPicker
import com.smaran.kotlipod.core.PickedFolder
import com.smaran.kotlipod.core.PlatformServices
import com.smaran.kotlipod.core.TaskRunner
import com.smaran.kotlipod.library.DocumentTreeScanner
import java.util.concurrent.Executors

// Must be called while the activity is being created
fun androidPlatformServices(activity: ComponentActivity): PlatformServices {
    val context = activity.applicationContext
    return PlatformServices(
        folderPicker = ActivityFolderPicker(activity),
        libraryScanner = DocumentTreeScanner(context),
        audioPlayer = MediaPlayerAudioPlayer(context),
        taskRunner = MainThreadTaskRunner(),
        report = { message ->
            Log.w("KotliPod", message)
            Handler(Looper.getMainLooper()).post { Toast.makeText(context, message, Toast.LENGTH_LONG).show() }
        },
    )
}

// Scanning and decoding run on workers but state is only safe to touch from the main thread.
class MainThreadTaskRunner : TaskRunner {
    private val executor = Executors.newCachedThreadPool { runnable ->
        Thread(runnable, "kotlipod-worker").apply { isDaemon = true }
    }
    private val main = Handler(Looper.getMainLooper())

    override fun <T> submit(work: () -> T, onResult: (Result<T>) -> Unit) {
        executor.execute {
            val result = runCatching(work)
            main.post { onResult(result) }
        }
    }
}

// Android no longer lets apps read arbitrary paths (as of 2025), so user should give one folder through the
// system picker and everything afterwards goes through the userperms grant.
class ActivityFolderPicker(private val activity: ComponentActivity) : FolderPicker {
    private var pending: ((PickedFolder?) -> Unit)? = null

    private val launcher = activity.registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        val callback = pending
        pending = null
        callback?.invoke(uri?.let(::toPickedFolder))
    }

    override fun pickFolder(initialLocation: String?, onResult: (PickedFolder?) -> Unit) {
        pending = onResult
        launcher.launch(initialLocation?.let(Uri::parse))
    }

    private fun toPickedFolder(uri: Uri): PickedFolder {
        try {
            activity.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (e: SecurityException) {
            // Some providers (e.g. Nothing) refuse persistent grants; the folder still works until the app closes.
        }
        // A tree id looks like "primary:Music/Rock"; the last segment is the name people recognise.
        val name = DocumentsContract.getTreeDocumentId(uri)
            .substringAfter(':')
            .substringAfterLast('/')
            .ifEmpty { "Storage" }
        return PickedFolder(uri.toString(), name)
    }
}
