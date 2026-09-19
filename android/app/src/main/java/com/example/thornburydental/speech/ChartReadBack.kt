package com.example.thornburydental.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/**
 * Builds the sentence spoken back to the clinician after a dictation is parsed.
 *
 * Read-back is the confirmation step that makes hands-free charting safe. A clinician with a
 * probe in the patient's mouth is not looking at the screen, so a value they cannot see is a
 * value they cannot check. Hearing "tooth 14, buccal, 3 millimetres" is what lets them catch
 * "tooth 40" or "13 millimetres" before it reaches the chart.
 *
 * Kept as a pure function so the wording can be tested without a speech engine.
 */
object ChartReadBack {

    /** Beyond this many readings, the tail is summarised rather than recited one by one. */
    private const val MAX_SPOKEN_READINGS = 6

    fun spokenSummary(command: ParsedVoiceCommand): String = when (command) {
        is ParsedVoiceCommand.Unrecognized ->
            "Not recognised. " + command.reason

        is ParsedVoiceCommand.SpokenConfirmation ->
            if (command.confirmed) "Confirmed. Applied to chart." else "Discarded."

        is ParsedVoiceCommand.SpokenUndo ->
            "Undone."

        is ParsedVoiceCommand.SinglePocketDepth ->
            prefixFor(command) + describe(command.entry) + "."

        is ParsedVoiceCommand.MultiplePocketDepths -> {
            val hasImplausible = command.entries.any { it.depthMm > VoiceCommandParser.MAX_ORDINARY_DEPTH_MM }
            val implausiblePrefix = if (hasImplausible) "Warning: Implausible depth over 12 millimetres detected. " else ""
            val isSixSiteSequence = command.entries.size == 6 && command.entries.map { it.toothNumber }.distinct().size == 1
            val spoken = command.entries.take(MAX_SPOKEN_READINGS).map { describe(it) }
            val remaining = command.entries.size - spoken.size
            val tail = if (remaining > 0) " And $remaining more." else ""
            val header = if (isSixSiteSequence) {
                "Tooth ${command.entries.first().toothNumber}, 6-site sequence. "
            } else {
                "${command.entries.size} readings. "
            }
            implausiblePrefix + prefixFor(command) + header + spoken.joinToString(". ") + "." + tail
        }

        is ParsedVoiceCommand.ClinicalNote -> {
            val words = command.entry.noteText.trim().split(Regex("\\s+")).count { it.isNotBlank() }
            val section = if (command.entry.targetSection == "diagnosis") "Diagnosis" else "Clinical note"
            prefixFor(command) + "$section recorded, $words words."
        }
    }

    /**
     * A reading the parser was unsure about is announced as needing a look, so the clinician is
     * told to check rather than left to notice the quieter on-screen warning.
     */
    private fun prefixFor(command: ParsedVoiceCommand): String {
        val hasImplausible = when (command) {
            is ParsedVoiceCommand.SinglePocketDepth -> command.entry.depthMm > VoiceCommandParser.MAX_ORDINARY_DEPTH_MM
            is ParsedVoiceCommand.MultiplePocketDepths -> command.entries.any { it.depthMm > VoiceCommandParser.MAX_ORDINARY_DEPTH_MM }
            else -> false
        }
        return if (hasImplausible) {
            "Warning: Implausible depth flagged for review. "
        } else if (command.confidence < VoiceCommandParser.AUTO_APPLY_CONFIDENCE) {
            "Please check. "
        } else {
            ""
        }
    }

    private fun describe(entry: PocketDepthResult): String {
        val site = if (entry.site == PerioSite.UNSPECIFIED) {
            ""
        } else {
            ", " + entry.site.label.lowercase(Locale.US)
        }
        val millimetres = if (entry.depthMm == 1) "1 millimetre" else "${entry.depthMm} millimetres"
        val bleeding = if (entry.isBleeding) ", bleeding" else ""
        val implausibleTag = if (entry.depthMm > VoiceCommandParser.MAX_ORDINARY_DEPTH_MM) " (implausible depth)" else ""
        return "Tooth ${entry.toothNumber}$site, $millimetres$bleeding$implausibleTag"
    }
}

/**
 * Speaks chart read-back through the device text-to-speech engine.
 *
 * Failure is silent by design: a device with no usable TTS voice must not block charting. The
 * on-screen preview remains the authoritative confirmation, and read-back is an additional
 * channel rather than the only one.
 */
class ChartReadBackSpeaker(context: Context) {

    companion object {
        private const val UTTERANCE_ID = "dentara-chart-readback"
    }

    @Volatile
    private var ready = false

    private val engine: TextToSpeech? = try {
        TextToSpeech(context.applicationContext) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (!ready) {
                safeReadBackLog("Text-to-speech unavailable (status $status); read-back disabled.")
            }
        }
    } catch (t: Throwable) {
        safeReadBackLog("Could not create a text-to-speech engine; read-back disabled.", t)
        null
    }

    fun speak(text: String) {
        if (text.isBlank()) return
        val tts = engine ?: return
        if (!ready) return
        try {
            // Flush rather than queue: the newest reading is the one being confirmed, and a
            // backlog of stale readings would be actively misleading.
            tts.setLanguage(Locale.US)
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
        } catch (t: Throwable) {
            safeReadBackLog("Failed to speak chart read-back", t)
        }
    }

    fun shutdown() {
        try {
            engine?.stop()
            engine?.shutdown()
        } catch (t: Throwable) {
            safeReadBackLog("Error shutting down text-to-speech", t)
        }
        ready = false
    }
}

/** Log is stubbed out under JVM unit tests; fall back to stdout there. */
private fun safeReadBackLog(message: String, t: Throwable? = null) {
    try {
        Log.w("ChartReadBack", message, t)
    } catch (_: Throwable) {
        println("[ChartReadBack] $message ${t?.message ?: ""}")
    }
}

/** Creates a speaker tied to the composition, shut down when it leaves. */
@Composable
fun rememberChartReadBackSpeaker(): ChartReadBackSpeaker {
    val context = LocalContext.current
    val speaker = remember(context) { ChartReadBackSpeaker(context) }
    DisposableEffect(speaker) {
        onDispose { speaker.shutdown() }
    }
    return speaker
}
