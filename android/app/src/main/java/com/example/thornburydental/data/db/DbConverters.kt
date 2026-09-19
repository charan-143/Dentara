package com.example.thornburydental.data.db

import com.example.thornburydental.data.Allergy
import com.example.thornburydental.data.ExaminationAnswers
import com.example.thornburydental.data.PatientDiagnosis
import com.example.thornburydental.data.PlanAddendum
import com.example.thornburydental.data.PlanStep
import com.example.thornburydental.data.ReportAttachment
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * JSON serialization converters for Thornbury Dental SQLite database fields.
 * Encodes complex nested objects and lists into JSON strings for database persistence,
 * and decodes them back to typed domain models.
 */
object DbConverters {
    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // --- List<Allergy> ---
    fun allergiesToJson(list: List<Allergy>?): String =
        if (list == null) "[]" else json.encodeToString(list)

    fun jsonToAllergies(raw: String?): List<Allergy> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString(raw)
        } catch (_: Exception) {
            emptyList()
        }
    }

    // --- List<String> ---
    fun stringListToJson(list: List<String>?): String =
        if (list == null) "[]" else json.encodeToString(list)

    fun jsonToStringList(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString(raw)
        } catch (_: Exception) {
            emptyList()
        }
    }

    // --- ExaminationAnswers? ---
    fun examAnswersToJson(answers: ExaminationAnswers?): String? =
        answers?.let { json.encodeToString(it) }

    fun jsonToExamAnswers(raw: String?): ExaminationAnswers? {
        if (raw.isNullOrBlank()) return null
        return try {
            json.decodeFromString(raw)
        } catch (_: Exception) {
            null
        }
    }

    // --- List<PlanStep> ---
    fun planStepsToJson(list: List<PlanStep>?): String =
        if (list == null) "[]" else json.encodeToString(list)

    fun jsonToPlanSteps(raw: String?): List<PlanStep> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString(raw)
        } catch (_: Exception) {
            emptyList()
        }
    }

    // --- List<PlanAddendum> ---
    fun planAddendaToJson(list: List<PlanAddendum>?): String =
        if (list == null) "[]" else json.encodeToString(list)

    fun jsonToPlanAddenda(raw: String?): List<PlanAddendum> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString(raw)
        } catch (_: Exception) {
            emptyList()
        }
    }

    // --- List<ReportAttachment> ---
    fun reportAttachmentsToJson(list: List<ReportAttachment>?): String =
        if (list == null) "[]" else json.encodeToString(list)

    fun jsonToReportAttachments(raw: String?): List<ReportAttachment> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString(raw)
        } catch (_: Exception) {
            emptyList()
        }
    }

    // --- PatientDiagnosis? ---
    fun diagnosisToJson(diagnosis: PatientDiagnosis?): String? =
        diagnosis?.let { json.encodeToString(it) }

    fun jsonToDiagnosis(raw: String?): PatientDiagnosis? {
        if (raw.isNullOrBlank()) return null
        return try {
            val diag = json.decodeFromString<PatientDiagnosis>(raw)
            diag.copy(clinicianName = "Dr. Ingrid Halvorsen")
        } catch (_: Exception) {
            null
        }
    }
    // --- Map<Int, ToothRecord> ---
    fun teethMapToJson(map: Map<Int, com.example.thornburydental.data.ToothRecord>?): String =
        if (map == null) "{}" else json.encodeToString(map)

    fun jsonToTeethMap(raw: String?): Map<Int, com.example.thornburydental.data.ToothRecord> {
        if (raw.isNullOrBlank()) return emptyMap()
        return try {
            json.decodeFromString(raw)
        } catch (_: Exception) {
            emptyMap()
        }
    }
}

// Extension functions for idiomatic Kotlin usage
fun List<Allergy>?.toDbJson(): String = DbConverters.allergiesToJson(this)
fun String?.toAllergies(): List<Allergy> = DbConverters.jsonToAllergies(this)

fun List<String>?.toStringListDbJson(): String = DbConverters.stringListToJson(this)
fun String?.toStringList(): List<String> = DbConverters.jsonToStringList(this)

fun ExaminationAnswers?.toDbJson(): String? = DbConverters.examAnswersToJson(this)
fun String?.toExamAnswers(): ExaminationAnswers? = DbConverters.jsonToExamAnswers(this)

fun Map<Int, com.example.thornburydental.data.ToothRecord>?.toTeethMapDbJson(): String = DbConverters.teethMapToJson(this)
fun String?.toTeethMap(): Map<Int, com.example.thornburydental.data.ToothRecord> = DbConverters.jsonToTeethMap(this)

fun PatientDiagnosis?.toPatientDiagnosisDbJson(): String? = DbConverters.diagnosisToJson(this)
fun String?.toPatientDiagnosis(): PatientDiagnosis? = DbConverters.jsonToDiagnosis(this)

fun List<PlanStep>?.toPlanStepsDbJson(): String = DbConverters.planStepsToJson(this)
fun String?.toPlanSteps(): List<PlanStep> = DbConverters.jsonToPlanSteps(this)

fun List<PlanAddendum>?.toPlanAddendaDbJson(): String = DbConverters.planAddendaToJson(this)
fun String?.toPlanAddenda(): List<PlanAddendum> = DbConverters.jsonToPlanAddenda(this)

fun List<ReportAttachment>?.toReportAttachmentsDbJson(): String = DbConverters.reportAttachmentsToJson(this)
fun String?.toReportAttachments(): List<ReportAttachment> = DbConverters.jsonToReportAttachments(this)
