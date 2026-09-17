package com.example.thornburydental.ui.clinic

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.thornburydental.data.*
import com.example.thornburydental.export.PatientPdfGenerator
import com.example.thornburydental.export.PatientShareOptions
import com.example.thornburydental.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Modern Material 3 Export & Share Hub Dialog for Patient Health Records.
 *
 * Features:
 * 1. Smart Presets: One-tap configuration (Full Record, Clinical Summary, Rx & Care Plans).
 * 2. Referral Anonymization Switch: Protect sensitive patient details for external sharing.
 * 3. Accessible Section Selection Cards: WCAG AA 48dp minimum touch targets.
 * 4. High-Fidelity Vector Document Preview: Live PDF page pagination with zoomable preview.
 * 5. Primary/Secondary CTA Footer: Clear action hierarchy for Share, Download, and Preview.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SharePatientPdfDialog(
    patient: Patient,
    treatmentPlans: List<TreatmentPlan>,
    prescriptions: List<Prescription>,
    reports: List<DiagnosticReport>,
    appointments: List<Appointment>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val clinicianName by DentalRepository.clinicianDisplayName.collectAsState()

    var activeTab by remember { mutableIntStateOf(0) } // 0 = Configure Sections, 1 = Document Preview

    var incDemographics by remember { mutableStateOf(true) }
    var incMedical by remember { mutableStateOf(true) }
    var incExamination by remember { mutableStateOf(true) }
    var incDiagnosis by remember { mutableStateOf(true) }
    var incTreatmentPlans by remember { mutableStateOf(true) }
    var incPrescriptions by remember { mutableStateOf(true) }
    var incReports by remember { mutableStateOf(true) }
    var incAppointments by remember { mutableStateOf(true) }
    var anonymizeData by remember { mutableStateOf(false) }

    var isProcessing by remember { mutableStateOf(false) }
    var processingStatusText by remember { mutableStateOf("") }

    var generatedPdfFile by remember { mutableStateOf<File?>(null) }
    var previewBitmaps by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var previewPageIndex by remember { mutableIntStateOf(0) }

    val selectedCount = listOf(
        incDemographics, incMedical, incExamination, incDiagnosis,
        incTreatmentPlans, incPrescriptions, incReports, incAppointments
    ).count { it }

    val hasAnySelected = selectedCount > 0

    fun resetPreview() {
        generatedPdfFile = null
        previewBitmaps = emptyList()
    }

    fun applyPreset(preset: String) {
        when (preset) {
            "FULL" -> {
                incDemographics = true; incMedical = true; incExamination = true; incDiagnosis = true
                incTreatmentPlans = true; incPrescriptions = true; incReports = true; incAppointments = true
            }
            "SUMMARY" -> {
                incDemographics = true; incMedical = true; incExamination = true; incDiagnosis = true
                incTreatmentPlans = false; incPrescriptions = false; incReports = false; incAppointments = false
            }
            "RX_CARE" -> {
                incDemographics = true; incMedical = true; incExamination = false; incDiagnosis = false
                incTreatmentPlans = true; incPrescriptions = true; incReports = false; incAppointments = false
            }
            "NONE" -> {
                incDemographics = false; incMedical = false; incExamination = false; incDiagnosis = false
                incTreatmentPlans = false; incPrescriptions = false; incReports = false; incAppointments = false
            }
        }
        resetPreview()
    }

    fun buildOptions(): PatientShareOptions {
        return PatientShareOptions(
            includeDemographics = incDemographics,
            includeMedicalHistory = incMedical,
            includeExamination = incExamination,
            includeDiagnosis = incDiagnosis,
            includeTreatmentPlans = incTreatmentPlans,
            includePrescriptions = incPrescriptions,
            includeReports = incReports,
            includeAppointments = incAppointments,
            anonymizePatientData = anonymizeData
        )
    }

    fun generateOrGetPdf(onReady: (File) -> Unit) {
        if (!hasAnySelected) {
            Toast.makeText(context, "Please select at least one section to export.", Toast.LENGTH_SHORT).show()
            return
        }

        generatedPdfFile?.let {
            onReady(it)
            return
        }

        isProcessing = true
        processingStatusText = "Generating clinical PDF..."
        coroutineScope.launch {
            try {
                val file = withContext(Dispatchers.IO) {
                    PatientPdfGenerator.generatePdfFile(
                        context = context,
                        patient = patient,
                        options = buildOptions(),
                        treatmentPlans = treatmentPlans,
                        prescriptions = prescriptions,
                        reports = reports,
                        appointments = appointments,
                        clinicianName = clinicianName
                    )
                }
                generatedPdfFile = file
                isProcessing = false
                onReady(file)
            } catch (e: Exception) {
                isProcessing = false
                Toast.makeText(context, "Failed to generate PDF: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun loadPreview() {
        if (!hasAnySelected) {
            Toast.makeText(context, "Please select at least one section to preview.", Toast.LENGTH_SHORT).show()
            return
        }

        isProcessing = true
        processingStatusText = "Rendering live PDF pages..."
        activeTab = 1
        coroutineScope.launch {
            try {
                val file = generatedPdfFile ?: withContext(Dispatchers.IO) {
                    PatientPdfGenerator.generatePdfFile(
                        context = context,
                        patient = patient,
                        options = buildOptions(),
                        treatmentPlans = treatmentPlans,
                        prescriptions = prescriptions,
                        reports = reports,
                        appointments = appointments,
                        clinicianName = clinicianName
                    )
                }
                generatedPdfFile = file
                val bitmaps = withContext(Dispatchers.IO) {
                    PatientPdfGenerator.renderPdfBitmaps(file, scaleFactor = 1.8f)
                }
                previewBitmaps = bitmaps
                previewPageIndex = 0
                isProcessing = false
            } catch (e: Exception) {
                isProcessing = false
                Toast.makeText(context, "Failed to render preview: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun downloadPdf() {
        generateOrGetPdf { file ->
            val result = PatientPdfGenerator.savePdfToPublicDownloads(context, file, patient)
            result.onSuccess { downloadedFile ->
                Toast.makeText(
                    context,
                    "Saved to Downloads: ${downloadedFile.name}",
                    Toast.LENGTH_LONG
                ).show()
                PatientPdfGenerator.viewPdfFile(context, file, patient)
            }.onFailure { err ->
                Toast.makeText(context, "Download failed: ${err.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun sharePdf() {
        generateOrGetPdf { file ->
            PatientPdfGenerator.sharePdfFile(context, file, patient)
        }
    }

    Dialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .adaptiveDialogWidth(720.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = ThornburyCanvas),
            border = BorderStroke(1.dp, ThornburyHairline),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
                    .padding(20.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ThornburyPrimaryWash,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    tint = ThornburyPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Export & Share Health Record",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                ),
                                color = ThornburyInk
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = patient.name,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = ThornburyInk
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = ThornburySurfaceSoft
                                ) {
                                    Text(
                                        text = patient.opNo,
                                        style = ClinicalCodeStyle.copy(fontSize = 11.sp),
                                        color = ThornburyPrimary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    FilledTonalIconButton(
                        onClick = { if (!isProcessing) onDismiss() },
                        modifier = Modifier.size(36.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = ThornburySurfaceSoft,
                            contentColor = ThornburyMuted
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Navigation Tabs
                TabRow(
                    selectedTabIndex = activeTab,
                    containerColor = ThornburySurfaceSoft,
                    contentColor = ThornburyPrimary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                            color = ThornburyPrimary,
                            height = 3.dp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        modifier = Modifier.heightIn(min = 48.dp),
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "1. Configure ($selectedCount/8)",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = {
                            if (previewBitmaps.isEmpty()) {
                                loadPreview()
                            } else {
                                activeTab = 1
                            }
                        },
                        modifier = Modifier.heightIn(min = 48.dp),
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "2. Live Preview",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // TAB CONTENT WITH SMOOTH DIRECTIONAL TRANSITION
                AnimatedContent(
                    targetState = activeTab,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInHorizontally(
                                animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
                                initialOffsetX = { fullWidth -> (fullWidth * 0.20f).toInt() }
                            ) + fadeIn(animationSpec = tween(280))) togetherWith (
                                slideOutHorizontally(
                                    animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
                                    targetOffsetX = { fullWidth -> (-fullWidth * 0.20f).toInt() }
                                ) + fadeOut(animationSpec = tween(280))
                            )
                        } else {
                            (slideInHorizontally(
                                animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
                                initialOffsetX = { fullWidth -> (-fullWidth * 0.20f).toInt() }
                            ) + fadeIn(animationSpec = tween(280))) togetherWith (
                                slideOutHorizontally(
                                    animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
                                    targetOffsetX = { fullWidth -> (fullWidth * 0.20f).toInt() }
                                ) + fadeOut(animationSpec = tween(280))
                            )
                        }
                    },
                    label = "ShareDialogTabTransition",
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp)
                ) { targetTab ->
                    if (targetTab == 0) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 380.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            // Preset Filter Chips Header
                            Text(
                                text = "SMART PRESETS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = ThornburyPrimary
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilterChip(
                                    selected = selectedCount == 8,
                                    onClick = { applyPreset("FULL") },
                                    label = { Text("Full Record (8)") },
                                    leadingIcon = {
                                        if (selectedCount == 8) {
                                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                )

                                FilterChip(
                                    selected = incDemographics && incMedical && incExamination && incDiagnosis && !incTreatmentPlans,
                                    onClick = { applyPreset("SUMMARY") },
                                    label = { Text("Clinical Summary") },
                                    leadingIcon = {
                                        Icon(Icons.Default.MedicalServices, contentDescription = null, modifier = Modifier.size(14.dp))
                                    }
                                )

                                FilterChip(
                                    selected = incTreatmentPlans && incPrescriptions && !incExamination,
                                    onClick = { applyPreset("RX_CARE") },
                                    label = { Text("Rx & Care Plans") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Medication, contentDescription = null, modifier = Modifier.size(14.dp))
                                    }
                                )

                                FilterChip(
                                    selected = selectedCount == 0,
                                    onClick = { applyPreset("NONE") },
                                    label = { Text("Clear All") }
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Confidentiality / Referral Anonymization Toggle Card
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (anonymizeData) ThornburyPrimaryWash.copy(alpha = 0.5f) else ThornburySurfaceSoft
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (anonymizeData) ThornburyPrimary.copy(alpha = 0.4f) else ThornburyHairline
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Security,
                                            contentDescription = null,
                                            tint = if (anonymizeData) ThornburyPrimary else ThornburyMuted,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "External Referral Mode",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = ThornburyInk
                                            )
                                            Text(
                                                text = "Mask patient phone, email, and street address on PDF",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = ThornburyMuted
                                            )
                                        }
                                    }
                                    Switch(
                                        checked = anonymizeData,
                                        onCheckedChange = {
                                            anonymizeData = it
                                            resetPreview()
                                        },
                                        colors = SwitchDefaults.colors(checkedThumbColor = ThornburyPrimary)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "INCLUDED CLINICAL SECTIONS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = ThornburyPrimary
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Section Checkbox Cards (Accessible 48dp+ Touch Targets)
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                AccessibleSectionCard(
                                    icon = Icons.Default.Person,
                                    title = "Demographics & Contact Info",
                                    subtitle = "Name, OP number, DOB, contact details",
                                    isChecked = incDemographics,
                                    onToggle = { incDemographics = !incDemographics; resetPreview() }
                                )

                                AccessibleSectionCard(
                                    icon = Icons.Default.FavoriteBorder,
                                    title = "Medical History & Systemic Alerts",
                                    subtitle = "Allergies, medical alerts, dental history",
                                    isChecked = incMedical,
                                    onToggle = { incMedical = !incMedical; resetPreview() }
                                )

                                AccessibleSectionCard(
                                    icon = Icons.Default.GridView,
                                    title = "Dental Odontogram & Exam",
                                    subtitle = "32-Teeth chart findings & CRA score",
                                    isChecked = incExamination,
                                    onToggle = { incExamination = !incExamination; resetPreview() }
                                )

                                AccessibleSectionCard(
                                    icon = Icons.AutoMirrored.Filled.FactCheck,
                                    title = "Diagnosis & Prognosis",
                                    subtitle = "Clinical findings & diagnostic summary",
                                    isChecked = incDiagnosis,
                                    onToggle = { incDiagnosis = !incDiagnosis; resetPreview() }
                                )

                                val planCount = treatmentPlans.filter { it.patientId == patient.id }.size
                                AccessibleSectionCard(
                                    icon = Icons.AutoMirrored.Filled.Assignment,
                                    title = "Phased Treatment Plans",
                                    subtitle = "$planCount Care plan(s) & procedures",
                                    isChecked = incTreatmentPlans,
                                    badgeText = "$planCount",
                                    onToggle = { incTreatmentPlans = !incTreatmentPlans; resetPreview() }
                                )

                                val rxCount = prescriptions.filter { it.patientId == patient.id }.size
                                AccessibleSectionCard(
                                    icon = Icons.Default.Medication,
                                    title = "Prescriptions & Formulary",
                                    subtitle = "$rxCount Medication order(s) & dosage instructions",
                                    isChecked = incPrescriptions,
                                    badgeText = "$rxCount",
                                    onToggle = { incPrescriptions = !incPrescriptions; resetPreview() }
                                )

                                val reportCount = reports.filter { it.patientId == patient.id }.size
                                AccessibleSectionCard(
                                    icon = Icons.Default.PhotoLibrary,
                                    title = "Diagnostic Reports & Imaging",
                                    subtitle = "$reportCount Radiograph & diagnostic scan summaries",
                                    isChecked = incReports,
                                    badgeText = "$reportCount",
                                    onToggle = { incReports = !incReports; resetPreview() }
                                )

                                val apptCount = appointments.filter { it.patientId == patient.id }.size
                                AccessibleSectionCard(
                                    icon = Icons.Default.Event,
                                    title = "Appointment History",
                                    subtitle = "$apptCount Scheduled & past clinic visit records",
                                    isChecked = incAppointments,
                                    badgeText = "$apptCount",
                                    onToggle = { incAppointments = !incAppointments; resetPreview() }
                                )
                            }
                        }
                    } else {
                        // TAB 1: LIVE PDF PREVIEW VIEW
                        if (isProcessing) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(320.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = ThornburyPrimary, strokeWidth = 3.dp)
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(
                                        text = processingStatusText.ifBlank { "Rendering preview..." },
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = ThornburyInk
                                    )
                                }
                            }
                        } else if (previewBitmaps.isNotEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 360.dp)
                            ) {
                                // Pagination Control Bar
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(ThornburySurfaceSoft, RoundedCornerShape(10.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FilledTonalIconButton(
                                        onClick = { if (previewPageIndex > 0) previewPageIndex-- },
                                        enabled = previewPageIndex > 0,
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Previous Page",
                                            tint = if (previewPageIndex > 0) ThornburyPrimary else ThornburyMuted
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = ThornburyCanvas,
                                        border = BorderStroke(1.dp, ThornburyHairline)
                                    ) {
                                        Text(
                                            text = "Page ${previewPageIndex + 1} of ${previewBitmaps.size}",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = ThornburyInk,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                        )
                                    }

                                    FilledTonalIconButton(
                                        onClick = { if (previewPageIndex < previewBitmaps.size - 1) previewPageIndex++ },
                                        enabled = previewPageIndex < previewBitmaps.size - 1,
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = "Next Page",
                                            tint = if (previewPageIndex < previewBitmaps.size - 1) ThornburyPrimary else ThornburyMuted
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Rendered PDF Page View
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 300.dp)
                                        .verticalScroll(rememberScrollState())
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(1.dp, ThornburyHairline, RoundedCornerShape(12.dp))
                                        .background(Color.White)
                                        .padding(6.dp)
                                ) {
                                    Image(
                                        bitmap = previewBitmaps[previewPageIndex].asImageBitmap(),
                                        contentDescription = "PDF Page ${previewPageIndex + 1}",
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(280.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = null,
                                        tint = ThornburyMuted,
                                        modifier = Modifier.size(52.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "No preview generated yet",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = ThornburyMuted
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = { loadPreview() },
                                        colors = ButtonDefaults.buttonColors(containerColor = ThornburyPrimary),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Render Live Preview")
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = ThornburyHairline)
                Spacer(modifier = Modifier.height(12.dp))

                // Action Bar (Footer)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AnimatedContent(
                        targetState = activeTab,
                        transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(220)) },
                        label = "FooterTabTransition",
                        modifier = Modifier.weight(1f)
                    ) { currentTab ->
                        if (currentTab == 0) {
                            OutlinedButton(
                                onClick = { loadPreview() },
                                enabled = hasAnySelected && !isProcessing,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, ThornburyHairline)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Preview",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        } else {
                            OutlinedButton(
                                onClick = { activeTab = 0 },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, ThornburyHairline)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Sections",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    FilledTonalButton(
                        onClick = { downloadPdf() },
                        enabled = hasAnySelected && !isProcessing,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = ThornburySurfaceSoft,
                            contentColor = ThornburyInk
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = ThornburyPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Download",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Button(
                        onClick = { sharePdf() },
                        enabled = hasAnySelected && !isProcessing,
                        modifier = Modifier
                            .weight(1.1f)
                            .heightIn(min = 48.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ThornburyPrimary,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Share PDF",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * Accessible Section Card Composable ensuring WCAG 2.2 AA compliance (min 48dp height).
 */
@Composable
private fun AccessibleSectionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isChecked: Boolean,
    badgeText: String? = null,
    onToggle: () -> Unit
) {
    Surface(
        onClick = onToggle,
        shape = RoundedCornerShape(12.dp),
        color = if (isChecked) ThornburyPrimaryWash.copy(alpha = 0.35f) else ThornburySurfaceSoft,
        border = BorderStroke(
            width = if (isChecked) 1.5.dp else 1.dp,
            color = if (isChecked) ThornburyPrimary.copy(alpha = 0.6f) else ThornburyHairline
        ),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isChecked,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = ThornburyPrimary,
                    uncheckedColor = ThornburyMuted
                ),
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Surface(
                shape = CircleShape,
                color = if (isChecked) ThornburyPrimaryWash else ThornburyCanvas,
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isChecked) ThornburyPrimary else ThornburyMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Medium,
                            color = if (isChecked) ThornburyInk else ThornburyBodyStrong
                        )
                    )
                    badgeText?.let { count ->
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = CircleShape,
                            color = if (isChecked) ThornburyPrimary else ThornburyMuted.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = count,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = if (isChecked) Color.White else ThornburyInk,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = ThornburyMuted
                )
            }
        }
    }
}
