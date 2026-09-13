package com.example.thornburydental.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// =============================================================================
// Thornbury Dental Repository
// Single source of truth containing realistic clinical seed data
// =============================================================================

object DentalRepository {

    val clinicians = listOf(
        Clinician(
            id = "c1",
            name = "Dr. Ingrid Halvorsen",
            credentials = "BDS (Hons), MFDS RCSEd, MClinDent (Periodontology)",
            specialty = "Periodontics and gum health",
            room = "Surgery 1",
            bio = "Leads the practice and treats gum disease, bone regeneration, recession and implant maintenance with microscope precision."
        ),
        Clinician(
            id = "c2",
            name = "Dr. Tomas Ferreira",
            credentials = "LMD (Lisbon), Dip. Endodontics",
            specialty = "Root canal treatment",
            room = "Surgery 2",
            bio = "Handles complex primary endodontics, retreatment and cracked-tooth cases under the high-magnification surgical operating microscope."
        ),
        Clinician(
            id = "c3",
            name = "Dr. Anaya Krishnamurthy",
            credentials = "BDS, MJDF RCS Eng, MSc (Implantology)",
            specialty = "Restorative and implants",
            room = "Surgery 3",
            bio = "Specialises in crowns, precision bridges, biomimetic bonding, and single-tooth implants including immediate temporaries."
        )
    )

    private fun generateDefaultTeeth(): Map<Int, ToothRecord> {
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
                medicalHistory = "Mild Asthma (albuterol inhaler PRN)\nAllergy to Penicillin and Amoxicillin (anaphylaxis reaction)",
                familyHistory = "Maternal history of early severe periodontitis with tooth loss by age 50\nFather has hypertension",
                pastDentalHistory = "Irregular dental attendance due to dental anxiety\nPast composite restoration on #3\nOrthodontic treatment completed age 16",
                lastVisit = "3 weeks ago",
                medicalAlerts = listOf("Mild Asthma (albuterol PRN)"),
                allergies = listOf(
                    Allergy("Penicillin", "Severe / Anaphylaxis", "Urticaria, bronchospasm"),
                    Allergy("Amoxicillin", "Severe / Anaphylaxis", "Cross-reactivity with penicillins")
                ),
                teeth = t1,
                examAnswers = ExaminationAnswers(
                    chiefComplaints = listOf("Toothache", "Sensitivity"),
                    chiefComplaintOther = "Discomfort on lower left quadrant when drinking chilled liquids",
                    painSeverity = "Moderate",
                    sensitivityTriggers = listOf("Cold", "Sweet / Acidic"),
                    periodontalBleeding = listOf("Bleeding on brushing"),
                    softTissue = listOf("Healthy & intact"),
                    functionalHabits = listOf("No clenching/grinding"),
                    brushingFrequency = "2x/day",
                    flossingFrequency = "Occasional",
                    cariesRisk = "Moderate Risk",
                    clinicianNotes = "Active carious lesion, prompt restoration advised. Generalized marginal gingivitis secondary to plaque accumulation."
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
                medicalHistory = "Stage 1 Essential Hypertension (managed with Amlodipine 5mg daily)\nLatex contact sensitivity",
                familyHistory = "No known hereditary dental or systemic conditions",
                pastDentalHistory = "Surgical extraction of tooth #1 in 2018\nStraumann dental implant placed at site #19 in 2021\nRegular 6-monthly recall visits",
                lastVisit = "3 days ago",
                medicalAlerts = listOf("Stage 1 Hypertension (Amlodipine 5mg)"),
                allergies = listOf(
                    Allergy("Latex", "Moderate", "Contact dermatitis, local swelling")
                ),
                teeth = t2,
                examAnswers = ExaminationAnswers(
                    chiefComplaints = listOf("Follow-up"),
                    chiefComplaintOther = "Post-surgical review following tooth extraction",
                    painSeverity = "Mild",
                    sensitivityTriggers = listOf("Biting / Mastication Pressure"),
                    periodontalBleeding = listOf("No bleeding"),
                    softTissue = listOf("Healthy & intact"),
                    functionalHabits = listOf("No clenching/grinding"),
                    brushingFrequency = "2x/day",
                    flossingFrequency = "Daily",
                    cariesRisk = "Low Risk",
                    clinicianNotes = "Extraction socket healing uneventfully. Oral hygiene is excellent."
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
                teeth = t3
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
                medicalAlerts = listOf("Type 2 Diabetes (HbA1c 6.8%)", "Anticoagulant (Apixaban 5mg BD)"),
                allergies = listOf(
                    Allergy("NSAIDs / Ibuprofen", "Moderate", "Gastric hemorrhage, bronchospasm")
                ),
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
                medicalHistory = "No known systemic illness. Allergy to Sulfa antibiotics.",
                familyHistory = "Mother has dental fluorosis",
                pastDentalHistory = "Composite restoration #30\nFissure sealants placed on all first molars at age 7",
                lastVisit = "4 months ago",
                medicalAlerts = emptyList(),
                allergies = listOf(
                    Allergy("Sulfa Drugs", "Mild", "Maculopapular rash")
                ),
                teeth = generateDefaultTeeth()
            )
        )
    }

    private val _patients = MutableStateFlow(initialPatients)
    val patients: StateFlow<List<Patient>> = _patients.asStateFlow()

    private val _appointments = MutableStateFlow(
        listOf(
            Appointment(
                id = "a1",
                patientId = "p1",
                patientName = "Rosalind Achebe",
                patientOpNo = "OP-40182",
                patientDob = "1984-03-11",
                clinicianId = "c1",
                clinicianName = "Dr. Ingrid Halvorsen",
                time = "09:00",
                durationMin = 45,
                room = "Surgery 1",
                procedure = "Subgingival Debridement Quad 1 & 4",
                allergyList = "Penicillin, Amoxicillin",
                status = "confirmed"
            ),
            Appointment(
                id = "a2",
                patientId = "p2",
                patientName = "Dmitri Vollmer",
                patientOpNo = "OP-40219",
                patientDob = "1971-11-02",
                clinicianId = "c2",
                clinicianName = "Dr. Tomas Ferreira",
                time = "10:15",
                durationMin = 60,
                room = "Surgery 2",
                procedure = "Root Canal Retreatment #19",
                allergyList = "Latex",
                status = "confirmed"
            ),
            Appointment(
                id = "a3",
                patientId = "p3",
                patientName = "Kavitha Nambiar",
                patientOpNo = "OP-40233",
                patientDob = "1996-06-24",
                clinicianId = "c3",
                clinicianName = "Dr. Anaya Krishnamurthy",
                time = "11:30",
                durationMin = 45,
                room = "Surgery 3",
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
                time = "14:00",
                durationMin = 30,
                room = "Surgery 1",
                procedure = "Periodontal Maintenance Recall",
                allergyList = "NSAIDs / Ibuprofen",
                status = "confirmed"
            ),
            Appointment(
                id = "a5",
                patientId = "p5",
                patientName = "Marisol Cabrera-Reyes",
                patientOpNo = "OP-40266",
                patientDob = "2001-09-30",
                clinicianId = "c3",
                clinicianName = "Dr. Anaya Krishnamurthy",
                time = "15:45",
                durationMin = 30,
                room = "Surgery 3",
                procedure = "Composite Restoration #30",
                allergyList = "Sulfa Drugs",
                status = "completed"
            )
        )
    )
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

    private val _prescriptions = MutableStateFlow(
        listOf(
            Prescription(
                id = "rx-8401",
                patientId = "p2",
                patientName = "Dmitri Vollmer",
                clinicianName = "Dr. Tomas Ferreira",
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
    )
    val prescriptions: StateFlow<List<Prescription>> = _prescriptions.asStateFlow()

    private val _treatmentPlans = MutableStateFlow(
        listOf(
            TreatmentPlan(
                id = "plan-901",
                patientId = "p1",
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
                id = "plan-902",
                patientId = "p2",
                clinicianName = "Dr. Tomas Ferreira",
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
    )
    val treatmentPlans: StateFlow<List<TreatmentPlan>> = _treatmentPlans.asStateFlow()

    fun updateAppointmentStatus(id: String, newStatus: String) {
        _appointments.value = _appointments.value.map {
            if (it.id == id) it.copy(status = newStatus) else it
        }
    }

    fun scheduleAppointment(
        patient: Patient,
        clinicianId: String,
        clinicianName: String,
        time: String,
        durationMin: Int,
        room: String,
        procedure: String
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
            time = time,
            durationMin = durationMin,
            room = room,
            procedure = procedure,
            allergyList = allergyStr,
            status = "confirmed"
        )
        _appointments.value = listOf(newAppt) + _appointments.value
        return newAppt
    }

    fun updateToothCondition(patientId: String, toothNumber: Int, newCondition: ToothCondition, notes: String) {
        _patients.value = _patients.value.map { p ->
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

    fun issuePrescription(
        patient: Patient,
        clinicianName: String,
        drugName: String,
        dosage: String,
        frequency: String,
        duration: String,
        instructions: String
    ): Prescription {
        val newRx = Prescription(
            id = "rx-${(8400 + _prescriptions.value.size + 1)}",
            patientId = patient.id,
            patientName = patient.name,
            clinicianName = clinicianName,
            drugName = drugName,
            dosage = dosage,
            frequency = frequency,
            duration = duration,
            instructions = instructions,
            issueDate = "Today, Just now"
        )
        _prescriptions.value = listOf(newRx) + _prescriptions.value
        return newRx
    }

    fun togglePlanLock(planId: String) {
        _treatmentPlans.value = _treatmentPlans.value.map { plan ->
            if (plan.id == planId) {
                val nextLock = !plan.isLocked
                val newHash = if (nextLock) {
                    val digest = MessageDigest.getInstance("SHA-256")
                    val hash = digest.digest("${plan.id}-${plan.patientId}-${System.currentTimeMillis()}".toByteArray())
                    "sha256:" + hash.joinToString("") { "%02x".format(it) }
                } else {
                    "sha256:UNLOCKED_EDITING_STAGE"
                }
                plan.copy(isLocked = nextLock, tamperHash = newHash)
            } else plan
        }
    }

    fun togglePlanStepCompletion(planId: String, stepId: String) {
        _treatmentPlans.value = _treatmentPlans.value.map { plan ->
            if (plan.id == planId) {
                val updatedSteps = plan.steps.map { step ->
                    if (step.id == stepId) step.copy(completed = !step.completed) else step
                }
                plan.copy(steps = updatedSteps)
            } else plan
        }
    }

    fun createTreatmentPlan(
        patientId: String,
        clinicianName: String,
        diagnosis: String,
        steps: List<PlanStep>
    ): TreatmentPlan {
        val planId = "plan-" + System.currentTimeMillis()
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest("$planId-$patientId-${System.currentTimeMillis()}".toByteArray())
        val tamperHash = "sha256:" + hash.joinToString("") { "%02x".format(it) }
        val dateCreated = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val newPlan = TreatmentPlan(
            id = planId,
            patientId = patientId,
            clinicianName = clinicianName,
            diagnosis = diagnosis,
            dateCreated = dateCreated,
            isLocked = true,
            tamperHash = tamperHash,
            steps = steps,
            addenda = emptyList()
        )
        _treatmentPlans.value = listOf(newPlan) + _treatmentPlans.value
        return newPlan
    }

    fun addPlanAddendum(planId: String, author: String, note: String) {
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        _treatmentPlans.value = _treatmentPlans.value.map { plan ->
            if (plan.id == planId) {
                val newAddendum = PlanAddendum(
                    id = "add-${System.currentTimeMillis()}",
                    author = author,
                    text = note,
                    date = dateStr
                )
                plan.copy(addenda = plan.addenda + newAddendum)
            } else plan
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
                (lowerDrug.contains("amoxicillin") && lowerAllergen.contains("penicillin")) ||
                (lowerDrug.contains("penicillin") && lowerAllergen.contains("amoxicillin")) ||
                (lowerDrug.contains("ibuprofen") && lowerAllergen.contains("nsaid")) ||
                (lowerDrug.contains("aspirin") && lowerAllergen.contains("nsaid"))
            ) {
                return "ALLERGY WARNING: Patient has recorded ${allergy.severity} allergy to ${allergy.allergen} (${allergy.reaction})."
            }
        }
        return null
    }

    // =========================================================================
    // Diagnostic Reports & Imaging
    // =========================================================================
    private val _reports = MutableStateFlow(
        listOf(
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
                clinicianName = "Dr. Tomas Ferreira",
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
    )
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
        _reports.value = listOf(newReport) + _reports.value
        return newReport
    }

    fun toggleReportRelease(reportId: String) {
        _reports.value = _reports.value.map { report ->
            if (report.id == reportId) {
                report.copy(
                    releasedAt = if (report.releasedAt == null) "Today, Just now" else null
                )
            } else {
                report
            }
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
        val nextNum = _patients.value.size + 1
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

        _patients.value = listOf(newPatient) + _patients.value
        return newPatient
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
        _patients.value = _patients.value.map { p ->
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

    fun updateExaminationAnswers(patientId: String, answers: ExaminationAnswers) {
        _patients.value = _patients.value.map { p ->
            if (p.id == patientId) {
                p.copy(examAnswers = answers)
            } else p
        }
    }

    fun addPatientAllergy(patientId: String, allergen: String, severity: String, reaction: String) {
        val newAllergy = Allergy(
            allergen = allergen.trim(),
            severity = severity.trim(),
            reaction = reaction.trim()
        )
        _patients.value = _patients.value.map { patient ->
            if (patient.id == patientId) {
                patient.copy(allergies = patient.allergies + newAllergy)
            } else {
                patient
            }
        }
    }

    fun removePatientAllergy(patientId: String, allergen: String) {
        _patients.value = _patients.value.map { patient ->
            if (patient.id == patientId) {
                patient.copy(allergies = patient.allergies.filterNot { it.allergen.equals(allergen.trim(), ignoreCase = true) })
            } else {
                patient
            }
        }
    }

    fun addPatientMedicalAlert(patientId: String, alert: String) {
        val trimmed = alert.trim()
        if (trimmed.isEmpty()) return
        _patients.value = _patients.value.map { patient ->
            if (patient.id == patientId) {
                patient.copy(medicalAlerts = patient.medicalAlerts + trimmed)
            } else {
                patient
            }
        }
    }

    fun removePatientMedicalAlert(patientId: String, alert: String) {
        val trimmed = alert.trim()
        _patients.value = _patients.value.map { patient ->
            if (patient.id == patientId) {
                patient.copy(medicalAlerts = patient.medicalAlerts.filterNot { it.equals(trimmed, ignoreCase = true) })
            } else {
                patient
            }
        }
    }
}

