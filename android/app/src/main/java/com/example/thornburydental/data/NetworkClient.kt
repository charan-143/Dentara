package com.example.thornburydental.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

// =============================================================================
// Data Transfer Objects (DTOs)
// =============================================================================

@Serializable
data class AttachmentDto(
    val id: String = "",
    val name: String = "",
    val sizeStr: String = "",
    val mimeType: String = "",
    val uri: String? = null
) {
    fun toDomain(): ReportAttachment = ReportAttachment(
        id = id,
        name = name,
        sizeStr = sizeStr,
        mimeType = mimeType,
        uri = uri
    )

    companion object {
        fun fromDomain(domain: ReportAttachment): AttachmentDto = AttachmentDto(
            id = domain.id,
            name = domain.name,
            sizeStr = domain.sizeStr,
            mimeType = domain.mimeType,
            uri = domain.uri
        )
    }
}

@Serializable
data class PatientDto(
    val id: String = "",
    val opNo: String = "",
    val name: String = "",
    val dob: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val medicalHistory: String = "",
    val familyHistory: String = "",
    val pastDentalHistory: String = "",
    val lastVisit: String = "never",
    val medicalAlerts: List<String> = emptyList(),
    val allergies: List<Allergy> = emptyList(),
    val examAnswers: ExaminationAnswers? = null
) {
    fun toDomain(): Patient = Patient(
        id = id.ifEmpty { "p-${System.currentTimeMillis()}" },
        opNo = opNo,
        name = name,
        dob = dob,
        phone = phone,
        email = email,
        address = address,
        medicalHistory = medicalHistory,
        familyHistory = familyHistory,
        pastDentalHistory = pastDentalHistory,
        lastVisit = lastVisit,
        medicalAlerts = medicalAlerts,
        allergies = allergies,
        teeth = DentalRepository.generateDefaultTeeth(),
        examAnswers = examAnswers
    )

    companion object {
        fun fromDomain(domain: Patient): PatientDto = PatientDto(
            id = domain.id,
            opNo = domain.opNo,
            name = domain.name,
            dob = domain.dob,
            phone = domain.phone,
            email = domain.email,
            address = domain.address,
            medicalHistory = domain.medicalHistory,
            familyHistory = domain.familyHistory,
            pastDentalHistory = domain.pastDentalHistory,
            lastVisit = domain.lastVisit,
            medicalAlerts = domain.medicalAlerts,
            allergies = domain.allergies,
            examAnswers = domain.examAnswers
        )
    }
}

@Serializable
data class AppointmentDto(
    val id: String = "",
    val patientId: String = "",
    val patientName: String = "",
    val patientOpNo: String = "",
    val patientDob: String = "",
    val clinicianId: String = "",
    val clinicianName: String = "",
    val time: String = "",
    val durationMin: Int = 45,
    val room: String = "Surgery 1",
    val procedure: String = "",
    val allergyList: String? = null,
    val status: String = "confirmed"
) {
    fun toDomain(): Appointment = Appointment(
        id = id.ifEmpty { "a-${System.currentTimeMillis()}" },
        patientId = patientId,
        patientName = patientName,
        patientOpNo = patientOpNo,
        patientDob = patientDob,
        clinicianId = clinicianId,
        clinicianName = clinicianName,
        time = time,
        durationMin = durationMin,
        room = room,
        procedure = procedure,
        allergyList = allergyList,
        status = status
    )

    companion object {
        fun fromDomain(domain: Appointment): AppointmentDto = AppointmentDto(
            id = domain.id,
            patientId = domain.patientId,
            patientName = domain.patientName,
            patientOpNo = domain.patientOpNo,
            patientDob = domain.patientDob,
            clinicianId = domain.clinicianId,
            clinicianName = domain.clinicianName,
            time = domain.time,
            durationMin = domain.durationMin,
            room = domain.room,
            procedure = domain.procedure,
            allergyList = domain.allergyList,
            status = domain.status
        )
    }
}

@Serializable
data class PrescriptionDto(
    val id: String = "",
    val patientId: String = "",
    val patientName: String = "",
    val clinicianName: String = "",
    val drugName: String = "",
    val dosage: String = "",
    val frequency: String = "",
    val duration: String = "",
    val instructions: String = "",
    val issueDate: String = "",
    val isDispensed: Boolean = false
) {
    fun toDomain(): Prescription = Prescription(
        id = id.ifEmpty { "rx-${System.currentTimeMillis()}" },
        patientId = patientId,
        patientName = patientName,
        clinicianName = clinicianName,
        drugName = drugName,
        dosage = dosage,
        frequency = frequency,
        duration = duration,
        instructions = instructions,
        issueDate = issueDate,
        isDispensed = isDispensed
    )

    companion object {
        fun fromDomain(domain: Prescription): PrescriptionDto = PrescriptionDto(
            id = domain.id,
            patientId = domain.patientId,
            patientName = domain.patientName,
            clinicianName = domain.clinicianName,
            drugName = domain.drugName,
            dosage = domain.dosage,
            frequency = domain.frequency,
            duration = domain.duration,
            instructions = domain.instructions,
            issueDate = domain.issueDate,
            isDispensed = domain.isDispensed
        )
    }
}

@Serializable
data class TreatmentPlanDto(
    val id: String = "",
    val patientId: String = "",
    val clinicianName: String = "",
    val diagnosis: String = "",
    val dateCreated: String = "",
    val isLocked: Boolean = true,
    val tamperHash: String = "",
    val steps: List<PlanStep> = emptyList(),
    val addenda: List<PlanAddendum> = emptyList()
) {
    fun toDomain(): TreatmentPlan = TreatmentPlan(
        id = id.ifEmpty { "plan-${System.currentTimeMillis()}" },
        patientId = patientId,
        clinicianName = clinicianName,
        diagnosis = diagnosis,
        dateCreated = dateCreated,
        isLocked = isLocked,
        tamperHash = tamperHash,
        steps = steps,
        addenda = addenda
    )

    companion object {
        fun fromDomain(domain: TreatmentPlan): TreatmentPlanDto = TreatmentPlanDto(
            id = domain.id,
            patientId = domain.patientId,
            clinicianName = domain.clinicianName,
            diagnosis = domain.diagnosis,
            dateCreated = domain.dateCreated,
            isLocked = domain.isLocked,
            tamperHash = domain.tamperHash,
            steps = domain.steps,
            addenda = domain.addenda
        )
    }
}

@Serializable
data class ReportDto(
    val id: String = "",
    val patientId: String = "",
    val clinicianName: String = "",
    val kind: String = "",
    val title: String = "",
    val summary: String = "",
    val takenAt: String = "",
    val releasedAt: String? = null,
    val image: String? = null,
    val attachments: List<AttachmentDto> = emptyList()
) {
    fun toDomain(): DiagnosticReport = DiagnosticReport(
        id = id.ifEmpty { "rp-${System.currentTimeMillis()}" },
        patientId = patientId,
        clinicianName = clinicianName,
        kind = kind,
        title = title,
        summary = summary,
        takenAt = takenAt,
        releasedAt = releasedAt,
        image = image,
        attachments = attachments.map { it.toDomain() }
    )

    companion object {
        fun fromDomain(domain: DiagnosticReport): ReportDto = ReportDto(
            id = domain.id,
            patientId = domain.patientId,
            clinicianName = domain.clinicianName,
            kind = domain.kind,
            title = domain.title,
            summary = domain.summary,
            takenAt = domain.takenAt,
            releasedAt = domain.releasedAt,
            image = domain.image,
            attachments = domain.attachments.map { AttachmentDto.fromDomain(it) }
        )
    }
}

// Request Body DTOs

@Serializable
private data class CreatePatientRequest(
    val name: String,
    val opNo: String,
    val dob: String,
    val phone: String,
    val email: String,
    val address: String
)

@Serializable
private data class BookAppointmentRequest(
    val patientId: String,
    val clinicianName: String,
    val time: String,
    val durationMin: Int,
    val room: String,
    val procedure: String
)

@Serializable
private data class UpdateStatusRequest(
    val status: String
)

@Serializable
private data class IssuePrescriptionRequest(
    val patientId: String,
    val drug: String,
    val dose: String,
    val frequency: String,
    val durationDays: Int,
    val indication: String,
    val overrideReason: String? = null
)

@Serializable
private data class CreateTreatmentPlanRequest(
    val patientId: String,
    val diagnosis: String,
    val details: String
)

@Serializable
private data class UploadReportRequest(
    val patientId: String,
    val kind: String,
    val title: String,
    val clinician: String,
    val summary: String,
    val attachments: List<AttachmentDto> = emptyList()
)

// =============================================================================
// Network HTTP REST Client
// =============================================================================

object NetworkClient {

    private const val PRIMARY_BASE_URL = "http://10.0.2.2:5178/api/v1"
    private const val FALLBACK_BASE_URL = "http://localhost:5178/api/v1"

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
        isLenient = true
    }

    private suspend fun <T> executeRequest(
        path: String,
        method: String = "GET",
        body: String? = null,
        parse: (String) -> T
    ): Result<T> = withContext(Dispatchers.IO) {
        val baseUrls = listOf(PRIMARY_BASE_URL, FALLBACK_BASE_URL)
        var lastError: Exception? = null

        for (baseUrl in baseUrls) {
            var connection: HttpURLConnection? = null
            try {
                val url = URL("$baseUrl$path")
                connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = method
                connection.connectTimeout = 3000
                connection.readTimeout = 5000
                connection.setRequestProperty("Accept", "application/json")

                if (body != null && (method == "POST" || method == "PUT" || method == "PATCH")) {
                    connection.doOutput = true
                    connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    connection.outputStream.use { os ->
                        val input = body.toByteArray(Charsets.UTF_8)
                        os.write(input, 0, input.size)
                    }
                }

                val responseCode = connection.responseCode
                val stream = if (responseCode in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream ?: connection.inputStream
                }

                val responseText = stream?.bufferedReader()?.use { it.readText() } ?: ""

                if (responseCode in 200..299) {
                    val parsed = parse(responseText)
                    return@withContext Result.success(parsed)
                } else {
                    lastError = IOException("HTTP $responseCode: $responseText")
                }
            } catch (e: Exception) {
                lastError = e
            } finally {
                connection?.disconnect()
            }
        }
        Result.failure(lastError ?: IOException("Network request to $path failed"))
    }

    suspend fun fetchPatients(): Result<List<Patient>> {
        return executeRequest("/patients", "GET") { text ->
            json.decodeFromString<List<PatientDto>>(text).map { it.toDomain() }
        }
    }

    suspend fun createPatient(
        name: String,
        opNo: String,
        dob: String,
        phone: String,
        email: String,
        address: String
    ): Result<Patient> {
        val req = CreatePatientRequest(name, opNo, dob, phone, email, address)
        val body = json.encodeToString(CreatePatientRequest.serializer(), req)
        return executeRequest("/patients", "POST", body) { text ->
            json.decodeFromString<PatientDto>(text).toDomain()
        }
    }

    suspend fun fetchAppointments(): Result<List<Appointment>> {
        return executeRequest("/appointments", "GET") { text ->
            json.decodeFromString<List<AppointmentDto>>(text).map { it.toDomain() }
        }
    }

    suspend fun bookAppointment(
        patientId: String,
        clinicianName: String,
        time: String,
        durationMin: Int,
        room: String,
        procedure: String
    ): Result<Appointment> {
        val req = BookAppointmentRequest(patientId, clinicianName, time, durationMin, room, procedure)
        val body = json.encodeToString(BookAppointmentRequest.serializer(), req)
        return executeRequest("/appointments", "POST", body) { text ->
            json.decodeFromString<AppointmentDto>(text).toDomain()
        }
    }

    suspend fun updateAppointmentStatus(appointmentId: String, status: String): Result<Boolean> {
        val req = UpdateStatusRequest(status)
        val body = json.encodeToString(UpdateStatusRequest.serializer(), req)
        return executeRequest("/appointments/$appointmentId/status", "PATCH", body) { true }
    }

    suspend fun fetchPrescriptions(): Result<List<Prescription>> {
        return executeRequest("/prescriptions", "GET") { text ->
            json.decodeFromString<List<PrescriptionDto>>(text).map { it.toDomain() }
        }
    }

    suspend fun issuePrescription(
        patientId: String,
        drug: String,
        dose: String,
        frequency: String,
        durationDays: Int,
        indication: String,
        overrideReason: String?
    ): Result<Prescription> {
        val req = IssuePrescriptionRequest(patientId, drug, dose, frequency, durationDays, indication, overrideReason)
        val body = json.encodeToString(IssuePrescriptionRequest.serializer(), req)
        return executeRequest("/prescriptions", "POST", body) { text ->
            json.decodeFromString<PrescriptionDto>(text).toDomain()
        }
    }

    suspend fun fetchTreatmentPlans(): Result<List<TreatmentPlan>> {
        return executeRequest("/treatment-plans", "GET") { text ->
            json.decodeFromString<List<TreatmentPlanDto>>(text).map { it.toDomain() }
        }
    }

    suspend fun createTreatmentPlan(patientId: String, diagnosis: String, details: String): Result<TreatmentPlan> {
        val req = CreateTreatmentPlanRequest(patientId, diagnosis, details)
        val body = json.encodeToString(CreateTreatmentPlanRequest.serializer(), req)
        return executeRequest("/treatment-plans", "POST", body) { text ->
            json.decodeFromString<TreatmentPlanDto>(text).toDomain()
        }
    }

    suspend fun fetchReports(): Result<List<DentalReport>> {
        return executeRequest("/reports", "GET") { text ->
            json.decodeFromString<List<ReportDto>>(text).map { it.toDomain() }
        }
    }

    suspend fun uploadReport(
        patientId: String,
        kind: String,
        title: String,
        clinician: String,
        summary: String,
        attachments: List<ReportAttachment>
    ): Result<DentalReport> {
        val attachmentDtos = attachments.map { AttachmentDto.fromDomain(it) }
        val req = UploadReportRequest(patientId, kind, title, clinician, summary, attachmentDtos)
        val body = json.encodeToString(UploadReportRequest.serializer(), req)
        return executeRequest("/reports", "POST", body) { text ->
            json.decodeFromString<ReportDto>(text).toDomain()
        }
    }

}
