package com.example.thornburydental.speech

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
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
 *
 * Never substitute synthetic audio for a real recording. Doing so would let the
 * dictation pipeline produce clinical findings from audio the clinician never spoke.
 */
class MicrophoneUnavailableException(
    message: String,
    cause: Throwable? = null
) : IllegalStateException(message, cause)

/**
 * Manages low-latency 16kHz 16-bit mono AudioRecord streams for hands-free clinical dictation.
 * Provides real-time microphone gain amplitude levels for audio visualizers.
 */
class AudioRecordManager(private val context: Context? = null) {

    companion object {
        private const val TAG = "AudioRecordManager"
        const val SAMPLE_RATE = 16000
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private var isRecording = false

    private val _amplitudeFlow = MutableStateFlow(0f)
    val amplitudeFlow: StateFlow<Float> = _amplitudeFlow.asStateFlow()

    private val recordedAudioData = mutableListOf<Short>()

    private fun safeLogE(tag: String, msg: String, t: Throwable? = null) {
        try {
            Log.e(tag, msg, t)
        } catch (_: Throwable) {
            println("[$tag] $msg ${t?.message ?: ""}")
        }
    }

    /**
     * Starts hands-free PCM microphone recording.
     *
     * @throws MicrophoneUnavailableException if the microphone cannot be opened. Callers must
     *         surface this to the clinician — there is no fallback audio source.
     */
    @SuppressLint("MissingPermission")
    fun startRecording(scope: CoroutineScope, onChunkRecorded: ((ShortArray) -> Unit)? = null) {
        if (isRecording) return

        val record = try {
            val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
            val bufferSize = max(max(minBufferSize, 0), SAMPLE_RATE * 2)
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )
        } catch (t: Throwable) {
            // SecurityException when RECORD_AUDIO is not granted; other throwables when the
            // device has no usable capture hardware.
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

        recordingJob = scope.launch(Dispatchers.IO) {
            val tempBuffer = ShortArray(1024)
            while (isActive && isRecording) {
                val readCount = audioRecord?.read(tempBuffer, 0, tempBuffer.size) ?: break
                if (readCount <= 0) continue

                val chunk = tempBuffer.copyOf(readCount)
                synchronized(recordedAudioData) {
                    for (s in chunk) {
                        recordedAudioData.add(s)
                    }
                }

                // Calculate RMS amplitude normalized (0.0 to 1.0)
                var sum = 0.0
                for (i in 0 until readCount) {
                    sum += abs(chunk[i].toInt())
                }
                val avg = sum / readCount
                _amplitudeFlow.value = min(1f, (avg / 10000f).toFloat())

                onChunkRecorded?.invoke(chunk)
            }
        }
    }

    /**
     * Stops microphone audio recording and returns all captured PCM samples.
     */
    fun stopRecording(): ShortArray {
        isRecording = false
        recordingJob?.cancel()
        recordingJob = null

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

        val result: ShortArray
        synchronized(recordedAudioData) {
            result = recordedAudioData.toShortArray()
            recordedAudioData.clear()
        }
        return result
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
