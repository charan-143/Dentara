package com.example.thornburydental.export

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.thornburydental.data.*
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Clean, structured vector PDF generator for patient health records.
 * Uses Android's native PdfDocument engine for fast, high-quality, crisp vector output.
 */
object PatientPdfGenerator {

    private const val PAGE_WIDTH = 595   // Standard A4 at 72 DPI
    private const val PAGE_HEIGHT = 842  // Standard A4 at 72 DPI
    private const val MARGIN_LEFT = 36f
    private const val MARGIN_RIGHT = 559f
    private const val MARGIN_TOP = 36f
    private const val MARGIN_BOTTOM = 806f
    private const val CONTENT_WIDTH = MARGIN_RIGHT - MARGIN_LEFT

    // Clinical Brand Colors (ARGB)
    private const val COLOR_PRIMARY = 0xFF0F766E.toInt()      // Thornbury Deep Teal
    private const val COLOR_PRIMARY_DARK = 0xFF115E59.toInt()
    private const val COLOR_PRIMARY_WASH = 0xFFCCFBF1.toInt()
    private const val COLOR_INK = 0xFF0F172A.toInt()          // Dark Slate Ink
    private const val COLOR_BODY = 0xFF334155.toInt()         // Body text
    private const val COLOR_MUTED = 0xFF64748B.toInt()        // Muted gray
    private const val COLOR_HAIRLINE = 0xFFE2E8F0.toInt()     // Border line
    private const val COLOR_CARD_BG = 0xFFF8FAFC.toInt()      // Soft surface
    private const val COLOR_ALERT_RED = 0xFFDC2626.toInt()    // Allergy/Alert Red
    private const val COLOR_ALERT_BG = 0xFFFEF2F2.toInt()     // Alert background
    private const val COLOR_SUCCESS_GREEN = 0xFF16A34A.toInt()

    fun generateAndSharePdf(
        context: Context,
        patient: Patient,
        options: PatientShareOptions,
        treatmentPlans: List<TreatmentPlan>,
        prescriptions: List<Prescription>,
        reports: List<DiagnosticReport>,
        appointments: List<Appointment>,
        clinicianName: String = "Dr. Dental Clinician"
    ): File {
        val pdfFile = generatePdfFile(
            context = context,
            patient = patient,
            options = options,
            treatmentPlans = treatmentPlans,
            prescriptions = prescriptions,
            reports = reports,
            appointments = appointments,
            clinicianName = clinicianName
        )

        sharePdfFile(context, pdfFile, patient)
        return pdfFile
    }

    fun generatePdfFile(
        context: Context,
        patient: Patient,
        options: PatientShareOptions,
        treatmentPlans: List<TreatmentPlan>,
        prescriptions: List<Prescription>,
        reports: List<DiagnosticReport>,
        appointments: List<Appointment>,
        clinicianName: String = "Dr. Dental Clinician"
    ): File {
        val document = PdfDocument()
        val pages = mutableListOf<PdfDocument.Page>()
        var pageNum = 1

        var currentPageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create()
        var currentPage = document.startPage(currentPageInfo)
        var canvas = currentPage.canvas
        pages.add(currentPage)

        var yPos = MARGIN_TOP

        // Initialize reusable paints
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_INK
            textSize = 10f
            typeface = Typeface.DEFAULT
        }
        val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_INK
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
        }
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_PRIMARY
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
        }
        val sectionHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_PRIMARY
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
        }
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1f
            color = COLOR_HAIRLINE
        }

        fun checkPageBreak(neededHeight: Float) {
            if (yPos + neededHeight > MARGIN_BOTTOM) {
                document.finishPage(currentPage)
                pageNum++
                currentPageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create()
                currentPage = document.startPage(currentPageInfo)
                canvas = currentPage.canvas
                pages.add(currentPage)
                yPos = MARGIN_TOP

                // Running Mini Header on subsequent pages
                fillPaint.color = COLOR_PRIMARY_WASH
                canvas.drawRect(MARGIN_LEFT, yPos, MARGIN_RIGHT, yPos + 22f, fillPaint)
                boldPaint.textSize = 9f
                boldPaint.color = COLOR_PRIMARY_DARK
                canvas.drawText("DENTARA CLINICAL HEALTH RECORD • ${patient.name} (${patient.opNo})", MARGIN_LEFT + 8f, yPos + 15f, boldPaint)
                textPaint.textSize = 8f
                textPaint.color = COLOR_MUTED
                canvas.drawText("Page $pageNum", MARGIN_RIGHT - 40f, yPos + 15f, textPaint)
                yPos += 32f
            }
        }

        // =========================================================================
        // 1. PRACTICE & DOCUMENT HEADER (PAGE 1)
        // =========================================================================
        // Top banner background
        fillPaint.color = COLOR_CARD_BG
        canvas.drawRoundRect(RectF(MARGIN_LEFT, yPos, MARGIN_RIGHT, yPos + 64f), 8f, 8f, fillPaint)
        strokePaint.color = COLOR_HAIRLINE
        canvas.drawRoundRect(RectF(MARGIN_LEFT, yPos, MARGIN_RIGHT, yPos + 64f), 8f, 8f, strokePaint)

        // Brand Title
        headerPaint.color = COLOR_PRIMARY
        headerPaint.textSize = 20f
        canvas.drawText("Dentara", MARGIN_LEFT + 14f, yPos + 26f, headerPaint)

        boldPaint.textSize = 9f
        boldPaint.color = COLOR_PRIMARY_DARK
        canvas.drawText("COMPREHENSIVE DENTAL PRACTICE & OPERATORY", MARGIN_LEFT + 14f, yPos + 40f, boldPaint)

        textPaint.textSize = 8f
        textPaint.color = COLOR_MUTED
        canvas.drawText("Electronic Health Record & Diagnostic Summary Export", MARGIN_LEFT + 14f, yPos + 52f, textPaint)

        // Header Right: Date & Clinician
        val sdf = SimpleDateFormat("dd/MM/yyyy • HH:mm", Locale.getDefault())
        val timestamp = sdf.format(Date())
        textPaint.textAlign = Paint.Align.RIGHT
        textPaint.textSize = 8f
        textPaint.color = COLOR_MUTED
        canvas.drawText("Exported: $timestamp", MARGIN_RIGHT - 14f, yPos + 24f, textPaint)
        canvas.drawText("Attending: $clinicianName", MARGIN_RIGHT - 14f, yPos + 38f, textPaint)
        boldPaint.textAlign = Paint.Align.RIGHT
        boldPaint.textSize = 8f
        boldPaint.color = COLOR_PRIMARY
        canvas.drawText("CONFIDENTIAL MEDICAL RECORD", MARGIN_RIGHT - 14f, yPos + 52f, boldPaint)
        textPaint.textAlign = Paint.Align.LEFT
        boldPaint.textAlign = Paint.Align.LEFT

        yPos += 74f

        // =========================================================================
        // 2. PATIENT EXECUTIVE DEMOGRAPHICS (IF INCLUDED)
        // =========================================================================
        if (options.includeDemographics) {
            checkPageBreak(90f)
            drawSectionHeader(canvas, sectionHeaderPaint, strokePaint, "1. Patient Demographics & Identification", yPos)
            yPos += 20f

            fillPaint.color = Color.WHITE
            canvas.drawRoundRect(RectF(MARGIN_LEFT, yPos, MARGIN_RIGHT, yPos + 68f), 6f, 6f, fillPaint)
            strokePaint.color = COLOR_HAIRLINE
            canvas.drawRoundRect(RectF(MARGIN_LEFT, yPos, MARGIN_RIGHT, yPos + 68f), 6f, 6f, strokePaint)

            val col1X = MARGIN_LEFT + 12f
            val col2X = MARGIN_LEFT + 180f
            val col3X = MARGIN_LEFT + 360f

            // Row 1
            val categoryStr = if (patient.isChild) "Child • Primary Dentition" else "Adult • Permanent Dentition"
            drawLabelValue(canvas, boldPaint, textPaint, "Full Name:", patient.name, col1X, yPos + 16f)
            drawLabelValue(canvas, boldPaint, textPaint, "OP Number:", patient.opNo, col2X, yPos + 16f)
            drawLabelValue(canvas, boldPaint, textPaint, "Category:", categoryStr, col3X, yPos + 16f)

            // Row 2
            val displayPhone = if (options.anonymizePatientData) "•••-•••-•••• [Referral Mode]" else patient.phone.ifBlank { "None" }
            val displayEmail = if (options.anonymizePatientData) "••••@••••.com [Referral Mode]" else patient.email.ifBlank { "None" }
            val displayAddress = if (options.anonymizePatientData) "[Anonymized for External Referral]" else patient.address.ifBlank { "Not provided" }

            drawLabelValue(canvas, boldPaint, textPaint, "Date of Birth:", com.example.thornburydental.util.formatAsDdMmYyyy(patient.dob).ifBlank { "Not recorded" }, col1X, yPos + 34f)
            drawLabelValue(canvas, boldPaint, textPaint, "Phone:", displayPhone, col2X, yPos + 34f)
            drawLabelValue(canvas, boldPaint, textPaint, "Last Visit:", patient.lastVisit, col3X, yPos + 34f)

            // Row 3
            drawLabelValue(canvas, boldPaint, textPaint, "Address:", displayAddress, col1X, yPos + 52f)

            yPos += 78f
        }

        // =========================================================================
        // 3. MEDICAL HISTORY, ALERTS & ALLERGIES
        // =========================================================================
        if (options.includeMedicalHistory) {
            checkPageBreak(110f)
            drawSectionHeader(canvas, sectionHeaderPaint, strokePaint, "2. Systemic Medical History & Clinical Alerts", yPos)
            yPos += 20f

            val hasAlerts = patient.medicalAlerts.isNotEmpty() || patient.allergies.isNotEmpty()
            if (hasAlerts) {
                fillPaint.color = COLOR_ALERT_BG
                strokePaint.color = COLOR_ALERT_RED
                canvas.drawRoundRect(RectF(MARGIN_LEFT, yPos, MARGIN_RIGHT, yPos + 36f), 6f, 6f, fillPaint)
                canvas.drawRoundRect(RectF(MARGIN_LEFT, yPos, MARGIN_RIGHT, yPos + 36f), 6f, 6f, strokePaint)

                boldPaint.color = COLOR_ALERT_RED
                boldPaint.textSize = 9f
                canvas.drawText("MEDICAL ALERTS & ALLERGIES:", MARGIN_LEFT + 10f, yPos + 14f, boldPaint)

                textPaint.color = COLOR_INK
                textPaint.textSize = 8.5f
                val alertsStr = buildString {
                    if (patient.allergies.isNotEmpty()) {
                        append("Allergies: ")
                        append(patient.allergies.joinToString("; ") { "${it.allergen} (${it.severity})" })
                    }
                    if (patient.medicalAlerts.isNotEmpty()) {
                        if (isNotEmpty()) append(" | ")
                        append("Alerts: ")
                        append(patient.medicalAlerts.joinToString(", "))
                    }
                }
                canvas.drawText(alertsStr.take(110), MARGIN_LEFT + 10f, yPos + 26f, textPaint)
                yPos += 42f
            }

            // Medical History Narrative
            fillPaint.color = COLOR_CARD_BG
            strokePaint.color = COLOR_HAIRLINE
            canvas.drawRoundRect(RectF(MARGIN_LEFT, yPos, MARGIN_RIGHT, yPos + 46f), 6f, 6f, fillPaint)
            canvas.drawRoundRect(RectF(MARGIN_LEFT, yPos, MARGIN_RIGHT, yPos + 46f), 6f, 6f, strokePaint)

            drawLabelValue(canvas, boldPaint, textPaint, "Medical History:", patient.medicalHistory.ifBlank { "No systemic medical conditions recorded." }, MARGIN_LEFT + 10f, yPos + 15f)
            drawLabelValue(canvas, boldPaint, textPaint, "Past Dental History:", patient.pastDentalHistory.ifBlank { "Routine general dentistry." }, MARGIN_LEFT + 10f, yPos + 28f)
            drawLabelValue(canvas, boldPaint, textPaint, "Family History:", patient.familyHistory.ifBlank { "No significant hereditary dental conditions." }, MARGIN_LEFT + 10f, yPos + 40f)

            yPos += 54f
        }

        // =========================================================================
        // 4. CLINICAL EXAMINATION & ODONTOGRAM FINDINGS
        // =========================================================================
        if (options.includeExamination) {
            checkPageBreak(120f)
            drawSectionHeader(canvas, sectionHeaderPaint, strokePaint, "3. Dental Examination & Odontogram Status", yPos)
            yPos += 20f

            // Teeth Condition Breakdown
            val abnormalTeeth = patient.teeth.values.filter { it.condition != ToothCondition.SOUND }
            fillPaint.color = COLOR_CARD_BG
            strokePaint.color = COLOR_HAIRLINE
            canvas.drawRoundRect(RectF(MARGIN_LEFT, yPos, MARGIN_RIGHT, yPos + 42f), 6f, 6f, fillPaint)
            canvas.drawRoundRect(RectF(MARGIN_LEFT, yPos, MARGIN_RIGHT, yPos + 42f), 6f, 6f, strokePaint)

            boldPaint.color = COLOR_INK
            boldPaint.textSize = 9f
            val headerTitle = if (patient.isChild) "Odontogram Charting Summary (20 Primary Teeth - FDI):" else "Odontogram Charting Summary (32 Adult Permanent Teeth - FDI):"
            canvas.drawText(headerTitle, MARGIN_LEFT + 10f, yPos + 14f, boldPaint)

            textPaint.color = COLOR_BODY
            textPaint.textSize = 8.5f
            if (abnormalTeeth.isEmpty()) {
                canvas.drawText("All charted teeth sound (no active decay, restorations, or missing teeth noted).", MARGIN_LEFT + 10f, yPos + 28f, textPaint)
            } else {
                val summaryList = abnormalTeeth.take(6).joinToString(", ") { "FDI ${it.fdiNumber} (${it.condition.label})" }
                val extra = if (abnormalTeeth.size > 6) " (+${abnormalTeeth.size - 6} more)" else ""
                canvas.drawText("Notable findings: $summaryList$extra", MARGIN_LEFT + 10f, yPos + 28f, textPaint)
            }
            yPos += 48f

            // Examination Questionnaire Answers
            patient.examAnswers?.let { exam ->
                checkPageBreak(80f)
                fillPaint.color = Color.WHITE
                canvas.drawRoundRect(RectF(MARGIN_LEFT, yPos, MARGIN_RIGHT, yPos + 60f), 6f, 6f, fillPaint)
                canvas.drawRoundRect(RectF(MARGIN_LEFT, yPos, MARGIN_RIGHT, yPos + 60f), 6f, 6f, strokePaint)

                drawLabelValue(canvas, boldPaint, textPaint, "Chief Complaint:", exam.chiefComplaints.ifEmpty { listOf("None reported") }.joinToString(", "), MARGIN_LEFT + 10f, yPos + 14f)
                drawLabelValue(canvas, boldPaint, textPaint, "Pain Severity:", exam.painSeverity.ifBlank { "Asymptomatic" }, MARGIN_LEFT + 10f, yPos + 27f)
                drawLabelValue(canvas, boldPaint, textPaint, "Caries Risk:", exam.cariesRisk.ifBlank { "Standard / Moderate" }, MARGIN_LEFT + 250f, yPos + 27f)
                drawLabelValue(canvas, boldPaint, textPaint, "Periodontal Screening:", exam.periodontalBleeding.ifEmpty { listOf("Healthy tissue") }.joinToString(", "), MARGIN_LEFT + 10f, yPos + 40f)
                drawLabelValue(canvas, boldPaint, textPaint, "Clinician Notes:", exam.clinicianNotes.ifBlank { "No additional directives." }, MARGIN_LEFT + 10f, yPos + 53f)

                yPos += 66f
            }
        }

        // =========================================================================
        // 5. CLINICAL DIAGNOSIS & PROGNOSIS
        // =========================================================================
        if (options.includeDiagnosis) {
            checkPageBreak(85f)
            drawSectionHeader(canvas, sectionHeaderPaint, strokePaint, "4. Clinical Diagnosis & Prognosis", yPos)
            yPos += 20f

            val diag = patient.diagnosis
            fillPaint.color = COLOR_CARD_BG
            strokePaint.color = COLOR_HAIRLINE
            canvas.drawRoundRect(RectF(MARGIN_LEFT, yPos, MARGIN_RIGHT, yPos + 58f), 6f, 6f, fillPaint)
            canvas.drawRoundRect(RectF(MARGIN_LEFT, yPos, MARGIN_RIGHT, yPos + 58f), 6f, 6f, strokePaint)

            if (diag != null && diag.primaryDiagnosis.isNotBlank()) {
                drawLabelValue(canvas, boldPaint, textPaint, "Primary Diagnosis:", diag.primaryDiagnosis, MARGIN_LEFT + 10f, yPos + 15f)
                drawLabelValue(canvas, boldPaint, textPaint, "Clinical Findings:", diag.clinicalFindings.ifBlank { "Consistent with primary diagnosis." }, MARGIN_LEFT + 10f, yPos + 28f)
                drawLabelValue(canvas, boldPaint, textPaint, "Prognosis:", diag.prognosis, MARGIN_LEFT + 10f, yPos + 41f)
                drawLabelValue(canvas, boldPaint, textPaint, "Date Recorded:", com.example.thornburydental.util.formatAsDdMmYyyy(diag.dateRecorded).ifBlank { "Active" }, MARGIN_LEFT + 250f, yPos + 41f)
                drawLabelValue(canvas, boldPaint, textPaint, "Diagnostician:", diag.clinicianName.ifBlank { clinicianName }, MARGIN_LEFT + 10f, yPos + 54f)
            } else {
                textPaint.color = COLOR_MUTED
                canvas.drawText("No formalized clinical diagnosis on file.", MARGIN_LEFT + 10f, yPos + 24f, textPaint)
            }

            yPos += 66f
        }

        // =========================================================================
        // 6. PHASED TREATMENT PLANS
        // =========================================================================
        if (options.includeTreatmentPlans) {
            checkPageBreak(80f)
            drawSectionHeader(canvas, sectionHeaderPaint, strokePaint, "5. Phased Treatment Plans & Procedures", yPos)
            yPos += 20f

            val pPlans = treatmentPlans.filter { it.patientId == patient.id }
            if (pPlans.isEmpty()) {
                fillPaint.color = COLOR_CARD_BG
                strokePaint.color = COLOR_HAIRLINE
                canvas.drawRoundRect(RectF(MARGIN_LEFT, yPos, MARGIN_RIGHT, yPos + 28f), 6f, 6f, fillPaint)
                canvas.drawRoundRect(RectF(MARGIN_LEFT, yPos, MARGIN_RIGHT, yPos + 28f), 6f, 6f, strokePaint)
                textPaint.color = COLOR_MUTED
                canvas.drawText("No active or proposed treatment plans.", MARGIN_LEFT + 10f, yPos + 17f, textPaint)
                yPos += 34f
            } else {
                pPlans.forEach { plan ->
                    val neededCardHeight = 32f + (plan.steps.size * 16f)
                    checkPageBreak(neededCardHeight)

                    fillPaint.color = Color.WHITE
                    strokePaint.color = COLOR_HAIRLINE
                    canvas.drawRoundRect(RectF(MARGIN_LEFT, yPos, MARGIN_RIGHT, yPos + neededCardHeight), 6f, 6f, fillPaint)
                    canvas.drawRoundRect(RectF(MARGIN_LEFT, yPos, MARGIN_RIGHT, yPos + neededCardHeight), 6f, 6f, strokePaint)

                    boldPaint.color = COLOR_PRIMARY_DARK
                    boldPaint.textSize = 9.5f
                    canvas.drawText("Plan #${plan.id}: ${plan.title.ifBlank { "Comprehensive Care" }}", MARGIN_LEFT + 10f, yPos + 15f, boldPaint)

                    val displayDate = com.example.thornburydental.util.formatAsDdMmYyyy(plan.dateCreated)
                    canvas.drawText("Created: $displayDate • Clinician: ${plan.clinicianName}", MARGIN_LEFT + 10f, yPos + 26f, textPaint)

                    var stepY = yPos + 40f
                    plan.steps.forEachIndexed { sIdx, step ->
                        boldPaint.color = if (step.completed) COLOR_SUCCESS_GREEN else COLOR_INK
                        boldPaint.textSize = 8.5f
                        val statusStr = if (step.completed) "[COMPLETED]" else "[PROPOSED]"
                        val toothStr = if (step.effectiveToothNumbers.isNotEmpty()) " (${step.toothDisplayString})" else ""
                        canvas.drawText("${sIdx + 1}. $statusStr ${step.procedure}$toothStr", MARGIN_LEFT + 18f, stepY, boldPaint)
                        stepY += 16f
                    }

                    yPos += neededCardHeight + 8f
                }
            }
        }

        // =========================================================================
        // 7. PRESCRIPTIONS & MEDICATIONS
        // =========================================================================
        if (options.includePrescriptions) {
            checkPageBreak(80f)
            drawSectionHeader(canvas, sectionHeaderPaint, strokePaint, "6. Prescriptions & Medication Formulary", yPos)
            yPos += 20f

            val pRx = prescriptions.filter { it.patientId == patient.id }
            if (pRx.isEmpty()) {
                fillPaint.color = COLOR_CARD_BG
                strokePaint.color = COLOR_HAIRLINE
                canvas.drawRoundRect(RectF(MARGIN_LEFT, yPos, MARGIN_RIGHT, yPos + 28f), 6f, 6f, fillPaint)
                canvas.drawRoundRect(RectF(MARGIN_LEFT, yPos, MARGIN_RIGHT, yPos + 28f), 6f, 6f, strokePaint)
                textPaint.color = COLOR_MUTED
                canvas.drawText("No prescriptions issued for this patient.", MARGIN_LEFT + 10f, yPos + 17f, textPaint)
                yPos += 34f
            } else {
                pRx.forEach { rx ->
                    checkPageBreak(38f)
                    fillPaint.color = COLOR_CARD_BG
                    strokePaint.color = COLOR_HAIRLINE
                    canvas.drawRoundRect(RectF(MARGIN_LEFT, yPos, MARGIN_RIGHT, yPos + 34f), 6f, 6f, fillPaint)
                    canvas.drawRoundRect(RectF(MARGIN_LEFT, yPos, MARGIN_RIGHT, yPos + 34f), 6f, 6f, strokePaint)

                    boldPaint.color = COLOR_PRIMARY
                    boldPaint.textSize = 9.5f
                    canvas.drawText("Rx: ${rx.drugName} • ${rx.dosage}", MARGIN_LEFT + 10f, yPos + 14f, boldPaint)

                    textPaint.color = COLOR_BODY
                    textPaint.textSize = 8.5f
                    canvas.drawText("Sig: ${rx.frequency} for ${rx.duration} | Instructions: ${rx.instructions}", MARGIN_LEFT + 10f, yPos + 26f, textPaint)

                    yPos += 40f
                }
            }
        }

        // =========================================================================
        // 8. DIAGNOSTIC REPORTS & IMAGING
        // =========================================================================
        if (options.includeReports) {
            checkPageBreak(80f)
            drawSectionHeader(canvas, sectionHeaderPaint, strokePaint, "7. Diagnostic Reports & Clinical Imaging", yPos)
            yPos += 20f

            val pReports = reports.filter { it.patientId == patient.id }
            if (pReports.isEmpty()) {
                fillPaint.color = COLOR_CARD_BG
                strokePaint.color = COLOR_HAIRLINE
                canvas.drawRoundRect(RectF(MARGIN_LEFT, yPos, MARGIN_RIGHT, yPos + 28f), 6f, 6f, fillPaint)
                canvas.drawRoundRect(RectF(MARGIN_LEFT, yPos, MARGIN_RIGHT, yPos + 28f), 6f, 6f, strokePaint)
                textPaint.color = COLOR_MUTED
                canvas.drawText("No diagnostic reports or imaging summaries on file.", MARGIN_LEFT + 10f, yPos + 17f, textPaint)
                yPos += 34f
            } else {
                pReports.forEach { rep ->
                    checkPageBreak(40f)
                    fillPaint.color = Color.WHITE
                    strokePaint.color = COLOR_HAIRLINE
                    canvas.drawRoundRect(RectF(MARGIN_LEFT, yPos, MARGIN_RIGHT, yPos + 36f), 6f, 6f, fillPaint)
                    canvas.drawRoundRect(RectF(MARGIN_LEFT, yPos, MARGIN_RIGHT, yPos + 36f), 6f, 6f, strokePaint)

                    boldPaint.color = COLOR_INK
                    boldPaint.textSize = 9f
                    canvas.drawText("${rep.kind}: ${rep.title}", MARGIN_LEFT + 10f, yPos + 14f, boldPaint)

                    textPaint.color = COLOR_BODY
                    textPaint.textSize = 8f
                    canvas.drawText("Summary: ${rep.summary.take(90)} | Clinician: ${rep.clinicianName}", MARGIN_LEFT + 10f, yPos + 26f, textPaint)

                    yPos += 42f
                }
            }
        }

        // =========================================================================
        // 9. APPOINTMENT & VISIT HISTORY
        // =========================================================================
        if (options.includeAppointments) {
            checkPageBreak(80f)
            drawSectionHeader(canvas, sectionHeaderPaint, strokePaint, "8. Appointment & Visit History", yPos)
            yPos += 20f

            val pAppts = appointments.filter { it.patientId == patient.id }
            if (pAppts.isEmpty()) {
                fillPaint.color = COLOR_CARD_BG
                strokePaint.color = COLOR_HAIRLINE
                canvas.drawRoundRect(RectF(MARGIN_LEFT, yPos, MARGIN_RIGHT, yPos + 28f), 6f, 6f, fillPaint)
                canvas.drawRoundRect(RectF(MARGIN_LEFT, yPos, MARGIN_RIGHT, yPos + 28f), 6f, 6f, strokePaint)
                textPaint.color = COLOR_MUTED
                canvas.drawText("No recorded appointment history.", MARGIN_LEFT + 10f, yPos + 17f, textPaint)
                yPos += 34f
            } else {
                pAppts.forEach { appt ->
                    checkPageBreak(30f)
                    fillPaint.color = COLOR_CARD_BG
                    strokePaint.color = COLOR_HAIRLINE
                    canvas.drawRoundRect(RectF(MARGIN_LEFT, yPos, MARGIN_RIGHT, yPos + 26f), 6f, 6f, fillPaint)
                    canvas.drawRoundRect(RectF(MARGIN_LEFT, yPos, MARGIN_RIGHT, yPos + 26f), 6f, 6f, strokePaint)

                    boldPaint.color = COLOR_INK
                    boldPaint.textSize = 8.5f
                    canvas.drawText("${com.example.thornburydental.util.formatAsDdMmYyyy(appt.date)} (${appt.time}) • ${appt.procedure}", MARGIN_LEFT + 10f, yPos + 14f, boldPaint)

                    textPaint.color = COLOR_MUTED
                    textPaint.textSize = 8f
                    canvas.drawText("Status: ${appt.status.replaceFirstChar { it.uppercase() }} | Clinician: ${appt.clinicianName}", MARGIN_LEFT + 10f, yPos + 23f, textPaint)

                    yPos += 30f
                }
            }
        }

        // =========================================================================
        // FOOTER / SIGN-OFF
        // =========================================================================
        checkPageBreak(50f)
        yPos += 10f
        strokePaint.color = COLOR_HAIRLINE
        canvas.drawLine(MARGIN_LEFT, yPos, MARGIN_RIGHT, yPos, strokePaint)
        yPos += 14f

        textPaint.color = COLOR_MUTED
        textPaint.textSize = 7.5f
        canvas.drawText("This health document is electronically generated by Dentara Clinical Systems. Confidential Medical Record.", MARGIN_LEFT, yPos, textPaint)
        yPos += 10f
        canvas.drawText("Authorizing Clinician: $clinicianName • Verification: Dentara Practice Systems", MARGIN_LEFT, yPos, textPaint)

        document.finishPage(currentPage)

        // Write to cache directory file
        val outputDir = File(context.cacheDir, "pdf_exports").apply { mkdirs() }
        val cleanOp = patient.opNo.replace("[^a-zA-Z0-9_-]".toRegex(), "")
        val cleanName = patient.name.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
        val outputFile = File(outputDir, "Dentara_EHR_${cleanOp}_$cleanName.pdf")

        FileOutputStream(outputFile).use { out ->
            document.writeTo(out)
        }
        document.close()

        return outputFile
    }

    private fun drawSectionHeader(
        canvas: Canvas,
        headerPaint: Paint,
        strokePaint: Paint,
        title: String,
        y: Float
    ) {
        headerPaint.textSize = 11f
        headerPaint.color = COLOR_PRIMARY
        canvas.drawText(title, MARGIN_LEFT, y + 10f, headerPaint)
        strokePaint.color = COLOR_PRIMARY_WASH
        strokePaint.strokeWidth = 1.5f
        canvas.drawLine(MARGIN_LEFT, y + 14f, MARGIN_RIGHT, y + 14f, strokePaint)
    }

    private fun drawLabelValue(
        canvas: Canvas,
        labelPaint: Paint,
        valuePaint: Paint,
        label: String,
        value: String,
        x: Float,
        y: Float
    ) {
        labelPaint.color = COLOR_MUTED
        labelPaint.textSize = 8.5f
        canvas.drawText(label, x, y, labelPaint)

        val labelWidth = labelPaint.measureText(label)
        valuePaint.color = COLOR_INK
        valuePaint.textSize = 8.5f
        canvas.drawText(" $value", x + labelWidth, y, valuePaint)
    }

    fun sharePdfFile(context: Context, pdfFile: File, patient: Patient) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_SUBJECT, "Dentara Health Record - ${patient.name} (${patient.opNo})")
            putExtra(Intent.EXTRA_TEXT, "Attached is the clinical health record and electronic dental chart for ${patient.name} (${patient.opNo}) generated via Dentara Clinical Systems.")
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, "Share Patient Health Record PDF via:")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    /**
     * Renders all pages of the generated PDF as Android Bitmaps for live in-app preview.
     */
    fun renderPdfBitmaps(pdfFile: File, scaleFactor: Float = 1.6f): List<Bitmap> {
        val bitmaps = mutableListOf<Bitmap>()
        var fileDescriptor: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        try {
            fileDescriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(fileDescriptor)
            val count = renderer.pageCount
            for (i in 0 until count) {
                val page = renderer.openPage(i)
                val width = (page.width * scaleFactor).toInt().coerceAtLeast(1)
                val height = (page.height * scaleFactor).toInt().coerceAtLeast(1)
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                bitmaps.add(bitmap)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try { renderer?.close() } catch (_: Exception) {}
            try { fileDescriptor?.close() } catch (_: Exception) {}
        }
        return bitmaps
    }

    /**
     * Saves the PDF directly to the device's public Downloads directory.
     */
    fun savePdfToPublicDownloads(context: Context, sourceFile: File, patient: Patient): Result<File> {
        return runCatching {
            val cleanOp = patient.opNo.replace("[^a-zA-Z0-9_-]".toRegex(), "")
            val cleanName = patient.name.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
            val fileName = "Dentara_EHR_${cleanOp}_$cleanName.pdf"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val itemUri = resolver.insert(collection, values)
                    ?: throw IllegalStateException("Failed to create MediaStore entry in Downloads")

                resolver.openOutputStream(itemUri)?.use { out ->
                    FileInputStream(sourceFile).use { input ->
                        input.copyTo(out)
                    }
                }

                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(itemUri, values, null, null)
                sourceFile
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                downloadsDir.mkdirs()
                val targetFile = File(downloadsDir, fileName)
                sourceFile.copyTo(targetFile, overwrite = true)
                targetFile
            }
        }
    }

    /**
     * Opens the PDF in the device's default PDF viewer.
     */
    fun viewPdfFile(context: Context, pdfFile: File, patient: Patient) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val chooser = Intent.createChooser(intent, "Open Patient Health Record PDF with:")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
