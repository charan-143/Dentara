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
            actions.add("Immediate Emergency Department referral for surgical airway assessment & IV antibiotic therapy")
            actions.add("Maintain patient upright; DO NOT place in supine position if breathing or swallowing is impaired")
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
            actions.add("Apply direct firm pressure with gauze soaked in 5% Tranexamic Acid (TXA)")
            actions.add("Check INR / anticoagulant status and initiate emergency EMS transfer if hemostasis fails")
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
            actions.add("Urgent hospital admission for blood cultures and high-dose parenteral broad-spectrum antimicrobials")
            actions.add("Continuous monitoring of temperature, MAP, and neurological status")
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
            actions.add("Administer IM Epinephrine (1:1,000, 0.3-0.5 mg in anterolateral thigh) immediately")
            actions.add("Activate Emergency Medical Services (EMS / 911)")
            actions.add("Administer high-flow supplemental oxygen (10-15 L/min)")
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
            actions.add("Immediate 911 / EMS activation and continuous vital sign / ECG monitoring")
            actions.add("Administer chewable aspirin (300 mg) unless contraindicated by active hemorrhage")
        }

        // -------------------------------------------------------------
        // 6. Medication & Allergy Cross-Reference Screening
        // -------------------------------------------------------------
        val patientAllergies = query.vitalsContext.patientAllergies.map { it.lowercase(Locale.ROOT) }
        if (patientAllergies.any { it.contains("penicillin") || it.contains("amoxicillin") }) {
            if (combinedAssertedText.contains("infection") || combinedAssertedText.contains("abscess") || combinedAssertedText.contains("swelling")) {
                reasons.add("PATIENT PENICILLIN ALLERGY: Beta-lactams / Amoxicillin strictly contraindicated")
                actions.add("Prescribe Clindamycin (300 mg TID) or Azithromycin (500 mg day 1, then 250 mg daily) for odontogenic infections")
            }
        }

        val patientConditions = query.vitalsContext.medicalConditions.map { it.lowercase(Locale.ROOT) }
        if (patientConditions.any { it.contains("bisphosphonate") || it.contains("mronj") || it.contains("denosumab") }) {
            if (combinedAssertedText.contains("extraction") || combinedAssertedText.contains("exposed bone") || combinedAssertedText.contains("pain")) {
                reasons.add("MRONJ Risk: Patient on antiresorptive / bisphosphonate therapy")
                actions.add("Avoid elective surgical dentoalveolar extractions without OMFS specialist consultation")
                actions.add("Prioritize conservative endodontic therapy and 0.12% Chlorhexidine irrigation")
            }
        }

        if (reasons.isNotEmpty()) {
            return RedFlagResult(
                hasRedFlag = true,
                redFlagReason = reasons.joinToString(" | "),
                emergencyGuidance = "CRITICAL CLINICAL RED FLAG DETECTED — REQUIRE IMMEDIATE EMERGENCY EVALUATION",
                recommendedActions = actions.distinct()
            )
        }

        return RedFlagResult(
            hasRedFlag = false,
            redFlagReason = null,
            emergencyGuidance = null,
            recommendedActions = emptyList()
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
