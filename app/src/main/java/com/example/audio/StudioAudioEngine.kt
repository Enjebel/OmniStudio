package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import com.example.model.AudioTrackOption
import java.util.Locale
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.sin

class StudioAudioEngine(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    private var audioTrack: AudioTrack? = null
    private var isPlayingMusic = false
    private var currentMusicThread: Thread? = null

    var bgmVolume: Float = 0.7f
        set(value) {
            field = value.coerceIn(0f, 1f)
            audioTrack?.setVolume(field)
        }

    var voicePitch: Float = 1.0f
        set(value) {
            field = value.coerceIn(0.5f, 2.0f)
            tts?.setPitch(field)
        }

    var voiceSpeed: Float = 1.0f
        set(value) {
            field = value.coerceIn(0.5f, 2.0f)
            tts?.setSpeechRate(field)
        }

    init {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.e("StudioAudioEngine", "Error initializing TTS", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.let { engine ->
                val result = engine.setLanguage(Locale.US)
                if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                    isTtsReady = true
                    engine.setPitch(voicePitch)
                    engine.setSpeechRate(voiceSpeed)
                }
            }
        }
    }

    fun speak(text: String, onDone: (() -> Unit)? = null) {
        if (!isTtsReady || text.isBlank()) return
        tts?.setPitch(voicePitch)
        tts?.setSpeechRate(voiceSpeed)
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ad_voiceover_${System.currentTimeMillis()}")
    }

    fun stopSpeaking() {
        tts?.stop()
    }

    fun playMusic(trackOption: AudioTrackOption) {
        stopMusic()
        isPlayingMusic = true

        val sampleRate = 44100
        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ) * 2

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack?.setVolume(bgmVolume)
        audioTrack?.play()

        currentMusicThread = thread(start = true, name = "StudioMusicSynth") {
            // Chord root frequencies based on track genre
            val chordProgressions = when (trackOption) {
                AudioTrackOption.CORPORATE_UPBEAT -> listOf(261.63, 329.63, 392.0, 440.0) // C, E, G, A
                AudioTrackOption.LUXURY_AMBIENT -> listOf(220.0, 277.18, 329.63, 246.94) // A minor dreamy
                AudioTrackOption.TECH_FUTURE -> listOf(146.83, 174.61, 220.0, 293.66) // D synthwave
                AudioTrackOption.MOTIVATIONAL -> listOf(293.66, 369.99, 440.0, 523.25) // D major epic
                AudioTrackOption.UPBEAT_TRAP -> listOf(130.81, 155.56, 174.61, 196.0) // 808 sub bass
            }

            val samplesPerBeat = (sampleRate * (60.0 / trackOption.bpm)).toInt()
            val chunk = ShortArray(samplesPerBeat)

            var phase = 0.0
            var chordIndex = 0

            while (isPlayingMusic) {
                val baseFreq = chordProgressions[chordIndex % chordProgressions.size]
                val subFreq = baseFreq / 2.0
                val fifthFreq = baseFreq * 1.5

                for (i in 0 until samplesPerBeat) {
                    val t = i.toDouble() / sampleRate
                    // Envelope attack-decay
                    val envelope = if (i < samplesPerBeat / 8) {
                        (i.toDouble() / (samplesPerBeat / 8))
                    } else {
                        (1.0 - ((i - samplesPerBeat / 8).toDouble() / (samplesPerBeat * 7 / 8))).coerceAtLeast(0.15)
                    }

                    // Synth waves composite: root sine + sub bass + subtle harmonic
                    val wave = sin(2 * PI * baseFreq * t) * 0.5 +
                            sin(2 * PI * subFreq * t) * 0.35 +
                            sin(2 * PI * fifthFreq * t) * 0.15

                    val sampleVal = (wave * envelope * 16000).toInt().coerceIn(-32768, 32767)
                    chunk[i] = sampleVal.toShort()
                }

                if (!isPlayingMusic) break
                audioTrack?.write(chunk, 0, samplesPerBeat)
                chordIndex++
            }
        }
    }

    fun stopMusic() {
        isPlayingMusic = false
        try {
            currentMusicThread?.interrupt()
            currentMusicThread = null
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.stop()
            audioTrack?.release()
            audioTrack = null
        } catch (e: Exception) {
            Log.e("StudioAudioEngine", "Error stopping music track", e)
        }
    }

    fun isPlaying(): Boolean = isPlayingMusic

    fun release() {
        stopMusic()
        stopSpeaking()
        try {
            tts?.shutdown()
            tts = null
        } catch (e: Exception) {
            Log.e("StudioAudioEngine", "Error shutting down TTS", e)
        }
    }
}
