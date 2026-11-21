package com.example.thornburydental.data.cds

import java.util.Locale

/**
 * Advanced, non-LLM clinical safety and emergency escalation evaluation engine.
 *
 * Implements:
 * 1. NegEx clinical negation scope analysis (prevents false alarms on "patient denies SOB / no fever").
 * 2. Clinical shorthand and acronym expansion ("SOB", "FOM", "Ludwig's").
 * 3. Hemodynamic Shock Index (SI = HR / SBP >= 0.9) calculation.
 * 4. Patient allergy and pharmacological contraindication screening.
 */
object RedFlagSafetyChecker {

    /**
     * Evaluates query for immediate red flags, contraindications, and emergency escalation.
     */
    fun evaluate(query: SymptomInputQuery): RedFlagResult {
        val selectedNames = query.selectedSymptoms.map { it.name.lowercase(Locale.ROOT) }
        val rawText = query.freeTextDescription.lowercase(Locale.ROOT)
        val expandedText = expandClinicalAcronyms(rawText)

        // Extract affirmatively asserted tokens/phrases using NegEx negation filtering
        val assertedConcepts = extractAssertedConcepts(expandedText)
        val combinedAssertedText = (selectedNames + assertedConcepts).joinToString(" ")

        val actions = mutableListOf<String>()
        val reasons = mutableListOf<String>()

        // -------------------------------------------------------------
        // 1. Ludwig's Angina & Airway Compromise Risk
        // -------------------------------------------------------------
        val airwayRiskKeywords = listOf(
            "airway compromise", "breathing difficulty", "shortness of breath",
            "stridor", "dysphagia", "inability to swallow", "floor of mouth swelling",
            "tongue elevation", "submandibular space infection", "ludwig's angina",
            "drooling with fever", "trismus with dysphagia"
        )
        val hasAirwayKeywords = airwayRiskKeywords.any { combinedAssertedText.contains(it) }
        val hasLudwigsCombo = (combinedAssertedText.contains("submandibular") || combinedAssertedText.contains("swelling")) &&
                (combinedAssertedText.contains("fever") || combinedAssertedText.contains("trismus") || combinedAssertedText.contains("swallow"))

        if (hasAirwayKeywords || hasLudwigsCombo) {
            reasons.add("Potential Airway Compromise / Deep Space Infection (Ludwig's Angina Risk)")
            actions.add("Emergency escalation: activate local emergency medical services and arrange immediate emergency department assessment for suspected airway compromise")
            actions.add("Keep the patient under direct observation and follow local emergency airway protocols; do not delay transfer for this advisory")
        }

        // -------------------------------------------------------------
        // 2. Uncontrolled Bleeding & Hemodynamic Shock Index
        // -------------------------------------------------------------
        val bleedingKeywords = listOf(
            "uncontrolled bleeding", "arterial spurting", "profuse hemorrhage",
            "persistent post-extraction bleeding", "socket bleeding refractory to pressure"
        )
        val hasBleeding = bleedingKeywords.any { combinedAssertedText.contains(it) }

        val hr = query.vitalsContext.heartRateBpm
        val sbp = query.vitalsContext.bloodPressureSystolic
        val shockIndex = if (hr != null && sbp != null && sbp > 0) hr.toFloat() / sbp.toFloat() else 0f
        val isHemodynamicallyUnstable = shockIndex >= 0.9f || (sbp != null && sbp < 90)

        if (hasBleeding || (isHemodynamicallyUnstable && (combinedAssertedText.contains("bleeding") || combinedAssertedText.contains("trauma")))) {
            val shockNote = if (isHemodynamicallyUnstable) " (Shock Index: ${"%.2f".format(shockIndex)} - Elevated Hemodynamic Risk)" else ""
            reasons.add("Active Severe Hemorrhage with Risk of Hemodynamic Shock$shockNote")
            actions.add("Emergency escalation: apply appropriate direct pressure per local protocol and activate emergency services for uncontrolled bleeding or instability")
            actions.add("Urgently assess anticoagulant use, bleeding history, and hemodynamic status; medication or reversal decisions require a clinician")
        }

        // -------------------------------------------------------------
        // 3. Cavernous Sinus Thrombosis / Systemic Sepsis / High Fever
        // -------------------------------------------------------------
        val temp = query.vitalsContext.temperatureCelsius ?: 37.0f
        val hasHighFever = temp >= 39.0f
        val septicKeywords = listOf(
            "periorbital edema", "proptosis", "cavernous sinus", "altered mental status",
            "lethargy", "rigors", "confused", "cranial nerve deficit", "diplopia"
        )
        val hasSepticKeywords = septicKeywords.any { combinedAssertedText.contains(it) }

        if (hasHighFever || hasSepticKeywords) {
            reasons.add("Severe Systemic Toxicity / Septicemia or Cavernous Sinus Thrombosis Risk")
            actions.add("Emergency escalation: arrange immediate hospital assessment for possible severe infection or neurological emergency")
            actions.add("Antimicrobial and monitoring decisions require the receiving clinical team and applicable local protocol")
        }

        // -------------------------------------------------------------
        // 4. Anaphylaxis / Severe Allergic Reaction
        // -------------------------------------------------------------
        val anaphylaxisKeywords = listOf(
            "anaphylaxis", "lip angioedema", "urticaria and wheezing", "throat tightness",
            "respiratory distress post-injection", "diffuse hives"
        )
        val hasAnaphylaxis = anaphylaxisKeywords.any { combinedAssertedText.contains(it) }
        if (hasAnaphylaxis) {
            reasons.add("Suspected Acute Anaphylactic Reaction")
            actions.add("Emergency escalation: activate local emergency medical services immediately and follow the site's anaphylaxis protocol")
            actions.add("A trained clinician should assess and treat suspected anaphylaxis without delay; follow local protocol for medication and supportive care")
        }

        // -------------------------------------------------------------
        // 5. Acute Cardiovascular or Stroke Signs
        // -------------------------------------------------------------
        val cardioStrokeKeywords = listOf(
            "chest pain", "radiating left arm pain", "radiating left jaw pain with diaphoresis",
            "facial droop", "slurred speech", "acute arm weakness"
        )
        val hasCardioStroke = cardioStrokeKeywords.any { combinedAssertedText.contains(it) }
        if (hasCardioStroke) {
            reasons.add("Acute Cardiovascular or Neurological Emergency (MI / CVA Risk)")
            actions.add("Emergency escalation: activate local emergency medical services immediately for possible cardiac or stroke emergency")
            actions.add("Do not initiate medication from this advisory; treatment requires clinician assessment and local emergency protocol")
        }

        // -------------------------------------------------------------
        // 6. Medication & Allergy Cross-Reference Screening
        // -------------------------------------------------------------
        val patientAllergies = query.vitalsContext.patientAllergies.map { it.lowercase(Locale.ROOT) }
        if (patientAllergies.any { it.contains("penicillin") || it.contains("amoxicillin") }) {
            if (combinedAssertedText.contains("infection") || combinedAssertedText.contains("abscess") || combinedAssertedText.contains("swelling")) {
                reasons.add("Medication safety alert: reported penicillin-family allergy requires verification before considering a beta-lactam")
                actions.add("Verify the allergy agent, reaction, severity, and timing with the patient and record; do not infer an alternative antibiotic from allergy history alone")
                actions.add("Any prescription requires clinician confirmation against a current, locally approved medication knowledge base, including renal function, pregnancy status, interactions, and medical history")
            }
        }

        val patientConditions = query.vitalsContext.medicalConditions.map { it.lowercase(Locale.ROOT) }
        val medicationContextIncomplete = query.vitalsContext.renalFunctionSummary.isNullOrBlank() ||
                query.vitalsContext.pregnancyStatus.isNullOrBlank()
        if ((patientAllergies.isNotEmpty() || query.vitalsContext.currentMedications.isNotEmpty()) && medicationContextIncomplete) {
            reasons.add("Medication review context incomplete: renal function and/or pregnancy status not documented")
            actions.add("Resolve missing medication safety context and check current medications, interactions, and medical history before prescribing")
        }
        if (patientConditions.any { it.contains("bisphosphonate") || it.contains("mronj") || it.contains("denosumab") }) {
            if (combinedAssertedText.contains("extraction") || combinedAssertedText.contains("exposed bone") || combinedAssertedText.contains("pain")) {
                reasons.add("MRONJ Risk: Patient on antiresorptive / bisphosphonate therapy")
                actions.add("Assess medication-related jaw osteonecrosis risk and consult an appropriate specialist before elective dentoalveolar surgery")
                actions.add("Select management with the treating clinician using current specialist guidance and patient-specific factors")
            }
        }

        val citations = mutableListOf<String>()
        var requiresOverride = false

        if (reasons.any { it.contains("PENICILLIN") }) {
            citations.add("ADA Clinical Practice Guideline on Antibiotic Use for the Urgent Management of Pulpal- and Periapical-Related Dental Pain and Intraoral Swelling (2019)")
            requiresOverride = true
        }
        if (reasons.any { it.contains("MRONJ") }) {
            citations.add("American Association of Oral and Maxillofacial Surgeons (AAOMS) 2022 Position Paper on Medication-Related Osteonecrosis of the Jaws")
            requiresOverride = true
        }
        if (reasons.any { it.contains("Hemorrhage") || it.contains("Shock") }) {
            citations.add("Advanced Trauma Life Support (ATLS) / ADA Guidelines on Anticoagulated Patient Management")
            requiresOverride = true
        }
        if (reasons.any { it.contains("Ludwig") || it.contains("Airway") }) {
            citations.add("Consensus Guidelines on Deep Neck Space Infection & Airway Management in Maxillofacial Emergencies")
            requiresOverride = true
        }

        if (reasons.isNotEmpty()) {
            val allergyAlert = reasons.any { it.startsWith("Medication safety alert:") }
            val alerts = buildList {
                if (allergyAlert) add(ClinicalAlert(
                    category = "MEDICATION_ALLERGY",
                    severity = "WARNING_REQUIRES_VERIFICATION",
                    message = "Reported allergy is a safety signal; allergy history alone does not establish an appropriate alternative regimen.",
                    evidenceSource = "Local, versioned medication knowledge base required",
                    guidelineVersion = "Not configured",
                    reviewDate = null
                ))
            }
            return RedFlagResult(
                hasRedFlag = true,
                redFlagReason = reasons.joinToString(" | "),
                emergencyGuidance = if (actions.any { it.startsWith("Emergency escalation:") })
                    "EMERGENCY ESCALATION: follow local emergency pathways and obtain immediate clinician assessment"
                else "CLINICAL ADVISORY: clinician review required before medication decisions",
                recommendedActions = actions.distinct(),
                isAdvisory = true,
                guidelineCitations = citations.distinct(),
                requiresClinicianOverride = requiresOverride,
                overrideProtocol = if (requiresOverride) "MANDATORY_CLINICIAN_SIGNED_OVERRIDE_WITH_IDENTITY_AND_JUSTIFICATION" else null,
                alerts = alerts,
                requiresClinicianConfirmation = allergyAlert || reasons.any { it.startsWith("Medication review context incomplete:") },
                evidenceReviewedDate = null
            )
        }

        return RedFlagResult(
            hasRedFlag = false,
            redFlagReason = null,
            emergencyGuidance = null,
            recommendedActions = emptyList(),
            isAdvisory = false,
            guidelineCitations = emptyList(),
            requiresClinicianOverride = false,
            overrideProtocol = null,
            requiresClinicianConfirmation = false
        )
    }

    /**
     * Expands medical shorthand and acronyms to full descriptive forms.
     */
    fun expandClinicalAcronyms(text: String): String {
        return text
            .replace(Regex("(?i)\\bsob\\b"), "shortness of breath")
            .replace(Regex("(?i)\\bfom\\b"), "floor of mouth swelling")
            .replace(Regex("(?i)\\bludwig\\b"), "ludwig's angina")
            .replace(Regex("(?i)\\bludwigs\\b"), "ludwig's angina")
            .replace(Regex("(?i)\\bpe\\b"), "pulmonary embolism")
            .replace(Regex("(?i)\\bmi\\b"), "myocardial infarction")
            .replace(Regex("(?i)\\bcva\\b"), "cerebrovascular accident")
            .replace(Regex("(?i)\\blast\\b"), "local anesthetic systemic toxicity")
            .replace(Regex("(?i)\\bmronj\\b"), "medication related osteonecrosis of the jaw")
            .replace(Regex("(?i)\\bbronj\\b"), "bisphosphonate related osteonecrosis of the jaw")
    }

    /**
     * NegEx-style clinical negation extractor.
     * Segments sentences by clauses and strips out negated findings.
     */
    fun extractAssertedConcepts(text: String): List<String> {
        if (text.isBlank()) return emptyList()

        // Split on major clause boundaries
        val clauses = text.split(Regex("[,;\\.]+|\\b(?:but|however|although|except|yet)\\b"))
        val assertedTokens = mutableListOf<String>()

        val preNegationTriggers = listOf(
            "no", "denies", "denied", "without", "free of", "ruled out", "negative for",
            "not experiencing", "no signs of", "rules out", "does not have", "denying"
        )
        val postNegationTriggers = listOf(
            "unlikely", "negative", "ruled out", "absent", "resolved"
        )

        for (clause in clauses) {
            val cleanClause = clause.trim()
            if (cleanClause.isEmpty()) continue

            val words = cleanClause.split(Regex("\\s+"))
            var isNegated = false

            // Check for pre-negation trigger at start of clause
            for (i in words.indices) {
                val window = words.subList(0, (i + 1).coerceAtMost(words.size)).joinToString(" ")
                if (preNegationTriggers.any { window.startsWith(it) || window.contains(" $it") }) {
                    // Check pseudo-negation exceptions ("no change", "without difficulty", "no doubt")
                    if (!window.contains("no change") && !window.contains("no doubt") && !window.contains("without difficulty")) {
                        isNegated = true
                        break
                    }
                }
            }

            // Check for post-negation triggers at end of clause
            if (!isNegated && words.isNotEmpty()) {
                val lastWords = words.takeLast(2).joinToString(" ")
                if (postNegationTriggers.any { lastWords.contains(it) }) {
                    isNegated = true
                }
            }

            if (!isNegated) {
                assertedTokens.add(cleanClause)
            }
        }

        return assertedTokens
    }
}
