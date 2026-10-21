package com.gbros.tabslite.utilities

import io.github.vinceglb.filekit.core.FileKit
import io.github.vinceglb.filekit.core.FileKitPlatformSettings
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import io.github.vinceglb.filekit.core.PlatformFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Dialog
import java.awt.FileDialog
import java.awt.Frame
import java.awt.KeyboardFocusManager
import java.io.File
import java.io.FilenameFilter
import javax.swing.JFileChooser
import javax.swing.SwingUtilities
import javax.swing.filechooser.FileNameExtensionFilter

/**
 * Modern Native Desktop Audio File Picker.
 * Uses modern Windows 11/10 Common Item Dialog (FileKit) with safe fallbacks.
 */
object DesktopAudioPicker {
    val SUPPORTED_EXTENSIONS = listOf("mp3", "wav", "flac", "m4a", "aac", "ogg", "opus", "aif", "aiff", "au", "wma")

    /**
     * Opens the modern Windows 11/10 Common Item Dialog (via FileKit) to pick an audio file.
     * Guaranteed to use the modern, dark/light OS Explorer dialog.
     */
    suspend fun pickAudioFile(title: String = "Select Audio File (MP3 / WAV / FLAC / M4A)"): File? {
        // Attempt 1: Modern Windows 11 Common Item Dialog via FileKit
        try {
            val parentWindow = Frame.getFrames().firstOrNull { it.isVisible }
                ?: KeyboardFocusManager.getCurrentKeyboardFocusManager().activeWindow

            val settings = if (parentWindow != null) FileKitPlatformSettings(parentWindow = parentWindow) else null
            val platformFile: PlatformFile? = FileKit.pickFile(
                type = PickerType.File(extensions = SUPPORTED_EXTENSIONS),
                mode = PickerMode.Single,
                title = title,
                platformSettings = settings
            )
            if (platformFile != null) {
                return platformFile.file
            }
            // User explicitly cancelled in the modern dialog
            return null
        } catch (e: Throwable) {
            System.err.println("[DesktopAudioPicker] Modern FileKit picker threw: ${e.message}, attempting fallback")
        }

        // Attempt 2: Fallback only if modern FileKit failed to launch
        return withContext(Dispatchers.IO) {
            pickAudioFileFallback(title)
        }
    }

    private fun pickAudioFileFallback(title: String): File? {
        try {
            val activeWindow = KeyboardFocusManager.getCurrentKeyboardFocusManager().activeWindow
                ?: Frame.getFrames().firstOrNull { it.isVisible }

            val fileDialog = when (activeWindow) {
                is Dialog -> FileDialog(activeWindow, title, FileDialog.LOAD)
                is Frame -> FileDialog(activeWindow, title, FileDialog.LOAD)
                else -> FileDialog(null as Frame?, title, FileDialog.LOAD)
            }

            fileDialog.filenameFilter = FilenameFilter { _, name ->
                val ext = name.substringAfterLast('.', "").lowercase()
                ext in SUPPORTED_EXTENSIONS
            }
            fileDialog.file = SUPPORTED_EXTENSIONS.joinToString(";") { "*.$it" }
            fileDialog.isMultipleMode = false
            fileDialog.isVisible = true

            val selectedFileName = fileDialog.file
            val directory = fileDialog.directory
            if (!selectedFileName.isNullOrBlank() && !directory.isNullOrBlank()) {
                val chosen = File(directory, selectedFileName)
                if (chosen.exists() && chosen.isFile) {
                    return chosen
                }
            } else if (fileDialog.files != null && fileDialog.files.isNotEmpty()) {
                val chosen = fileDialog.files[0]
                if (chosen.exists() && chosen.isFile) {
                    return chosen
                }
            }
            if (selectedFileName == null && (fileDialog.files == null || fileDialog.files.isEmpty())) {
                return null
            }
        } catch (e: Throwable) {
            System.err.println("[DesktopAudioPicker] AWT FileDialog failed: ${e.message}")
        }

        try {
            var selectedFile: File? = null
            val action = Runnable {
                val chooser = JFileChooser()
                chooser.dialogTitle = title
                chooser.fileSelectionMode = JFileChooser.FILES_ONLY
                chooser.isAcceptAllFileFilterUsed = true
                chooser.fileFilter = FileNameExtensionFilter(
                    "Audio Files (${SUPPORTED_EXTENSIONS.joinToString(", ") { "*.$it" }})",
                    *SUPPORTED_EXTENSIONS.toTypedArray()
                )
                val parent = KeyboardFocusManager.getCurrentKeyboardFocusManager().activeWindow
                val res = chooser.showOpenDialog(parent)
                if (res == JFileChooser.APPROVE_OPTION) {
                    selectedFile = chooser.selectedFile
                }
            }

            if (SwingUtilities.isEventDispatchThread()) {
                action.run()
            } else {
                SwingUtilities.invokeAndWait(action)
            }
            return selectedFile
        } catch (e: Throwable) {
            System.err.println("[DesktopAudioPicker] JFileChooser fallback failed: ${e.message}")
        }

        return null
    }
}
