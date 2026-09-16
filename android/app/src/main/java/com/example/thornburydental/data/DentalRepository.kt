package com.example.thornburydental.data

import android.util.Log
import com.example.thornburydental.data.db.LocalDatabaseManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.thornburydental.util.addDaysToIsoDate
import com.example.thornburydental.util.parseTimeToMinutes
import com.example.thornburydental.util.todayIsoDate
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// =============================================================================
// Thornbury Dental Repository
// Single source of truth containing realistic clinical seed data backed by SQLite
// =============================================================================

object DentalRepository {

    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // -------------------------------------------------------------------
    // Lightweight, session-scoped app preferences (not yet persisted to
    // disk — they reset on process restart, unlike patient/clinical data).
    // -------------------------------------------------------------------
    private val _isDarkModeEnabled = MutableStateFlow(false)
    val isDarkModeEnabled: StateFlow<Boolean> = _isDarkModeEnabled.asStateFlow()
    fun setDarkModeEnabled(enabled: Boolean) {
        _isDarkModeEnabled.value = enabled
    }

    private val _defaultSurgeryRoom = MutableStateFlow("Surgery 1")
    val defaultSurgeryRoom: StateFlow<String> = _defaultSurgeryRoom.asStateFlow()
    fun setDefaultSurgeryRoom(room: String) {
        _defaultSurgeryRoom.value = room
    }

    private val _clinicianDisplayName = MutableStateFlow("Dr. Ingrid Halvorsen")
    val clinicianDisplayName: StateFlow<String> = _clinicianDisplayName.asStateFlow()

    fun updateClinicianName(name: String) {
        val trimmed = name.trim().ifBlank { "Dr. Ingrid Halvorsen" }
        _clinicianDisplayName.value = trimmed
        AuthRepository.updateCurrentUserName(trimmed)
        val currentPrefs = _userPreferences.value ?: UserProfilePreferences()
        val updatedPrefs = currentPrefs.copy(fullName = trimmed)
        _userPreferences.value = updatedPrefs
        repositoryScope.launch {
            if (LocalDatabaseManager.isInitialized) {
                LocalDatabaseManager.userPreferencesDao.savePreferences(updatedPrefs)
            }
        }
    }

    private val _morningReminderEnabled = MutableStateFlow(true)
    val morningReminderEnabled: StateFlow<Boolean> = _morningReminderEnabled.asStateFlow()

    private val _morningReminderTime = MutableStateFlow("08:00")
    val morningReminderTime: StateFlow<String> = _morningReminderTime.asStateFlow()

    private val _chairsideReminderDefaultMin = MutableStateFlow(15)
    val chairsideReminderDefaultMin: StateFlow<Int> = _chairsideReminderDefaultMin.asStateFlow()

    fun updateMorningReminderSettings(enabled: Boolean, timeString: String) {
        _morningReminderEnabled.value = enabled
        _morningReminderTime.value = timeString
        val currentPrefs = _userPreferences.value ?: UserProfilePreferences()
        val updated = currentPrefs.copy(
            morningReminderEnabled = enabled,
            morningReminderTime = timeString
        )
        _userPreferences.value = updated
        repositoryScope.launch {
            if (LocalDatabaseManager.isInitialized) {
                LocalDatabaseManager.userPreferencesDao.savePreferences(updated)
            }
        }
        val ctx = LocalDatabaseManager.appContext
        if (ctx != null) {
            if (enabled) {
                com.example.thornburydental.reminder.ReminderManager.scheduleDailyMorningBriefing(ctx, timeString)
            } else {
                com.example.thornburydental.reminder.ReminderManager.cancelDailyMorningBriefing(ctx)
            }
        }
    }

    fun updateChairsideReminderDefault(leadMin: Int) {
        _chairsideReminderDefaultMin.value = leadMin
        val currentPrefs = _userPreferences.value ?: UserProfilePreferences()
        val updated = currentPrefs.copy(chairsideReminderDefaultMin = leadMin)
        _userPreferences.value = updated
        repositoryScope.launch {
            if (LocalDatabaseManager.isInitialized) {
                LocalDatabaseManager.userPreferencesDao.savePreferences(updated)
            }
        }
    }

    // Note: this flag is not yet wired to any real reminder/notification
    // scheduling (no such system exists in the app yet) — it only makes the
    // switch stop silently resetting between screens. Building actual
    // appointment-reminder notifications is a separate feature, not a gap fix.
    private val _appointmentRemindersEnabled = MutableStateFlow(true)
    val appointmentRemindersEnabled: StateFlow<Boolean> = _appointmentRemindersEnabled.asStateFlow()
    fun setAppointmentRemindersEnabled(enabled: Boolean) {
        _appointmentRemindersEnabled.value = enabled
    }

    init {
        initializeFromDatabase()
        startBackgroundSync()
    }

    fun initializeFromDatabase() {
        repositoryScope.launch {
            if (LocalDatabaseManager.isInitialized) {
                if (LocalDatabaseManager.isDatabaseEmpty()) {
                    LocalDatabaseManager.seedInitialDataIfEmpty(
                        patients = initialPatients,
                        appointments = initialAppointments,
                        prescriptions = initialPrescriptions,
                        treatmentPlans = initialTreatmentPlans,
                        reports = initialReports
                    )
                }
                reloadFromDatabase()
            }
        }
    }

    suspend fun reloadFromDatabase() = withContext(Dispatchers.IO) {
        if (!LocalDatabaseManager.isInitialized) return@withContext
        try {
            val dbPatients = LocalDatabaseManager.patientDao.getAllPatients()
            if (dbPatients.isNotEmpty()) {
                _patients.value = dbPatients
            }
            val dbAppointments = LocalDatabaseManager.appointmentDao.getAllAppointments()
            if (dbAppointments.isNotEmpty()) {
                _appointments.value = dbAppointments
            }
            val dbPlans = LocalDatabaseManager.treatmentPlanDao.getAllTreatmentPlans()
            if (dbPlans.isNotEmpty()) {
                _treatmentPlans.value = dbPlans
            }
            val dbPrescriptions = LocalDatabaseManager.prescriptionDao.getAllPrescriptions()
            if (dbPrescriptions.isNotEmpty()) {
                _prescriptions.value = dbPrescriptions
            }
            val dbReports = LocalDatabaseManager.reportDao.getAllReports()
            if (dbReports.isNotEmpty()) {
                _reports.value = dbReports
            }
            val dbPrefs = LocalDatabaseManager.userPreferencesDao.getPreferences()
            if (dbPrefs != null) {
                _userPreferences.value = dbPrefs
                if (dbPrefs.fullName.isNotBlank()) {
                    _clinicianDisplayName.value = dbPrefs.fullName
                    AuthRepository.updateCurrentUserName(dbPrefs.fullName)
                }
                _morningReminderEnabled.value = dbPrefs.morningReminderEnabled
                _morningReminderTime.value = dbPrefs.morningReminderTime
                _chairsideReminderDefaultMin.value = dbPrefs.chairsideReminderDefaultMin
            }
            val dbPresets = LocalDatabaseManager.medicationPresetDao.getAllPresets()
            if (dbPresets.isNotEmpty()) {
                _medicationPresets.value = dbPresets
            }
        } catch (e: Exception) {
            Log.e("DentalRepository", "Error reloading from local database", e)
        }
    }

    private fun startBackgroundSync() {
        repositoryScope.launch {
            refreshFromNetwork()
        }
    }

    suspend fun refreshFromNetwork() {
        NetworkClient.fetchPatients().onSuccess { fetched ->
            if (fetched.isNotEmpty()) {
                _patients.update { existing ->
                    val existingIds = existing.map { it.id }.toSet()
                    val newItems = fetched.filter { it.id !in existingIds }
                    if (LocalDatabaseManager.isInitialized) {
                        newItems.forEach { LocalDatabaseManager.patientDao.insertPatient(it) }
                    }
                    existing + newItems
                }
            }
        }

        NetworkClient.fetchAppointments().onSuccess { fetched ->
            if (fetched.isNotEmpty()) {
                _appointments.update { existing ->
                    val existingIds = existing.map { it.id }.toSet()
                    val newItems = fetched.filter { it.id !in existingIds }
                    if (LocalDatabaseManager.isInitialized) {
                        newItems.forEach { LocalDatabaseManager.appointmentDao.insertAppointment(it) }
                    }
                    existing + newItems
                }
            }
        }

        NetworkClient.fetchPrescriptions().onSuccess { fetched ->
            if (fetched.isNotEmpty()) {
                _prescriptions.update { existing ->
                    val existingIds = existing.map { it.id }.toSet()
                    val newItems = fetched.filter { it.id !in existingIds }
                    if (LocalDatabaseManager.isInitialized) {
                        newItems.forEach { LocalDatabaseManager.prescriptionDao.insertPrescription(it) }
                    }
                    existing + newItems
                }
            }
        }

        NetworkClient.fetchTreatmentPlans().onSuccess { fetched ->
            if (fetched.isNotEmpty()) {
                _treatmentPlans.update { existing ->
                    val existingIds = existing.map { it.id }.toSet()
                    val newItems = fetched.filter { it.id !in existingIds }
                    if (LocalDatabaseManager.isInitialized) {
                        newItems.forEach { LocalDatabaseManager.treatmentPlanDao.insertTreatmentPlan(it) }
                    }
                    existing + newItems
                }
            }
        }

        NetworkClient.fetchReports().onSuccess { fetched ->
            if (fetched.isNotEmpty()) {
                _reports.update { existing ->
                    val existingIds = existing.map { it.id }.toSet()
                    val newItems = fetched.filter { it.id !in existingIds }
                    if (LocalDatabaseManager.isInitialized) {
                        newItems.forEach { LocalDatabaseManager.reportDao.insertReport(it) }
                    }
                    existing + newItems
                }
            }
        }
    }

    val clinicians: List<Clinician>
        get() {
            val name = _clinicianDisplayName.value.ifBlank {
                val current = AuthRepository.currentUser.value
                if (current != null && current.role == UserRole.CLINICIAN && current.name.isNotBlank()) {
                    current.name
                } else {
                    "Dr. Ingrid Halvorsen"
                }
            }
            val current = AuthRepository.currentUser.value
            val id = if (current != null && current.role == UserRole.CLINICIAN) {
                current.id
            } else {
                "c1"
            }
            return listOf(
                Clinician(
                    id = id,
                    name = name,
                    credentials = "BDS (Hons), MFDS RCSEd, MClinDent (Periodontology)",
                    specialty = "Comprehensive Dental Care & Surgery",
                    room = "Surgery 1",
                    bio = "Lead clinician providing comprehensive dental diagnosis, treatment planning, and surgical care."
                )
            )
        }

    fun generateDefaultTeeth(): Map<Int, ToothRecord> {
        val map = mutableMapOf<Int, ToothRecord>()
        val namesUpper = listOf(
            "Third Molar (Wisdom)", "Second Molar", "First Molar", "Second Premolar",
            "First Premolar", "Canine (Cuspid)", "Lateral Incisor", "Central Incisor",
            "Central Incisor", "Lateral Incisor", "Canine (Cuspid)", "First Premolar",
            "Second Premolar", "First Molar", "Second Molar", "Third Molar (Wisdom)"
        )
        val namesLower = listOf(
            "Third Molar (Wisdom)", "Second Molar", "First Molar", "Second Premolar",
            "First Premolar", "Canine (Cuspid)", "Lateral Incisor", "Central Incisor",
            "Central Incisor", "Lateral Incisor", "Canine (Cuspid)", "First Premolar",
            "Second Premolar", "First Molar", "Second Molar", "Third Molar (Wisdom)"
        )

        for (i in 1..16) {
            val fdi = if (i <= 8) 19 - i else 20 + (i - 8)
            map[i] = ToothRecord(
                number = i,
                fdiNumber = fdi,
                name = "Maxillary Right/Left " + namesUpper[i - 1],
                arch = "Maxillary (Upper Arch)",
                condition = ToothCondition.SOUND
            )
        }
        for (i in 17..32) {
            val fdi = if (i <= 24) 30 + (25 - i) else 40 + (i - 24)
            map[i] = ToothRecord(
                number = i,
                fdiNumber = fdi,
                name = "Mandibular Left/Right " + namesLower[i - 17],
                arch = "Mandibular (Lower Arch)",
                condition = ToothCondition.SOUND
            )
        }
        return map
    }

    private val initialPatients: List<Patient> = run {
        val t1 = generateDefaultTeeth().toMutableMap().apply {
            this[3] = this[3]!!.copy(condition = ToothCondition.FILLED, notes = "Composite DO restoration (2024)")
            this[14] = this[14]!!.copy(condition = ToothCondition.CROWN, notes = "Zirconia full contour crown")
            this[19] = this[19]!!.copy(condition = ToothCondition.ROOT_CANAL, notes = "Completed RCT with gutta-percha obturation")
            this[30] = this[30]!!.copy(condition = ToothCondition.DECAY, notes = "Active occlusal caries into dentin")
        }

        val t2 = generateDefaultTeeth().toMutableMap().apply {
            this[1] = this[1]!!.copy(condition = ToothCondition.MISSING, notes = "Surgically extracted 2018")
            this[16] = this[16]!!.copy(condition = ToothCondition.MISSING, notes = "Congenitally absent")
            this[19] = this[19]!!.copy(condition = ToothCondition.IMPLANT, notes = "Straumann 4.1mm tissue level implant")
            this[20] = this[20]!!.copy(condition = ToothCondition.FILLED, notes = "MOD amalgam restoration")
        }

        val t3 = generateDefaultTeeth().toMutableMap().apply {
            this[8] = this[8]!!.copy(condition = ToothCondition.FILLED, notes = "Class IV composite incisal edge repair")
            this[9] = this[9]!!.copy(condition = ToothCondition.SOUND)
            this[18] = this[18]!!.copy(condition = ToothCondition.DECAY, notes = "Early enamel demineralization")
        }

        listOf(
            Patient(
                id = "p1",
                opNo = "OP-40182",
                name = "Rosalind Achebe",
                dob = "1984-03-11",
                phone = "+1 (503) 224-7719",
                email = "rosalind.achebe@example.org",
                address = "742 Evergreen Terrace, Portland, OR 97201",
                medicalHistory = "No significant systemic medical history.",
                familyHistory = "Maternal history of early severe periodontitis with tooth loss by age 50\nFather has hypertension",
                pastDentalHistory = "Irregular dental attendance due to dental anxiety\nPast composite restoration on #3\nOrthodontic treatment completed age 16",
                lastVisit = "3 weeks ago",
                medicalAlerts = emptyList(),
                allergies = emptyList(),
                teeth = t1,
                examAnswers = ExaminationAnswers(
                    chiefComplaints = listOf("Toothache", "Sensitivity"),
                    chiefComplaintOther = "Discomfort on lower left quadrant when drinking chilled liquids",
                    painSeverity = "Moderate",
                    sensitivityTriggers = listOf("Cold", "Sweet / Acidic"),
                    periodontalBleeding = listOf("Bleeding on brushing"),
                    periodontalPockets = listOf("Mild Pockets (4 - 5 mm)"),
                    gingivalRecession = listOf("Mild (< 2 mm)"),
                    softTissue = listOf("Healthy & intact"),
                    stains = listOf("Extrinsic (Tea / Coffee)"),
                    calculus = listOf("Supragingival - Mild"),
                    tmjAssessment = listOf("Normal / Asymptomatic"),
                    functionalHabits = listOf("No clenching/grinding"),
                    brushingFrequency = "2x/day",
                    flossingFrequency = "Occasional",
                    cariesRisk = "Moderate Risk",
                    otherDiagnosesConditions = listOf("Localized Gingivitis"),
                    otherDiagnosesNotes = "Localized marginal gingivitis in lower anterior segment.",
                    clinicianNotes = "Active carious lesion, prompt restoration advised. Generalized marginal gingivitis secondary to plaque accumulation."
                ),
                diagnosis = PatientDiagnosis(
                    primaryDiagnosis = "Generalized Stage III, Grade B Periodontitis; localized deep pockets #3, #14; active occlusal caries #30",
                    clinicalFindings = "Generalized 4-5mm probing depths, bleeding on probing in anterior quadrant, subgingival calculus. Tooth #30 has active dentinal caries.",
                    prognosis = "Favourable with periodontal therapy and restorative intervention",
                    systemicConsiderations = "No contraindicating systemic conditions.",
                    dateRecorded = "2026-09-02",
                    lastUpdated = "2026-09-02",
                    clinicianName = "Dr. Ingrid Halvorsen"
                )
            ),
            Patient(
                id = "p2",
                opNo = "OP-40219",
                name = "Dmitri Vollmer",
                dob = "1971-11-02",
                phone = "+1 (503) 917-4402",
                email = "d.vollmer@example.org",
                address = "1208 NW 23rd Ave, Portland, OR 97210",
                medicalHistory = "Stage 1 Essential Hypertension (managed with Amlodipine 5mg daily)",
                familyHistory = "No known hereditary dental or systemic conditions",
                pastDentalHistory = "Surgical extraction of tooth #1 in 2018\nStraumann dental implant placed at site #19 in 2021\nRegular 6-monthly recall visits",
                lastVisit = "3 days ago",
                medicalAlerts = emptyList(),
                allergies = emptyList(),
                teeth = t2,
                examAnswers = ExaminationAnswers(
                    chiefComplaints = listOf("Follow-up"),
                    chiefComplaintOther = "Post-surgical review following tooth extraction",
                    painSeverity = "Mild",
                    sensitivityTriggers = listOf("Biting / Mastication Pressure"),
                    periodontalBleeding = listOf("No bleeding"),
                    periodontalPockets = listOf("Normal (1 - 3 mm)"),
                    gingivalRecession = listOf("None"),
                    softTissue = listOf("Healthy & intact"),
                    stains = listOf("None / Minimal"),
                    calculus = listOf("None"),
                    tmjAssessment = listOf("Normal / Asymptomatic"),
                    functionalHabits = listOf("No clenching/grinding"),
                    brushingFrequency = "2x/day",
                    flossingFrequency = "Daily",
                    cariesRisk = "Low Risk",
                    otherDiagnosesConditions = emptyList(),
                    otherDiagnosesNotes = "Post-surgical healing in progress.",
                    clinicianNotes = "Extraction socket healing uneventfully. Oral hygiene is excellent."
                ),
                diagnosis = PatientDiagnosis(
                    primaryDiagnosis = "Symptomatic apical periodontitis #19 with sub-optimal previous obturation",
                    clinicalFindings = "Tenderness to percussion #19, persistent periapical radiolucency on CBCT, mesial canals intact, suspected untreated MB2.",
                    prognosis = "Good following microscope-guided endodontic retreatment",
                    systemicConsiderations = "Controlled Hypertension (Amlodipine 5mg). Monitor BP.",
                    dateRecorded = "2026-09-08",
                    lastUpdated = "2026-09-08",
                    clinicianName = "Dr. Ingrid Halvorsen"
                )
            ),
            Patient(
                id = "p3",
                opNo = "OP-40233",
                name = "Kavitha Nambiar",
                dob = "1996-06-24",
                phone = "+1 (971) 288-6153",
                email = "k.nambiar@example.org",
                address = "3415 SE Division St, Portland, OR 97202",
                medicalHistory = "No significant systemic medical history. Non-smoker.",
                familyHistory = "Nil relevant familial dental abnormalities.",
                pastDentalHistory = "Trauma to #8 restored with Class IV composite in 2022\nEarly enamel caries #18 under topical fluoride monitoring",
                lastVisit = "2 months ago",
                medicalAlerts = emptyList(),
                allergies = emptyList(),
                teeth = t3,
                diagnosis = PatientDiagnosis(
                    primaryDiagnosis = "Incisal composite restoration margin integrity review #8; localized early enamel demineralization #18",
                    clinicalFindings = "Class IV composite stable. Incipient non-cavitated white spot lesion on occlusal surface of #18.",
                    prognosis = "Excellent",
                    systemicConsiderations = "Nil",
                    dateRecorded = "2026-07-14",
                    lastUpdated = "2026-07-14",
                    clinicianName = "Dr. Ingrid Halvorsen"
                )
            ),
            Patient(
                id = "p4",
                opNo = "OP-40251",
                name = "Owen Blackwood",
                dob = "1958-01-19",
                phone = "+1 (503) 661-2087",
                email = "o.blackwood@example.org",
                address = "883 SW Vista Ave, Portland, OR 97205",
                medicalHistory = "Type 2 Diabetes Mellitus (HbA1c 6.8%)\nAtrial Fibrillation on Apixaban (Eliquis) 5mg BD",
                familyHistory = "Cardiovascular disease (father), Type 2 Diabetes (mother)",
                pastDentalHistory = "Periodontal maintenance recalls every 3-4 months\nNo extractions in past 10 years",
                lastVisit = "9 days ago",
                medicalAlerts = emptyList(),
                allergies = emptyList(),
                teeth = generateDefaultTeeth()
            ),
            Patient(
                id = "p5",
                opNo = "OP-40266",
                name = "Marisol Cabrera-Reyes",
                dob = "2001-09-30",
                phone = "+1 (971) 402-9338",
                email = "m.cabrera@example.org",
                address = "1920 NE Alberta St, Portland, OR 97211",
                medicalHistory = "No known systemic illness.",
                familyHistory = "Mother has dental fluorosis",
                pastDentalHistory = "Composite restoration #30\nFissure sealants placed on all first molars at age 7",
                lastVisit = "4 months ago",
                medicalAlerts = emptyList(),
                allergies = emptyList(),
                teeth = generateDefaultTeeth()
            )
        )
    }

    private val _patients = MutableStateFlow(initialPatients)
    val patients: StateFlow<List<Patient>> = _patients.asStateFlow()

    private val initialAppointments = listOf(
        Appointment(
            id = "a1",
            patientId = "p1",
            patientName = "Rosalind Achebe",
            patientOpNo = "OP-40182",
            patientDob = "1984-03-11",
            clinicianId = "c1",
            clinicianName = "Dr. Ingrid Halvorsen",
            date = todayIsoDate(),
            time = "09:00",
            durationMin = 45,
            room = "Surgery 1",
            procedure = "Subgingival Debridement Quad 1 & 4",
            allergyList = null,
            status = "confirmed"
        ),
        Appointment(
            id = "a2",
            patientId = "p2",
            patientName = "Dmitri Vollmer",
            patientOpNo = "OP-40219",
            patientDob = "1971-11-02",
            clinicianId = "c1",
            clinicianName = "Dr. Ingrid Halvorsen",
            date = todayIsoDate(),
            time = "10:15",
            durationMin = 60,
            room = "Surgery 1",
            procedure = "Root Canal Retreatment #19",
            allergyList = null,
            status = "confirmed"
        ),
        Appointment(
            id = "a3",
            patientId = "p3",
            patientName = "Kavitha Nambiar",
            patientOpNo = "OP-40233",
            patientDob = "1996-06-24",
            clinicianId = "c1",
            clinicianName = "Dr. Ingrid Halvorsen",
            date = addDaysToIsoDate(todayIsoDate(), 1),
            time = "11:30",
            durationMin = 45,
            room = "Surgery 1",
            procedure = "Implant Crown Seating #19",
            allergyList = null,
            status = "confirmed"
        ),
        Appointment(
            id = "a4",
            patientId = "p4",
            patientName = "Owen Blackwood",
            patientOpNo = "OP-40251",
            patientDob = "1958-01-19",
            clinicianId = "c1",
            clinicianName = "Dr. Ingrid Halvorsen",
            date = todayIsoDate(),
            time = "14:00",
            durationMin = 30,
            room = "Surgery 1",
            procedure = "Periodontal Maintenance Recall",
            allergyList = null,
            status = "confirmed"
        ),
        Appointment(
            id = "a5",
            patientId = "p5",
            patientName = "Marisol Cabrera-Reyes",
            patientOpNo = "OP-40266",
            patientDob = "2001-09-30",
            clinicianId = "c1",
            clinicianName = "Dr. Ingrid Halvorsen",
            date = addDaysToIsoDate(todayIsoDate(), -1),
            time = "15:45",
            durationMin = 30,
            room = "Surgery 1",
            procedure = "Composite Restoration #30",
            allergyList = null,
            status = "completed"
        )
    )

    private val _appointments = MutableStateFlow(initialAppointments)
    val appointments: StateFlow<List<Appointment>> = _appointments.asStateFlow()

    private val _draftPlans = MutableStateFlow(
        listOf(
            DraftPlan("dp1", "p2", "Dmitri Vollmer", "Endodontic retreatment under microscope", "post"),
            DraftPlan("dp2", "p1", "Rosalind Achebe", "Full mouth periodontal debridement phase 2", "pre")
        )
    )
    val draftPlans: StateFlow<List<DraftPlan>> = _draftPlans.asStateFlow()

    private val _heldResults = MutableStateFlow(
        listOf(
            HeldResult("hr1", "p4", "Owen Blackwood", "OPG Panoramic Radiograph", "Imaging"),
            HeldResult("hr2", "p2", "Dmitri Vollmer", "CBCT 3D Scan #19 Apical Region", "Imaging")
        )
    )
    val heldResults: StateFlow<List<HeldResult>> = _heldResults.asStateFlow()

    private val initialPrescriptions = listOf(
        Prescription(
            id = "rx-8401",
            patientId = "p2",
            patientName = "Dmitri Vollmer",
            clinicianName = "Dr. Ingrid Halvorsen",
            drugName = "Amoxicillin",
            dosage = "500 mg capsules",
            frequency = "1 capsule every 8 hours",
            duration = "5 days",
            instructions = "Take with water after meals. Finish complete course.",
            issueDate = "Today, 10:20 AM"
        ),
        Prescription(
            id = "rx-8402",
            patientId = "p1",
            patientName = "Rosalind Achebe",
            clinicianName = "Dr. Ingrid Halvorsen",
            drugName = "Clindamycin",
            dosage = "300 mg capsules",
            frequency = "1 capsule every 6 hours",
            duration = "7 days",
            instructions = "Avoid with food. Penicillin-allergic alternative.",
            issueDate = "Today, 09:35 AM"
        )
    )

    private val _prescriptions = MutableStateFlow(initialPrescriptions)
    val prescriptions: StateFlow<List<Prescription>> = _prescriptions.asStateFlow()

    private val _medicationPresets = MutableStateFlow(MedicationPreset.defaultPresets)
    val medicationPresets: StateFlow<List<MedicationPreset>> = _medicationPresets.asStateFlow()

    private val initialTreatmentPlans = listOf(
        TreatmentPlan(
            id = "plan-901",
            patientId = "p1",
            title = "Phase 1: Periodontal Scaling & Caries Control",
            clinicianName = "Dr. Ingrid Halvorsen",
            diagnosis = "Generalized Stage III, Grade B Periodontitis with localized deep pockets #3, #14",
            dateCreated = "2026-09-02",
            isLocked = true,
            tamperHash = "sha256:7f83b1657ff1fc53b92dc18148a1d65dfc2d4b1fa3d677284addd200126d9069",
            steps = listOf(
                PlanStep("s1", null, "Full mouth periodontal charting and OHI", "D0180", 120.0, true),
                PlanStep("s2", 3, "Quadrant scaling and root planing (Upper Right)", "D4341", 280.0, true),
                PlanStep("s3", 30, "Quadrant scaling and root planing (Lower Right)", "D4341", 280.0, false),
                PlanStep("s4", 30, "Resin restoration - 2 surfaces, posterior", "D2392", 240.0, false)
            )
        ),
        TreatmentPlan(
            id = "plan-903",
            patientId = "p1",
            title = "Phase 2: Periodontal Re-evaluation & Maintenance",
            clinicianName = "Dr. Ingrid Halvorsen",
            diagnosis = "Generalized Stage III, Grade B Periodontitis",
            dateCreated = "2026-09-12",
            isLocked = false,
            tamperHash = "sha256:4b91f09c8d32e185c7f8a113941a2e8c2049e6f3b7d189c4501a382e79601d3a",
            steps = listOf(
                PlanStep("s8", null, "4-6 Week Periodontal Re-evaluation & Probing Depth Review", "D4910", 160.0, false),
                PlanStep("s9", null, "Supportive Periodontal Therapy & Topical Fluoride Application", "D1206", 85.0, false)
            )
        ),
        TreatmentPlan(
            id = "plan-902",
            patientId = "p2",
            title = "Endodontic Retreatment & Coronal Restoration",
            clinicianName = "Dr. Ingrid Halvorsen",
            diagnosis = "Symptomatic apical periodontitis #19 with previous sub-optimal obturation",
            dateCreated = "2026-09-08",
            isLocked = true,
            tamperHash = "sha256:3a7bd3e2360a3d29eea436fcfb7e44c735d117c42d1c1835420b6b9942dd4f1b",
            steps = listOf(
                PlanStep("s5", 19, "Endodontic retreatment - molar under microscope", "D3348", 1250.0, false),
                PlanStep("s6", 19, "Core build-up including any pins", "D2950", 295.0, false),
                PlanStep("s7", 19, "Crown - porcelain fused to high noble metal", "D2750", 1150.0, false)
            )
        )
    )

    private val _treatmentPlans = MutableStateFlow(initialTreatmentPlans)
    val treatmentPlans: StateFlow<List<TreatmentPlan>> = _treatmentPlans.asStateFlow()

    fun computeCanonicalPlanHash(planId: String, patientId: String, diagnosis: String, steps: List<PlanStep>): String {
        val input = "$planId|$patientId|$diagnosis|" + steps.sortedBy { it.id }.joinToString(";") {
            "${it.id}:${it.toothNumber ?: ""}:${it.procedureCode}:${it.fee}:${it.completed}"
        }
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray(Charsets.UTF_8))
        return "sha256:" + hash.joinToString("") { "%02x".format(it) }
    }

    fun updateAppointmentStatus(id: String, newStatus: String) {
        _appointments.update { list ->
            list.map {
                if (it.id == id) it.copy(status = newStatus) else it
            }
        }
        repositoryScope.launch {
            if (LocalDatabaseManager.isInitialized) {
                LocalDatabaseManager.appointmentDao.updateAppointmentStatus(id, newStatus)
            }
            NetworkClient.updateAppointmentStatus(id, newStatus)
        }
    }

    fun scheduleAppointment(
        patient: Patient,
        clinicianId: String,
        clinicianName: String,
        date: String = todayIsoDate(),
        time: String,
        durationMin: Int,
        room: String = "Surgery 1",
        procedure: String,
        reminderEnabled: Boolean = false,
        reminderLeadTimeMin: Int = 15
    ): Appointment {
        val allergyStr = if (patient.allergies.isNotEmpty()) {
            patient.allergies.joinToString(", ") { it.allergen }
        } else null

        val newAppt = Appointment(
            id = "a-${System.currentTimeMillis()}",
            patientId = patient.id,
            patientName = patient.name,
            patientOpNo = patient.opNo,
            patientDob = patient.dob,
            clinicianId = clinicianId,
            clinicianName = clinicianName,
            date = date,
            time = time,
            durationMin = durationMin,
            room = room,
            procedure = procedure,
            allergyList = allergyStr,
            status = "confirmed",
            reminderEnabled = reminderEnabled,
            reminderLeadTimeMin = reminderLeadTimeMin
        )
        // Insert in chronological (date, then time-of-day) order, not just
        // prepended — see AppointmentDao's getAllAppointments()/
        // getAppointmentsForPatient() for the same fix on the DB-backed load path.
        _appointments.update { (listOf(newAppt) + it).sortedWith(compareBy({ appt -> appt.date }, { appt -> parseTimeToMinutes(appt.time) })) }
        repositoryScope.launch {
            if (LocalDatabaseManager.isInitialized) {
                LocalDatabaseManager.appointmentDao.insertAppointment(newAppt)
            }
            NetworkClient.bookAppointment(
                patientId = patient.id,
                clinicianName = clinicianName,
                time = time,
                durationMin = durationMin,
                room = room,
                procedure = procedure
            )
        }
        val ctx = LocalDatabaseManager.appContext
        if (ctx != null && reminderEnabled) {
            com.example.thornburydental.reminder.ReminderManager.schedulePatientArrivalReminder(ctx, newAppt)
        }
        return newAppt
    }

    fun updateAppointmentReminder(id: String, enabled: Boolean, leadTimeMin: Int) {
        var targetAppt: Appointment? = null
        _appointments.update { list ->
            list.map { appt ->
                if (appt.id == id) {
                    val updated = appt.copy(reminderEnabled = enabled, reminderLeadTimeMin = leadTimeMin)
                    targetAppt = updated
                    updated
                } else appt
            }
        }
        repositoryScope.launch {
            if (LocalDatabaseManager.isInitialized) {
                LocalDatabaseManager.appointmentDao.updateAppointmentReminder(id, enabled, leadTimeMin)
            }
        }
        val ctx = LocalDatabaseManager.appContext
        if (ctx != null) {
            if (enabled && targetAppt != null) {
                com.example.thornburydental.reminder.ReminderManager.schedulePatientArrivalReminder(ctx, targetAppt!!)
            } else {
                com.example.thornburydental.reminder.ReminderManager.cancelPatientArrivalReminder(ctx, id)
            }
        }
    }

    fun bookAppointment(
        patient: Patient,
        clinicianId: String,
        clinicianName: String,
        date: String = todayIsoDate(),
        time: String,
        durationMin: Int,
        room: String = "Surgery 1",
        procedure: String,
        reminderEnabled: Boolean = false,
        reminderLeadTimeMin: Int = 15
    ): Appointment = scheduleAppointment(
        patient = patient,
        clinicianId = clinicianId,
        clinicianName = clinicianName,
        date = date,
        time = time,
        durationMin = durationMin,
        room = room,
        procedure = procedure,
        reminderEnabled = reminderEnabled,
        reminderLeadTimeMin = reminderLeadTimeMin
    )

    fun updateToothCondition(patientId: String, toothNumber: Int, newCondition: ToothCondition, notes: String) {
        _patients.update { list ->
            list.map { p ->
                if (p.id == patientId) {
                    val updatedTeeth = p.teeth.toMutableMap()
                    val current = updatedTeeth[toothNumber]
                    if (current != null) {
                        updatedTeeth[toothNumber] = current.copy(
                            condition = newCondition,
                            notes = if (notes.isNotBlank()) notes else current.notes
                        )
                    }
                    p.copy(teeth = updatedTeeth)
                } else {
                    p
                }
            }
        }
        repositoryScope.launch {
            if (LocalDatabaseManager.isInitialized) {
                LocalDatabaseManager.toothDao.updateToothCondition(patientId, toothNumber, newCondition, notes)
            }
        }
    }

    fun issuePrescription(
        patient: Patient,
        clinicianName: String,
        drugName: String,
        dosage: String,
        frequency: String,
        duration: String,
        instructions: String,
        overrideReason: String? = null,
        saveAsPreset: Boolean = false,
        presetCategory: String = "Custom"
    ): Prescription {
        if (saveAsPreset && drugName.isNotBlank()) {
            val exists = _medicationPresets.value.any { it.name.equals(drugName.trim(), ignoreCase = true) }
            if (!exists) {
                addMedicationPreset(
                    name = drugName,
                    dosage = dosage,
                    frequency = frequency,
                    duration = duration,
                    instructions = instructions,
                    category = presetCategory
                )
            }
        }
        val finalInstructions = if (overrideReason != null) {
            "$instructions [CLINICAL OVERRIDE: $overrideReason]"
        } else {
            instructions
        }
        var createdRx: Prescription? = null
        _prescriptions.update { current ->
            val newRx = Prescription(
                id = "rx-${(8400 + current.size + 1)}",
                patientId = patient.id,
                patientName = patient.name,
                clinicianName = clinicianName,
                drugName = drugName,
                dosage = dosage,
                frequency = frequency,
                duration = duration,
                instructions = finalInstructions,
                issueDate = "Today, Just now"
            )
            createdRx = newRx
            listOf(newRx) + current
        }
        val rx = createdRx!!
        repositoryScope.launch {
            if (LocalDatabaseManager.isInitialized) {
                LocalDatabaseManager.prescriptionDao.insertPrescription(rx)
            }
            val durationDays = duration.filter { it.isDigit() }.toIntOrNull() ?: 5
            NetworkClient.issuePrescription(
                patientId = patient.id,
                drug = drugName,
                dose = dosage,
                frequency = frequency,
                durationDays = durationDays,
                indication = instructions,
                overrideReason = overrideReason
            )
        }
        return rx
    }

    fun addMedicationPreset(
        name: String,
        dosage: String,
        frequency: String,
        duration: String,
        instructions: String,
        category: String = "Custom"
    ): MedicationPreset {
        val newPreset = MedicationPreset(
            id = "preset-${System.currentTimeMillis()}",
            name = name.trim(),
            dosage = dosage.trim(),
            frequency = frequency.trim(),
            duration = duration.trim(),
            instructions = instructions.trim(),
            category = category.trim().ifBlank { "Custom" },
            isCustom = true
        )
        _medicationPresets.update { current ->
            current + newPreset
        }
        repositoryScope.launch {
            if (LocalDatabaseManager.isInitialized) {
                LocalDatabaseManager.medicationPresetDao.insertPreset(newPreset)
            }
        }
        return newPreset
    }

    fun updateMedicationPreset(preset: MedicationPreset) {
        _medicationPresets.update { current ->
            current.map { if (it.id == preset.id) preset else it }
        }
        repositoryScope.launch {
            if (LocalDatabaseManager.isInitialized) {
                LocalDatabaseManager.medicationPresetDao.updatePreset(preset)
            }
        }
    }

    fun deleteMedicationPreset(presetId: String) {
        _medicationPresets.update { current ->
            current.filterNot { it.id == presetId }
        }
        repositoryScope.launch {
            if (LocalDatabaseManager.isInitialized) {
                LocalDatabaseManager.medicationPresetDao.deletePreset(presetId)
            }
        }
    }

    fun resetMedicationPresetsToDefaults() {
        val defaults = MedicationPreset.defaultPresets
        _medicationPresets.value = defaults
        repositoryScope.launch {
            if (LocalDatabaseManager.isInitialized) {
                LocalDatabaseManager.medicationPresetDao.resetToDefaults(defaults)
            }
        }
    }

    fun togglePlanLock(planId: String) {
        var updatedLock = true
        var updatedHash = ""
        _treatmentPlans.update { list ->
            list.map { plan ->
                if (plan.id == planId) {
                    val nextLock = !plan.isLocked
                    val newHash = if (nextLock) {
                        computeCanonicalPlanHash(plan.id, plan.patientId, plan.diagnosis, plan.steps)
                    } else {
                        "sha256:UNLOCKED_EDITING_STAGE"
                    }
                    updatedLock = nextLock
                    updatedHash = newHash
                    plan.copy(isLocked = nextLock, tamperHash = newHash)
                } else plan
            }
        }
        repositoryScope.launch {
            if (LocalDatabaseManager.isInitialized) {
                LocalDatabaseManager.treatmentPlanDao.togglePlanLock(planId, updatedLock, updatedHash)
            }
        }
    }

    fun togglePlanStepCompletion(planId: String, stepId: String) {
        _treatmentPlans.update { list ->
            list.map { plan ->
                if (plan.id == planId) {
                    val updatedSteps = plan.steps.map { step ->
                        if (step.id == stepId) step.copy(completed = !step.completed) else step
                    }
                    plan.copy(steps = updatedSteps)
                } else plan
            }
        }
        repositoryScope.launch {
            if (LocalDatabaseManager.isInitialized) {
                LocalDatabaseManager.treatmentPlanDao.toggleStepCompletion(planId, stepId)
            }
        }
    }

    fun createTreatmentPlan(
        patientId: String,
        title: String = "Comprehensive Treatment Plan",
        clinicianName: String,
        diagnosis: String = "",
        steps: List<PlanStep>
    ): TreatmentPlan {
        val planId = "plan-" + System.currentTimeMillis()
        val tamperHash = computeCanonicalPlanHash(planId, patientId, diagnosis, steps)
        val dateCreated = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val newPlan = TreatmentPlan(
            id = planId,
            patientId = patientId,
            title = title,
            clinicianName = clinicianName,
            diagnosis = diagnosis,
            dateCreated = dateCreated,
            isLocked = true,
            tamperHash = tamperHash,
            steps = steps,
            addenda = emptyList()
        )
        _treatmentPlans.update { listOf(newPlan) + it }
        val details = steps.joinToString("; ") { "${it.procedure} (${it.code})" }
        repositoryScope.launch {
            if (LocalDatabaseManager.isInitialized) {
                LocalDatabaseManager.treatmentPlanDao.insertTreatmentPlan(newPlan)
            }
            NetworkClient.createTreatmentPlan(
                patientId = patientId,
                diagnosis = diagnosis,
                details = details
            )
        }
        return newPlan
    }

    /**
     * Updates or records the singular, persistent clinical diagnosis for a patient.
     * Persisted to SQLite and updated in reactive state.
     */
    fun updatePatientDiagnosis(patientId: String, diagnosis: PatientDiagnosis) {
        _patients.update { list ->
            list.map { p ->
                if (p.id == patientId) {
                    p.copy(diagnosis = diagnosis)
                } else p
            }
        }
        repositoryScope.launch {
            if (LocalDatabaseManager.isInitialized) {
                LocalDatabaseManager.patientDao.updateDiagnosis(patientId, diagnosis)
            }
        }
    }

    fun addPlanAddendum(planId: String, author: String, note: String) {
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        var createdAddendum: PlanAddendum? = null
        _treatmentPlans.update { list ->
            list.map { plan ->
                if (plan.id == planId) {
                    val newAddendum = PlanAddendum(
                        id = "add-${System.currentTimeMillis()}",
                        author = author,
                        text = note,
                        date = dateStr
                    )
                    createdAddendum = newAddendum
                    plan.copy(addenda = plan.addenda + newAddendum)
                } else plan
            }
        }
        repositoryScope.launch {
            if (LocalDatabaseManager.isInitialized && createdAddendum != null) {
                LocalDatabaseManager.treatmentPlanDao.addPlanAddendum(planId, createdAddendum)
            }
        }
    }

    /**
     * Safety cross-check: Checks whether the chosen drug triggers an allergy warning for the patient.
     */
    fun checkAllergyConflict(patient: Patient, drugName: String): String? {
        val lowerDrug = drugName.lowercase()
        for (allergy in patient.allergies) {
            val lowerAllergen = allergy.allergen.lowercase()
            if (lowerDrug.contains(lowerAllergen) ||
                ((lowerDrug.contains("amoxicillin") || lowerDrug.contains("co-amoxiclav") || lowerDrug.contains("ampicillin")) && lowerAllergen.contains("penicillin")) ||
                (lowerDrug.contains("penicillin") && (lowerAllergen.contains("amoxicillin") || lowerAllergen.contains("co-amoxiclav") || lowerAllergen.contains("ampicillin"))) ||
                ((lowerDrug.contains("ibuprofen") || lowerDrug.contains("aspirin") || lowerDrug.contains("naproxen")) && lowerAllergen.contains("nsaid"))
            ) {
                return "ALLERGY WARNING: Patient has recorded ${allergy.severity} allergy to ${allergy.allergen} (${allergy.reaction})."
            }
        }
        return null
    }

    // =========================================================================
    // Diagnostic Reports & Imaging
    // =========================================================================
    private val initialReports = listOf(
        DiagnosticReport(
            id = "rp1",
            patientId = "p1",
            clinicianName = "Dr. Ingrid Halvorsen",
            kind = "Radiograph",
            title = "OPG Panoramic Radiograph",
            summary = "Bilateral alveolar bone loss consistent with generalised stage III grade B periodontitis. Furcation involvement tooth 46.",
            takenAt = "Today, 08:30",
            releasedAt = "Today, 09:15"
        ),
        DiagnosticReport(
            id = "rp2",
            patientId = "p1",
            clinicianName = "Dr. Ingrid Halvorsen",
            kind = "Charting",
            title = "Six point periodontal chart",
            summary = "Generalised probing depths of 2mm to 3mm. Isolated 5mm pocket distal to tooth 36 with bleeding on probing.",
            takenAt = "3 weeks ago",
            releasedAt = "3 weeks ago"
        ),
        DiagnosticReport(
            id = "rp3",
            patientId = "p2",
            clinicianName = "Dr. Ingrid Halvorsen",
            kind = "CBCT Scan",
            title = "CBCT 3D Scan #19 Apical Region",
            summary = "Persistent radiolucency at distal root apex of 36/19. Mesial root canals adequately obturated; missed MB2 canal indicated.",
            takenAt = "Yesterday, 14:15",
            releasedAt = "Today, 08:45"
        ),
        DiagnosticReport(
            id = "rp4",
            patientId = "p4",
            clinicianName = "Dr. Ingrid Halvorsen",
            kind = "Radiograph",
            title = "Bite-wing Radiographs (Right & Left)",
            summary = "No new interproximal caries detected. Stable crestal bone levels under regular maintenance.",
            takenAt = "9 days ago",
            releasedAt = "9 days ago"
        )
    )

    private val _reports = MutableStateFlow(initialReports)
    val reports: StateFlow<List<DiagnosticReport>> = _reports.asStateFlow()

    fun addDiagnosticReport(
        patientId: String,
        clinicianName: String,
        kind: String,
        title: String,
        summary: String,
        releasedImmediately: Boolean = false,
        image: String? = null
    ): DiagnosticReport {
        val timestamp = "Today, " + SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val newReport = DiagnosticReport(
            id = "rp-${System.currentTimeMillis()}",
            patientId = patientId,
            clinicianName = clinicianName,
            kind = kind,
            title = title,
            summary = summary,
            takenAt = timestamp,
            releasedAt = if (releasedImmediately) timestamp else null,
            image = image
        )
        _reports.update { listOf(newReport) + it }
        repositoryScope.launch {
            if (LocalDatabaseManager.isInitialized) {
                LocalDatabaseManager.reportDao.insertReport(newReport)
            }
        }
        return newReport
    }

    fun addReport(
        patientId: String,
        kind: String,
        title: String,
        summary: String,
        clinicianName: String = AuthRepository.currentUser.value?.name ?: "Dr. Ingrid Halvorsen",
        attachments: List<ReportAttachment> = emptyList()
    ): DiagnosticReport {
        val timestamp = "Today, " + SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val newReport = DiagnosticReport(
            id = "rp-${System.currentTimeMillis()}",
            patientId = patientId,
            clinicianName = clinicianName,
            kind = kind,
            title = title,
            summary = summary,
            takenAt = timestamp,
            releasedAt = null,
            image = null,
            attachments = attachments
        )
        _reports.update { listOf(newReport) + it }
        repositoryScope.launch {
            if (LocalDatabaseManager.isInitialized) {
                LocalDatabaseManager.reportDao.insertReport(newReport)
            }
            NetworkClient.uploadReport(
                patientId = patientId,
                kind = kind,
                title = title,
                clinician = clinicianName,
                summary = summary,
                attachments = attachments
            )
        }
        return newReport
    }

    fun toggleReportRelease(reportId: String) {
        _reports.update { list ->
            list.map { report ->
                if (report.id == reportId) {
                    report.copy(
                        releasedAt = if (report.releasedAt == null) "Today, Just now" else null
                    )
                } else {
                    report
                }
            }
        }
        repositoryScope.launch {
            if (LocalDatabaseManager.isInitialized) {
                LocalDatabaseManager.reportDao.toggleReportRelease(reportId)
            }
        }
    }

    /**
     * Reads the bytes behind a content:// Uri and uploads them to the backend's
     * /uploads endpoint, returning a ReportAttachment whose `uri` is the resulting
     * remote (server-hosted) URL rather than the local content:// Uri.
     */
    /**
     * Saves a user-picked document/image as a real report attachment.
     *
     * This app runs fully offline, on a single phone, with no companion server it can rely on
     * being reachable - so attachments are copied into this app's own private on-device storage
     * (`context.filesDir`) rather than uploaded anywhere. That storage survives app restarts and
     * needs no permissions or network connectivity, matching how the rest of this repository
     * (patients, reports, etc.) is already persisted locally via [LocalDatabaseManager].
     */
    suspend fun uploadAttachment(
        context: android.content.Context,
        uri: android.net.Uri,
        displayName: String,
        mimeType: String,
        sizeStr: String
    ): Result<ReportAttachment> = withContext(Dispatchers.IO) {
        try {
            val attachmentsDir = java.io.File(context.filesDir, "report_attachments").apply { mkdirs() }
            val id = "att-${System.currentTimeMillis()}-${(0..999).random()}"
            val safeName = displayName.replace(Regex("[^A-Za-z0-9._-]"), "_")
            val destFile = java.io.File(attachmentsDir, "$id-$safeName")

            val copied = context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output -> input.copyTo(output) }
                true
            } ?: false

            if (!copied) {
                return@withContext Result.failure(java.io.IOException("Could not read the selected file"))
            }

            Result.success(
                ReportAttachment(
                    id = id,
                    name = displayName,
                    sizeStr = sizeStr,
                    mimeType = mimeType,
                    uri = destFile.absolutePath
                )
            )
        } catch (e: Exception) {
            Log.e("DentalRepository", "uploadAttachment: failed to save selected file locally", e)
            Result.failure(e)
        }
    }

    // =========================================================================
    // Patient Registration & Demographics Management
    // =========================================================================
    fun registerPatient(
        name: String,
        opNo: String?,
        dob: String,
        phone: String,
        email: String,
        address: String,
        allergies: List<Allergy>,
        medicalAlerts: List<String>,
        medicalHistory: String
    ): Patient {
        var createdPatient: Patient? = null
        _patients.update { current ->
            val nextNum = current.size + 1
            val newId = "p${System.currentTimeMillis()}"
            val finalOpNo = if (opNo.isNullOrBlank()) "OP-${40000 + nextNum}" else opNo

            val newPatient = Patient(
                id = newId,
                opNo = finalOpNo,
                name = name,
                dob = dob,
                phone = phone,
                email = email,
                address = address,
                medicalHistory = medicalHistory,
                familyHistory = "None recorded at registration.",
                pastDentalHistory = "Initial registration visit.",
                lastVisit = "Today (New)",
                medicalAlerts = medicalAlerts,
                allergies = allergies,
                teeth = generateDefaultTeeth()
            )
            createdPatient = newPatient
            listOf(newPatient) + current
        }
        val p = createdPatient!!
        repositoryScope.launch {
            if (LocalDatabaseManager.isInitialized) {
                LocalDatabaseManager.patientDao.insertPatient(p)
            }
            NetworkClient.createPatient(
                name = p.name,
                opNo = p.opNo,
                dob = p.dob,
                phone = p.phone,
                email = p.email,
                address = p.address
            )
        }
        return p
    }

    fun updatePatientDemographics(
        patientId: String,
        opNo: String?,
        name: String,
        phone: String,
        email: String,
        address: String,
        medicalHistory: String,
        familyHistory: String,
        pastDentalHistory: String,
        dob: String? = null
    ) {
        _patients.update { list ->
            list.map { p ->
                if (p.id == patientId) {
                    p.copy(
                        opNo = opNo ?: p.opNo,
                        name = name,
                        phone = phone,
                        email = email,
                        address = address,
                        medicalHistory = medicalHistory,
                        familyHistory = familyHistory,
                        pastDentalHistory = pastDentalHistory,
                        dob = dob ?: p.dob
                    )
                } else p
            }
        }
        repositoryScope.launch {
            if (LocalDatabaseManager.isInitialized) {
                LocalDatabaseManager.patientDao.updatePatientDemographics(
                    patientId = patientId,
                    opNo = opNo,
                    name = name,
                    phone = phone,
                    email = email,
                    address = address,
                    medicalHistory = medicalHistory,
                    familyHistory = familyHistory,
                    pastDentalHistory = pastDentalHistory,
                    dob = dob
                )
            }
        }
    }

    fun updateExaminationAnswers(patientId: String, answers: ExaminationAnswers) {
        _patients.update { list ->
            list.map { p ->
                if (p.id == patientId) {
                    p.copy(examAnswers = answers)
                } else p
            }
        }
        repositoryScope.launch {
            if (LocalDatabaseManager.isInitialized) {
                LocalDatabaseManager.patientDao.updateExaminationAnswers(patientId, answers)
            }
        }
    }

    fun addPatientAllergy(patientId: String, allergen: String, severity: String, reaction: String) {
        val newAllergy = Allergy(
            allergen = allergen.trim(),
            severity = severity.trim(),
            reaction = reaction.trim()
        )
        _patients.update { list ->
            list.map { patient ->
                if (patient.id == patientId) {
                    patient.copy(allergies = patient.allergies + newAllergy)
                } else {
                    patient
                }
            }
        }
        repositoryScope.launch {
            if (LocalDatabaseManager.isInitialized) {
                LocalDatabaseManager.patientDao.addAllergy(patientId, newAllergy)
            }
        }
    }

    fun removePatientAllergy(patientId: String, allergen: String) {
        val trimmed = allergen.trim()
        _patients.update { list ->
            list.map { patient ->
                if (patient.id == patientId) {
                    patient.copy(allergies = patient.allergies.filterNot { it.allergen.equals(trimmed, ignoreCase = true) })
                } else {
                    patient
                }
            }
        }
        repositoryScope.launch {
            if (LocalDatabaseManager.isInitialized) {
                LocalDatabaseManager.patientDao.removeAllergy(patientId, trimmed)
            }
        }
    }

    fun addPatientMedicalAlert(patientId: String, alert: String) {
        val trimmed = alert.trim()
        if (trimmed.isEmpty()) return
        _patients.update { list ->
            list.map { patient ->
                if (patient.id == patientId) {
                    patient.copy(medicalAlerts = patient.medicalAlerts + trimmed)
                } else {
                    patient
                }
            }
        }
        repositoryScope.launch {
            if (LocalDatabaseManager.isInitialized) {
                LocalDatabaseManager.patientDao.addMedicalAlert(patientId, trimmed)
            }
        }
    }

    fun removePatientMedicalAlert(patientId: String, alert: String) {
        val trimmed = alert.trim()
        _patients.update { list ->
            list.map { patient ->
                if (patient.id == patientId) {
                    patient.copy(medicalAlerts = patient.medicalAlerts.filterNot { it.equals(trimmed, ignoreCase = true) })
                } else {
                    patient
                }
            }
        }
        repositoryScope.launch {
            if (LocalDatabaseManager.isInitialized) {
                LocalDatabaseManager.patientDao.removeMedicalAlert(patientId, trimmed)
            }
        }
    }

    // --- User Profile & Preferences Management ---

    private val _userPreferences = MutableStateFlow<UserProfilePreferences?>(null)
    val userPreferences: StateFlow<UserProfilePreferences?> = _userPreferences.asStateFlow()

    fun saveUserPreferences(prefs: UserProfilePreferences) {
        _userPreferences.value = prefs
        if (prefs.fullName.isNotBlank()) {
            _clinicianDisplayName.value = prefs.fullName
            AuthRepository.updateCurrentUserName(prefs.fullName)
        }
        _morningReminderEnabled.value = prefs.morningReminderEnabled
        _morningReminderTime.value = prefs.morningReminderTime
        _chairsideReminderDefaultMin.value = prefs.chairsideReminderDefaultMin

        repositoryScope.launch {
            if (LocalDatabaseManager.isInitialized) {
                LocalDatabaseManager.userPreferencesDao.savePreferences(prefs)
            }
        }

        val ctx = LocalDatabaseManager.appContext
        if (ctx != null) {
            if (prefs.morningReminderEnabled) {
                com.example.thornburydental.reminder.ReminderManager.scheduleDailyMorningBriefing(ctx, prefs.morningReminderTime)
            } else {
                com.example.thornburydental.reminder.ReminderManager.cancelDailyMorningBriefing(ctx)
            }
        }
    }

    fun isOnboardingCompleted(): Boolean {
        if (_userPreferences.value?.isOnboardingCompleted == true) return true
        return if (LocalDatabaseManager.isInitialized) {
            LocalDatabaseManager.userPreferencesDao.isOnboardingCompleted()
        } else {
            false
        }
    }

    fun completeOnboarding(prefs: UserProfilePreferences) {
        val completed = prefs.copy(isOnboardingCompleted = true)
        saveUserPreferences(completed)
    }
}


