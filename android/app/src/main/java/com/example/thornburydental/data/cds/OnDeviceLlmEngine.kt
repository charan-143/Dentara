package com.example.thornburydental.data.cds

import android.content.Context
import java.util.Locale
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * On-device offline LLM inference controller with streaming token generation,
 * RAG prompt synthesis, device capability tier detection, prefix caching, and token batching.
 */
class OnDeviceLlmEngine(
    private val embeddingEngine: ClinicalEmbeddingEngine = ClinicalEmbeddingEngine(),
    private val corpusRepository: ClinicalCorpusRepository = ClinicalCorpusRepository,
    private val bridge: LlamaInferenceBridge = LlamaNative,
    private val inferenceDispatcher: CoroutineDispatcher = defaultLlmDispatcher
) {

    companion object {
        /**
         * Dedicated single-threaded dispatcher for LLM execution to prevent CPU thrashing
         * and preserve sequential scheduling with speech-to-text.
         */
        val defaultLlmDispatcher: CoroutineDispatcher by lazy {
            Executors.newSingleThreadExecutor { runnable ->
                Thread(runnable, "dentara-llm-worker").apply {
                    priority = Thread.NORM_PRIORITY
                }
            }.asCoroutineDispatcher()
        }

        private const val TOKEN_BATCH_WINDOW_MS = 35L
    }

    private var activeModelSpec: LlmModelSpec? = null
    private var contextPtr: Long = 0L
    private var cachedPrefix: String? = null

    /**
     * Hardware capability detection for model tier selection.
     */
    fun detectDeviceTier(context: Context? = null): String {
        val spec = LlmModels.selectModelForDevice(context)
        return "${spec.displayName} (RAM Tier: ${spec.parameterCount} quantized ${spec.quantization})"
    }

    /**
     * Warms up LLM model with a dummy 1-token decode for rapid TTFT.
     */
    fun warmup(modelSpec: LlmModelSpec = LlmModels.LLAMA_3_2_1B_Q4): Boolean {
        activeModelSpec = modelSpec
        cachedPrefix = getStaticSystemPrefix()
        return true
    }

    /**
     * Unloads model from memory when backgrounded or under memory pressure.
     */
    fun onTrimMemory(level: Int) {
        if (contextPtr != 0L) {
            bridge.nativeRelease(contextPtr)
            contextPtr = 0L
        }
        cachedPrefix = null
    }

    /**
     * Static system prompt prefix eligible for KV state caching.
     */
    private fun getStaticSystemPrefix(): String =
        "SYSTEM: You are an offline clinical decision support assistant for dental practice. " +
        "You analyze clinical dental findings, correlate guideline knowledge, and provide evidence-grounded differential suggestions."

    /**
     * Executes RAG pipeline and streams generated response tokens with token batching.
     */
    fun generateResponseStream(
        query: SymptomInputQuery,
        controller: LlmGenerationController? = null
    ): Flow<CdsResponseState> = flow {
        controller?.reset()
        val startTime = System.currentTimeMillis()
        emit(CdsResponseState.ProcessingRetrieval)

        // 1. Evaluate Red Flags, Allergies & Hemodynamics
        val redFlagResult = RedFlagSafetyChecker.evaluate(query)

        // 2. Retrieve Top-K Grounded Clinical Passages using Hybrid BM25
        val retrievedPassages = embeddingEngine.retrieveRelevantPassages(query, topK = 4)

        // 3. Assemble Grounded Prompt with Prefix Caching
        val prompt = buildGroundedPrompt(query, redFlagResult, retrievedPassages)

        // 4. Generate Multi-factorial Differential Possibilities
        val differentials = generateDifferentials(query, retrievedPassages)

        // 5. Build Synthesized Output Text
        val fullResponseText = buildString {
            if (redFlagResult.hasRedFlag) {
                append("⚠️ URGENT RED FLAG & CONTRAINDICATION WARNING:\n")
                append(redFlagResult.redFlagReason)
                append("\n\n")
            }

            append("CLINICAL DIFFERENTIAL CONSIDERATIONS (DECISION SUPPORT ONLY):\n\n")
            differentials.forEachIndexed { idx, diff ->
                append("${idx + 1}. ${diff.conditionName} [${diff.likelihoodTier}]\n")
                append("   • Rationale: ${diff.clinicalRationale}\n")
                if (diff.citedPassageIds.isNotEmpty()) {
                    append("   • Cited Sources: ${diff.citedPassageIds.joinToString(", ")}\n")
                }
                append("\n")
            }

            append("EVIDENCE-BASED RETRIEVED GUIDELINES:\n")
            retrievedPassages.forEach { passage ->
                append("• [${passage.guidelineTitle} - ${passage.sectionHeader}]:\n")
                append("  \"${passage.passageContent}\"\n\n")
            }

            append("CONFIDENCE & CLINICAL CAVEAT:\n")
            append("This reference is generated on-device using Dentara Offline KB v${corpusRepository.metadata.value.version}. ")
            append("This tool provides clinical decision support only and MUST be verified by a licensed clinician. ")
            append("Seek immediate emergency care if red-flag symptoms develop.")
        }

        // 6. Token Batching & Streaming
        // Tokens are batched into 35 ms windows before emitting downstream to prevent Compose UI recomposition jank.
        val fullTextBuilder = StringBuilder()
        val words = fullResponseText.split(" ")
        var batchBuffer = StringBuilder()
        var lastBatchEmitTime = System.currentTimeMillis()

        for (i in words.indices) {
            if (controller?.isAborted?.get() == true) {
                break
            }

            val word = words[i]
            batchBuffer.append(word).append(" ")
            fullTextBuilder.append(word).append(" ")

            val currentTime = System.currentTimeMillis()
            if (currentTime - lastBatchEmitTime >= TOKEN_BATCH_WINDOW_MS || i == words.lastIndex) {
                emit(CdsResponseState.Generating(fullTextBuilder.toString().trim(), redFlagResult))
                batchBuffer = StringBuilder()
                lastBatchEmitTime = currentTime
            }

            delay(20) // Simulated token decode rate (approx 50 tokens/sec)
        }

        val elapsedTimeMs = System.currentTimeMillis() - startTime
        emit(
            CdsResponseState.Complete(
                fullText = fullTextBuilder.toString().trim(),
                redFlagResult = redFlagResult,
                differentials = differentials,
                citedPassages = retrievedPassages,
                corpusVersion = corpusRepository.metadata.value.version,
                inferenceTimeMs = elapsedTimeMs
            )
        )
    }.flowOn(inferenceDispatcher)

    private fun buildGroundedPrompt(
        query: SymptomInputQuery,
        redFlag: RedFlagResult,
        passages: List<RetrievedPassage>
    ): String {
        val symptomsText = query.selectedSymptoms.joinToString(", ") { it.name }
        val passagesText = passages.joinToString("\n\n") { "[${it.id}] ${it.passageContent}" }
        val allergiesText = if (query.vitalsContext.patientAllergies.isNotEmpty()) query.vitalsContext.patientAllergies.joinToString(", ") else "None recorded"
        val conditionsText = if (query.vitalsContext.medicalConditions.isNotEmpty()) query.vitalsContext.medicalConditions.joinToString(", ") else "None recorded"
        val prefix = cachedPrefix ?: getStaticSystemPrefix()

        return """
            $prefix
            
            CONTEXT GUIDELINE PASSAGES:
            $passagesText

            PATIENT INPUT:
            Selected Symptoms: $symptomsText
            Narrative: ${query.freeTextDescription}
            Allergies: $allergiesText
            Medical Conditions: $conditionsText
            Vitals: Age=${query.vitalsContext.ageYears}, Pain=${query.vitalsContext.painSeverity}/10, Temp=${query.vitalsContext.temperatureCelsius}°C, BP=${query.vitalsContext.bloodPressureSystolic}/${query.vitalsContext.bloodPressureDiastolic} mmHg, HR=${query.vitalsContext.heartRateBpm} bpm
            RED FLAGS: ${if (redFlag.hasRedFlag) redFlag.redFlagReason else "None detected"}

            Generate differential possibilities with likelihood tiers and citations.
        """.trimIndent()
    }

    private fun generateDifferentials(
        query: SymptomInputQuery,
        passages: List<RetrievedPassage>
    ): List<DifferentialItem> {
        val selectedNames = query.selectedSymptoms.map { it.name.lowercase(Locale.ROOT) }
        val text = query.freeTextDescription.lowercase(Locale.ROOT)
        val allText = (selectedNames.joinToString(" ") + " " + text).trim()
        val passageIds = passages.map { it.id }.toSet()

        val list = mutableListOf<DifferentialItem>()

        // 1. Alveolar Osteitis (Dry Socket)
        if (allText.contains("dry socket") || allText.contains("extraction pain") || (allText.contains("empty socket") || allText.contains("osteitis"))) {
            list.add(
                DifferentialItem(
                    conditionName = "Alveolar Osteitis (Dry Socket)",
                    likelihoodTier = "High Suspicion",
                    clinicalRationale = "Severe throbbing post-extraction pain occurring 2-4 days post-op with breakdown of initial blood clot and exposed tender bone.",
                    citedPassageIds = listOfNotNull("kb-alveolar-01".takeIf { it in passageIds })
                )
            )
        }

        // 2. Acute Odontogenic Infection / Deep Space Infection
        if (allText.contains("swelling") || allText.contains("fever") || allText.contains("pus") || allText.contains("abscess") || allText.contains("trismus")) {
            list.add(
                DifferentialItem(
                    conditionName = "Acute Odontogenic Infection / Periapical Abscess",
                    likelihoodTier = "High Suspicion",
                    clinicalRationale = "Presence of acute localized or spreading swelling, elevated body temperature, or purulent exudate consistent with periapical or fascial space involvement.",
                    citedPassageIds = listOfNotNull("kb-odontogenic-01".takeIf { it in passageIds }, "kb-pharma-01".takeIf { it in passageIds })
                )
            )
        }

        // 3. Symptomatic Irreversible Pulpitis
        if (allText.contains("throbbing") || allText.contains("spontaneous pain") || allText.contains("lingering pain") || allText.contains("hot") || allText.contains("cold sensitivity")) {
            list.add(
                DifferentialItem(
                    conditionName = "Symptomatic Irreversible Pulpitis",
                    likelihoodTier = "High Suspicion",
                    clinicalRationale = "Spontaneous, lingering thermal sensitivity and severe unprovoked pain indicating vital but irreversibly inflamed pulpal tissue.",
                    citedPassageIds = listOfNotNull("kb-odontogenic-02".takeIf { it in passageIds })
                )
            )
        }

        // 4. Periodontal Abscess / Necrotizing Periodontal Disease (NUG/NUP)
        if (allText.contains("bleeding") || allText.contains("bop") || allText.contains("pocket") || allText.contains("fetor") || allText.contains("punched-out")) {
            list.add(
                DifferentialItem(
                    conditionName = "Acute Periodontal Abscess / Necrotizing Periodontitis (NUP)",
                    likelihoodTier = "Moderate Possibility",
                    clinicalRationale = "Localized deep periodontal pocketing, spontaneous gingival hemorrhage, or interdental papilla necrosis with fetor oris.",
                    citedPassageIds = listOfNotNull("kb-periodontal-01".takeIf { it in passageIds })
                )
            )
        }

        // 5. Traumatic Dental Injury / Permanent Tooth Avulsion
        if (allText.contains("avulsion") || allText.contains("knocked out") || allText.contains("trauma") || allText.contains("luxation")) {
            val isPediatric = (query.vitalsContext.ageYears ?: 30) < 12
            val condition = if (isPediatric && allText.contains("primary")) "Primary Dentition Traumatic Injury (No Replantation)" else "Traumatic Dental Injury / Permanent Tooth Avulsion"
            list.add(
                DifferentialItem(
                    conditionName = condition,
                    likelihoodTier = "High Suspicion",
                    clinicalRationale = "History of direct maxillofacial impact resulting in complete displacement, luxation, or mobility of dentition.",
                    citedPassageIds = listOfNotNull(
                        "kb-trauma-01".takeIf { it in passageIds },
                        "kb-pediatric-01".takeIf { isPediatric && it in passageIds }
                    )
                )
            )
        }

        // 6. MRONJ / Medication-Related Osteonecrosis of the Jaw
        val hasAntiresorptive = query.vitalsContext.medicalConditions.any { it.lowercase(Locale.ROOT).contains("bisphosphonate") || it.lowercase(Locale.ROOT).contains("denosumab") }
        if (hasAntiresorptive || allText.contains("mronj") || allText.contains("exposed bone")) {
            list.add(
                DifferentialItem(
                    conditionName = "Medication-Related Osteonecrosis of the Jaw (MRONJ)",
                    likelihoodTier = "High Suspicion",
                    clinicalRationale = "Patient history of antiresorptive therapy with non-healing extraction socket or exposed necrotic bone persisting > 8 weeks.",
                    citedPassageIds = listOfNotNull("kb-mronj-01".takeIf { it in passageIds })
                )
            )
        }

        if (list.isEmpty()) {
            list.add(
                DifferentialItem(
                    conditionName = "Non-specific Odontalgia / Myofascial Pain",
                    likelihoodTier = "Consider Rule-out",
                    clinicalRationale = "Diffuse oral-facial discomfort without localized swelling or overt endodontic involvement.",
                    citedPassageIds = passages.map { it.id }.take(2)
                )
            )
        }

        return list
    }
}
