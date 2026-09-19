package com.example.thornburydental.ui.cds

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thornburydental.data.cds.*
import com.example.thornburydental.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClinicalDecisionSupportScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val llmEngine = remember { OnDeviceLlmEngine() }
    val corpusRepo = ClinicalCorpusRepository
    val coroutineScope = rememberCoroutineScope()

    var selectedSymptoms by remember { mutableStateOf<List<SymptomItem>>(emptyList()) }
    var freeTextDescription by remember { mutableStateOf("") }
    var vitalsContext by remember { mutableStateOf(PatientVitalsContext()) }

    var responseState by remember { mutableStateOf<CdsResponseState>(CdsResponseState.Idle) }
    var selectedPassageForSourceView by remember { mutableStateOf<RetrievedPassage?>(null) }
    var showCorpusInfoDialog by remember { mutableStateOf(false) }

    val deviceTier = remember { llmEngine.detectDeviceTier() }
    val corpusMetadata by corpusRepo.metadata.collectAsState()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            Surface(
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ThornburyInk)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Offline Decision Support",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = ThornburyInk
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SignalCellularOff,
                                contentDescription = "Offline",
                                modifier = Modifier.size(12.dp),
                                tint = ThornburySuccess
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "100% On-Device • Zero Network Egress",
                                style = MaterialTheme.typography.labelSmall,
                                color = ThornburyMuted
                            )
                        }
                    }

                    // Hardware Tier Badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ThornburyPrimaryWash,
                        border = BorderStroke(1.dp, ThornburyHairline)
                    ) {
                        Text(
                            text = deviceTier.split(" ").firstOrNull() ?: "Device",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = ThornburyPrimaryText,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(onClick = { showCorpusInfoDialog = true }) {
                        Icon(Icons.Default.Info, contentDescription = "Corpus Info", tint = ThornburyPrimary)
                    }
                }
            }
        },
        containerColor = ThornburyCanvas
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Persistent Mandatory Disclaimer Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                border = BorderStroke(1.dp, Color(0xFFFFE082)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Disclaimer",
                        tint = Color(0xFFF57F17),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Clinical Reference Only — Not a Diagnosis. Grounded decision-support tool for licensed clinicians. Every output must be confirmed independently; seek emergency care for red-flag symptoms.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = Color(0xFF5D4037)
                    )
                }
            }

            // Symptom & Vitals Input Picker
            SymptomPickerComponent(
                selectedSymptoms = selectedSymptoms,
                onSymptomToggle = { symptom ->
                    selectedSymptoms = if (selectedSymptoms.any { it.id == symptom.id }) {
                        selectedSymptoms.filterNot { it.id == symptom.id }
                    } else {
                        selectedSymptoms + symptom
                    }
                },
                freeTextDescription = freeTextDescription,
                onFreeTextChange = { freeTextDescription = it },
                vitalsContext = vitalsContext,
                onVitalsChange = { vitalsContext = it }
            )

            // Submit Button
            Button(
                onClick = {
                    val query = SymptomInputQuery(
                        selectedSymptoms = selectedSymptoms,
                        freeTextDescription = freeTextDescription,
                        vitalsContext = vitalsContext
                    )
                    coroutineScope.launch {
                        llmEngine.generateResponseStream(query).collect { state ->
                            responseState = state
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ThornburyPrimary,
                    contentColor = Color.White
                ),
                enabled = selectedSymptoms.isNotEmpty() || freeTextDescription.isNotBlank() || responseState !is CdsResponseState.ProcessingRetrieval
            ) {
                Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (responseState is CdsResponseState.ProcessingRetrieval || responseState is CdsResponseState.Generating) "Evaluating Offline Knowledge Base..." else "Evaluate Symptoms (Offline RAG Engine)",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }

            // Evaluation Results Display
            when (val state = responseState) {
                is CdsResponseState.Idle -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = ThornburySurface),
                        border = BorderStroke(1.dp, ThornburyHairline)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.MedicalServices,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = ThornburyMuted
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Ready for Symptom Evaluation",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = ThornburyInk
                            )
                            Text(
                                text = "Select symptoms above to query the offline clinical knowledge base.",
                                style = MaterialTheme.typography.bodySmall,
                                color = ThornburyMuted
                            )
                        }
                    }
                }

                is CdsResponseState.ProcessingRetrieval -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = ThornburySurface),
                        border = BorderStroke(1.dp, ThornburyHairline)
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = ThornburyPrimary, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "Searching Local Vector Index...",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = ThornburyInk
                                )
                                Text(
                                    text = "Retrieving evidence-based guidelines & checking red-flag rules",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ThornburyMuted
                                )
                            }
                        }
                    }
                }

                is CdsResponseState.Generating -> {
                    CdsOutputCard(
                        text = state.partialText,
                        redFlagResult = state.redFlagResult,
                        isGenerating = true,
                        onViewSourceClick = { passage -> selectedPassageForSourceView = passage }
                    )
                }

                is CdsResponseState.Complete -> {
                    CdsOutputCard(
                        text = state.fullText,
                        redFlagResult = state.redFlagResult,
                        differentials = state.differentials,
                        citedPassages = state.citedPassages,
                        isGenerating = false,
                        inferenceTimeMs = state.inferenceTimeMs,
                        onViewSourceClick = { passage -> selectedPassageForSourceView = passage }
                    )
                }

                is CdsResponseState.Error -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                        border = BorderStroke(1.dp, Color(0xFFEF9A9A))
                    ) {
                        Text(
                            text = "Error: ${state.errorMessage}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFC62828),
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }

    // Source Citation Modal / Bottom Sheet
    selectedPassageForSourceView?.let { passage ->
        AlertDialog(
            onDismissRequest = { selectedPassageForSourceView = null },
            confirmButton = {
                TextButton(onClick = { selectedPassageForSourceView = null }) {
                    Text("Close Source View", color = ThornburyPrimary)
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MenuBook, contentDescription = null, tint = ThornburyPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Cited Clinical Source", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = passage.guidelineTitle,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = ThornburyInk
                    )
                    Text(
                        text = "${passage.sectionHeader} • Version ${passage.version}",
                        style = MaterialTheme.typography.labelSmall,
                        color = ThornburyMuted
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = ThornburyCanvas,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, ThornburyHairline)
                    ) {
                        Text(
                            text = passage.passageContent,
                            style = MaterialTheme.typography.bodyMedium,
                            color = ThornburyInk,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Last Clinician Review: ${passage.lastReviewedDate}",
                        style = MaterialTheme.typography.labelSmall,
                        color = ThornburyMuted
                    )
                }
            }
        )
    }

    // Knowledge Base Info & Checksum Dialog
    if (showCorpusInfoDialog) {
        val isIntegrityValid = remember { corpusRepo.verifyCorpusIntegrity() }
        AlertDialog(
            onDismissRequest = { showCorpusInfoDialog = false },
            confirmButton = {
                TextButton(onClick = { showCorpusInfoDialog = false }) {
                    Text("Done", color = ThornburyPrimary)
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = ThornburySuccess)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Knowledge Base Details", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }
            },
            text = {
                Column {
                    Text(text = corpusMetadata.title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Version: ${corpusMetadata.version}", style = MaterialTheme.typography.bodySmall)
                    Text(text = "Passages Indexed: ${corpusMetadata.passageCount}", style = MaterialTheme.typography.bodySmall)
                    Text(text = "Last Updated: ${corpusMetadata.lastUpdatedIso}", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isIntegrityValid) Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = null,
                            tint = if (isIntegrityValid) ThornburySuccess else Color.Red,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isIntegrityValid) "SHA-256 Checksum Verified" else "Integrity Warning",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isIntegrityValid) ThornburySuccess else Color.Red
                        )
                    }
                }
            }
        )
    }
}

@Composable
private fun CdsOutputCard(
    text: String,
    redFlagResult: RedFlagResult,
    differentials: List<DifferentialItem> = emptyList(),
    citedPassages: List<RetrievedPassage> = emptyList(),
    isGenerating: Boolean,
    inferenceTimeMs: Long = 0,
    onViewSourceClick: (RetrievedPassage) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ThornburySurface),
        border = BorderStroke(1.dp, ThornburyHairline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = ThornburyPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Clinical Decision Support Output",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = ThornburyInk
                    )
                }
                if (inferenceTimeMs > 0) {
                    Text(
                        text = "${inferenceTimeMs}ms",
                        style = MaterialTheme.typography.labelSmall,
                        color = ThornburyMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Red Flag Banner
            if (redFlagResult.hasRedFlag) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    border = BorderStroke(1.dp, Color(0xFFEF9A9A)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFC62828))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "IMMEDIATE ESCALATION REQUIRED",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFFC62828)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = redFlagResult.redFlagReason ?: "Red flag condition detected",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFB71C1C)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        redFlagResult.recommendedActions.forEach { action ->
                            Text(text = "• $action", style = MaterialTheme.typography.bodySmall, color = Color(0xFFC62828))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Raw / Streaming Text Box
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                color = ThornburyInk
            )

            if (isGenerating) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = ThornburyPrimary)
            }

            // Cited Sources Buttons
            if (citedPassages.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = ThornburyHairline)
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Grounded Source Citations:",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = ThornburyInk
                )
                Spacer(modifier = Modifier.height(6.dp))
                citedPassages.forEach { passage ->
                    OutlinedButton(
                        onClick = { onViewSourceClick(passage) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, ThornburyHairline)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(16.dp), tint = ThornburyPrimary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${passage.guidelineTitle} (${passage.sectionHeader})",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = ThornburyPrimaryText,
                                    maxLines = 1
                                )
                            }
                            Text(text = "View Source", style = MaterialTheme.typography.labelSmall, color = ThornburyPrimary)
                        }
                    }
                }
            }
        }
    }
}
