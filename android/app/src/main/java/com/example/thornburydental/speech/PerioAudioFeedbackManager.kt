package com.example.thornburydental.speech

import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log

/**
 * Low-latency audio tone feedback generator for chairside perio probing and hands-free voice charting.
 *
 * Provides immediate auditory confirmation during examination so the dentist's eyes
 * never have to leave the patient's oral cavity:
 * - 1–3 mm normal pockets: Crisp, subtle low confirmation beep
 * - 4 mm moderate pockets: Neutral mid tone
 * - 5+ mm deep pockets or Bleeding on Probing (BOP): Distinct high alert double-tone
 */
object PerioAudioFeedbackManager {

    private const val TAG = "PerioAudioFeedback"

    private var toneGenerator: ToneGenerator? = try {
        ToneGenerator(AudioManager.STREAM_NOTIFICATION, 60)
    } catch (e: Throwable) {
        null
    }

    private fun safeLogW(msg: String, e: Throwable? = null) {
        try {
            Log.w(TAG, msg, e)
        } catch (_: Throwable) {}
    }

    private fun ensureToneGenerator(): ToneGenerator? {
        if (toneGenerator == null) {
            try {
                toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 60)
            } catch (e: Throwable) {
                safeLogW("ToneGenerator not available on this device", e)
            }
        }
        return toneGenerator
    }

    /**
     * Plays an instant auditory confirmation tailored to periodontal pocket depth and bleeding status.
     *
     * @param depthMm Periodontal pocket depth in millimeters (1-12 mm).
     * @param isBleeding Whether bleeding on probing (BOP) was detected at the site.
     */
    fun playPocketConfirmed(depthMm: Int, isBleeding: Boolean) {
        try {
            val tg = ensureToneGenerator() ?: return
            when {
                // Severe: Deep pathological pocket (>= 5mm) or active bleeding on probing
                depthMm >= 5 || isBleeding -> {
                    // Alert tone: urgent auditory cue for periodontal defect
                    tg.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, 140)
                }
                // Moderate pocket (4mm)
                depthMm == 4 -> {
                    // Mid tone
                    tg.startTone(ToneGenerator.TONE_PROP_BEEP2, 80)
                }
                // Normal healthy pocket (1-3mm)
                else -> {
                    // Subtle crisp low confirmation beep
                    tg.startTone(ToneGenerator.TONE_PROP_BEEP, 60)
                }
            }
        } catch (e: Throwable) {
            safeLogW("Failed to play perio audio tone", e)
        }
    }

    /**
     * Plays subtle confirmation tone for general voice command success.
     */
    fun playCommandSuccess() {
        try {
            ensureToneGenerator()?.startTone(ToneGenerator.TONE_PROP_ACK, 75)
        } catch (_: Throwable) {}
    }

    /**
     * Plays a low descending tone when an entry is undone via voice.
     */
    fun playUndoTone() {
        try {
            ensureToneGenerator()?.startTone(ToneGenerator.TONE_PROP_PROMPT, 90)
        } catch (_: Throwable) {}
    }

    fun release() {
        try {
            toneGenerator?.release()
            toneGenerator = null
        } catch (_: Throwable) {}
    }
}
