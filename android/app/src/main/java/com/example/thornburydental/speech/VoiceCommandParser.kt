package com.example.thornburydental.speech

import java.util.Locale

/**
 * Parsed Voice Command data structures for dental charting.
 */
data class PocketDepthResult(
    val toothNumber: Int,
    val depthMm: Int,
    val isBleeding: Boolean = false,
    val surface: String = "general",
    val rawText: String = ""
)

data class ClinicalNoteResult(
    val noteText: String,
    val targetSection: String = "clinicianNotes"
)

sealed interface ParsedVoiceCommand {
    data class SinglePocketDepth(val entry: PocketDepthResult) : ParsedVoiceCommand
    data class MultiplePocketDepths(val entries: List<PocketDepthResult>) : ParsedVoiceCommand
    data class ClinicalNote(val entry: ClinicalNoteResult) : ParsedVoiceCommand
    data class Unrecognized(val rawTranscript: String) : ParsedVoiceCommand
}

/**
 * Domain-specific natural language voice command parser converting transcribed spoken text
 * into structured dental findings and pocket depths.
 */
class VoiceCommandParser {

    companion object {
        private val BLEEDING_KEYWORDS = setOf("bleeding", "bop", "blood", "b.o.p", "bleed")
        private val SURFACE_KEYWORDS = mapOf(
            "mesial" to "mesial",
            "distal" to "distal",
            "buccal" to "buccal",
            "lingual" to "lingual",
            "facial" to "facial",
            "palatal" to "palatal"
        )
    }

    /**
     * Parses a dictation transcript into structured commands based on active dictation target mode.
     */
    fun parseTranscript(transcript: String, mode: DictationTargetMode): ParsedVoiceCommand {
        val trimmed = transcript.trim()
        if (trimmed.isBlank()) return ParsedVoiceCommand.Unrecognized(transcript)

        return when (mode) {
            DictationTargetMode.PERIODONTAL_CHARTING -> parsePeriodontalDictation(trimmed)
            DictationTargetMode.CLINICAL_NOTES -> parseClinicalNotesDictation(trimmed)
        }
    }

    /**
     * Parses periodontal pocket depth commands like:
     * - "Tooth 14 pocket 3"
     * - "Tooth 19 pocket 5 mm bleeding"
     * - "Tooth 3 mesial pocket 4 mm"
     * - "Pockets 14 15 16: 3 4 3"
     */
    fun parsePeriodontalDictation(text: String): ParsedVoiceCommand {
        val lowerText = text.lowercase(Locale.getDefault())

        val isBleeding = BLEEDING_KEYWORDS.any { lowerText.contains(it) }

        var surface = "general"
        for ((key, value) in SURFACE_KEYWORDS) {
            if (lowerText.contains(key)) {
                surface = value
                break
            }
        }

        // Match patterns like "Tooth 14 pocket 3", "Tooth 14 pocket depth 3 mm", "14 pocket 3"
        val singlePattern = Regex("(?i)(?:tooth|number|#)?\\s*(\\d{1,2})\\s*(?:pocket|depth|probing|mm)?\\s*(?:mesial|distal|buccal|lingual|facial|palatal)?\\s*(?:pocket|depth|mm)?\\s*(\\d{1,2})")
        val match = singlePattern.find(text)

        if (match != null) {
            val toothNum = match.groupValues[1].toIntOrNull()
            val depth = match.groupValues[2].toIntOrNull()

            if (toothNum != null && depth != null && isTargetToothValid(toothNum) && depth in 1..15) {
                return ParsedVoiceCommand.SinglePocketDepth(
                    PocketDepthResult(
                        toothNumber = toothNum,
                        depthMm = depth,
                        isBleeding = isBleeding,
                        surface = surface,
                        rawText = text
                    )
                )
            }
        }

        // Check for multiple number sequences e.g. "Tooth 14 pocket 3 tooth 15 pocket 4"
        val multiPattern = Regex("(?i)(?:tooth|#)?\\s*(\\d{1,2})\\s*(?:pocket|depth|mm)?\\s*(\\d{1,2})")
        val allMatches = multiPattern.findAll(text).toList()

        if (allMatches.size > 1) {
            val results = mutableListOf<PocketDepthResult>()
            for (m in allMatches) {
                val toothNum = m.groupValues[1].toIntOrNull()
                val depth = m.groupValues[2].toIntOrNull()
                if (toothNum != null && depth != null && isTargetToothValid(toothNum) && depth in 1..15) {
                    results.add(
                        PocketDepthResult(
                            toothNumber = toothNum,
                            depthMm = depth,
                            isBleeding = isBleeding,
                            surface = surface,
                            rawText = m.value
                        )
                    )
                }
            }
            if (results.isNotEmpty()) {
                return ParsedVoiceCommand.MultiplePocketDepths(results)
            }
        }

        // Fallback: If text contains "note:" or general narrative in Periodontal mode
        if (text.length > 5) {
            return ParsedVoiceCommand.ClinicalNote(
                ClinicalNoteResult(noteText = text, targetSection = "clinicianNotes")
            )
        }

        return ParsedVoiceCommand.Unrecognized(text)
    }

    /**
     * Parses narrative clinical examination dictation text.
     */
    fun parseClinicalNotesDictation(text: String): ParsedVoiceCommand {
        var cleanText = text.trim()

        // Strip prefix triggers like "Note:", "Clinician note:", "Diagnosis:"
        var targetSection = "clinicianNotes"
        if (cleanText.startsWith("Diagnosis:", ignoreCase = true)) {
            targetSection = "diagnosis"
            cleanText = cleanText.substring("Diagnosis:".length).trim()
        } else if (cleanText.startsWith("Note:", ignoreCase = true)) {
            cleanText = cleanText.substring("Note:".length).trim()
        }

        // Capitalize first character
        if (cleanText.isNotEmpty()) {
            cleanText = cleanText.replaceFirstChar { it.uppercase() }
        }

        return ParsedVoiceCommand.ClinicalNote(
            ClinicalNoteResult(noteText = cleanText, targetSection = targetSection)
        )
    }

    private fun isTargetToothValid(num: Int): Boolean {
        // Universal permanent (1-32), FDI permanent (11-48), FDI primary (51-85)
        return (num in 1..32) || (num in 11..48) || (num in 51..85)
    }
}
