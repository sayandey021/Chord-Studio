package com.gbros.tabslite.utilities

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

data class WhisperModelInfo(
    val id: String,
    val name: String,
    val description: String,
    val sizeBytes: Long,
    val sizeText: String,
    val ramRequired: String,
    val accuracyLevel: String,
    val accuracyColor: Color,
    val recommendedSpecs: String,
    val downloadUrl: String
)

object WhisperModelManager {

    val AVAILABLE_MODELS = listOf(
        WhisperModelInfo(
            id = "base",
            name = "Base (Fast)",
            description = "Solid general-purpose balance of processing speed and vocal lyrics accuracy.",
            sizeBytes = 147951465L,
            sizeText = "~141 MB",
            ramRequired = "~1.5 GB RAM",
            accuracyLevel = "Standard (+30%)",
            accuracyColor = Color(0xFF38BDF8),
            recommendedSpecs = "Standard laptops & desktops with Intel Core i5 or AMD Ryzen 5",
            downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.bin"
        ),
        WhisperModelInfo(
            id = "small",
            name = "Small (Balanced)",
            description = "High-precision acoustic separation. Dramatically better at deciphering singing over loud guitars and Indian languages (Hindi, Bengali).",
            sizeBytes = 487601967L,
            sizeText = "~465 MB",
            ramRequired = "~2.5 GB RAM",
            accuracyLevel = "High Precision ★",
            accuracyColor = Color(0xFF10B981),
            recommendedSpecs = "Modern PCs with 8 GB+ RAM (Intel i5/i7, Ryzen 5/7)",
            downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-small.bin"
        ),
        WhisperModelInfo(
            id = "medium",
            name = "Medium (Studio)",
            description = "Studio-grade neural transcription engine. Exceptional vocabulary with complex harmonies, echoes, and noisy live recordings.",
            sizeBytes = 1533860472L,
            sizeText = "~1.46 GB",
            ramRequired = "~5 GB RAM",
            accuracyLevel = "Studio Reference",
            accuracyColor = Color(0xFFA855F7),
            recommendedSpecs = "High-performance workstations with 16 GB+ RAM and multi-core CPU",
            downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-medium.bin"
        ),
        WhisperModelInfo(
            id = "large-v3-turbo",
            name = "Turbo (Speed)",
            description = "Next-gen distilled architecture. Delivers state-of-the-art Large vocal precision with ~6x faster inference speed.",
            sizeBytes = 1624555275L,
            sizeText = "~1.51 GB",
            ramRequired = "~4 GB RAM",
            accuracyLevel = "Turbo Precision ★★★",
            accuracyColor = Color(0xFFF59E0B),
            recommendedSpecs = "Modern multicore PCs with 8 GB+ RAM (Intel i5/i7, AMD Ryzen 5/7)",
            downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-large-v3-turbo.bin"
        ),
        WhisperModelInfo(
            id = "large-v3",
            name = "Large (Max)",
            description = "OpenAI's flagship 32-layer encoder/decoder model. Supreme transcription accuracy for subtle vocal nuances, multi-language singing, and dense audio mixes.",
            sizeBytes = 3095033483L,
            sizeText = "~2.88 GB",
            ramRequired = "~6 GB RAM",
            accuracyLevel = "Flagship Master ★★★★",
            accuracyColor = Color(0xFFEC4899),
            recommendedSpecs = "High-end PCs or workstations with 16 GB+ RAM and multi-core CPU",
            downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-large-v3.bin"
        )
    )

    private val _downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, Float>> = _downloadProgress.asStateFlow()

    private val _downloadStatusText = MutableStateFlow<Map<String, String>>(emptyMap())
    val downloadStatusText: StateFlow<Map<String, String>> = _downloadStatusText.asStateFlow()

    private val downloadScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _activeJobs = mutableMapOf<String, Job>()

    private val _modelsVersion = MutableStateFlow(0)
    val modelsVersion: StateFlow<Int> = _modelsVersion.asStateFlow()

    /**
     * Resolves the primary directory where model files are stored.
     */
    fun getModelsDirectory(): File {
        val projectRoot = LyricsExtractor.findProjectRoot()
        if (projectRoot != null) {
            val f = File(projectRoot, "tools/whisper/models")
            if (f.exists() || f.parentFile?.exists() == true) {
                f.mkdirs()
                return f
            }
        }

        // Packaged Desktop app location
        val codeLocation = try {
            WhisperModelManager::class.java.protectionDomain?.codeSource?.location?.toURI()?.let { File(it) }
        } catch (_: Exception) { null }
        val appDir = if (codeLocation != null && !codeLocation.isDirectory) codeLocation.parentFile else codeLocation
        if (appDir != null) {
            val f = File(appDir, "whisper/models")
            if (f.exists() || f.parentFile?.exists() == true) {
                f.mkdirs()
                return f
            }
        }

        // Dedicated User App Data Directory
        val userHome = System.getProperty("user.home") ?: "."
        val f = File(userHome, ".chordstudio/models")
        f.mkdirs()
        return f
    }

    /**
     * Checks if a model binary is present and valid on disk.
     */
    fun isModelInstalled(modelId: String): Boolean {
        val file = LyricsExtractor.findWhisperModelFile(modelId)
        return file != null && file.exists() && file.length() > 50_000
    }

    /**
     * Returns the physical file for an installed model, or null if not present.
     */
    fun getInstalledModelFile(modelId: String): File? {
        val file = LyricsExtractor.findWhisperModelFile(modelId)
        return if (file != null && file.exists() && file.length() > 50_000) file else null
    }

    /**
     * Returns all currently installed models.
     */
    fun getInstalledModels(): List<WhisperModelInfo> {
        return AVAILABLE_MODELS.filter { isModelInstalled(it.id) }
    }

    /**
     * Checks if a model download is actively in progress.
     */
    fun isDownloading(modelId: String): Boolean {
        return _activeJobs[modelId]?.isActive == true
    }

    /**
     * Initiates an application-level resilient download that persists across tab switches and UI navigation.
     */
    fun startDownload(
        model: WhisperModelInfo,
        scope: CoroutineScope? = null,
        onSuccess: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        if (isDownloading(model.id)) return

        val job = downloadScope.launch {
            val targetDir = getModelsDirectory()
            targetDir.mkdirs()
            val finalFile = File(targetDir, "ggml-${model.id}.bin")
            val tempFile = File(targetDir, "ggml-${model.id}.bin.tmp")

            try {
                updateStatus(model.id, 0.01f, "Connecting to Hugging Face...")

                val existingBytes = if (tempFile.exists()) tempFile.length() else 0L

                var currentUrl = model.downloadUrl
                var connection: HttpURLConnection? = null
                var redirects = 0
                val maxRedirects = 6

                while (redirects < maxRedirects) {
                    val url = URI(currentUrl).toURL()
                    connection = (url.openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        instanceFollowRedirects = false
                        connectTimeout = 15000
                        readTimeout = 30000
                        setRequestProperty("User-Agent", "ChordStudio/1.0 (Desktop App; Multiplatform)")
                        if (existingBytes > 0) {
                            setRequestProperty("Range", "bytes=$existingBytes-")
                        }
                    }

                    val code = connection.responseCode
                    if (code == HttpURLConnection.HTTP_MOVED_PERM ||
                        code == HttpURLConnection.HTTP_MOVED_TEMP ||
                        code == HttpURLConnection.HTTP_SEE_OTHER ||
                        code == 307 || code == 308
                    ) {
                        val newLocation = connection.getHeaderField("Location") ?: break
                        connection.disconnect()
                        currentUrl = if (newLocation.startsWith("http")) newLocation else URI(currentUrl).resolve(newLocation).toString()
                        redirects++
                    } else {
                        break
                    }
                }

                val conn = connection ?: throw IllegalStateException("Could not establish download connection.")
                val isPartial = conn.responseCode == 206
                val isOk = conn.responseCode in 200..299

                if (!isPartial && !isOk) {
                    throw IllegalStateException("Server returned HTTP ${conn.responseCode}: ${conn.responseMessage}")
                }

                val outputStream = if (isPartial) {
                    FileOutputStream(tempFile, true)
                } else {
                    FileOutputStream(tempFile, false)
                }

                var totalRead = if (isPartial) existingBytes else 0L
                val totalLength = if (isPartial) {
                    existingBytes + conn.contentLengthLong.coerceAtLeast(0L)
                } else {
                    conn.contentLengthLong.let { if (it > 0) it else model.sizeBytes }
                }

                val inputStream = conn.inputStream
                val buffer = ByteArray(64 * 1024)
                var bytesRead: Int
                var lastUpdateTime = System.currentTimeMillis()
                var bytesSinceLastUpdate = 0L

                inputStream.use { input ->
                    outputStream.use { output ->
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            if (!isActive) {
                                tempFile.delete()
                                return@launch
                            }
                            output.write(buffer, 0, bytesRead)
                            totalRead += bytesRead
                            bytesSinceLastUpdate += bytesRead

                            val now = System.currentTimeMillis()
                            if (now - lastUpdateTime >= 250) {
                                val progress = (totalRead.toFloat() / totalLength.toFloat()).coerceIn(0.01f, 0.99f)
                                val timeDiffSec = (now - lastUpdateTime) / 1000.0
                                val speedMbps = if (timeDiffSec > 0) (bytesSinceLastUpdate / 1024.0 / 1024.0) / timeDiffSec else 0.0

                                val downloadedMb = totalRead / (1024 * 1024)
                                val totalMb = totalLength / (1024 * 1024)
                                val statusText = "${downloadedMb} MB / ${totalMb} MB (%.1f MB/s)".format(speedMbps)

                                updateStatus(model.id, progress, statusText)
                                lastUpdateTime = now
                                bytesSinceLastUpdate = 0L
                            }
                        }
                    }
                }

                // Verify file size sanity
                if (tempFile.length() < 50_000) {
                    tempFile.delete()
                    throw IllegalStateException("Downloaded file is incomplete or empty.")
                }

                // Atomic rename to final binary name
                if (finalFile.exists()) {
                    finalFile.delete()
                }
                if (!tempFile.renameTo(finalFile)) {
                    // Fallback copy if renameTo fails across file systems
                    tempFile.copyTo(finalFile, overwrite = true)
                    tempFile.delete()
                }

                clearStatus(model.id)
                _modelsVersion.value += 1
                withContext(Dispatchers.Main) {
                    try {
                        onSuccess?.invoke()
                    } catch (_: Exception) {}
                }

            } catch (e: CancellationException) {
                tempFile.delete()
                clearStatus(model.id)
            } catch (e: Exception) {
                clearStatus(model.id)
                withContext(Dispatchers.Main) {
                    try {
                        onError?.invoke(e.message ?: "Download failed")
                    } catch (_: Exception) {}
                }
            } finally {
                _activeJobs.remove(model.id)
            }
        }

        _activeJobs[model.id] = job
    }

    /**
     * Cancels an ongoing download job and cleans up temporary files.
     */
    fun cancelDownload(modelId: String) {
        _activeJobs[modelId]?.cancel()
        _activeJobs.remove(modelId)
        clearStatus(modelId)
        val tempFile = File(getModelsDirectory(), "ggml-$modelId.bin.tmp")
        if (tempFile.exists()) tempFile.delete()
    }

    /**
     * Deletes a model from local disk to reclaim storage.
     */
    fun deleteModel(modelId: String): Boolean {
        if (isDownloading(modelId)) {
            cancelDownload(modelId)
        }
        val file = LyricsExtractor.findWhisperModelFile(modelId)
        val success = if (file != null && file.exists()) {
            file.delete()
        } else false

        _modelsVersion.value += 1
        return success
    }

    private fun updateStatus(modelId: String, progress: Float, status: String) {
        _downloadProgress.value = _downloadProgress.value + (modelId to progress)
        _downloadStatusText.value = _downloadStatusText.value + (modelId to status)
    }

    private fun clearStatus(modelId: String) {
        _downloadProgress.value = _downloadProgress.value - modelId
        _downloadStatusText.value = _downloadStatusText.value - modelId
    }
}
