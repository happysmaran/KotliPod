package com.smaran.kotlipod.audio

import com.smaran.kotlipod.core.audio.AudioLoadException
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.ServiceConfigurationError
import java.util.concurrent.TimeUnit
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.UnsupportedAudioFileException

internal class DecodedAudio(val format: AudioFormat, val bytes: ByteArray)

private class Decoder(val name: String, val decode: (File) -> DecodedAudio)

// No single decoder can cover every machine, especially for mobile
private val decoders = listOf(
    Decoder("Java Sound", ::decodeWithJavaSound),
    Decoder("afconvert", ::decodeWithAfconvert),
    Decoder("ffmpeg", ::decodeWithFfmpeg),
)

internal fun decodeToPcm(file: File): DecodedAudio {
    if (!file.isFile) throw AudioLoadException("Audio file not found: ${file.path}")
    val problems = mutableListOf<String>()
    for (decoder in decoders) {
        try {
            return decoder.decode(file)
        } catch (e: Exception) {
            problems += "${decoder.name}: ${e.message ?: e.javaClass.simpleName}"
        } catch (e: ServiceConfigurationError) {
            // A broken Java Sound plugin must not prevent the external decoders from being tried at all.
            problems += "${decoder.name}: ${e.message}"
        }
    }
    throw AudioLoadException("Cannot decode ${file.name} (${problems.joinToString("; ")})")
}

// Clip can only play signed 16-bit PCM reliably across mixers, while files arrive as 8/24-bit PCM or,
// with a decoder plugin on the classpath, as compressed formats.
private fun decodeWithJavaSound(file: File): DecodedAudio {
    AudioSystem.getAudioInputStream(file).use { source ->
        val sourceFormat = source.format
        val pcmFormat = AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            sourceFormat.sampleRate,
            16,
            sourceFormat.channels,
            sourceFormat.channels * 2,
            sourceFormat.sampleRate,
            false,
        )
        AudioSystem.getAudioInputStream(pcmFormat, source).use { pcm ->
            return DecodedAudio(pcmFormat, pcm.readAllBytes())
        }
    }
}

private fun decodeWithAfconvert(file: File): DecodedAudio {
    val afconvert = File("/usr/bin/afconvert")
    if (!afconvert.canExecute()) throw IOException("not available on this system")

    val workDir = Files.createTempDirectory("kotlipod-decode").toFile()
    try {
        val wav = File(workDir, "decoded.wav")
        val process = ProcessBuilder(afconvert.path, "-f", "WAVE", "-d", "LEI16", file.absolutePath, wav.path)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readText()
        if (!process.waitFor(2, TimeUnit.MINUTES)) {
            process.destroyForcibly()
            throw IOException("timed out")
        }
        if (process.exitValue() != 0) throw IOException("exit code ${process.exitValue()}: ${output.trim()}")
        return decodeWithJavaSound(wav)
    } finally {
        workDir.deleteRecursively()
    }
}

// Raw PCM goes through a pipe so no intermediate file would exist.
private fun decodeWithFfmpeg(file: File): DecodedAudio {
    val ffmpeg = findExecutable("ffmpeg") ?: throw IOException("not installed")
    val process = ProcessBuilder(
        ffmpeg.path, "-v", "error", "-nostdin", "-i", file.absolutePath,
        "-vn", "-f", "s16le", "-acodec", "pcm_s16le", "-ar", "44100", "-ac", "2", "pipe:1",
    ).redirectError(ProcessBuilder.Redirect.DISCARD).start()

    val bytes = process.inputStream.use { it.readAllBytes() }
    if (!process.waitFor(30, TimeUnit.SECONDS)) {
        process.destroyForcibly()
        throw IOException("timed out")
    }
    if (process.exitValue() != 0) throw IOException("exit code ${process.exitValue()}")
    if (bytes.isEmpty()) throw IOException("produced no audio")

    return DecodedAudio(AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100f, 16, 2, 4, 44100f, false), bytes)
}

private fun findExecutable(name: String): File? {
    val directories = System.getenv("PATH").orEmpty().split(File.pathSeparator) +
        listOf("/opt/homebrew/bin", "/usr/local/bin", "/usr/bin")
    return directories
        .filter { it.isNotBlank() }
        .flatMap { directory -> listOf(File(directory, name), File(directory, "$name.exe")) }
        .firstOrNull { it.isFile && it.canExecute() }
}
