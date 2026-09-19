package com.example.thornburydental.speech

import com.example.thornburydental.data.ToothNumberingSystem
import java.util.Locale
import kotlin.math.min

/** A periodontal probing site on one tooth. */
enum class PerioSite(val label: String) {
    MESIAL("Mesial"),
    DISTAL("Distal"),
    BUCCAL("Buccal"),
    LINGUAL("Lingual"),
    FACIAL("Facial"),
    PALATAL("Palatal"),
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

    /**
     * Nothing chartable was recognised. [reason] says why, so the clinician can rephrase instead
     * of guessing. This is the only correct outcome for dictation the parser does not understand:
     * the implementation this replaced turned anything longer than five characters into a
     * free-text clinical note, quietly filing misheard measurements as prose.
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
 * Scans left to right over tokens rather than matching one regex, because the regex approach it
 * replaced could not tell a tooth number from a depth. It read "pocket 3 on tooth 14" as tooth 1
 * at 4 mm, and silently dropped everything after the first reading in a multi-tooth utterance.
 *
 * Disambiguation rules, in order:
 *  - a number right after "tooth" / "number" / "#" is a tooth number;
 *  - a number right after "pocket" / "depth" / "mm" is a depth;
 *  - otherwise, once a tooth is known, a number up to [MAX_ORDINARY_DEPTH_MM] is a depth, and
 *    anything larger that is a valid tooth number starts a new tooth;
 *  - depths heard before any tooth is named are held and bound to the tooth named next.
 *
 * Anything it cannot place lowers confidence and is reported, never guessed at.
 */
class VoiceCommandParser {

    companion object {
        /** Parses below this are shown for review and never applied on their own. */
        const val AUTO_APPLY_CONFIDENCE = 0.75f

        /**
         * Pockets deeper than this are rare enough that a bare number this large is far more
         * likely to be a tooth number. A depth this deep is still accepted when explicitly
         * stated after a depth word, with a warning.
         */
        private const val MAX_ORDINARY_DEPTH_MM = 12
        private const val MAX_STATED_DEPTH_MM = 15

        private val TOOTH_WORDS = setOf("tooth", "teeth", "number", "#")

        private val DEPTH_WORDS = setOf(
            "pocket", "pockets", "depth", "depths", "probing", "pd",
            "mm", "millimeter", "millimeters", "millimetre", "millimetres"
        )

        private val BLEEDING_WORDS = setOf("bleeding", "bleeds", "bleed", "bop", "blood")

        private val NEGATION_WORDS = setOf("no", "none", "negative", "without", "not")

        private val SITE_WORDS = mapOf(
            "mesial" to PerioSite.MESIAL,
            "mesially" to PerioSite.MESIAL,
            "distal" to PerioSite.DISTAL,
            "distally" to PerioSite.DISTAL,
            "buccal" to PerioSite.BUCCAL,
            "buccally" to PerioSite.BUCCAL,
            "lingual" to PerioSite.LINGUAL,
            "lingually" to PerioSite.LINGUAL,
            "facial" to PerioSite.FACIAL,
            "facially" to PerioSite.FACIAL,
            "palatal" to PerioSite.PALATAL,
            "palatally" to PerioSite.PALATAL
        )

        /** Connective words carrying no clinical meaning; they must not count against confidence. */
        private val FILLER_WORDS = setOf(
            "on", "at", "the", "is", "of", "and", "a", "an", "with", "to", "for",
            "has", "have", "shows", "showing", "reading", "reads", "measures", "measuring",
            "site", "sites", "surface", "surfaces", "then", "next", "also", "plus", "in", "it"
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
     *
     * @param numberingSystem must be the scheme the clinic actually dictates in. It is never
     *        inferred from the numbers themselves; see [ToothNumberingSystem].
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

        return when (mode) {
            DictationTargetMode.PERIODONTAL_CHARTING -> parsePeriodontalDictation(trimmed, numberingSystem)
            DictationTargetMode.CLINICAL_NOTES -> parseClinicalNotesDictation(trimmed)
        }
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
                            // A stated site stays in force until another is stated or a new tooth
                            // begins, so "buccal 3 2 3" records three buccal readings.
                            currentSite = SITE_WORDS.getValue(word)
                            depthKeywordActive = false
                        }

                        word in DEPTH_WORDS -> depthKeywordActive = true

                        word in BLEEDING_WORDS -> {
                            if (!negatedHere) {
                                val last = measurements.lastOrNull()
                                if (last != null) {
                                    // Bleeding attaches to the reading it follows, not to the
                                    // whole utterance.
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
                                // Held until a tooth is named, so "pocket 3 on tooth 14" works.
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

        if (pendingDepths.isNotEmpty()) {
            warnings += "Ignored ${pendingDepths.size} reading(s) heard before any tooth was named."
        }
        if (teethWithoutDepth.isNotEmpty()) {
            warnings += "No depth was heard for tooth ${teethWithoutDepth.joinToString(", ")}."
        }
        if (deepReadings > 0) {
            warnings += "A depth over $MAX_ORDINARY_DEPTH_MM mm is unusual. Check before accepting."
        }

        val confidence = scoreConfidence(
            inferredTeeth = inferredTeeth,
            unresolvedTeeth = teethWithoutDepth.size,
            strayDepths = pendingDepths.size,
            deepReadings = deepReadings,
            unknownWords = unknownWords
        )

        return if (measurements.size == 1) {
            ParsedVoiceCommand.SinglePocketDepth(measurements.first(), confidence, warnings.toList())
        } else {
            ParsedVoiceCommand.MultiplePocketDepths(measurements.toList(), confidence, warnings.toList())
        }
    }

    /**
     * Narrative examination dictation. Free text is the point here, unlike periodontal mode where
     * unrecognised speech must never become prose in the chart.
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
        // A tooth taken from a bare number rather than a spoken "tooth" is the likeliest route to
        // charting against the wrong tooth, so it costs the most.
        if (inferredTeeth > 0) score -= 0.30f
        if (unresolvedTeeth > 0) score -= 0.30f
        if (strayDepths > 0) score -= 0.30f
        // A reading past 12 mm is either a rare severe pocket or a mishear. Either way it
        // must land below the auto-apply threshold so a human looks at it.
        if (deepReadings > 0) score -= 0.30f
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
                // "forty eight" is one number; "forty" on its own is still forty.
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
