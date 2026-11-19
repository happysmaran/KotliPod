package com.smaran.kotlipod.audio

import com.smaran.kotlipod.core.FolderPicker
import com.smaran.kotlipod.core.PickedFolder
import com.smaran.kotlipod.core.PlatformServices
import com.smaran.kotlipod.core.TaskRunner
import com.smaran.kotlipod.library.IosLibraryScanner
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIViewController
import platform.UniformTypeIdentifiers.UTTypeFolder
import platform.darwin.DISPATCH_QUEUE_PRIORITY_DEFAULT
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_global_queue
import platform.darwin.dispatch_get_main_queue

fun iosPlatformServices() = PlatformServices(
    folderPicker = IosFolderPicker(),
    libraryScanner = IosLibraryScanner(),
    audioPlayer = AvAudioPlayerBackend(),
    taskRunner = DispatchTaskRunner(),
)

// Pod's state is only safe to touch on the main queue, while scanning and decoding must stay off it.
class DispatchTaskRunner : TaskRunner {
    @OptIn(ExperimentalForeignApi::class)
    override fun <T> submit(work: () -> T, onResult: (Result<T>) -> Unit) {
        dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT.toLong(), 0UL)) {
            val result = runCatching(work)
            dispatch_async(dispatch_get_main_queue()) { onResult(result) }
        }
    }
}

// iOS sandboxes apps away from other folders; the document picker hands back a URL whose access
// lasts only while startAccessingSecurityScopedResource is active, so it is held until another
// folder replaces it.
class IosFolderPicker : FolderPicker {
    private var accessedFolder: NSURL? = null

    // UIKit keeps the delegate weakly, so it needs an owner for as long as the picker is on screen.
    private var delegate: PickerDelegate? = null

    override fun pickFolder(initialLocation: String?, onResult: (PickedFolder?) -> Unit) {
        val presenter = topViewController()
        if (presenter == null) {
            onResult(null)
            return
        }
        val picker = UIDocumentPickerViewController(forOpeningContentTypes = listOfNotNull(UTTypeFolder))
        initialLocation?.let { picker.directoryURL = NSURL.fileURLWithPath(it) }

        delegate = PickerDelegate { url ->
            delegate = null
            if (url == null) {
                onResult(null)
            } else {
                accessedFolder?.stopAccessingSecurityScopedResource()
                url.startAccessingSecurityScopedResource()
                accessedFolder = url
                onResult(PickedFolder(url.path ?: "", url.lastPathComponent ?: "Folder"))
            }
        }
        picker.delegate = delegate
        presenter.presentViewController(picker, animated = true, completion = null)
    }

    private fun topViewController(): UIViewController? {
        var top = UIApplication.sharedApplication.keyWindow?.rootViewController
        while (true) {
            val next = top?.presentedViewController ?: break
            top = next
        }
        return top
    }

    private class PickerDelegate(private val onPicked: (NSURL?) -> Unit) : NSObject(), UIDocumentPickerDelegateProtocol {
        override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
            onPicked(didPickDocumentsAtURLs.firstOrNull() as? NSURL)
        }

        override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
            onPicked(null)
        }
    }
}
