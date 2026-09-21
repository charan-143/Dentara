package com.example.thornburydental.ui.cds

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thornburydental.data.DentalRepository
import com.example.thornburydental.data.Patient
import com.example.thornburydental.data.PatientDiagnosis
import com.example.thornburydental.data.PlanStep
import com.example.thornburydental.data.cds.*
import com.example.thornburydental.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CdsCopilotScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val orchestrator = remember { MultiAgentClinicalOrchestrator() }
    val coroutineScope = rememberCoroutineScope()
    val patients by DentalRepository.patients.collectAsState()

    var selectedPatient by remember { mutableStateOf<Patient?>(null) }
    var patientSearchQuery by remember { mutableStateOf("") }
    var showPatientSheet by remember { mutableStateOf(false) }

    var selectedSymptoms by remember { mutableStateOf<List<SymptomItem>>(emptyList()) }
    var freeTextDescription by remember { mutableStateOf("") }
    var customSymptomInput by remember { mutableStateOf("") }
    var vitalsContext by remember { mutableStateOf(PatientVitalsContext()) }
    
    // AI Configs
    var aiMode by remember { mutableStateOf(ClinicalAiExecutionMode.AGENT_REASONING) }
    var isWebSearchEnabled by remember { mutableStateOf(true) }

    var responseState by remember { mutableStateOf<MultiAgentExecutionState>(MultiAgentExecutionState.Idle) }
    
    // Clinician Feedback
    var clinicianSuggestionsList by remember { mutableStateOf<List<String>>(emptyList()) }
    var currentFeedbackInput by remember { mutableStateOf("") }

    val filteredPatients = remember(patientSearchQuery, patients) {
        if (patientSearchQuery.isBlank()) patients
        else patients.filter { it.name.contains(patientSearchQuery, ignoreCase = true) }
    }

    fun executeAi() {
        keyboardController?.hide()
        coroutineScope.launch {
            orchestrator.executeClinicalPipeline(
                context = context,
                patient = selectedPatient,
                freeTextNarrative = freeTextDescription,
                selectedSymptoms = selectedSymptoms,
                vitalsContext = vitalsContext,
                mediaItems = emptyList(),
                executionMode = aiMode,
                enableWebSearch = isWebSearchEnabled,
                clinicianSuggestions = clinicianSuggestionsList.map { 
                    ClinicianSuggestionRecord(
                        id = java.util.UUID.randomUUID().toString(),
                        suggestionText = it,
                        timestampIso = java.time.Instant.now().toString()
                    )
                }
            ).collect { state ->
                responseState = state
            }
        }
    }

    fun addCustomSymptom() {
        if (customSymptomInput.isNotBlank()) {
            selectedSymptoms = selectedSymptoms + SymptomItem(
                id = UUID.randomUUID().toString(),
                name = customSymptomInput.trim(),
                category = "Custom"
            )
            customSymptomInput = ""
        }
    }

    fun submitFeedbackAndRethink() {
        if (currentFeedbackInput.isNotBlank()) {
            clinicianSuggestionsList = clinicianSuggestionsList + currentFeedbackInput.trim()
            currentFeedbackInput = ""
            executeAi()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Clinical AI Copilot", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(0.dp)
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            
            // 1. Patient Selection
            item {
                Text(
                    text = "Patient",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedButton(
                    onClick = { showPatientSheet = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (selectedPatient != null) {
                                    Text(
                                        text = selectedPatient!!.name.firstOrNull()?.toString() ?: "P",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                } else {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        if (selectedPatient != null) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(selectedPatient!!.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                Text("OP No: ${selectedPatient!!.opNo}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                            }
                        } else {
                            Text("Select a patient...", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                        }
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // 2. Patient Symptoms Card
            item {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth().animateContentSize(),
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MonitorHeart, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Clinical Context & Symptoms", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        OutlinedTextField(
                            value = freeTextDescription,
                            onValueChange = { freeTextDescription = it },
                            placeholder = { Text("General clinical notes (optional)...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 3,
                            shape = RoundedCornerShape(10.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = customSymptomInput,
                            onValueChange = { customSymptomInput = it },
                            placeholder = { Text("Type symptom and press enter...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { addCustomSymptom() }),
                            trailingIcon = {
                                IconButton(onClick = { addCustomSymptom() }) {
                                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Add Chip", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val commonSymptoms = listOf(
                                SymptomItem("s1", "Severe Toothache", "Maxillofacial"),
                                SymptomItem("s3", "Facial Swelling", "Maxillofacial", isRedFlagTrigger = true),
                                SymptomItem("s5", "Bleeding on Probing", "Maxillofacial"),
                                SymptomItem("s12", "Fever", "Systemic", isRedFlagTrigger = true),
                            )
                            val allDisplaySymptoms = (commonSymptoms + selectedSymptoms.filter { it.category == "Custom" }).distinctBy { it.id }
                            
                            allDisplaySymptoms.forEach { symptom ->
                                val isSelected = selectedSymptoms.any { it.id == symptom.id }
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedSymptoms = if (isSelected) selectedSymptoms.filterNot { it.id == symptom.id }
                                        else selectedSymptoms + symptom
                                    },
                                    label = { Text(symptom.name, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, style = MaterialTheme.typography.bodySmall) },
                                    leadingIcon = if (isSelected) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                    } else null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    border = FilterChipDefaults.filterChipBorder(enabled = true, selected = isSelected, borderColor = MaterialTheme.colorScheme.outlineVariant)
                                )
                            }
                        }
                    }
                }
            }

            // 3. AI Configurations Card
            item {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoMode, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("AI Orchestrator Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        
                        Text("Reasoning Mode", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(bottom = 8.dp))
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected = aiMode == ClinicalAiExecutionMode.NORMAL,
                                onClick = { aiMode = ClinicalAiExecutionMode.NORMAL },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                            ) {
                                Text("Normal", style = MaterialTheme.typography.labelMedium)
                            }
                            SegmentedButton(
                                selected = aiMode == ClinicalAiExecutionMode.AGENT_REASONING,
                                onClick = { aiMode = ClinicalAiExecutionMode.AGENT_REASONING },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                            ) {
                                Text("Multi-Agent", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(14.dp))
                        
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Live Web Grounding", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                                Text("Search ADA & PubMed literature", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconToggleButton(
                                checked = isWebSearchEnabled,
                                onCheckedChange = { isWebSearchEnabled = it },
                                modifier = Modifier.background(
                                    color = if (isWebSearchEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(8.dp)
                                )
                            ) {
                                Icon(
                                    imageVector = if (isWebSearchEnabled) Icons.Default.Public else Icons.Default.PublicOff,
                                    contentDescription = "Web Search",
                                    tint = if (isWebSearchEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // 4. Analyze Button
            item {
                val isProcessing = responseState is MultiAgentExecutionState.Progress || responseState is MultiAgentExecutionState.StreamingText
                Button(
                    onClick = { executeAi() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    enabled = !isProcessing
                ) {
                    AnimatedContent(targetState = isProcessing, label = "ButtonContent") { processing ->
                        if (processing) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Analyzing Patient Data...", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Generate Clinical Insights", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 5. Execution Process & Results
            item {
                AnimatedVisibility(
                    visible = responseState is MultiAgentExecutionState.Progress || responseState is MultiAgentExecutionState.StreamingText || responseState is MultiAgentExecutionState.Completed,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    val steps = when (val state = responseState) {
                        is MultiAgentExecutionState.Progress -> state.steps
                        is MultiAgentExecutionState.StreamingText -> state.steps
                        is MultiAgentExecutionState.Completed -> state.result.agentSteps
                        else -> emptyList()
                    }
                    if (steps.isNotEmpty()) {
                        AgentProgressSection(steps = steps)
                    }
                }
            }

            item {
                AnimatedVisibility(
                    visible = responseState is MultiAgentExecutionState.Completed,
                    enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(tween(500)),
                    exit = fadeOut()
                ) {
                    if (responseState is MultiAgentExecutionState.Completed) {
                        val result = (responseState as MultiAgentExecutionState.Completed).result
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            DiagnosisCard(differentials = result.differentials)
                            
                            if (result.safetyAlerts.isNotEmpty()) {
                                SafetyAlertsCard(alerts = result.safetyAlerts)
                            }
                            
                            TreatmentPlanCard(plan = result.suggestedTreatmentPlan)

                            // Clinician Feedback Rethink Section
                            OutlinedCard(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Psychology, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("Refine Analysis", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    OutlinedTextField(
                                        value = currentFeedbackInput,
                                        onValueChange = { currentFeedbackInput = it },
                                        placeholder = { Text("E.g., Factor in patient's financial constraints...") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp),
                                        trailingIcon = {
                                            IconButton(onClick = { submitFeedbackAndRethink() }) {
                                                Icon(Icons.Default.Refresh, contentDescription = "Rethink", tint = MaterialTheme.colorScheme.primary)
                                            }
                                        },
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                                        keyboardActions = KeyboardActions(onGo = { submitFeedbackAndRethink() })
                                    )
                                }
                            }

                            // 6. Save to Chart Action
                            var showSaved by remember { mutableStateOf(false) }
                            Button(
                                onClick = {
                                    selectedPatient?.let { p ->
                                        val topDiff = result.differentials.firstOrNull()
                                        val primaryDiag = topDiff?.conditionName ?: result.summaryTitle
                                        val findings = buildString {
                                            if (result.differentials.isNotEmpty()) {
                                                append("Differentials:\n")
                                                result.differentials.forEach { diff ->
                                                    append("• ${diff.conditionName} (${diff.likelihoodTier}): ${diff.clinicalRationale}\n")
                                                }
                                            }
                                            if (result.safetyAlerts.isNotEmpty()) {
                                                append("\nSafety Alerts:\n")
                                                result.safetyAlerts.forEach { alert ->
                                                    append("• [${alert.drugOrTreatment}] ${alert.reason}\n")
                                                }
                                            }
                                        }.trim()

                                        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                                        // 1. Persist Clinical Diagnosis to Patient
                                        val diagRecord = PatientDiagnosis(
                                            primaryDiagnosis = primaryDiag,
                                            clinicalFindings = findings,
                                            prognosis = if (topDiff?.likelihoodTier?.contains("High", ignoreCase = true) == true) "Favourable" else "Guarded",
                                            systemicConsiderations = p.medicalHistory,
                                            dateRecorded = today,
                                            lastUpdated = today,
                                            clinicianName = "Clinical AI Copilot"
                                        )
                                        DentalRepository.updatePatientDiagnosis(p.id, diagRecord)

                                        // 2. Persist Treatment Plan with Steps to Patient
                                        val planPhases = result.suggestedTreatmentPlan?.phases.orEmpty()
                                        val planSteps = planPhases.flatMapIndexed { phaseIdx, phase ->
                                            phase.procedures.mapIndexed { procIdx, proc ->
                                                PlanStep(
                                                    id = "step-${System.currentTimeMillis()}-$phaseIdx-$procIdx",
                                                    toothNumber = proc.toothNumber,
                                                    toothNumbers = if (proc.toothNumber != null) listOf(proc.toothNumber) else emptyList(),
                                                    procedure = "[${phase.phaseTitle}] ${proc.procedureName}",
                                                    code = proc.cdtCode.ifEmpty { "D0140" },
                                                    fee = 120.0,
                                                    completed = false
                                                )
                                            }
                                        }
                                        if (planSteps.isNotEmpty()) {
                                            DentalRepository.createTreatmentPlan(
                                                patientId = p.id,
                                                title = result.suggestedTreatmentPlan?.planTitle?.ifEmpty { "AI Generated Treatment Plan" } ?: "AI Generated Treatment Plan",
                                                clinicianName = "Clinical AI Copilot",
                                                diagnosis = primaryDiag,
                                                steps = planSteps
                                            )
                                        }

                                        // 3. Save as AI Diagnostic Report
                                        DentalRepository.addReport(
                                            patientId = p.id,
                                            kind = "AI Clinical Plan",
                                            title = "AI Copilot Analysis - $primaryDiag",
                                            summary = "Primary Diagnosis: $primaryDiag\n\n$findings",
                                            clinicianName = "Clinical AI Copilot",
                                            attachments = emptyList()
                                        )
                                        
                                        showSaved = true
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                shape = RoundedCornerShape(14.dp),
                                enabled = selectedPatient != null && !showSaved
                            ) {
                                Icon(if (showSaved) Icons.Default.Check else Icons.Default.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(if (showSaved) "Added to Patient Chart!" else "Add to Patient Chart", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }
                }
            }
            
            item {
                AnimatedVisibility(
                    visible = responseState is MultiAgentExecutionState.Error,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    if (responseState is MultiAgentExecutionState.Error) {
                        val error = (responseState as MultiAgentExecutionState.Error).errorMessage
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                                Spacer(modifier = Modifier.width(16.dp))
                                Text("Analysis Failed: $error", color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(40.dp)) } // Bottom padding
        }
    }

    if (showPatientSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPatientSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(24.dp).fillMaxHeight(0.8f)) {
                Text("Select Patient", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(20.dp))
                OutlinedTextField(
                    value = patientSearchQuery,
                    onValueChange = { patientSearchQuery = it },
                    placeholder = { Text("Search by name or OP number...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(filteredPatients) { patient ->
                        ListItem(
                            headlineContent = { Text(patient.name, fontWeight = FontWeight.Bold) },
                            supportingContent = { Text("OP No: ${patient.opNo}") },
                            leadingContent = {
                                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(40.dp)) {
                                    Box(contentAlignment = Alignment.Center) { 
                                        Text(
                                            text = patient.name.firstOrNull()?.toString() ?: "P",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            },
                            modifier = Modifier
                                .clickable {
                                    selectedPatient = patient
                                    patientSearchQuery = ""
                                    showPatientSheet = false
                                }
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AgentProgressSection(steps: List<AgentExecutionStep>) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Memory, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text("AI Orchestration Log", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            
            steps.forEach { step ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 6.dp).fillMaxWidth()
                ) {
                    AnimatedContent(targetState = step.status, label = "StatusIcon") { status ->
                        when (status) {
                            AgentStatus.RUNNING -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp), 
                                    strokeWidth = 2.dp, 
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            AgentStatus.COMPLETED -> {
                                Icon(
                                    Icons.Default.CheckCircle, 
                                    contentDescription = "Done", 
                                    tint = Color(0xFF4CAF50), 
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            else -> {
                                Icon(
                                    Icons.Default.RadioButtonUnchecked, 
                                    contentDescription = "Pending", 
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), 
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = step.stepTitle, 
                            fontSize = 14.sp, 
                            fontWeight = if (step.status == AgentStatus.RUNNING) FontWeight.Bold else FontWeight.Medium,
                            color = if (step.status == AgentStatus.PENDING) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                        )
                        AnimatedVisibility(visible = step.status == AgentStatus.RUNNING && step.detailMessage.isNotEmpty()) {
                            Text(
                                text = step.detailMessage, 
                                fontSize = 12.sp, 
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DiagnosisCard(differentials: List<DifferentialDiagnosisRecommendation>) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.MedicalInformation, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text("Differential Diagnoses", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(14.dp))
            
            differentials.forEachIndexed { index, diff ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = diff.conditionName, 
                                style = MaterialTheme.typography.bodyMedium, 
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f).padding(end = 8.dp)
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (diff.likelihoodTier.contains("High", true)) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = diff.likelihoodTier,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (diff.likelihoodTier.contains("High", true)) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        if (diff.clinicalRationale.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = diff.clinicalRationale, 
                                style = MaterialTheme.typography.bodySmall, 
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SafetyAlertsCard(alerts: List<PharmacologicalAlert>) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onError, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text("Safety Guardrails", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
            }
            Spacer(modifier = Modifier.height(14.dp))
            
            alerts.forEach { alert ->
                Row(modifier = Modifier.padding(bottom = 10.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.PriorityHigh, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 2.dp).size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = alert.drugOrTreatment, 
                            style = MaterialTheme.typography.bodyMedium, 
                            fontWeight = FontWeight.Bold, 
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = alert.reason, 
                            style = MaterialTheme.typography.bodySmall, 
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}
