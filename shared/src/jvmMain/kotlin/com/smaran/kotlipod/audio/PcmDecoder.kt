package com.smaran.kotlipod.audio

import com.smaran.kotlipod.core.audio.AudioLoadException
import java.io.File
import java.io.IOException
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.UnsupportedAudioFileException

internal class DecodedAudio(val format: AudioFormat, val bytes: ByteArray)

// i hate javasound
internal fun decodeToPcm(file: File): DecodedAudio {
    if (!file.isFile) throw AudioLoadException("Audio file not found: ${file.path}")
    return try {
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
                DecodedAudio(pcmFormat, pcm.readAllBytes())
            }
        }
    } catch (e: UnsupportedAudioFileException) {
        throw AudioLoadException("Unsupported audio format: ${file.name}", e)
    } catch (e: IllegalArgumentException) {
        throw AudioLoadException("Cannot convert ${file.name} to PCM", e)
    } catch (e: IOException) {
        throw AudioLoadException("Cannot read ${file.name}", e)
    }
}
