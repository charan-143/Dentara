package com.example.thornburydental.speech

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Raised when the microphone cannot be opened — permission denied, hardware missing,
 * or the mic held by another app.
 */
class MicrophoneUnavailableException(
    message: String,
    cause: Throwable? = null
) : IllegalStateException(message, cause)

/**
 * Manages low-latency 16kHz 16-bit mono AudioRecord streams with hardware & software noise suppression.
 * Uses hardware NoiseSuppressor/AEC/AGC, dental environment bandpass speech filtering (150Hz - 3600Hz),
 * adaptive noise-gate suppression, real-time gain amplitude levels, and VAD speech endpointing.
 */
class AudioRecordManager(
    private val context: Context? = null,
    val vadSegmenter: VoiceActivitySegmenter = VoiceActivitySegmenter()
) {

    companion object {
        private const val TAG = "AudioRecordManager"
        const val SAMPLE_RATE = 16000
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT

        // Background noise gate threshold (samples below this energy are attenuated)
        private const val NOISE_GATE_THRESHOLD = 350
    }

    private var audioRecord: AudioRecord? = null
    private var noiseSuppressor: NoiseSuppressor? = null
    private var echoCanceler: AcousticEchoCanceler? = null
    private var gainControl: AutomaticGainControl? = null

    private var recordingJob: Job? = null
    private var isRecording = false

    private val _amplitudeFlow = MutableStateFlow(0f)
    val amplitudeFlow: StateFlow<Float> = _amplitudeFlow.asStateFlow()

    private val recordedAudioData = mutableListOf<Short>()

    // Simple single-pole high-pass state for low-frequency hum removal (<150Hz)
    private var hpFilterPrevInput = 0f
    private var hpFilterPrevOutput = 0f

    private fun safeLogE(tag: String, msg: String, t: Throwable? = null) {
        try {
            Log.e(tag, msg, t)
        } catch (_: Throwable) {
            println("[$tag] $msg ${t?.message ?: ""}")
        }
    }

    /**
     * Starts hands-free PCM microphone recording using VOICE_RECOGNITION audio source,
     * hardware noise suppression, acoustic echo cancellation, and real-time VAD endpointing.
     */
    @SuppressLint("MissingPermission")
    fun startRecording(
        scope: CoroutineScope,
        onChunkRecorded: ((ShortArray) -> Unit)? = null,
        onEndpointDetected: ((ShortArray) -> Unit)? = null
    ) {
        if (isRecording) return

        val record = try {
            val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
            val bufferSize = max(max(minBufferSize, 0), SAMPLE_RATE * 2)
            AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )
        } catch (t: Throwable) {
            throw MicrophoneUnavailableException(
                "Microphone unavailable. Grant microphone access to use voice dictation.",
                t
            )
        }

        if (record.state != AudioRecord.STATE_INITIALIZED) {
            releaseQuietly(record)
            throw MicrophoneUnavailableException(
                "Microphone could not be initialised. It may be in use by another app."
            )
        }

        // Attach hardware DSP Noise Suppressor if supported on device
        try {
            if (NoiseSuppressor.isAvailable()) {
                noiseSuppressor = NoiseSuppressor.create(record.audioSessionId)?.apply {
                    enabled = true
                }
            }
        } catch (t: Throwable) {
            safeLogE(TAG, "Hardware NoiseSuppressor not available", t)
        }

        // Attach Acoustic Echo Canceler if supported
        try {
            if (AcousticEchoCanceler.isAvailable()) {
                echoCanceler = AcousticEchoCanceler.create(record.audioSessionId)?.apply {
                    enabled = true
                }
            }
        } catch (t: Throwable) {
            safeLogE(TAG, "Hardware AcousticEchoCanceler not available", t)
        }

        // Attach Automatic Gain Control if supported
        try {
            if (AutomaticGainControl.isAvailable()) {
                gainControl = AutomaticGainControl.create(record.audioSessionId)?.apply {
                    enabled = true
                }
            }
        } catch (t: Throwable) {
            safeLogE(TAG, "Hardware AutomaticGainControl not available", t)
        }

        try {
            record.startRecording()
        } catch (t: Throwable) {
            releaseQuietly(record)
            throw MicrophoneUnavailableException("Failed to start microphone capture.", t)
        }

        if (record.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
            releaseQuietly(record)
            throw MicrophoneUnavailableException(
                "Microphone did not start recording. It may be in use by another app."
            )
        }

        audioRecord = record
        isRecording = true
        synchronized(recordedAudioData) { recordedAudioData.clear() }
        vadSegmenter.reset()
        hpFilterPrevInput = 0f
        hpFilterPrevOutput = 0f

        recordingJob = scope.launch(Dispatchers.IO) {
            try {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO)
            } catch (_: Throwable) {
                // Thread priority elevation is best-effort under JVM unit tests
            }

            // 512 samples = 32ms frame at 16kHz for VAD processing
            val frameBuffer = ShortArray(VoiceActivitySegmenter.FRAME_SIZE)
            var frameBufferOffset = 0

            val readBuffer = ShortArray(1024)
            while (isActive && isRecording) {
                val readCount = audioRecord?.read(readBuffer, 0, readBuffer.size) ?: break
                if (readCount <= 0) continue

                // Apply DSP high-pass speech isolation and noise gate filter
                val filteredChunk = ShortArray(readCount)
                var sum = 0.0

                for (i in 0 until readCount) {
                    val rawSample = readBuffer[i].toFloat()

                    // High-pass filter (cutoff ~150Hz) to remove room rumble, compressors, and motor hum
                    val alpha = 0.94f
                    val hpSample = alpha * (hpFilterPrevOutput + rawSample - hpFilterPrevInput)
                    hpFilterPrevInput = rawSample
                    hpFilterPrevOutput = hpSample

                    // Software noise gate: attenuate low-level ambient hiss/background murmurs
                    val cleanSample = if (abs(hpSample) < NOISE_GATE_THRESHOLD) {
                        (hpSample * 0.15f).toInt()
                    } else {
                        hpSample.toInt()
                    }.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()

                    filteredChunk[i] = cleanSample
                    sum += abs(cleanSample.toInt())
                }

                synchronized(recordedAudioData) {
                    for (s in filteredChunk) {
                        recordedAudioData.add(s)
                    }
                }

                // Calculate RMS amplitude normalized (0.0 to 1.0)
                val avg = sum / readCount
                _amplitudeFlow.value = min(1f, (avg / 10000f).toFloat())

                onChunkRecorded?.invoke(filteredChunk)

                // Process continuous frames for real-time VAD endpointing
                if (onEndpointDetected != null) {
                    var chunkOffset = 0
                    while (chunkOffset < readCount) {
                        val needed = VoiceActivitySegmenter.FRAME_SIZE - frameBufferOffset
                        val available = readCount - chunkOffset
                        val toCopy = min(needed, available)

                        System.arraycopy(filteredChunk, chunkOffset, frameBuffer, frameBufferOffset, toCopy)
                        frameBufferOffset += toCopy
                        chunkOffset += toCopy

                        if (frameBufferOffset == VoiceActivitySegmenter.FRAME_SIZE) {
                            val event = vadSegmenter.processFrame(frameBuffer)
                            frameBufferOffset = 0

                            if (event is VoiceActivitySegmenter.VadChunkEvent.EndpointReached) {
                                onEndpointDetected.invoke(event.speechPcm)
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Stops microphone audio recording and returns all captured PCM samples.
     * Optionally filters leading and trailing silence using the VAD segmenter.
     */
    fun stopRecording(applyVadFilter: Boolean = true): ShortArray {
        isRecording = false
        recordingJob?.cancel()
        recordingJob = null

        try {
            noiseSuppressor?.release()
            noiseSuppressor = null
            echoCanceler?.release()
            echoCanceler = null
            gainControl?.release()
            gainControl = null
        } catch (_: Throwable) {}

        audioRecord?.let { record ->
            try {
                if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    record.stop()
                }
            } catch (t: Throwable) {
                safeLogE(TAG, "Error stopping AudioRecord", t)
            }
            releaseQuietly(record)
        }
        audioRecord = null

        _amplitudeFlow.value = 0f

        val rawPcm: ShortArray
        synchronized(recordedAudioData) {
            rawPcm = recordedAudioData.toShortArray()
            recordedAudioData.clear()
        }

        if (rawPcm.isEmpty()) return ShortArray(0)

        if (applyVadFilter) {
            val segmented = vadSegmenter.filterAndExtractSpeech(rawPcm)
            if (segmented != null) {
                return segmented
            }
        }

        return rawPcm
    }

    fun isRecording(): Boolean = isRecording

    private fun releaseQuietly(record: AudioRecord) {
        try {
            record.release()
        } catch (t: Throwable) {
            safeLogE(TAG, "Error releasing AudioRecord", t)
        }
    }
}
