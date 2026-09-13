package com.example.thornburydental.ui.clinic

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import com.example.thornburydental.data.*
import com.example.thornburydental.theme.*
import com.example.thornburydental.ui.components.AnatomicalToothView
import com.example.thornburydental.ui.components.AnatomicalToothCanvas

@Composable
fun PatientDetailChartScreen(
    patientId: String,
    onBack: () -> Unit,
    onOpenIssuePrescription: (Patient) -> Unit,
    modifier: Modifier = Modifier
) {
    val patients by DentalRepository.patients.collectAsState()
    val patient = patients.find { it.id == patientId } ?: patients.firstOrNull()
    val treatmentPlans by DentalRepository.treatmentPlans.collectAsState()
    val prescriptions by DentalRepository.prescriptions.collectAsState()

    val diagnosticReports by DentalRepository.reports.collectAsState()
    val allAppointments by DentalRepository.appointments.collectAsState()

    var selectedTabIndex by remember { mutableStateOf(0) }
    var selectedToothForEdit by remember { mutableStateOf<ToothRecord?>(null) }
    var selectedReportForLightbox by remember { mutableStateOf<DiagnosticReport?>(null) }
    var showAddReportDialog by remember { mutableStateOf(false) }
    var showCreatePlanDialog by remember { mutableStateOf(false) }
    var showBookAppointmentDialog by remember { mutableStateOf(false) }

    if (patient == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Patient not found", style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    val patientPlans = treatmentPlans.filter { it.patientId == patient.id }
    val patientPrescriptions = prescriptions.filter { it.patientId == patient.id }
    val patientReports = diagnosticReports.filter { it.patientId == patient.id }
    val patientAppointments = allAppointments.filter { it.patientId == patient.id }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ThornburyCanvas)
    ) {
        // --- Top App Bar ---
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = ThornburyCanvas,
            border = BorderStroke(1.dp, ThornburyHairline)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = ThornburyInk
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = patient.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif
                        ),
                        color = ThornburyInk
                    )
                    Text(
                        text = "OP: ${patient.opNo} • DOB: ${patient.dob}",
                        style = ClinicalCodeStyle.copy(fontSize = 11.sp),
                        color = ThornburyMuted
                    )
                }

                FilledTonalButton(
                    onClick = { onOpenIssuePrescription(patient) },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = ThornburyPrimary,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Medication,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Rx", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                }
            }
        }

        // --- Allergy & Medical Alert Banners ---
        if (patient.allergies.isNotEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = ThornburyErrorWash,
                border = BorderStroke(1.dp, ThornburyError.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = ThornburyError,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CONTRAINDICATION WARNING: " + patient.allergies.joinToString { "${it.allergen} (${it.severity})" },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = ThornburyError
                        )
                    )
                }
            }
        }

        if (patient.medicalAlerts.isNotEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = ThornburyWarningWash,
                border = BorderStroke(1.dp, ThornburyWarning.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = ThornburyWarning,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Medical History: " + patient.medicalAlerts.joinToString("; "),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium,
                            color = ThornburyWarning
                        )
                    )
                }
            }
        }

        // --- 6 Clinical Tabs Matching Web Structure ---
        val chartTabs = listOf(
            "Demographics / Overview" to null,
            "Examination" to null,
            "Reports & Imaging" to patientReports.size,
            "Diagnosis & Treatment" to patientPlans.size,
            "Prescriptions" to patientPrescriptions.size,
            "Visit History" to patientAppointments.size
        )

        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = ThornburyCanvas,
            contentColor = ThornburyPrimary,
            edgePadding = 12.dp,
            divider = { HorizontalDivider(color = ThornburyHairline) }
        ) {
            chartTabs.forEachIndexed { index, (title, count) ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (selectedTabIndex == index) ThornburyPrimaryText else ThornburyMuted
                            )
                            if (count != null && count > 0) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = if (selectedTabIndex == index) ThornburyPrimary else ThornburySurfaceSoft,
                                    contentColor = if (selectedTabIndex == index) Color.White else ThornburyInk
                                ) {
                                    Text(
                                        text = count.toString(),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                )
            }
        }

        // Tab Content
        when (selectedTabIndex) {
            0 -> PatientDemographicsM3View(patient = patient)
            1 -> ExaminationAndOdontogramTabView(
                patient = patient,
                selectedToothId = selectedToothForEdit?.number,
                onToothClick = { tooth -> selectedToothForEdit = tooth },
                onSaveAnswers = { newAnswers ->
                    DentalRepository.updateExaminationAnswers(patient.id, newAnswers)
                }
            )
            2 -> ReportsAndImagingView(
                reports = patientReports,
                onAddReportClick = { showAddReportDialog = true },
                onReportClick = { report -> selectedReportForLightbox = report },
                onToggleRelease = { reportId -> DentalRepository.toggleReportRelease(reportId) }
            )
            3 -> TreatmentPlansView(
                plans = patientPlans,
                onCreatePlanClick = { showCreatePlanDialog = true },
                onToggleLock = { planId -> DentalRepository.togglePlanLock(planId) },
                onToggleStepCompletion = { planId, stepId -> DentalRepository.togglePlanStepCompletion(planId, stepId) }
            )
            4 -> PatientPrescriptionsView(
                patient = patient,
                prescriptions = patientPrescriptions,
                onIssueNew = { onOpenIssuePrescription(patient) }
            )
            5 -> VisitHistoryView(
                patient = patient,
                appointments = patientAppointments,
                onBookAppointmentClick = { showBookAppointmentDialog = true },
                onUpdateStatus = { apptId, newStatus -> DentalRepository.updateAppointmentStatus(apptId, newStatus) }
            )
        }
    }

    // Tooth Edit Dialog
    selectedToothForEdit?.let { tooth ->
        ToothEditDialog(
            tooth = tooth,
            onDismiss = { selectedToothForEdit = null },
            onSave = { condition, notes ->
                DentalRepository.updateToothCondition(patient.id, tooth.number, condition, notes)
                selectedToothForEdit = null
            }
        )
    }

    // Report Viewer Lightbox Dialog
    selectedReportForLightbox?.let { report ->
        val currentReport = diagnosticReports.find { it.id == report.id } ?: report
        ReportViewerLightboxDialog(
            report = currentReport,
            onDismiss = { selectedReportForLightbox = null },
            onToggleRelease = {
                DentalRepository.toggleReportRelease(currentReport.id)
            }
        )
    }

    // Add Report Dialog
    if (showAddReportDialog) {
        AddReportDialog(
            patient = patient,
            onDismiss = { showAddReportDialog = false },
            onSave = { kind, title, clinician, summary, releasedImmediately ->
                DentalRepository.addDiagnosticReport(
                    patientId = patient.id,
                    clinicianName = clinician,
                    kind = kind,
                    title = title,
                    summary = summary,
                    releasedImmediately = releasedImmediately
                )
                showAddReportDialog = false
            }
        )
    }

    // Create Treatment Plan Dialog
    if (showCreatePlanDialog) {
        CreateTreatmentPlanDialog(
            patient = patient,
            onDismiss = { showCreatePlanDialog = false },
            onSave = { diagnosis, clinicianName, steps ->
                DentalRepository.createTreatmentPlan(
                    patientId = patient.id,
                    clinicianName = clinicianName,
                    diagnosis = diagnosis,
                    steps = steps
                )
                showCreatePlanDialog = false
            }
        )
    }

    // Book Follow-up Appointment Dialog
    if (showBookAppointmentDialog) {
        BookAppointmentDialog(
            patient = patient,
            onDismiss = { showBookAppointmentDialog = false },
            onSave = { procedure, clinicianId, clinicianName, time, duration, room ->
                DentalRepository.scheduleAppointment(
                    patient = patient,
                    clinicianId = clinicianId,
                    clinicianName = clinicianName,
                    time = time,
                    durationMin = duration,
                    room = room,
                    procedure = procedure
                )
                showBookAppointmentDialog = false
            }
        )
    }
}

// =============================================================================
// Examination & Odontogram Integrated Tab
// Includes sub-tabs for Clinical Questions and Odontogram Chart with Maxillary / Mandibular Split
// =============================================================================

@Composable
private fun ExaminationAndOdontogramTabView(
    patient: Patient,
    selectedToothId: Int? = null,
    onToothClick: (ToothRecord) -> Unit,
    onSaveAnswers: (ExaminationAnswers) -> Unit
) {
    var examSubTab by remember { mutableStateOf(0) } // 0: Odontogram Chart, 1: Clinical Assessment Questions

    Column(modifier = Modifier.fillMaxSize()) {
        // Sub-mode pill selector header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = ThornburyCanvas,
            border = BorderStroke(1.dp, ThornburyHairline)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = examSubTab == 0,
                    onClick = { examSubTab = 0 },
                    label = {
                        Text(
                            text = "Odontogram Chart (32 Teeth)",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (examSubTab == 0) FontWeight.Bold else FontWeight.Medium
                            )
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.GridView,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ThornburyPrimary,
                        selectedLabelColor = Color.White,
                        selectedLeadingIconColor = Color.White,
                        containerColor = ThornburySurfaceSoft,
                        labelColor = ThornburyInk,
                        iconColor = ThornburyMuted
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = examSubTab == 0,
                        borderColor = ThornburyHairline,
                        selectedBorderColor = ThornburyPrimary
                    )
                )

                FilterChip(
                    selected = examSubTab == 1,
                    onClick = { examSubTab = 1 },
                    label = {
                        Text(
                            text = "Clinical Assessment",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (examSubTab == 1) FontWeight.Bold else FontWeight.Medium
                            )
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Assignment,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ThornburyPrimary,
                        selectedLabelColor = Color.White,
                        selectedLeadingIconColor = Color.White,
                        containerColor = ThornburySurfaceSoft,
                        labelColor = ThornburyInk,
                        iconColor = ThornburyMuted
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = examSubTab == 1,
                        borderColor = ThornburyHairline,
                        selectedBorderColor = ThornburyPrimary
                    )
                )
            }
        }

        when (examSubTab) {
            0 -> OdontogramView(
                teeth = patient.teeth,
                selectedToothId = selectedToothId,
                onToothClick = onToothClick
            )
            1 -> ExaminationQuestionsSection(
                patientId = patient.id,
                initialAnswers = patient.examAnswers,
                onSaveAnswers = onSaveAnswers,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

// =============================================================================
// Odontogram Component with Maxillary & Mandibular Arch Separation
// =============================================================================

enum class ArchSelection(val title: String) {
    SPLIT("Both Arches"),
    MAXILLARY("Maxillary (Upper 1-16)"),
    MANDIBULAR("Mandibular (Lower 17-32)")
}

@Composable
private fun OdontogramView(
    teeth: Map<Int, ToothRecord>,
    selectedToothId: Int? = null,
    onToothClick: (ToothRecord) -> Unit
) {
    val scrollState = rememberScrollState()
    var selectedArch by remember { mutableStateOf(ArchSelection.SPLIT) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Arch Switcher Bar for fast mobile ergonomics
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = ThornburySurfaceSoft),
            border = BorderStroke(1.dp, ThornburyHairline)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ArchSelection.values().forEach { arch ->
                    val isSelected = arch == selectedArch
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedArch = arch },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) ThornburyPrimary else Color.Transparent,
                        border = if (isSelected) null else BorderStroke(1.dp, ThornburyHairline.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = when (arch) {
                                ArchSelection.SPLIT -> "Full Mouth"
                                ArchSelection.MAXILLARY -> "Upper Arch"
                                ArchSelection.MANDIBULAR -> "Lower Arch"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (isSelected) Color.White else ThornburyInk,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Condition Legend Bar
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = ThornburyCanvas),
            border = BorderStroke(1.dp, ThornburyHairline)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Odontogram Legend (Tap any tooth to inspect or chart):",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = ThornburyInk
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    LegendItem("Sound", ToothSound, border = true)
                    LegendItem("Decay", ToothDecay)
                    LegendItem("Filled", ToothFilled)
                    LegendItem("Crown", ToothCrown)
                    LegendItem("RCT", ToothRootCanal)
                    LegendItem("Implant", ToothImplant)
                    LegendItem("Missing", ToothMissing)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Upper Arch (Maxillary Teeth 1 to 16)
        if (selectedArch == ArchSelection.SPLIT || selectedArch == ArchSelection.MAXILLARY) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = ThornburyCanvas),
                border = BorderStroke(1.dp, ThornburyHairline)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(ThornburyPrimary, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Maxillary Arch (Upper Teeth 1 - 16)",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Serif
                                ),
                                color = ThornburyInk
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ThornburyPrimaryWash
                        ) {
                            Text(
                                text = "UR 1-8  |  UL 9-16",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = ThornburyPrimaryText,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Upper Teeth Row with Quadrant Midline
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Upper Right (1 to 8)
                        for (num in 1..8) {
                            val tooth = teeth[num] ?: ToothRecord(num, num, "Tooth $num", "Upper")
                            AnatomicalToothView(
                                tooth = tooth,
                                isSelected = selectedToothId == tooth.number,
                                onClick = { onToothClick(tooth) }
                            )
                        }

                        // Midline Divider
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(80.dp)
                                .background(ThornburyPrimary.copy(alpha = 0.4f), RoundedCornerShape(1.dp))
                        )

                        // Upper Left (9 to 16)
                        for (num in 9..16) {
                            val tooth = teeth[num] ?: ToothRecord(num, num, "Tooth $num", "Upper")
                            AnatomicalToothView(
                                tooth = tooth,
                                isSelected = selectedToothId == tooth.number,
                                onClick = { onToothClick(tooth) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Lower Arch (Mandibular Teeth 17 - 32)
        if (selectedArch == ArchSelection.SPLIT || selectedArch == ArchSelection.MANDIBULAR) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = ThornburyCanvas),
                border = BorderStroke(1.dp, ThornburyHairline)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(ThornburyAccentTeal, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Mandibular Arch (Lower Teeth 17 - 32)",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Serif
                                ),
                                color = ThornburyInk
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ThornburyInfoWash
                        ) {
                            Text(
                                text = "LL 17-24  |  LR 25-32",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = ThornburyPrimaryText,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Lower Teeth Row with Quadrant Midline
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Lower Left (17 to 24)
                        for (num in 17..24) {
                            val tooth = teeth[num] ?: ToothRecord(num, num, "Tooth $num", "Lower")
                            AnatomicalToothView(
                                tooth = tooth,
                                isSelected = selectedToothId == tooth.number,
                                onClick = { onToothClick(tooth) }
                            )
                        }

                        // Midline Divider
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(80.dp)
                                .background(ThornburyAccentTeal.copy(alpha = 0.4f), RoundedCornerShape(1.dp))
                        )

                        // Lower Right (25 to 32)
                        for (num in 25..32) {
                            val tooth = teeth[num] ?: ToothRecord(num, num, "Tooth $num", "Lower")
                            AnatomicalToothView(
                                tooth = tooth,
                                isSelected = selectedToothId == tooth.number,
                                onClick = { onToothClick(tooth) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Selected Teeth Summary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = ThornburySurfaceCard),
            border = BorderStroke(1.dp, ThornburyHairline)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Restorative Charting Summary",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif
                    ),
                    color = ThornburyInk
                )
                Spacer(modifier = Modifier.height(8.dp))

                val nonSoundTeeth = teeth.values.filter { it.condition != ToothCondition.SOUND }
                if (nonSoundTeeth.isEmpty()) {
                    Text(
                        text = "All 32 teeth charted as sound. No active restorative pathologies flagged.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ThornburySuccess
                    )
                } else {
                    nonSoundTeeth.forEach { t ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(getToothColor(t.condition), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "#${t.number} (${t.fdiNumber}): ${t.condition.label}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = ThornburyInk
                                )
                            }
                            if (t.notes.isNotBlank()) {
                                Text(
                                    text = t.notes,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ThornburyMuted,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToothCell(
    tooth: ToothRecord,
    onClick: () -> Unit
) {
    val toothColor = getToothColor(tooth.condition)
    val isMissing = tooth.condition == ToothCondition.MISSING

    Surface(
        modifier = Modifier
            .size(width = 44.dp, height = 70.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(6.dp),
        color = ThornburyCanvas,
        border = BorderStroke(1.dp, if (tooth.condition != ToothCondition.SOUND) toothColor else ThornburyHairline)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Universal Number
            Text(
                text = "${tooth.number}",
                style = ClinicalCodeStyle.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                color = ThornburyInk,
                modifier = Modifier.padding(top = 2.dp)
            )

            // Tooth Visual Representation
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(toothColor)
                    .border(
                        1.dp,
                        if (tooth.condition == ToothCondition.SOUND) ThornburyHairline else toothColor,
                        RoundedCornerShape(6.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isMissing) {
                    Text("✕", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                } else if (tooth.condition != ToothCondition.SOUND) {
                    Text(
                        tooth.condition.code,
                        color = if (toothColor == ToothSound) ThornburyInk else Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // FDI Number
            Text(
                text = "${tooth.fdiNumber}",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = ThornburyMuted,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }
    }
}

@Composable
private fun LegendItem(label: String, color: Color, border: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, RoundedCornerShape(2.dp))
                .then(if (border) Modifier.border(1.dp, ThornburyHairline, RoundedCornerShape(2.dp)) else Modifier)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = ThornburyBody)
    }
}

fun getToothColor(condition: ToothCondition): Color {
    return when (condition) {
        ToothCondition.SOUND -> ToothSound
        ToothCondition.DECAY -> ToothDecay
        ToothCondition.FILLED -> ToothFilled
        ToothCondition.CROWN -> ToothCrown
        ToothCondition.ROOT_CANAL -> ToothRootCanal
        ToothCondition.IMPLANT -> ToothImplant
        ToothCondition.MISSING -> ToothMissing
    }
}

// =============================================================================
// Tooth Condition Edit Modal
// =============================================================================

@Composable
private fun ToothEditDialog(
    tooth: ToothRecord,
    onDismiss: () -> Unit,
    onSave: (ToothCondition, String) -> Unit
) {
    var selectedCondition by remember { mutableStateOf(tooth.condition) }
    var notesText by remember { mutableStateOf(tooth.notes) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ThornburyCanvas),
            border = BorderStroke(1.dp, ThornburyHairline)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Tooth #${tooth.number} (FDI ${tooth.fdiNumber})",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif
                            ),
                            color = ThornburyInk
                        )
                        Text(
                            text = tooth.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = ThornburyMuted
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(width = 46.dp, height = 66.dp)
                            .background(ThornburyCanvas, RoundedCornerShape(8.dp))
                            .border(1.dp, ThornburyHairline, RoundedCornerShape(8.dp))
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AnatomicalToothCanvas(
                            tooth = tooth.copy(condition = selectedCondition),
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Assign Condition:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = ThornburyInk
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ToothCondition.values().forEach { cond ->
                        val isSelected = cond == selectedCondition
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedCondition = cond },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) ThornburySurfaceSoft else ThornburyCanvas,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) ThornburyPrimary else ThornburyHairline
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(getToothColor(cond), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = cond.label,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (isSelected) ThornburyPrimaryText else ThornburyInk
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Clinical Notes / Procedure Detail") },
                    placeholder = { Text("e.g. Composite restoration, recurrent caries...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ThornburyCanvas,
                        unfocusedContainerColor = ThornburyCanvas,
                        focusedBorderColor = ThornburyPrimary,
                        unfocusedBorderColor = ThornburyHairline
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = ThornburyMuted)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(selectedCondition, notesText) },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ThornburyPrimary,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Save Record")
                    }
                }
            }
        }
    }
}

// =============================================================================
// Treatment Plans View with Tamper-Evident Hash Lock
// =============================================================================

@Composable
private fun TreatmentPlansView(
    plans: List<TreatmentPlan>,
    onCreatePlanClick: () -> Unit,
    onToggleLock: (String) -> Unit,
    onToggleStepCompletion: (String, String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Treatment Plans Header Action Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = ThornburyCanvas,
            border = BorderStroke(1.dp, ThornburyHairline)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Treatment Plans",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif
                        ),
                        color = ThornburyInk
                    )
                    Text(
                        text = "Tamper-evident cryptographically signed plans",
                        style = MaterialTheme.typography.labelSmall,
                        color = ThornburyMuted
                    )
                }

                Button(
                    onClick = onCreatePlanClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ThornburyPrimary,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "New Plan", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                }
            }
        }

        if (plans.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No treatment plans recorded yet for this patient.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ThornburyMuted
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onCreatePlanClick,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, ThornburyPrimary)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = ThornburyPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Create Initial Treatment Plan", color = ThornburyPrimary)
                    }
                }
            }
            return
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(plans, key = { it.id }) { plan ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = ThornburySurfaceCard),
                    border = BorderStroke(1.dp, ThornburyHairline)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Treatment Plan #${plan.id}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Serif
                                    ),
                                    color = ThornburyInk
                                )
                                Text(
                                    text = "By ${plan.clinicianName} • Created ${plan.dateCreated}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ThornburyMuted
                                )
                            }

                            // Tamper Lock Status Badge
                            Surface(
                                shape = RoundedCornerShape(9999.dp),
                                color = if (plan.isLocked) ThornburySurfaceDark else ThornburyWarningWash,
                                border = BorderStroke(
                                    1.dp,
                                    if (plan.isLocked) ThornburySurfaceDarkElevated else ThornburyWarning
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (plan.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                        contentDescription = null,
                                        tint = if (plan.isLocked) ThornburyAccentTeal else ThornburyWarning,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (plan.isLocked) "LOCKED & HASHED" else "DRAFT STAGE",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (plan.isLocked) ThornburyOnDark else ThornburyWarning
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Clinical Diagnosis: ${plan.diagnosis}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = ThornburyInk
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Tamper Hash String Display
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(6.dp),
                            color = ThornburySurfaceSoft,
                            border = BorderStroke(1.dp, ThornburyHairlineSoft)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = "TAMPER-EVIDENT RECORD HASH",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        letterSpacing = 0.5.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = ThornburyMuted
                                )
                                Text(
                                    text = plan.tamperHash,
                                    style = ClinicalCodeStyle.copy(fontSize = 10.sp),
                                    color = ThornburyPrimaryText
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Steps with interactive toggle checkboxes
                        Text(
                            text = "Procedure Steps (${plan.steps.count { it.completed }}/${plan.steps.size} completed):",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = ThornburyInk
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        plan.steps.forEach { step ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clickable {
                                        onToggleStepCompletion(plan.id, step.id)
                                    },
                                shape = RoundedCornerShape(6.dp),
                                color = if (step.completed) ThornburySuccessWash.copy(alpha = 0.4f) else Color.Transparent,
                                border = if (step.completed) BorderStroke(1.dp, ThornburySuccess.copy(alpha = 0.3f)) else null
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 6.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = { onToggleStepCompletion(plan.id, step.id) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (step.completed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                                contentDescription = if (step.completed) "Mark incomplete" else "Mark complete",
                                                tint = if (step.completed) ThornburySuccess else ThornburyMuted,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(
                                                text = "${step.code}: ${step.procedure}" + (if (step.toothNumber != null) " (Tooth #${step.toothNumber})" else ""),
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontWeight = if (step.completed) FontWeight.SemiBold else FontWeight.Normal
                                                ),
                                                color = if (step.completed) ThornburySuccess else ThornburyInk
                                            )
                                            if (step.completed) {
                                                Text(
                                                    text = "Completed in surgery",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                    color = ThornburySuccess
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = "$${"%.2f".format(step.fee)}",
                                        style = ClinicalCodeStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                                        color = ThornburyInk
                                    )
                                }
                            }
                        }

                        // Plan Addenda Section
                        if (plan.addenda.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = ThornburyHairlineSoft)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Plan Addenda & Clinical Amendments (${plan.addenda.size}):",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = ThornburyMuted
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            plan.addenda.forEach { addendum ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    color = ThornburySurfaceSoft,
                                    border = BorderStroke(1.dp, ThornburyHairline)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "By ${addendum.author}",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = ThornburyPrimaryText
                                            )
                                            Text(
                                                text = addendum.date,
                                                style = ClinicalCodeStyle.copy(fontSize = 10.sp),
                                                color = ThornburyMuted
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = addendum.text,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = ThornburyBodyStrong
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val total = plan.steps.sumOf { it.fee }
                            Text(
                                text = "Total Estimated Fee: $${"%.2f".format(total)}",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = ThornburyInk
                            )

                            TextButton(
                                onClick = { onToggleLock(plan.id) }
                            ) {
                                Text(
                                    text = if (plan.isLocked) "Unlock for Addenda" else "Publish & Lock Plan",
                                    color = ThornburyPrimaryText,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// Prescriptions View
// =============================================================================

@Composable
private fun PatientPrescriptionsView(
    patient: Patient,
    prescriptions: List<Prescription>,
    onIssueNew: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Prescription History",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif
                ),
                color = ThornburyInk
            )

            Button(
                onClick = onIssueNew,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ThornburyPrimary,
                    contentColor = Color.White
                )
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Issue New Script")
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (prescriptions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No active medications or prescriptions recorded.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ThornburyMuted
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(prescriptions, key = { it.id }) { rx ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = ThornburySurfaceCard),
                        border = BorderStroke(1.dp, ThornburyHairline)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = rx.drugName,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = ThornburyInk
                                )
                                Surface(
                                    shape = RoundedCornerShape(9999.dp),
                                    color = ThornburySurfaceSoft,
                                    border = BorderStroke(1.dp, ThornburyHairline)
                                ) {
                                    Text(
                                        text = rx.dosage,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ThornburyPrimaryText
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Sig: ${rx.frequency} for ${rx.duration}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = ThornburyBodyStrong
                            )
                            Text(
                                text = "Instructions: ${rx.instructions}",
                                style = MaterialTheme.typography.bodySmall,
                                color = ThornburyBody
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = ThornburyHairlineSoft)
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Issued by ${rx.clinicianName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ThornburyMuted
                                    )
                                    Text(
                                        text = rx.issueDate,
                                        style = ClinicalCodeStyle.copy(fontSize = 11.sp),
                                        color = ThornburyMuted
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        val shareText = """
                                            THORNBURY DENTAL PRACTICE - OFFICIAL PRESCRIPTION
                                            Patient: ${patient.name} (OP: ${patient.opNo}, DOB: ${patient.dob})
                                            Medication: ${rx.drugName} ${rx.dosage}
                                            Sig: ${rx.frequency} for ${rx.duration}
                                            Instructions: ${rx.instructions}
                                            Prescriber: ${rx.clinicianName}
                                            Date Issued: ${rx.issueDate}
                                            Rx ID: ${rx.id}
                                        """.trimIndent()
                                        val sendIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, shareText)
                                            type = "text/plain"
                                        }
                                        val shareIntent = Intent.createChooser(sendIntent, "Share Prescription Slip")
                                        context.startActivity(shareIntent)
                                    },
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, ThornburyHairline),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Share",
                                        tint = ThornburyPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Share Rx",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = ThornburyPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// Demographics / Overview View (Matches patient-demographics-view.tsx)
// =============================================================================

private enum class DemographicsEditField(val title: String) {
    OP_NO("Edit OP Number"),
    NAME_DOB("Edit Patient Name & DOB"),
    PHONE_EMAIL("Edit Phone & Email"),
    ADDRESS("Edit Address"),
    MEDICAL_HISTORY("Edit Medical History"),
    FAMILY_HISTORY("Edit Family History"),
    PAST_DENTAL_HISTORY("Edit Past Dental History")
}

private fun parseBulletLines(raw: String): List<String> {
    if (raw.isBlank()) return emptyList()
    val lines = raw.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
    val result = mutableListOf<String>()
    for (line in lines) {
        val cleaned = line.replace(Regex("^(\\s*[-*•]\\s*|\\s*\\d+[.)]\\s*)"), "").trim()
        if (cleaned.isNotEmpty()) result.add(cleaned)
    }
    if (result.size <= 1 && raw.contains(".")) {
        val sentences = raw.split(Regex("\\.\\s+")).map { it.trim().removeSuffix(".") }.filter { it.isNotEmpty() }
        if (sentences.size > 1) return sentences
    }
    return if (result.isNotEmpty()) result else listOf(raw.trim())
}

@Composable
private fun PatientDemographicsView(patient: Patient) {
    val scrollState = rememberScrollState()

    // Inline edit states
    var editingField by remember { mutableStateOf<DemographicsEditField?>(null) }
    var opNoInput by remember(patient.opNo) { mutableStateOf(patient.opNo) }
    var nameInput by remember(patient.name) { mutableStateOf(patient.name) }
    var dobInput by remember(patient.dob) { mutableStateOf(patient.dob) }
    var phoneInput by remember(patient.phone) { mutableStateOf(patient.phone) }
    var emailInput by remember(patient.email) { mutableStateOf(patient.email) }
    var addressInput by remember(patient.address) { mutableStateOf(patient.address) }
    var medHistInput by remember(patient.medicalHistory) { mutableStateOf(patient.medicalHistory) }
    var famHistInput by remember(patient.familyHistory) { mutableStateOf(patient.familyHistory) }
    var dentalHistInput by remember(patient.pastDentalHistory) { mutableStateOf(patient.pastDentalHistory) }

    // Allergy add dialog states
    var showAddAllergyDialog by remember { mutableStateOf(false) }
    var newAllergen by remember { mutableStateOf("") }
    var newSeverity by remember { mutableStateOf("Moderate") }
    var newReaction by remember { mutableStateOf("") }

    // Medical alert add dialog states
    var showAddAlertDialog by remember { mutableStateOf(false) }
    var newAlertText by remember { mutableStateOf("") }

    val medBullets = remember(patient.medicalHistory) { parseBulletLines(patient.medicalHistory) }
    val famBullets = remember(patient.familyHistory) { parseBulletLines(patient.familyHistory) }
    val dentalBullets = remember(patient.pastDentalHistory) { parseBulletLines(patient.pastDentalHistory) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section Header Card - Clean title without badge
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = ThornburySurfaceCard),
            border = BorderStroke(1.dp, ThornburyHairline)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Badge,
                    contentDescription = null,
                    tint = ThornburyPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Patient Demographics & Clinical Profile",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif
                    ),
                    color = ThornburyInk
                )
            }
        }

        // --- 1. OP Number Field (Inline Editing) ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = ThornburySurfaceSoft),
            border = BorderStroke(1.dp, ThornburyHairline)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Tag, contentDescription = null, tint = ThornburyMuted, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Outpatient (OP) Number", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = ThornburyMuted)
                    }
                    TextButton(
                        onClick = {
                            if (editingField == DemographicsEditField.OP_NO) editingField = null
                            else {
                                opNoInput = patient.opNo
                                editingField = DemographicsEditField.OP_NO
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = if (editingField == DemographicsEditField.OP_NO) Icons.Default.Close else Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = ThornburyPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (editingField == DemographicsEditField.OP_NO) "Cancel" else "Edit", style = MaterialTheme.typography.labelSmall, color = ThornburyPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                if (editingField == DemographicsEditField.OP_NO) {
                    OutlinedTextField(
                        value = opNoInput,
                        onValueChange = { opNoInput = it },
                        label = { Text("OP Number") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = ThornburyCanvas,
                            unfocusedContainerColor = ThornburyCanvas,
                            focusedBorderColor = ThornburyPrimary,
                            unfocusedBorderColor = ThornburyHairline
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            DentalRepository.updatePatientDemographics(
                                patientId = patient.id,
                                opNo = opNoInput,
                                name = patient.name,
                                phone = patient.phone,
                                email = patient.email,
                                address = patient.address,
                                medicalHistory = patient.medicalHistory,
                                familyHistory = patient.familyHistory,
                                pastDentalHistory = patient.pastDentalHistory
                            )
                            editingField = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ThornburyPrimary, contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Save OP Number")
                    }
                } else {
                    Text(
                        text = patient.opNo,
                        style = ClinicalCodeStyle.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                        color = ThornburyInk
                    )
                }
            }
        }

        // --- 2. Patient Name & DOB Field (Inline Editing) ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = ThornburySurfaceSoft),
            border = BorderStroke(1.dp, ThornburyHairline)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = ThornburyMuted, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Patient Name & Date of Birth", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = ThornburyMuted)
                    }
                    TextButton(
                        onClick = {
                            if (editingField == DemographicsEditField.NAME_DOB) editingField = null
                            else {
                                nameInput = patient.name
                                dobInput = patient.dob
                                editingField = DemographicsEditField.NAME_DOB
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = if (editingField == DemographicsEditField.NAME_DOB) Icons.Default.Close else Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = ThornburyPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (editingField == DemographicsEditField.NAME_DOB) "Cancel" else "Edit", style = MaterialTheme.typography.labelSmall, color = ThornburyPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                if (editingField == DemographicsEditField.NAME_DOB) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Full Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = ThornburyCanvas,
                            unfocusedContainerColor = ThornburyCanvas,
                            focusedBorderColor = ThornburyPrimary,
                            unfocusedBorderColor = ThornburyHairline
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = dobInput,
                        onValueChange = { dobInput = it },
                        label = { Text("Date of Birth (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = ThornburyCanvas,
                            unfocusedContainerColor = ThornburyCanvas,
                            focusedBorderColor = ThornburyPrimary,
                            unfocusedBorderColor = ThornburyHairline
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            DentalRepository.updatePatientDemographics(
                                patientId = patient.id,
                                opNo = patient.opNo,
                                name = nameInput,
                                phone = patient.phone,
                                email = patient.email,
                                address = patient.address,
                                medicalHistory = patient.medicalHistory,
                                familyHistory = patient.familyHistory,
                                pastDentalHistory = patient.pastDentalHistory,
                                dob = dobInput
                            )
                            editingField = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ThornburyPrimary, contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Save Name & DOB")
                    }
                } else {
                    Text(text = patient.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = ThornburyInk)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = "Date of Birth: ${patient.dob} • Last Visit: ${patient.lastVisit}", style = MaterialTheme.typography.bodySmall, color = ThornburyMuted)
                }
            }
        }

        // --- 3. Phone & Email Field (Inline Editing) ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = ThornburySurfaceSoft),
            border = BorderStroke(1.dp, ThornburyHairline)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Phone, contentDescription = null, tint = ThornburyMuted, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Phone & Email", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = ThornburyMuted)
                    }
                    TextButton(
                        onClick = {
                            if (editingField == DemographicsEditField.PHONE_EMAIL) editingField = null
                            else {
                                phoneInput = patient.phone
                                emailInput = patient.email
                                editingField = DemographicsEditField.PHONE_EMAIL
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = if (editingField == DemographicsEditField.PHONE_EMAIL) Icons.Default.Close else Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = ThornburyPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (editingField == DemographicsEditField.PHONE_EMAIL) "Cancel" else "Edit", style = MaterialTheme.typography.labelSmall, color = ThornburyPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                if (editingField == DemographicsEditField.PHONE_EMAIL) {
                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { phoneInput = it },
                        label = { Text("Phone Number") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = ThornburyCanvas,
                            unfocusedContainerColor = ThornburyCanvas,
                            focusedBorderColor = ThornburyPrimary,
                            unfocusedBorderColor = ThornburyHairline
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Email Address") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = ThornburyCanvas,
                            unfocusedContainerColor = ThornburyCanvas,
                            focusedBorderColor = ThornburyPrimary,
                            unfocusedBorderColor = ThornburyHairline
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            DentalRepository.updatePatientDemographics(
                                patientId = patient.id,
                                opNo = patient.opNo,
                                name = patient.name,
                                phone = phoneInput,
                                email = emailInput,
                                address = patient.address,
                                medicalHistory = patient.medicalHistory,
                                familyHistory = patient.familyHistory,
                                pastDentalHistory = patient.pastDentalHistory
                            )
                            editingField = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ThornburyPrimary, contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Save Phone & Email")
                    }
                } else {
                    Text(
                        text = if (patient.phone.isNotBlank()) patient.phone else "Not recorded",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = ThornburyInk
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (patient.email.isNotBlank()) "Email: ${patient.email}" else "Email: Not recorded",
                        style = MaterialTheme.typography.bodySmall,
                        color = ThornburyMuted
                    )
                }
            }
        }

        // --- 4. Postal Address Field (Inline Editing) ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = ThornburySurfaceSoft),
            border = BorderStroke(1.dp, ThornburyHairline)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Place, contentDescription = null, tint = ThornburyMuted, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Postal Address", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = ThornburyMuted)
                    }
                    TextButton(
                        onClick = {
                            if (editingField == DemographicsEditField.ADDRESS) editingField = null
                            else {
                                addressInput = patient.address
                                editingField = DemographicsEditField.ADDRESS
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = if (editingField == DemographicsEditField.ADDRESS) Icons.Default.Close else Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = ThornburyPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (editingField == DemographicsEditField.ADDRESS) "Cancel" else "Edit", style = MaterialTheme.typography.labelSmall, color = ThornburyPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                if (editingField == DemographicsEditField.ADDRESS) {
                    OutlinedTextField(
                        value = addressInput,
                        onValueChange = { addressInput = it },
                        label = { Text("Postal Address") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = ThornburyCanvas,
                            unfocusedContainerColor = ThornburyCanvas,
                            focusedBorderColor = ThornburyPrimary,
                            unfocusedBorderColor = ThornburyHairline
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            DentalRepository.updatePatientDemographics(
                                patientId = patient.id,
                                opNo = patient.opNo,
                                name = patient.name,
                                phone = patient.phone,
                                email = patient.email,
                                address = addressInput,
                                medicalHistory = patient.medicalHistory,
                                familyHistory = patient.familyHistory,
                                pastDentalHistory = patient.pastDentalHistory
                            )
                            editingField = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ThornburyPrimary, contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Save Address")
                    }
                } else {
                    Text(
                        text = if (patient.address.isNotBlank()) patient.address else "No address recorded on file.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ThornburyInk
                    )
                }
            }
        }

        HorizontalDivider(color = ThornburyHairline, modifier = Modifier.padding(vertical = 4.dp))

        // --- 5. Medical History (Inline Editing) ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = ThornburySurfaceSoft),
            border = BorderStroke(1.dp, ThornburyHairline)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.LocalHospital, contentDescription = null, tint = ThornburyPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Medical History", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = ThornburyInk)
                    }
                    TextButton(
                        onClick = {
                            if (editingField == DemographicsEditField.MEDICAL_HISTORY) editingField = null
                            else {
                                medHistInput = medBullets.joinToString("\n")
                                editingField = DemographicsEditField.MEDICAL_HISTORY
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = if (editingField == DemographicsEditField.MEDICAL_HISTORY) Icons.Default.Close else Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = ThornburyPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (editingField == DemographicsEditField.MEDICAL_HISTORY) "Cancel" else "Edit", style = MaterialTheme.typography.labelSmall, color = ThornburyPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (editingField == DemographicsEditField.MEDICAL_HISTORY) {
                    OutlinedTextField(
                        value = medHistInput,
                        onValueChange = { medHistInput = it },
                        label = { Text("Enter each condition on a new line") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = ThornburyCanvas,
                            unfocusedContainerColor = ThornburyCanvas,
                            focusedBorderColor = ThornburyPrimary,
                            unfocusedBorderColor = ThornburyHairline
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            DentalRepository.updatePatientDemographics(
                                patientId = patient.id,
                                opNo = patient.opNo,
                                name = patient.name,
                                phone = patient.phone,
                                email = patient.email,
                                address = patient.address,
                                medicalHistory = medHistInput,
                                familyHistory = patient.familyHistory,
                                pastDentalHistory = patient.pastDentalHistory
                            )
                            editingField = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ThornburyPrimary, contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Save Medical History")
                    }
                } else {
                    if (medBullets.isEmpty()) {
                        Text(text = "No medical conditions or systemic illnesses recorded.", style = MaterialTheme.typography.bodySmall.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic), color = ThornburyMuted)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            medBullets.forEach { item ->
                                Row(verticalAlignment = Alignment.Top) {
                                    Text(text = "• ", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = ThornburyPrimary)
                                    Text(text = item, style = MaterialTheme.typography.bodyMedium, color = ThornburyInk)
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 6. Family History (Inline Editing) ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = ThornburySurfaceSoft),
            border = BorderStroke(1.dp, ThornburyHairline)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Groups, contentDescription = null, tint = ThornburyPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Family History", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = ThornburyInk)
                    }
                    TextButton(
                        onClick = {
                            if (editingField == DemographicsEditField.FAMILY_HISTORY) editingField = null
                            else {
                                famHistInput = famBullets.joinToString("\n")
                                editingField = DemographicsEditField.FAMILY_HISTORY
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = if (editingField == DemographicsEditField.FAMILY_HISTORY) Icons.Default.Close else Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = ThornburyPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (editingField == DemographicsEditField.FAMILY_HISTORY) "Cancel" else "Edit", style = MaterialTheme.typography.labelSmall, color = ThornburyPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (editingField == DemographicsEditField.FAMILY_HISTORY) {
                    OutlinedTextField(
                        value = famHistInput,
                        onValueChange = { famHistInput = it },
                        label = { Text("Enter each familial trait on a new line") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = ThornburyCanvas,
                            unfocusedContainerColor = ThornburyCanvas,
                            focusedBorderColor = ThornburyPrimary,
                            unfocusedBorderColor = ThornburyHairline
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            DentalRepository.updatePatientDemographics(
                                patientId = patient.id,
                                opNo = patient.opNo,
                                name = patient.name,
                                phone = patient.phone,
                                email = patient.email,
                                address = patient.address,
                                medicalHistory = patient.medicalHistory,
                                familyHistory = famHistInput,
                                pastDentalHistory = patient.pastDentalHistory
                            )
                            editingField = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ThornburyPrimary, contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Save Family History")
                    }
                } else {
                    if (famBullets.isEmpty()) {
                        Text(text = "No hereditary or family medical conditions noted.", style = MaterialTheme.typography.bodySmall.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic), color = ThornburyMuted)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            famBullets.forEach { item ->
                                Row(verticalAlignment = Alignment.Top) {
                                    Text(text = "• ", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = ThornburyPrimary)
                                    Text(text = item, style = MaterialTheme.typography.bodyMedium, color = ThornburyInk)
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 7. Past Dental History (Inline Editing) ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = ThornburySurfaceSoft),
            border = BorderStroke(1.dp, ThornburyHairline)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.MedicalServices, contentDescription = null, tint = ThornburyPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Past Dental History", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = ThornburyInk)
                    }
                    TextButton(
                        onClick = {
                            if (editingField == DemographicsEditField.PAST_DENTAL_HISTORY) editingField = null
                            else {
                                dentalHistInput = dentalBullets.joinToString("\n")
                                editingField = DemographicsEditField.PAST_DENTAL_HISTORY
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = if (editingField == DemographicsEditField.PAST_DENTAL_HISTORY) Icons.Default.Close else Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = ThornburyPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (editingField == DemographicsEditField.PAST_DENTAL_HISTORY) "Cancel" else "Edit", style = MaterialTheme.typography.labelSmall, color = ThornburyPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (editingField == DemographicsEditField.PAST_DENTAL_HISTORY) {
                    OutlinedTextField(
                        value = dentalHistInput,
                        onValueChange = { dentalHistInput = it },
                        label = { Text("Enter each past treatment or restoration on a new line") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = ThornburyCanvas,
                            unfocusedContainerColor = ThornburyCanvas,
                            focusedBorderColor = ThornburyPrimary,
                            unfocusedBorderColor = ThornburyHairline
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            DentalRepository.updatePatientDemographics(
                                patientId = patient.id,
                                opNo = patient.opNo,
                                name = patient.name,
                                phone = patient.phone,
                                email = patient.email,
                                address = patient.address,
                                medicalHistory = patient.medicalHistory,
                                familyHistory = patient.familyHistory,
                                pastDentalHistory = dentalHistInput
                            )
                            editingField = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ThornburyPrimary, contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Save Past Dental History")
                    }
                } else {
                    if (dentalBullets.isEmpty()) {
                        Text(text = "No previous restorations, trauma, or treatments noted.", style = MaterialTheme.typography.bodySmall.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic), color = ThornburyMuted)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            dentalBullets.forEach { item ->
                                Row(verticalAlignment = Alignment.Top) {
                                    Text(text = "• ", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = ThornburyPrimary)
                                    Text(text = item, style = MaterialTheme.typography.bodyMedium, color = ThornburyInk)
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 8. Recorded Allergies & Adverse Reactions ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = ThornburySurfaceSoft),
            border = BorderStroke(1.dp, ThornburyHairline)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = ThornburyError, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Allergies & Contraindications (${patient.allergies.size})", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = ThornburyInk)
                    }

                    TextButton(
                        onClick = { showAddAllergyDialog = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = ThornburyPrimary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Allergy", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = ThornburyPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (patient.allergies.isEmpty()) {
                    Text(
                        text = "No recorded drug or material allergies (NKA).",
                        style = MaterialTheme.typography.bodySmall.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                        color = ThornburyMuted
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        patient.allergies.forEach { allergy ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                color = ThornburyErrorWash,
                                border = BorderStroke(1.dp, ThornburyError.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = allergy.allergen,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = ThornburyError
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                shape = RoundedCornerShape(9999.dp),
                                                color = ThornburyError.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = allergy.severity.uppercase(),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = ThornburyError
                                                    )
                                                )
                                            }
                                        }
                                        if (allergy.reaction.isNotBlank()) {
                                            Text(
                                                text = "Reaction: ${allergy.reaction}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = ThornburyError.copy(alpha = 0.8f)
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { DentalRepository.removePatientAllergy(patient.id, allergy.allergen) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove Allergy",
                                            tint = ThornburyError,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 9. Systemic Medical Alerts & Special Precautions ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = ThornburySurfaceSoft),
            border = BorderStroke(1.dp, ThornburyHairline)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = ThornburyWarning, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Systemic Medical Alerts (${patient.medicalAlerts.size})", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = ThornburyInk)
                    }

                    TextButton(
                        onClick = { showAddAlertDialog = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = ThornburyPrimary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Alert", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = ThornburyPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (patient.medicalAlerts.isEmpty()) {
                    Text(
                        text = "No active systemic medical alerts or surgical cautions.",
                        style = MaterialTheme.typography.bodySmall.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                        color = ThornburyMuted
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        patient.medicalAlerts.forEach { alert ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                color = ThornburyWarningWash,
                                border = BorderStroke(1.dp, ThornburyWarning.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = alert,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                        color = ThornburyWarning,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { DentalRepository.removePatientMedicalAlert(patient.id, alert) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove Alert",
                                            tint = ThornburyWarning,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Allergy Dialog
    if (showAddAllergyDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showAddAllergyDialog = false }) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = ThornburySurfaceCard,
                border = BorderStroke(1.dp, ThornburyHairline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Add Patient Allergy", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = ThornburyInk)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newAllergen,
                        onValueChange = { newAllergen = it },
                        label = { Text("Allergen (e.g. Penicillin, Latex)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Severity:", style = MaterialTheme.typography.labelSmall, color = ThornburyMuted)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Mild", "Moderate", "Severe").forEach { sev ->
                            FilterChip(
                                selected = newSeverity == sev,
                                onClick = { newSeverity = sev },
                                label = { Text(sev) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newReaction,
                        onValueChange = { newReaction = it },
                        label = { Text("Clinical Reaction (e.g. Anaphylaxis, Rash)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showAddAllergyDialog = false }) { Text("Cancel") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newAllergen.isNotBlank()) {
                                    DentalRepository.addPatientAllergy(patient.id, newAllergen, newSeverity, newReaction)
                                    newAllergen = ""
                                    newReaction = ""
                                    showAddAllergyDialog = false
                                }
                            },
                            enabled = newAllergen.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = ThornburyPrimary)
                        ) {
                            Text("Add Allergy")
                        }
                    }
                }
            }
        }
    }

    // Add Medical Alert Dialog
    if (showAddAlertDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showAddAlertDialog = false }) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = ThornburySurfaceCard,
                border = BorderStroke(1.dp, ThornburyHairline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Add Medical Alert", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = ThornburyInk)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newAlertText,
                        onValueChange = { newAlertText = it },
                        label = { Text("Alert Description (e.g. On daily Warfarin)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showAddAlertDialog = false }) { Text("Cancel") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newAlertText.isNotBlank()) {
                                    DentalRepository.addPatientMedicalAlert(patient.id, newAlertText)
                                    newAlertText = ""
                                    showAddAlertDialog = false
                                }
                            },
                            enabled = newAlertText.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = ThornburyPrimary)
                        ) {
                            Text("Add Alert")
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// Reports & Imaging View (Matches reports-and-imaging-view.tsx)
// =============================================================================

@Composable
private fun ReportsAndImagingView(
    reports: List<DiagnosticReport>,
    onAddReportClick: () -> Unit,
    onReportClick: (DiagnosticReport) -> Unit,
    onToggleRelease: (String) -> Unit
) {
    var selectedFilter by remember { mutableStateOf("All") }
    val kinds = listOf("All", "Radiograph", "Charting", "CBCT Scan", "Chairside test", "Lab Report")

    val filtered = remember(reports, selectedFilter) {
        if (selectedFilter == "All") reports
        else reports.filter { it.kind.equals(selectedFilter, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Diagnostic Reports & Imaging",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif
                    ),
                    color = ThornburyInk
                )
                Text(
                    text = "${reports.size} Records • Tap card to open high-res lightbox",
                    style = MaterialTheme.typography.labelSmall,
                    color = ThornburyMuted
                )
            }

            Button(
                onClick = onAddReportClick,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ThornburyPrimary,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Report", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Filter chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            kinds.forEach { kind ->
                val isSelected = selectedFilter == kind
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedFilter = kind },
                    label = { Text(kind) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ThornburyPrimary,
                        selectedLabelColor = Color.White,
                        containerColor = ThornburySurfaceSoft,
                        labelColor = ThornburyInk
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No diagnostic tests or imaging found.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ThornburyMuted
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onAddReportClick,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, ThornburyPrimary)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = ThornburyPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add New Test / Radiograph", color = ThornburyPrimary)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filtered, key = { it.id }) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onReportClick(item) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = ThornburySurfaceCard),
                        border = BorderStroke(1.dp, ThornburyHairline)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = when (item.kind) {
                                            "Radiograph" -> Icons.Default.Image
                                            "Charting" -> Icons.Default.FormatListNumbered
                                            "CBCT Scan" -> Icons.Default.ViewInAr
                                            else -> Icons.Default.Science
                                        },
                                        contentDescription = null,
                                        tint = ThornburyPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = ThornburyInk
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(9999.dp),
                                    color = ThornburySurfaceSoft,
                                    border = BorderStroke(1.dp, ThornburyHairline)
                                ) {
                                    Text(
                                        text = item.kind,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ThornburyPrimaryText
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = item.summary,
                                style = MaterialTheme.typography.bodySmall,
                                color = ThornburyBodyStrong
                            )

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = ThornburyHairlineSoft)
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "By ${item.clinicianName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ThornburyMuted
                                    )
                                    Text(
                                        text = "Taken: ${item.takenAt}",
                                        style = ClinicalCodeStyle.copy(fontSize = 10.sp),
                                        color = ThornburyMuted
                                    )
                                }

                                // Interactive Release Status Button
                                Surface(
                                    modifier = Modifier.clickable { onToggleRelease(item.id) },
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (item.releasedAt != null) ThornburySuccessWash else ThornburyWarningWash,
                                    border = BorderStroke(
                                        1.dp,
                                        if (item.releasedAt != null) ThornburySuccess.copy(alpha = 0.4f) else ThornburyWarning.copy(alpha = 0.4f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (item.releasedAt != null) Icons.Default.CheckCircle else Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = if (item.releasedAt != null) ThornburySuccess else ThornburyWarning,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (item.releasedAt != null) "RELEASED TO PORTAL" else "HELD IN SURGERY",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp
                                            ),
                                            color = if (item.releasedAt != null) ThornburySuccess else ThornburyWarning
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// Visit History View (Matches appointment history)
// =============================================================================

@Composable
private fun VisitHistoryView(
    patient: Patient,
    appointments: List<Appointment>,
    onBookAppointmentClick: () -> Unit,
    onUpdateStatus: (String, String) -> Unit
) {
    var statusPickerApptId by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Appointment & Visit History",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif
                    ),
                    color = ThornburyInk
                )
                Text(
                    text = "${appointments.size} Scheduled & Past Visits",
                    style = MaterialTheme.typography.labelSmall,
                    color = ThornburyMuted
                )
            }

            Button(
                onClick = onBookAppointmentClick,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ThornburyPrimary,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(imageVector = Icons.Default.EventAvailable, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Book Follow-up", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (appointments.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No visit or appointment history found for this patient.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ThornburyMuted
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onBookAppointmentClick,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, ThornburyPrimary)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = ThornburyPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Schedule Initial Visit", color = ThornburyPrimary)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(appointments, key = { it.id }) { appt ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = ThornburySurfaceCard),
                        border = BorderStroke(1.dp, ThornburyHairline)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = appt.procedure,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = ThornburyInk,
                                    modifier = Modifier.weight(1f)
                                )

                                Box {
                                    Surface(
                                        modifier = Modifier.clickable {
                                            statusPickerApptId = if (statusPickerApptId == appt.id) null else appt.id
                                        },
                                        shape = RoundedCornerShape(9999.dp),
                                        color = when (appt.status) {
                                            "completed" -> ThornburySuccessWash
                                            "confirmed" -> ThornburyPrimaryWash
                                            else -> ThornburySurfaceSoft
                                        },
                                        border = BorderStroke(
                                            1.dp,
                                            when (appt.status) {
                                                "completed" -> ThornburySuccess
                                                "confirmed" -> ThornburyPrimary.copy(alpha = 0.4f)
                                                else -> ThornburyHairline
                                            }
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = appt.status.uppercase(),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.sp
                                                ),
                                                color = when (appt.status) {
                                                    "completed" -> ThornburySuccess
                                                    "confirmed" -> ThornburyPrimaryText
                                                    else -> ThornburyMuted
                                                }
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Icon(
                                                imageVector = Icons.Default.ArrowDropDown,
                                                contentDescription = "Change status",
                                                tint = ThornburyMuted,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = statusPickerApptId == appt.id,
                                        onDismissRequest = { statusPickerApptId = null }
                                    ) {
                                        listOf("confirmed", "completed", "cancelled").forEach { statusOption ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = statusOption.uppercase(),
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                        color = when (statusOption) {
                                                            "completed" -> ThornburySuccess
                                                            "confirmed" -> ThornburyPrimaryText
                                                            else -> ThornburyMuted
                                                        }
                                                    )
                                                },
                                                onClick = {
                                                    onUpdateStatus(appt.id, statusOption)
                                                    statusPickerApptId = null
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Time: ${appt.time} (${appt.durationMin} mins) • ${appt.room}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = ThornburyBodyStrong
                            )
                            Text(
                                text = "Clinician: ${appt.clinicianName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = ThornburyBody
                            )

                            if (appt.allergyList != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = ThornburyErrorWash,
                                    border = BorderStroke(1.dp, ThornburyError.copy(alpha = 0.3f))
                                ) {
                                    Text(
                                        text = "Allergies: ${appt.allergyList}",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(color = ThornburyError)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

