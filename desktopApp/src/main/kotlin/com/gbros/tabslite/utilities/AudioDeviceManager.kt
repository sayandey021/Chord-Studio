package com.gbros.tabslite.utilities

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.sound.sampled.*

object AudioDeviceManager {

    @Volatile
    private var cachedInputDevices: List<String> = listOf("Default System Microphone")

    @Volatile
    private var cachedOutputDevices: List<String> = listOf("Default System Speakers")

    private val isScanning = AtomicBoolean(false)

    init {
        // Asynchronously scan hardware devices on startup in background
        refreshDevicesAsync()
    }

    fun refreshDevicesAsync(onComplete: (() -> Unit)? = null) {
        if (isScanning.compareAndSet(false, true)) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val inputs = mutableListOf("Default System Microphone")
                    val outputs = mutableListOf("Default System Speakers")
                    val mixers = AudioSystem.getMixerInfo()
                    for (m in mixers) {
                        try {
                            val mixer = AudioSystem.getMixer(m)
                            val name = m.name.trim()
                            if (name.isNotBlank()) {
                                if (mixer.targetLineInfo.isNotEmpty() && !inputs.contains(name)) {
                                    inputs.add(name)
                                }
                                if (mixer.sourceLineInfo.isNotEmpty() && !outputs.contains(name)) {
                                    outputs.add(name)
                                }
                            }
                        } catch (_: Throwable) {}
                    }
                    cachedInputDevices = inputs
                    cachedOutputDevices = outputs
                } catch (e: Throwable) {
                    println("Audio device discovery notice: ${e.message}")
                } finally {
                    isScanning.set(false)
                    onComplete?.invoke()
                }
            }
        }
    }

    fun getInputDevices(): List<String> {
        return cachedInputDevices
    }

    fun getOutputDevices(): List<String> {
        return cachedOutputDevices
    }

    fun openTargetLine(format: AudioFormat, bufferSize: Int, deviceName: String?): TargetDataLine {
        val info = DataLine.Info(TargetDataLine::class.java, format)
        if (!deviceName.isNullOrBlank() && deviceName != "Default System Microphone") {
            try {
                val mixers = AudioSystem.getMixerInfo()
                val matched = mixers.find { it.name.trim() == deviceName.trim() }
                if (matched != null) {
                    val mixer = AudioSystem.getMixer(matched)
                    if (mixer.isLineSupported(info)) {
                        val line = mixer.getLine(info) as TargetDataLine
                        line.open(format, bufferSize)
                        return line
                    }
                }
            } catch (e: Exception) {
                println("Could not open custom input line '$deviceName': ${e.message}. Falling back to default.")
            }
        }
        val line = AudioSystem.getLine(info) as TargetDataLine
        line.open(format, bufferSize)
        return line
    }

    fun openSourceLine(format: AudioFormat, bufferSize: Int, deviceName: String?): SourceDataLine {
        val info = DataLine.Info(SourceDataLine::class.java, format)
        if (!deviceName.isNullOrBlank() && deviceName != "Default System Speakers") {
            try {
                val mixers = AudioSystem.getMixerInfo()
                val matched = mixers.find { it.name.trim() == deviceName.trim() }
                if (matched != null) {
                    val mixer = AudioSystem.getMixer(matched)
                    if (mixer.isLineSupported(info)) {
                        val line = mixer.getLine(info) as SourceDataLine
                        line.open(format, bufferSize)
                        return line
                    }
                }
            } catch (e: Exception) {
                println("Could not open custom output line '$deviceName': ${e.message}. Falling back to default.")
            }
        }
        val line = AudioSystem.getLine(info) as SourceDataLine
        line.open(format, bufferSize)
        return line
    }
}
