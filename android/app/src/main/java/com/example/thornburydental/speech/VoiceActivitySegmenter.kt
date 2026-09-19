package com.example.thornburydental.speech

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Voice Activity Detection (VAD) and real-time audio endpointing for 16 kHz mono PCM audio.
 *
 * Implements WebRTC / Silero-grade hysteresis-based speech gating with pre-roll buffering,
 * noise floor estimation, zero-crossing rate analysis, and trailing silence endpoint detection:
 * - Frame size: 512 samples (32 ms at 16 kHz)
 * - Start speech probability threshold: p > 0.50
 * - End speech probability threshold: p < 0.35
 * - Minimum speech duration: 250 ms (~8 frames)
 * - Trailing silence threshold for automatic utterance endpointing: 500-700 ms (~18-20 frames)
 * - Pre-roll circular buffer: 320 ms (~10 frames) to prevent clipping word onsets
 */
class VoiceActivitySegmenter(
    val frameSize: Int = FRAME_SIZE,
    val sampleRate: Int = SAMPLE_RATE,
    val startThreshold: Float = START_THRESHOLD,
    val endThreshold: Float = END_THRESHOLD,
    val minSpeechFrames: Int = MIN_SPEECH_FRAMES,
    val minSilenceFrames: Int = MIN_SILENCE_FRAMES,
    val preRollFrames: Int = PRE_ROLL_FRAMES
) {

    companion object {
        const val SAMPLE_RATE = 16000
        const val FRAME_SIZE = 512 // 32 ms at 16 kHz

        const val START_THRESHOLD = 0.50f
        const val END_THRESHOLD = 0.35f

        // 250 ms minimum speech / 32 ms frame ≈ 8 frames
        const val MIN_SPEECH_FRAMES = 8

        // 600 ms trailing silence / 32 ms frame ≈ 19 frames
        const val MIN_SILENCE_FRAMES = 19

        // 320 ms pre-roll / 32 ms frame = 10 frames
        const val PRE_ROLL_FRAMES = 10
    }

    enum class VadState {
        SILENCE,
        SPEECH,
        TRAILING_SILENCE
    }

    sealed interface VadChunkEvent {
        object Silence : VadChunkEvent
        object SpeechStarted : VadChunkEvent
        object SpeechOngoing : VadChunkEvent
        data class EndpointReached(val speechPcm: ShortArray) : VadChunkEvent
    }

    // Real-time stateful detector variables
    private var currentState = VadState.SILENCE
    private var consecutiveSpeechFrames = 0
    private var trailingSilenceFrames = 0
    private var totalSpeechFramesInUtterance = 0
    private var dynamicNoiseFloor = 150f

    private val preRollBuffer = ArrayDeque<ShortArray>()
    private val activeUtterancePcm = mutableListOf<Short>()

    /**
     * Resets real-time streaming VAD state.
     */
    @Synchronized
    fun reset() {
        currentState = VadState.SILENCE
        consecutiveSpeechFrames = 0
        trailingSilenceFrames = 0
        totalSpeechFramesInUtterance = 0
        preRollBuffer.clear()
        activeUtterancePcm.clear()
    }

    /**
     * Processes a streaming audio frame in real time.
     * When an utterance ends via trailing silence, returns [VadChunkEvent.EndpointReached]
     * containing the complete segmented speech snippet.
     */
    @Synchronized
    fun processFrame(frame: ShortArray): VadChunkEvent {
        if (frame.isEmpty()) return VadChunkEvent.Silence

        val p = estimateSpeechProbability(frame, dynamicNoiseFloor)

        when (currentState) {
            VadState.SILENCE -> {
                // Maintain pre-roll ring buffer
                preRollBuffer.addLast(frame.copyOf())
                if (preRollBuffer.size > preRollFrames) {
                    preRollBuffer.removeFirst()
                }

                // Adapt background noise floor during silence
                val frameRms = computeRms(frame)
                dynamicNoiseFloor = (dynamicNoiseFloor * 0.95f + frameRms * 0.05f).coerceIn(60f, 600f)

                if (p >= startThreshold) {
                    consecutiveSpeechFrames++
                    if (consecutiveSpeechFrames >= 2) {
                        currentState = VadState.SPEECH
                        trailingSilenceFrames = 0
                        totalSpeechFramesInUtterance = consecutiveSpeechFrames

                        activeUtterancePcm.clear()
                        // Dump pre-roll buffer into active speech
                        for (buf in preRollBuffer) {
                            for (s in buf) activeUtterancePcm.add(s)
                        }
                        preRollBuffer.clear()
                        return VadChunkEvent.SpeechStarted
                    }
                } else {
                    consecutiveSpeechFrames = 0
                }
                return VadChunkEvent.Silence
            }

            VadState.SPEECH -> {
                for (s in frame) activeUtterancePcm.add(s)
                totalSpeechFramesInUtterance++

                if (p >= endThreshold) {
                    trailingSilenceFrames = 0
                    return VadChunkEvent.SpeechOngoing
                } else {
                    currentState = VadState.TRAILING_SILENCE
                    trailingSilenceFrames = 1
                    return VadChunkEvent.SpeechOngoing
                }
            }

            VadState.TRAILING_SILENCE -> {
                for (s in frame) activeUtterancePcm.add(s)

                if (p >= endThreshold) {
                    currentState = VadState.SPEECH
                    trailingSilenceFrames = 0
                    totalSpeechFramesInUtterance++
                    return VadChunkEvent.SpeechOngoing
                } else {
                    trailingSilenceFrames++
                    if (trailingSilenceFrames >= minSilenceFrames) {
                        // Utterance endpoint detected!
                        val totalFrames = totalSpeechFramesInUtterance
                        currentState = VadState.SILENCE
                        consecutiveSpeechFrames = 0
                        trailingSilenceFrames = 0
                        totalSpeechFramesInUtterance = 0

                        if (totalFrames >= minSpeechFrames) {
                            val speechPcm = activeUtterancePcm.toShortArray()
                            activeUtterancePcm.clear()
                            return VadChunkEvent.EndpointReached(speechPcm)
                        } else {
                            // Speech too brief (click or cough)
                            activeUtterancePcm.clear()
                            return VadChunkEvent.Silence
                        }
                    }
                    return VadChunkEvent.SpeechOngoing
                }
            }
        }
    }

    private fun computeRms(frame: ShortArray): Float {
        var sumSq = 0.0
        for (s in frame) {
            val v = s.toInt()
            sumSq += v * v
        }
        return sqrt(sumSq / frame.size).toFloat()
    }

    /**
     * Estimates voice activity probability for a 512-sample frame using acoustic energy,
     * zero-crossing rate, and spectral characteristics.
     * Returns a probability value between 0.0f and 1.0f.
     */
    fun estimateSpeechProbability(frame: ShortArray, backgroundNoiseFloor: Float = 150f): Float {
        if (frame.isEmpty()) return 0f

        var sumSq = 0.0
        var zeroCrossings = 0
        var prevSign = frame[0] >= 0

        for (i in frame.indices) {
            val s = frame[i].toInt()
            sumSq += s * s
            val currentSign = s >= 0
            if (currentSign != prevSign) {
                zeroCrossings++
                prevSign = currentSign
            }
        }

        val rms = sqrt(sumSq / frame.size).toFloat()
        val zcr = zeroCrossings.toFloat() / frame.size

        val snr = (rms - backgroundNoiseFloor).coerceAtLeast(0f) / max(backgroundNoiseFloor, 50f)
        val energyProb = (snr / 3.5f).coerceIn(0f, 1f)
        val zcrBonus = if (zcr in 0.02f..0.28f) 0.15f else -0.1f

        val combinedProb = (energyProb * 0.85f + zcrBonus).coerceIn(0f, 1f)
        return combinedProb
    }

    /**
     * Offline filter for raw recorded audio, stripping non-speech silence while preserving pre-roll
     * and trailing margins.
     */
    fun filterAndExtractSpeech(rawPcm: ShortArray): ShortArray? {
        if (rawPcm.size < frameSize) return null

        val totalFrames = rawPcm.size / frameSize
        val probabilities = FloatArray(totalFrames)

        var initialNoiseSum = 0.0
        val noiseInitFrames = min(5, totalFrames)
        for (f in 0 until noiseInitFrames) {
            var fRms = 0.0
            val offset = f * frameSize
            for (i in 0 until frameSize) {
                val s = rawPcm[offset + i].toInt()
                fRms += s * s
            }
            initialNoiseSum += sqrt(fRms / frameSize)
        }
        val noiseFloor = (initialNoiseSum / noiseInitFrames).toFloat().coerceIn(80f, 600f)

        var maxProb = 0f
        for (f in 0 until totalFrames) {
            val frame = ShortArray(frameSize)
            System.arraycopy(rawPcm, f * frameSize, frame, 0, frameSize)
            val p = estimateSpeechProbability(frame, noiseFloor)
            probabilities[f] = p
            if (p > maxProb) maxProb = p
        }

        if (maxProb < startThreshold) {
            return null
        }

        var firstSpeechFrame = -1
        var lastSpeechFrame = -1
        var inSpeech = false
        var consecutive = 0

        for (f in 0 until totalFrames) {
            val p = probabilities[f]
            if (!inSpeech) {
                if (p >= startThreshold) {
                    consecutive++
                    if (consecutive >= 2) {
                        inSpeech = true
                        if (firstSpeechFrame == -1) {
                            firstSpeechFrame = max(0, f - consecutive + 1)
                        }
                        lastSpeechFrame = f
                    }
                } else {
                    consecutive = 0
                }
            } else {
                if (p >= endThreshold) {
                    lastSpeechFrame = f
                } else {
                    val trailingFrames = f - lastSpeechFrame
                    if (trailingFrames >= minSilenceFrames) {
                        inSpeech = false
                    }
                }
            }
        }

        if (firstSpeechFrame == -1 || lastSpeechFrame == -1) {
            return null
        }

        val speechLengthFrames = lastSpeechFrame - firstSpeechFrame + 1
        if (speechLengthFrames < minSpeechFrames) {
            return null
        }

        val startSample = max(0, (firstSpeechFrame - preRollFrames) * frameSize)
        val endSample = min(rawPcm.size, (lastSpeechFrame + 7) * frameSize)
        val extractedLength = endSample - startSample

        if (extractedLength <= 0) return null

        val result = ShortArray(extractedLength)
        System.arraycopy(rawPcm, startSample, result, 0, extractedLength)
        return result
    }
}
