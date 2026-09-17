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
                LocalDatabaseManager.purgeDevelopmentSeedData()
                reloadFromDatabase()
            }
        }
    }

    suspend fun reloadFromDatabase() = withContext(Dispatchers.IO) {
        if (!LocalDatabaseManager.isInitialized) return@withContext
        try {
            val dbPatients = LocalDatabaseManager.patientDao.getAllPatients()
            _patients.value = dbPatients
            val dbAppointments = LocalDatabaseManager.appointmentDao.getAllAppointments()
            _appointments.value = dbAppointments
            val dbPlans = LocalDatabaseManager.treatmentPlanDao.getAllTreatmentPlans()
            _treatmentPlans.value = dbPlans
            val dbPrescriptions = LocalDatabaseManager.prescriptionDao.getAllPrescriptions()
            _prescriptions.value = dbPrescriptions
            val dbReports = LocalDatabaseManager.reportDao.getAllReports()
            _reports.value = dbReports
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

    fun generateDefaultTeeth(isChild: Boolean = false): Map<Int, ToothRecord> {
        val map = mutableMapOf<Int, ToothRecord>()
        if (isChild) {
            // Child / Pediatric Primary Dentition (20 teeth: FDI 51-55, 61-65, 71-75, 81-85)
            val primaryUpperRight = listOf(
                55 to "Primary Maxillary Right Second Molar",
                54 to "Primary Maxillary Right First Molar",
                53 to "Primary Maxillary Right Canine",
                52 to "Primary Maxillary Right Lateral Incisor",
                51 to "Primary Maxillary Right Central Incisor"
            )
            val primaryUpperLeft = listOf(
                61 to "Primary Maxillary Left Central Incisor",
                62 to "Primary Maxillary Left Lateral Incisor",
                63 to "Primary Maxillary Left Canine",
                64 to "Primary Maxillary Left First Molar",
                65 to "Primary Maxillary Left Second Molar"
            )
            val primaryLowerLeft = listOf(
                71 to "Primary Mandibular Left Central Incisor",
                72 to "Primary Mandibular Left Lateral Incisor",
                73 to "Primary Mandibular Left Canine",
                74 to "Primary Mandibular Left First Molar",
                75 to "Primary Mandibular Left Second Molar"
            )
            val primaryLowerRight = listOf(
                81 to "Primary Mandibular Right Central Incisor",
                82 to "Primary Mandibular Right Lateral Incisor",
                83 to "Primary Mandibular Right Canine",
                84 to "Primary Mandibular Right First Molar",
                85 to "Primary Mandibular Right Second Molar"
            )

            (primaryUpperRight + primaryUpperLeft).forEach { (fdi, name) ->
                map[fdi] = ToothRecord(
                    number = fdi,
                    fdiNumber = fdi,
                    name = name,
                    arch = "Maxillary (Upper Primary Arch)",
                    condition = ToothCondition.SOUND
                )
            }
            (primaryLowerLeft + primaryLowerRight).forEach { (fdi, name) ->
                map[fdi] = ToothRecord(
                    number = fdi,
                    fdiNumber = fdi,
                    name = name,
                    arch = "Mandibular (Lower Primary Arch)",
                    condition = ToothCondition.SOUND
                )
            }
        } else {
            // Adult Permanent Dentition (32 teeth: FDI 11-18, 21-28, 31-38, 41-48)
            val q1UpperRight = listOf(
                18 to "Maxillary Right Third Molar (Wisdom)",
                17 to "Maxillary Right Second Molar",
                16 to "Maxillary Right First Molar",
                15 to "Maxillary Right Second Premolar",
                14 to "Maxillary Right First Premolar",
                13 to "Maxillary Right Canine (Cuspid)",
                12 to "Maxillary Right Lateral Incisor",
                11 to "Maxillary Right Central Incisor"
            )
            val q2UpperLeft = listOf(
                21 to "Maxillary Left Central Incisor",
                22 to "Maxillary Left Lateral Incisor",
                23 to "Maxillary Left Canine (Cuspid)",
                24 to "Maxillary Left First Premolar",
                25 to "Maxillary Left Second Premolar",
                26 to "Maxillary Left First Molar",
                27 to "Maxillary Left Second Molar",
                28 to "Maxillary Left Third Molar (Wisdom)"
            )
            val q3LowerLeft = listOf(
                31 to "Mandibular Left Central Incisor",
                32 to "Mandibular Left Lateral Incisor",
                33 to "Mandibular Left Canine (Cuspid)",
                34 to "Mandibular Left First Premolar",
                35 to "Mandibular Left Second Premolar",
                36 to "Mandibular Left First Molar",
                37 to "Mandibular Left Second Molar",
                38 to "Mandibular Left Third Molar (Wisdom)"
            )
            val q4LowerRight = listOf(
                41 to "Mandibular Right Central Incisor",
                42 to "Mandibular Right Lateral Incisor",
                43 to "Mandibular Right Canine (Cuspid)",
                44 to "Mandibular Right First Premolar",
                45 to "Mandibular Right Second Premolar",
                46 to "Mandibular Right First Molar",
                47 to "Mandibular Right Second Molar",
                48 to "Mandibular Right Third Molar (Wisdom)"
            )

            (q1UpperRight + q2UpperLeft).forEach { (fdi, name) ->
                map[fdi] = ToothRecord(
                    number = fdi,
                    fdiNumber = fdi,
                    name = name,
                    arch = "Maxillary (Upper Arch)",
                    condition = ToothCondition.SOUND
                )
            }
            (q3LowerLeft + q4LowerRight).forEach { (fdi, name) ->
                map[fdi] = ToothRecord(
                    number = fdi,
                    fdiNumber = fdi,
                    name = name,
                    arch = "Mandibular (Lower Arch)",
                    condition = ToothCondition.SOUND
                )
            }
        }
        return map
    }


    private val _patients = MutableStateFlow<List<Patient>>(emptyList())
    val patients: StateFlow<List<Patient>> = _patients.asStateFlow()

    private val _appointments = MutableStateFlow<List<Appointment>>(emptyList())
    val appointments: StateFlow<List<Appointment>> = _appointments.asStateFlow()

    private val _draftPlans = MutableStateFlow<List<DraftPlan>>(emptyList())
    val draftPlans: StateFlow<List<DraftPlan>> = _draftPlans.asStateFlow()

    private val _heldResults = MutableStateFlow<List<HeldResult>>(emptyList())
    val heldResults: StateFlow<List<HeldResult>> = _heldResults.asStateFlow()

    private val _prescriptions = MutableStateFlow<List<Prescription>>(emptyList())
    val prescriptions: StateFlow<List<Prescription>> = _prescriptions.asStateFlow()

    private val _medicationPresets = MutableStateFlow(MedicationPreset.defaultPresets)
    val medicationPresets: StateFlow<List<MedicationPreset>> = _medicationPresets.asStateFlow()

    private val _treatmentPlans = MutableStateFlow<List<TreatmentPlan>>(emptyList())
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
                com.example.thornburydental.reminder.ReminderManager.schedulePatientArrivalReminder(ctx, targetAppt)
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
        val dateCreated = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

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
    private val _reports = MutableStateFlow<List<DiagnosticReport>>(emptyList())
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
        medicalHistory: String,
        isChild: Boolean = false
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
                isChild = isChild,
                medicalHistory = medicalHistory,
                familyHistory = "None recorded at registration.",
                pastDentalHistory = "Initial registration visit.",
                lastVisit = "Today (New)",
                medicalAlerts = medicalAlerts,
                allergies = allergies,
                teeth = generateDefaultTeeth(isChild)
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
        dob: String? = null,
        isChild: Boolean = false
    ) {
        _patients.update { list ->
            list.map { p ->
                if (p.id == patientId) {
                    val teethChanged = p.isChild != isChild
                    val newTeeth = if (teethChanged) generateDefaultTeeth(isChild) else p.teeth
                    p.copy(
                        opNo = opNo ?: p.opNo,
                        name = name,
                        phone = phone,
                        email = email,
                        address = address,
                        isChild = isChild,
                        medicalHistory = medicalHistory,
                        familyHistory = familyHistory,
                        pastDentalHistory = pastDentalHistory,
                        dob = dob ?: p.dob,
                        teeth = newTeeth
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
                    dob = dob,
                    isChild = isChild
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

    internal fun seedTestFixturesForUnitTests(
        testPatients: List<Patient> = emptyList(),
        testAppointments: List<Appointment> = emptyList(),
        testPrescriptions: List<Prescription> = emptyList(),
        testTreatmentPlans: List<TreatmentPlan> = emptyList(),
        testReports: List<DiagnosticReport> = emptyList()
    ) {
        _patients.value = testPatients
        _appointments.value = testAppointments
        _prescriptions.value = testPrescriptions
        _treatmentPlans.value = testTreatmentPlans
        _reports.value = testReports
    }
}


