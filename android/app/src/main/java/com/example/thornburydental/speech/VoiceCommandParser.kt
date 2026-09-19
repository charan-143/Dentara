package com.example.thornburydental.speech

import com.example.thornburydental.data.ToothNumberingSystem
import java.util.Locale
import kotlin.math.min

/** A periodontal probing site on one tooth. */
enum class PerioSite(val label: String) {
    DISTOBUCCAL("Distobuccal"),
    BUCCAL("Buccal"),
    MESIOBUCCAL("Mesiobuccal"),
    DISTOLINGUAL("Distolingual"),
    LINGUAL("Lingual"),
    MESIOLINGUAL("Mesiolingual"),
    DISTOFACIAL("Distofacial"),
    FACIAL("Facial"),
    MESIOFACIAL("Mesiofacial"),
    DISTOPALATAL("Distopalatal"),
    PALATAL("Palatal"),
    MESIOPALATAL("Mesiopalatal"),
    MESIAL("Mesial"),
    DISTAL("Distal"),
    UNSPECIFIED("Site not stated")
}

data class PocketDepthResult(
    val toothNumber: Int,
    val depthMm: Int,
    val isBleeding: Boolean = false,
    val site: PerioSite = PerioSite.UNSPECIFIED,
    val rawText: String = ""
)

data class ClinicalNoteResult(
    val noteText: String,
    val targetSection: String = "clinicianNotes"
)

sealed interface ParsedVoiceCommand {
    /**
     * How much of the utterance the parser could account for, 0 to 1. Below
     * [VoiceCommandParser.AUTO_APPLY_CONFIDENCE] the reading must be reviewed by the clinician
     * rather than written to the chart on its own.
     */
    val confidence: Float

    /** Specific things to check before accepting. Shown to the clinician verbatim. */
    val warnings: List<String>

    data class SinglePocketDepth(
        val entry: PocketDepthResult,
        override val confidence: Float = 1f,
        override val warnings: List<String> = emptyList()
    ) : ParsedVoiceCommand

    data class MultiplePocketDepths(
        val entries: List<PocketDepthResult>,
        override val confidence: Float = 1f,
        override val warnings: List<String> = emptyList()
    ) : ParsedVoiceCommand

    data class ClinicalNote(
        val entry: ClinicalNoteResult,
        override val confidence: Float = 1f,
        override val warnings: List<String> = emptyList()
    ) : ParsedVoiceCommand

    data class SpokenConfirmation(
        val confirmed: Boolean,
        val rawTranscript: String,
        val phrase: String = rawTranscript,
        override val confidence: Float = 1f,
        override val warnings: List<String> = emptyList()
    ) : ParsedVoiceCommand

    data class SpokenUndo(
        val rawTranscript: String,
        override val confidence: Float = 1f,
        override val warnings: List<String> = emptyList()
    ) : ParsedVoiceCommand

    /**
     * Nothing chartable was recognised. [reason] says why, so the clinician can rephrase instead
     * of guessing.
     */
    data class Unrecognized(
        val rawTranscript: String,
        val reason: String = "No tooth number and pocket depth were recognised."
    ) : ParsedVoiceCommand {
        override val confidence: Float get() = 0f
        override val warnings: List<String> get() = emptyList()
    }
}

/**
 * Turns a dictation transcript into structured periodontal findings.
 *
 * Scans left to right over tokens rather than matching one regex, because the regex approach
 * could not tell a tooth number from a depth.
 *
 * Disambiguation rules, in order:
 *  - a number right after "tooth" / "number" / "#" is a tooth number;
 *  - a number right after "pocket" / "depth" / "mm" is a depth;
 *  - otherwise, once a tooth is known, a number up to [MAX_ORDINARY_DEPTH_MM] is a depth, and
 *    anything larger that is a valid tooth number starts a new tooth;
 *  - depths heard before any tooth is named are held and bound to the tooth named next;
 *  - 6 consecutive depths on a tooth without explicit individual sites are mapped to the standard
 *    6-site periodontal sequence: Distobuccal, Buccal, Mesiobuccal, Distolingual, Lingual, Mesiolingual;
 *  - depths > 12mm are flagged as implausible and require mandatory clinician review.
 */
class VoiceCommandParser {

    companion object {
        /** Parses below this are shown for review and never applied on their own. */
        const val AUTO_APPLY_CONFIDENCE = 0.75f

        /**
         * Pockets deeper than this (12 mm) are rare and physiologically extreme. Depths > 12 mm
         * are flagged as implausible for mandatory clinical review with spoken TTS alert.
         */
        const val MAX_ORDINARY_DEPTH_MM = 12
        const val MAX_STATED_DEPTH_MM = 15

        /** Standard continuous 6-site probing sequence for comprehensive periodontal charting. */
        val SIX_SITE_SEQUENCE = listOf(
            PerioSite.DISTOBUCCAL,
            PerioSite.BUCCAL,
            PerioSite.MESIOBUCCAL,
            PerioSite.DISTOLINGUAL,
            PerioSite.LINGUAL,
            PerioSite.MESIOLINGUAL
        )

        private val TOOTH_WORDS = setOf("tooth", "teeth", "number", "#")

        private val DEPTH_WORDS = setOf(
            "pocket", "pockets", "depth", "depths", "probing", "pd",
            "mm", "millimeter", "millimeters", "millimetre", "millimetres"
        )

        private val BLEEDING_WORDS = setOf("bleeding", "bleeds", "bleed", "bop", "blood")

        private val NEGATION_WORDS = setOf("no", "none", "negative", "without", "not")

        private val CONFIRM_WORDS = setOf(
            "confirm", "confirmed", "accept", "accepted", "apply", "applied",
            "yes", "correct", "save", "saved", "proceed", "okay", "ok"
        )

        private val DISCARD_WORDS = setOf(
            "cancel", "cancelled", "discard", "discarded", "reject", "rejected", "no", "drop"
        )

        private val UNDO_WORDS = setOf(
            "undo", "revert", "scratch that", "undo last", "go back"
        )

        private val SITE_WORDS = mapOf(
            "distobuccal" to PerioSite.DISTOBUCCAL,
            "disto-buccal" to PerioSite.DISTOBUCCAL,
            "midbuccal" to PerioSite.BUCCAL,
            "buccal" to PerioSite.BUCCAL,
            "buccally" to PerioSite.BUCCAL,
            "mesiobuccal" to PerioSite.MESIOBUCCAL,
            "mesio-buccal" to PerioSite.MESIOBUCCAL,
            "distolingual" to PerioSite.DISTOLINGUAL,
            "disto-lingual" to PerioSite.DISTOLINGUAL,
            "midlingual" to PerioSite.LINGUAL,
            "lingual" to PerioSite.LINGUAL,
            "lingually" to PerioSite.LINGUAL,
            "mesiolingual" to PerioSite.MESIOLINGUAL,
            "mesio-lingual" to PerioSite.MESIOLINGUAL,
            "distofacial" to PerioSite.DISTOFACIAL,
            "facial" to PerioSite.FACIAL,
            "facially" to PerioSite.FACIAL,
            "mesiofacial" to PerioSite.MESIOFACIAL,
            "distopalatal" to PerioSite.DISTOPALATAL,
            "palatal" to PerioSite.PALATAL,
            "palatally" to PerioSite.PALATAL,
            "mesiopalatal" to PerioSite.MESIOPALATAL,
            "mesial" to PerioSite.MESIAL,
            "mesially" to PerioSite.MESIAL,
            "distal" to PerioSite.DISTAL,
            "distally" to PerioSite.DISTAL
        )

        /** Connective words carrying no clinical meaning; they must not count against confidence. */
        private val FILLER_WORDS = setOf(
            "on", "at", "the", "is", "of", "and", "a", "an", "with", "to", "for",
            "has", "have", "shows", "showing", "reading", "reads", "measures", "measuring",
            "site", "sites", "surface", "surfaces", "then", "next", "also", "plus", "in", "it", "six", "sequence"
        )

        private val NUMBER_WORDS = mapOf(
            "zero" to 0, "one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5,
            "six" to 6, "seven" to 7, "eight" to 8, "nine" to 9, "ten" to 10,
            "eleven" to 11, "twelve" to 12, "thirteen" to 13, "fourteen" to 14,
            "fifteen" to 15, "sixteen" to 16, "seventeen" to 17, "eighteen" to 18,
            "nineteen" to 19
        )

        private val TENS_WORDS = mapOf("twenty" to 20, "thirty" to 30, "forty" to 40)
    }

    private sealed interface Token {
        data class Num(val value: Int) : Token
        data class Word(val text: String) : Token
    }

    /**
     * Parses a transcript in the active dictation mode.
     */
    fun parseTranscript(
        transcript: String,
        mode: DictationTargetMode,
        numberingSystem: ToothNumberingSystem = ToothNumberingSystem.DEFAULT
    ): ParsedVoiceCommand {
        val trimmed = transcript.trim()
        if (trimmed.isBlank()) {
            return ParsedVoiceCommand.Unrecognized(transcript, "Nothing was heard.")
        }

        // Check for hands-free spoken control commands first
        val controlCmd = checkSpokenControlCommand(trimmed)
        if (controlCmd != null) {
            return controlCmd
        }

        return when (mode) {
            DictationTargetMode.PERIODONTAL_CHARTING -> parsePeriodontalDictation(trimmed, numberingSystem)
            DictationTargetMode.CLINICAL_NOTES -> parseClinicalNotesDictation(trimmed)
        }
    }

    /**
     * Inspects if the spoken utterance is a hands-free confirmation, discard, or undo command.
     */
    fun checkSpokenControlCommand(text: String): ParsedVoiceCommand? {
        val normalized = text.lowercase(Locale.US).replace(Regex("[^a-z0-9\\s]"), " ").trim()
        val tokens = normalized.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return null

        if (tokens.size <= 4) {
            if (tokens.any { it in UNDO_WORDS } || normalized == "scratch that" || normalized == "undo last") {
                return ParsedVoiceCommand.SpokenUndo(text)
            }
            if (tokens.all { it in CONFIRM_WORDS } || normalized == "apply to chart" || normalized == "confirm reading") {
                return ParsedVoiceCommand.SpokenConfirmation(confirmed = true, rawTranscript = text)
            }
            if (tokens.all { it in DISCARD_WORDS } || normalized == "discard reading" || normalized == "cancel dictation") {
                return ParsedVoiceCommand.SpokenConfirmation(confirmed = false, rawTranscript = text)
            }
        }
        return null
    }

    fun parsePeriodontalDictation(
        text: String,
        numberingSystem: ToothNumberingSystem = ToothNumberingSystem.DEFAULT
    ): ParsedVoiceCommand {
        val tokens = tokenize(text)
        if (tokens.isEmpty()) {
            return ParsedVoiceCommand.Unrecognized(text, "Nothing was heard.")
        }

        val measurements = mutableListOf<PocketDepthResult>()
        val warnings = mutableListOf<String>()
        val teethWithoutDepth = mutableListOf<Int>()
        val pendingDepths = mutableListOf<Pair<Int, PerioSite>>()

        var currentTooth: Int? = null
        var currentToothHasMeasurement = false
        var currentSite = PerioSite.UNSPECIFIED
        var expectTooth = false
        var depthKeywordActive = false
        var pendingBleeding = false
        var previousWasNegation = false
        var inferredTeeth = 0
        var deepReadings = 0
        var unknownWords = 0

        fun record(tooth: Int, depth: Int, site: PerioSite) {
            measurements += PocketDepthResult(
                toothNumber = tooth,
                depthMm = depth,
                isBleeding = pendingBleeding,
                site = site,
                rawText = text
            )
            pendingBleeding = false
            currentToothHasMeasurement = true
            if (depth > MAX_ORDINARY_DEPTH_MM) deepReadings++
        }

        fun closeCurrentTooth() {
            val tooth = currentTooth
            if (tooth != null && !currentToothHasMeasurement) {
                teethWithoutDepth += tooth
            }
        }

        fun beginTooth(tooth: Int, inferred: Boolean) {
            closeCurrentTooth()
            currentTooth = tooth
            currentToothHasMeasurement = false
            if (inferred) inferredTeeth++
            if (pendingDepths.isNotEmpty()) {
                pendingDepths.forEach { (depth, site) -> record(tooth, depth, site) }
                pendingDepths.clear()
            }
        }

        for (token in tokens) {
            when (token) {
                is Token.Word -> {
                    val word = token.text
                    val negatedHere = previousWasNegation
                    previousWasNegation = word in NEGATION_WORDS

                    when {
                        word in TOOTH_WORDS -> {
                            expectTooth = true
                            depthKeywordActive = false
                        }

                        SITE_WORDS.containsKey(word) -> {
                            currentSite = SITE_WORDS.getValue(word)
                            depthKeywordActive = false
                        }

                        word in DEPTH_WORDS -> depthKeywordActive = true

                        word in BLEEDING_WORDS -> {
                            if (!negatedHere) {
                                val last = measurements.lastOrNull()
                                if (last != null) {
                                    measurements[measurements.lastIndex] = last.copy(isBleeding = true)
                                } else {
                                    pendingBleeding = true
                                }
                            }
                        }

                        word in NEGATION_WORDS -> Unit
                        word in FILLER_WORDS -> Unit
                        else -> unknownWords++
                    }
                }

                is Token.Num -> {
                    previousWasNegation = false
                    val value = token.value
                    val maxDepthHere = if (depthKeywordActive) MAX_STATED_DEPTH_MM else MAX_ORDINARY_DEPTH_MM

                    when {
                        expectTooth -> {
                            expectTooth = false
                            if (numberingSystem.isValidToothNumber(value)) {
                                beginTooth(value, inferred = false)
                            } else {
                                warnings += "\"$value\" is not a tooth number in " +
                                    "${numberingSystem.displayName} numbering (${numberingSystem.example})."
                            }
                        }

                        currentTooth == null -> {
                            if (value in 1..maxDepthHere) {
                                pendingDepths += value to currentSite
                            } else if (numberingSystem.isValidToothNumber(value)) {
                                beginTooth(value, inferred = true)
                            } else {
                                warnings += "\"$value\" is neither a pocket depth nor a tooth " +
                                    "number in ${numberingSystem.displayName} numbering."
                            }
                        }

                        value in 1..maxDepthHere -> record(currentTooth, value, currentSite)

                        numberingSystem.isValidToothNumber(value) -> beginTooth(value, inferred = true)

                        else -> {
                            warnings += "\"$value\" is neither a pocket depth nor a tooth " +
                                "number in ${numberingSystem.displayName} numbering."
                        }
                    }

                    depthKeywordActive = false
                }
            }
        }

        closeCurrentTooth()

        if (expectTooth) {
            warnings += "A tooth number was expected but not heard."
        }

        if (measurements.isEmpty()) {
            val reason = when {
                pendingDepths.isNotEmpty() ->
                    "Heard a depth but no tooth number. Say the tooth first, for example " +
                        "\"tooth 14 pocket 3 millimetres\"."
                teethWithoutDepth.isNotEmpty() ->
                    "Heard tooth ${teethWithoutDepth.joinToString(", ")} but no pocket depth."
                else -> "No tooth number and pocket depth were recognised."
            }
            return ParsedVoiceCommand.Unrecognized(text, reason)
        }

        // Standard Continuous 6-Site Sequence Detection:
        // If a single tooth has exactly 6 consecutive readings with UNSPECIFIED site,
        // map them in canonical periodontal order: DB, B, MB, DL, L, ML.
        val postProcessed = mutableListOf<PocketDepthResult>()
        val groupedByTooth = measurements.groupBy { it.toothNumber }
        for ((tooth, entries) in groupedByTooth) {
            if (entries.size == 6 && entries.all { it.site == PerioSite.UNSPECIFIED }) {
                entries.forEachIndexed { idx, entry ->
                    postProcessed += entry.copy(site = SIX_SITE_SEQUENCE[idx])
                }
            } else {
                postProcessed += entries
            }
        }

        if (pendingDepths.isNotEmpty()) {
            warnings += "Ignored ${pendingDepths.size} reading(s) heard before any tooth was named."
        }
        if (teethWithoutDepth.isNotEmpty()) {
            warnings += "No depth was heard for tooth ${teethWithoutDepth.joinToString(", ")}."
        }
        if (deepReadings > 0) {
            val deepDepths = postProcessed.filter { it.depthMm > MAX_ORDINARY_DEPTH_MM }.map { it.depthMm }
            warnings += "Implausible pocket depth of ${deepDepths.joinToString(", ")}mm (>12mm) flagged for clinical review."
            warnings += "A depth over $MAX_ORDINARY_DEPTH_MM mm is unusual. Check before accepting."
        }

        val confidence = scoreConfidence(
            inferredTeeth = inferredTeeth,
            unresolvedTeeth = teethWithoutDepth.size,
            strayDepths = pendingDepths.size,
            deepReadings = deepReadings,
            unknownWords = unknownWords
        )

        return if (postProcessed.size == 1) {
            ParsedVoiceCommand.SinglePocketDepth(postProcessed.first(), confidence, warnings.toList())
        } else {
            ParsedVoiceCommand.MultiplePocketDepths(postProcessed.toList(), confidence, warnings.toList())
        }
    }

    /**
     * Narrative examination dictation. Free text is the point here.
     */
    fun parseClinicalNotesDictation(text: String): ParsedVoiceCommand {
        var cleanText = text.trim()
        var targetSection = "clinicianNotes"

        if (cleanText.startsWith("Diagnosis:", ignoreCase = true)) {
            targetSection = "diagnosis"
            cleanText = cleanText.substring("Diagnosis:".length).trim()
        } else if (cleanText.startsWith("Note:", ignoreCase = true)) {
            cleanText = cleanText.substring("Note:".length).trim()
        }

        if (cleanText.isBlank()) {
            return ParsedVoiceCommand.Unrecognized(text, "Nothing was dictated after the heading.")
        }

        cleanText = cleanText.replaceFirstChar { it.uppercase() }

        return ParsedVoiceCommand.ClinicalNote(
            ClinicalNoteResult(noteText = cleanText, targetSection = targetSection)
        )
    }

    private fun scoreConfidence(
        inferredTeeth: Int,
        unresolvedTeeth: Int,
        strayDepths: Int,
        deepReadings: Int,
        unknownWords: Int
    ): Float {
        var score = 1f
        if (inferredTeeth > 0) score -= 0.30f
        if (unresolvedTeeth > 0) score -= 0.30f
        if (strayDepths > 0) score -= 0.30f
        // A reading past 12 mm is flagged as implausible; strictly penalize confidence below auto-apply threshold
        if (deepReadings > 0) score -= 0.35f
        score -= min(0.30f, unknownWords * 0.10f)
        return score.coerceIn(0f, 1f)
    }

    private fun tokenize(text: String): List<Token> {
        val words = text.lowercase(Locale.US)
            .replace(Regex("[^a-z0-9#]+"), " ")
            .trim()
            .split(" ")
            .filter { it.isNotBlank() }

        val tokens = mutableListOf<Token>()
        var index = 0
        while (index < words.size) {
            val word = words[index]

            val digits = word.toIntOrNull()
            if (digits != null) {
                tokens += Token.Num(digits)
                index++
                continue
            }

            val tens = TENS_WORDS[word]
            if (tens != null) {
                val unit = words.getOrNull(index + 1)?.let { NUMBER_WORDS[it] }
                if (unit != null && unit in 1..9) {
                    tokens += Token.Num(tens + unit)
                    index += 2
                } else {
                    tokens += Token.Num(tens)
                    index++
                }
                continue
            }

            val spelled = NUMBER_WORDS[word]
            if (spelled != null) {
                tokens += Token.Num(spelled)
                index++
                continue
            }

            tokens += Token.Word(word)
            index++
        }
        return tokens
    }
}
