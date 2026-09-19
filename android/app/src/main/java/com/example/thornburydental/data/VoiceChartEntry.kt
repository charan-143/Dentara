package com.example.thornburydental.data

import kotlinx.serialization.Serializable

/**
 * Audit record for one value written to a chart by voice dictation.
 *
 * Without this, a dictated pocket depth is indistinguishable from one a clinician typed and
 * checked. Anyone reading the chart later - a colleague, an auditor, the clinician themselves
 * after a long list - needs to be able to tell which values came from speech recognition, what
 * was actually said, and whether the parser was confident enough to apply it unattended.
 *
 * Stored inside the ExaminationAnswers JSON blob rather than in its own table, so it needs no
 * schema migration: the field has a default and the database Json is configured with
 * ignoreUnknownKeys, so older records deserialize and newer records stay readable.
 */
@Serializable
data class VoiceChartEntry(
    /** Epoch milliseconds, matching the convention used elsewhere in this data layer. */
    val recordedAtEpochMs: Long = System.currentTimeMillis(),

    /** Who was signed in when the value was dictated. Empty when nobody was. */
    val clinicianName: String = "",

    /** Exactly what the decoder produced, before parsing. The evidence for the value. */
    val transcript: String = "",

    /** Parser confidence, 0 to 1, as scored by VoiceCommandParser. */
    val confidence: Float = 0f,

    /** Human-readable summary of what this entry wrote, for an audit view. */
    val applied: String = "",

    /**
     * True when confidence was below the auto-apply threshold, meaning a clinician had to accept
     * it explicitly. False means it was written without anyone confirming the value.
     */
    val reviewRequired: Boolean = false
)
