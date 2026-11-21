package com.example.thornburydental.speech

import android.content.Context
import android.util.Log
import com.example.thornburydental.data.ToothCondition
import com.example.thornburydental.data.cds.GeminiMultimodalClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.Locale

@Serializable
data class AllergyFinding(
    val allergen: String,
    val severity: String = "Moderate", // "Severe / Anaphylaxis", "Moderate", "Mild"
    val reaction: String = ""
)

@Serializable
data class DemographicsFinding(
    val name: String = "",
    val dob: String = "",
    val ageYears: Int? = null,
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val isChild: Boolean? = null,
    val medicalHistory: String = "",
    val familyHistory: String = "",
    val pastDentalHistory: String = "",
    val allergies: List<AllergyFinding> = emptyList(),
    val medicalAlerts: List<String> = emptyList()
) {
    val hasAnyField: Boolean
        get() = name.isNotBlank() ||
                dob.isNotBlank() ||
                ageYears != null ||
                phone.isNotBlank() ||
                email.isNotBlank() ||
                address.isNotBlank() ||
                isChild != null ||
                medicalHistory.isNotBlank() ||
                familyHistory.isNotBlank() ||
                pastDentalHistory.isNotBlank() ||
                allergies.isNotEmpty() ||
                medicalAlerts.isNotEmpty()
}

@Serializable
data class ToothConditionFinding(
    val toothNumber: Int,
    val condition: ToothCondition,
    val surface: String = "", // e.g. "O", "MO", "MOD", "B", "L"
    val notes: String = ""
)

@Serializable
data class PerioMeasurementFinding(
    val toothNumber: Int,
    val site: String = "", // "MB", "B", "DB", "ML", "L", "DL" or "Mesial", "Distal", "Buccal", "Lingual"
    val depthMm: Int,
    val isBleeding: Boolean = false
)

@Serializable
data class ClinicalExamFindings(
    val chiefComplaints: List<String> = emptyList(),
    val chiefComplaintOther: String = "",
    val painSeverity: String = "",
    val sensitivityTriggers: List<String> = emptyList(),
    val gingivalRecession: List<String> = emptyList(),
    val softTissue: List<String> = emptyList(),
    val calculus: List<String> = emptyList(),
    val stains: List<String> = emptyList(),
    val tmjAssessment: List<String> = emptyList(),
    val functionalHabits: List<String> = emptyList(),
    val brushingFrequency: String = "",
    val flossingFrequency: String = "",
    val cariesRisk: String = "",
    val oralHygiene: String = "",
    val diagnosis: String = "",
    val prognosis: String = "Good",
    val extraNotes: List<String> = emptyList()
)

@Serializable
data class ClinicalNoteFinding(
    val category: String = "Examination", // "Chief Complaint", "Diagnosis", "Examination", "Oral Hygiene"
    val text: String
)

@Serializable
data class TreatmentPlanFinding(
    val toothNumber: Int? = null,
    val procedure: String,
    val code: String = "",
    val estimatedCost: Double = 0.0,
    val priority: String = "Normal"
)

@Serializable
data class ClinicalFindings(
    val rawTranscript: String,
    val toothConditions: List<ToothConditionFinding> = emptyList(),
    val perioMeasurements: List<PerioMeasurementFinding> = emptyList(),
    val clinicalNotes: List<ClinicalNoteFinding> = emptyList(),
    val treatmentPlanItems: List<TreatmentPlanFinding> = emptyList(),
    val examFindings: ClinicalExamFindings = ClinicalExamFindings(),
    val demographics: DemographicsFinding? = null,
    val confidence: Float = 1.0f,
    val isAiExtracted: Boolean = false,
    val warnings: List<String> = emptyList()
) {
    val totalCount: Int
        get() = toothConditions.size +
                perioMeasurements.size +
                treatmentPlanItems.size +
                examFindings.chiefComplaints.size +
                examFindings.sensitivityTriggers.size +
                examFindings.gingivalRecession.size +
                examFindings.softTissue.size +
                examFindings.calculus.size +
                examFindings.stains.size +
                examFindings.tmjAssessment.size +
                examFindings.functionalHabits.size +
                examFindings.extraNotes.size +
                (if (examFindings.chiefComplaintOther.isNotBlank()) 1 else 0) +
                (if (examFindings.diagnosis.isNotBlank()) 1 else 0) +
                (if (examFindings.painSeverity.isNotBlank()) 1 else 0) +
                (if (examFindings.cariesRisk.isNotBlank()) 1 else 0) +
                (if (examFindings.brushingFrequency.isNotBlank()) 1 else 0) +
                (if (examFindings.flossingFrequency.isNotBlank()) 1 else 0) +
                (if (examFindings.oralHygiene.isNotBlank()) 1 else 0) +
                (if (demographics?.name?.isNotBlank() == true) 1 else 0) +
                (if (demographics?.dob?.isNotBlank() == true || demographics?.ageYears != null) 1 else 0) +
                (if (demographics?.phone?.isNotBlank() == true) 1 else 0) +
                (if (demographics?.email?.isNotBlank() == true) 1 else 0) +
                (if (demographics?.address?.isNotBlank() == true) 1 else 0) +
                (if (demographics?.medicalHistory?.isNotBlank() == true) 1 else 0) +
                (if (demographics?.familyHistory?.isNotBlank() == true) 1 else 0) +
                (if (demographics?.pastDentalHistory?.isNotBlank() == true) 1 else 0) +
                (demographics?.allergies?.size ?: 0) +
                (demographics?.medicalAlerts?.size ?: 0)

    val isEmpty: Boolean
        get() = totalCount == 0 && clinicalNotes.isEmpty() && (demographics == null || !demographics.hasAnyField)

    val requiresReview: Boolean
        get() = warnings.isNotEmpty() || confidence < 0.75f || perioMeasurements.any { it.depthMm > 12 }
}

/**
 * Intelligent Clinical Dental Entity Extractor.
 *
 * Transcribed clinical speech is analyzed and parsed into discrete clinical categories:
 * 1. Odontogram Tooth States & Surfaces
 * 2. Periodontal Probing Depths & Bleeding on Probing (BOP)
 * 3. Treatment Plans & Procedures
 * 4. Structured Clinical Examination Fields (Chief Complaints, Pain, Sensitivity, Soft Tissue, Calculus, Caries Risk, Diagnosis)
 * 5. Extra unclassified narrative notes into Clinical Notes.
 */
object ClinicalVoiceExtractor {

    private const val TAG = "ClinicalVoiceExtractor"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val SYSTEM_PROMPT = """
        You are an expert Dental Clinical Scribe AI. Your task is to analyze transcribed clinical speech spoken by a dentist or dental hygienist and extract structured clinical findings into discrete clinical fields in JSON format.
        
        Output JSON strictly matching this schema:
        {
          "demographics": {
            "name": "Sarah Connor",
            "dob": "15/04/1985",
            "ageYears": 39,
            "phone": "+1 555-0199",
            "email": "sarah.connor@example.com",
            "address": "45 Elm Street, Springfield",
            "isChild": false,
            "medicalHistory": "Hypertension, asthma",
            "familyHistory": "Maternal diabetes",
            "pastDentalHistory": "Orthodontic braces in teens",
            "allergies": [
              {
                "allergen": "Penicillin",
                "severity": "Severe / Anaphylaxis",
                "reaction": "Airway swelling"
              }
            ],
            "medicalAlerts": ["Pre-medication required", "Blood thinners"]
          },
          "toothConditions": [
            {
              "toothNumber": 14,
              "condition": "DECAY",
              "surface": "Occlusal",
              "notes": "Active deep caries"
            }
          ],
          "perioMeasurements": [
            {
              "toothNumber": 3,
              "site": "MB",
              "depthMm": 4,
              "isBleeding": true
            }
          ],
          "treatmentPlanItems": [
            {
              "toothNumber": 14,
              "procedure": "Composite Resin Restoration - 1 Surface (Occlusal)",
              "code": "D2391",
              "estimatedCost": 175.0,
              "priority": "High"
            }
          ],
          "examFindings": {
            "chiefComplaints": ["Sensitivity to cold in upper right", "Throbbing pain on lower left"],
            "chiefComplaintOther": "",
            "painSeverity": "Moderate (6/10)",
            "sensitivityTriggers": ["Cold", "Sweet"],
            "gingivalRecession": ["Tooth 14: 2mm buccal recession"],
            "softTissue": ["Mild aphthous ulcer on right buccal mucosa"],
            "calculus": ["Moderate supragingival calculus in lower anterior"],
            "stains": ["Extrinsic tea/coffee stains"],
            "tmjAssessment": ["Normal range of motion, slight clicking on right"],
            "functionalHabits": ["Bruxism / Night grinding", "Mouth breathing"],
            "brushingFrequency": "Twice daily",
            "flossingFrequency": "Daily",
            "cariesRisk": "High",
            "oralHygiene": "Good",
            "diagnosis": "Localized Periodontitis Stage II, Moderate Caries",
            "prognosis": "Good",
            "extraNotes": ["Patient prefers morning appointments", "Pre-medication not required"]
          }
        }
        
        Permitted ToothCondition enum values:
        "SOUND", "DECAY", "FILLED", "CROWN", "MISSING", "IMPLANT", "ROOT_CANAL", "EXFOLIATED", "UNERUPTED"
        
        Permitted Periodontal Sites:
        "MB" (Mesiobuccal), "B" (Buccal/Facial), "DB" (Distobuccal), "ML" (Mesiolingual), "L" (Lingual/Palatal), "DL" (Distolingual), "Mesial", "Distal", "Buccal", "Lingual", "Palatal"
        
        Categorization Rules:
        1. Patient demographics, age, DOB, contact details, allergies, alerts, and medical history belong in "demographics".
        2. Tooth conditions (caries, missing, crown, rct, implant, filled) belong in "toothConditions".
        3. Probing depths and bleeding belong in "perioMeasurements".
        4. Recommended treatments and procedures belong in "treatmentPlanItems".
        5. Complaints, pain scale, triggers, soft tissue, calculus, stains, tmj, habits, brushing, flossing, risk, and diagnosis belong in their specific "examFindings" fields.
        6. Any general narrative or extra remarks that do not fit the above fields belong in "examFindings.extraNotes".
        7. Return ONLY valid JSON with no markdown formatting or code fences.
    """.trimIndent()

    val gemma4Engine by lazy { Gemma4AudioEngine().apply { initialize() } }

    /**
     * Extracts structured clinical findings directly from raw 16kHz PCM audio
     * using Google Gemma 4 E2B Multimodal Native Audio Architecture.
     */
    suspend fun extractFromAudio(
        pcmAudio: ShortArray,
        transcriptHint: String? = null,
        context: Context? = null
    ): ClinicalFindings {
        return gemma4Engine.processAudioUtterance(
            pcmData = pcmAudio,
            context = context,
            transcriptHint = transcriptHint
        )
    }

    /**
     * Extracts structured clinical findings from speech transcript.
     */
    suspend fun extract(
        transcript: String,
        context: Context? = null
    ): ClinicalFindings = withContext(Dispatchers.Default) {
        if (transcript.isBlank()) {
            return@withContext ClinicalFindings(rawTranscript = transcript, confidence = 0f)
        }

        // 1. Try Gemini AI extraction if API key is available
        if (context != null) {
            val apiKey = GeminiMultimodalClient.getApiKey(context)
            if (!apiKey.isNullOrBlank()) {
                try {
                    val promptText = "Extract structured dental clinical findings into separate dedicated fields from this transcription:\n\"$transcript\""
                    val result = GeminiMultimodalClient.generateMultimodalClinicalResponse(
                        context = context,
                        systemInstruction = SYSTEM_PROMPT,
                        promptText = promptText,
                        mediaItems = emptyList()
                    )

                    result.getOrNull()?.let { response ->
                        val parsed = parseGeminiResponse(transcript, response.responseText)
                        if (parsed != null && !parsed.isEmpty) {
                            return@withContext parsed.copy(isAiExtracted = true)
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Gemini clinical extraction failed, using deterministic fallback", e)
                }
            }
        }

        // 2. Deterministic local fallback extraction
        return@withContext extractDeterministic(transcript)
    }

    /**
     * Parses Gemini JSON response into ClinicalFindings.
     */
    private fun parseGeminiResponse(rawTranscript: String, jsonText: String): ClinicalFindings? {
        return try {
            val cleanJson = jsonText
                .replace("```json", "")
                .replace("```", "")
                .trim()

            val root = json.parseToJsonElement(cleanJson).jsonObject

            val demoObj = root["demographics"]?.jsonObject
            val demographics = if (demoObj != null) {
                val name = demoObj["name"]?.jsonPrimitive?.content ?: ""
                val dob = demoObj["dob"]?.jsonPrimitive?.content ?: ""
                val ageYears = demoObj["ageYears"]?.jsonPrimitive?.intOrNull
                val phone = demoObj["phone"]?.jsonPrimitive?.content ?: ""
                val email = demoObj["email"]?.jsonPrimitive?.content ?: ""
                val address = demoObj["address"]?.jsonPrimitive?.content ?: ""
                val isChild = demoObj["isChild"]?.jsonPrimitive?.booleanOrNull
                val medicalHistory = demoObj["medicalHistory"]?.jsonPrimitive?.content ?: ""
                val familyHistory = demoObj["familyHistory"]?.jsonPrimitive?.content ?: ""
                val pastDentalHistory = demoObj["pastDentalHistory"]?.jsonPrimitive?.content ?: ""
                val allergies = mutableListOf<AllergyFinding>()
                demoObj["allergies"]?.jsonArray?.forEach { allergyEl ->
                    if (allergyEl is JsonObject) {
                        val allergen = allergyEl["allergen"]?.jsonPrimitive?.content ?: return@forEach
                        val severity = allergyEl["severity"]?.jsonPrimitive?.content ?: "Moderate"
                        val reaction = allergyEl["reaction"]?.jsonPrimitive?.content ?: ""
                        allergies.add(AllergyFinding(allergen, severity, reaction))
                    }
                }
                val medicalAlerts = demoObj["medicalAlerts"]?.jsonArray?.mapNotNull { it.jsonPrimitive.content } ?: emptyList()
                DemographicsFinding(
                    name = name,
                    dob = dob,
                    ageYears = ageYears,
                    phone = phone,
                    email = email,
                    address = address,
                    isChild = isChild,
                    medicalHistory = medicalHistory,
                    familyHistory = familyHistory,
                    pastDentalHistory = pastDentalHistory,
                    allergies = allergies,
                    medicalAlerts = medicalAlerts
                )
            } else null

            val toothConditions = mutableListOf<ToothConditionFinding>()
            root["toothConditions"]?.jsonArray?.forEach { el ->
                if (el is JsonObject) {
                    val toothNum = el["toothNumber"]?.jsonPrimitive?.intOrNull ?: return@forEach
                    val condStr = el["condition"]?.jsonPrimitive?.content?.uppercase(Locale.US) ?: "SOUND"
                    val condition = try {
                        ToothCondition.valueOf(condStr)
                    } catch (_: Exception) {
                        when {
                            condStr.contains("CARIES") || condStr.contains("DECAY") -> ToothCondition.DECAY
                            condStr.contains("FILL") || condStr.contains("RESTORE") -> ToothCondition.FILLED
                            condStr.contains("CROWN") || condStr.contains("CAP") -> ToothCondition.CROWN
                            condStr.contains("MISS") || condStr.contains("EXTRACT") -> ToothCondition.MISSING
                            condStr.contains("ROOT") || condStr.contains("RCT") -> ToothCondition.ROOT_CANAL
                            condStr.contains("IMPLANT") -> ToothCondition.IMPLANT
                            else -> ToothCondition.SOUND
                        }
                    }
                    val surface = el["surface"]?.jsonPrimitive?.content ?: ""
                    val notes = el["notes"]?.jsonPrimitive?.content ?: ""
                    toothConditions.add(ToothConditionFinding(toothNum, condition, surface, notes))
                }
            }

            val perioMeasurements = mutableListOf<PerioMeasurementFinding>()
            root["perioMeasurements"]?.jsonArray?.forEach { el ->
                if (el is JsonObject) {
                    val toothNum = el["toothNumber"]?.jsonPrimitive?.intOrNull ?: return@forEach
                    val depthMm = el["depthMm"]?.jsonPrimitive?.intOrNull ?: return@forEach
                    val site = el["site"]?.jsonPrimitive?.content ?: ""
                    val isBleeding = el["isBleeding"]?.jsonPrimitive?.booleanOrNull ?: false
                    perioMeasurements.add(PerioMeasurementFinding(toothNum, site, depthMm, isBleeding))
                }
            }

            val treatmentPlans = mutableListOf<TreatmentPlanFinding>()
            root["treatmentPlanItems"]?.jsonArray?.forEach { el ->
                if (el is JsonObject) {
                    val toothNum = el["toothNumber"]?.jsonPrimitive?.intOrNull
                    val procedure = el["procedure"]?.jsonPrimitive?.content ?: return@forEach
                    val code = el["code"]?.jsonPrimitive?.content ?: ""
                    val cost = el["estimatedCost"]?.jsonPrimitive?.doubleOrNull ?: 0.0
                    val priority = el["priority"]?.jsonPrimitive?.content ?: "Normal"
                    treatmentPlans.add(TreatmentPlanFinding(toothNum, procedure, code, cost, priority))
                }
            }

            val examObj = root["examFindings"]?.jsonObject
            val chiefComplaints = examObj?.get("chiefComplaints")?.jsonArray?.mapNotNull { it.jsonPrimitive.content } ?: emptyList()
            val chiefComplaintOther = examObj?.get("chiefComplaintOther")?.jsonPrimitive?.content ?: ""
            val painSeverity = examObj?.get("painSeverity")?.jsonPrimitive?.content ?: ""
            val sensitivityTriggers = examObj?.get("sensitivityTriggers")?.jsonArray?.mapNotNull { it.jsonPrimitive.content } ?: emptyList()
            val gingivalRecession = examObj?.get("gingivalRecession")?.jsonArray?.mapNotNull { it.jsonPrimitive.content } ?: emptyList()
            val softTissue = examObj?.get("softTissue")?.jsonArray?.mapNotNull { it.jsonPrimitive.content } ?: emptyList()
            val calculus = examObj?.get("calculus")?.jsonArray?.mapNotNull { it.jsonPrimitive.content } ?: emptyList()
            val stains = examObj?.get("stains")?.jsonArray?.mapNotNull { it.jsonPrimitive.content } ?: emptyList()
            val tmjAssessment = examObj?.get("tmjAssessment")?.jsonArray?.mapNotNull { it.jsonPrimitive.content } ?: emptyList()
            val functionalHabits = examObj?.get("functionalHabits")?.jsonArray?.mapNotNull { it.jsonPrimitive.content } ?: emptyList()
            val brushingFrequency = examObj?.get("brushingFrequency")?.jsonPrimitive?.content ?: ""
            val flossingFrequency = examObj?.get("flossingFrequency")?.jsonPrimitive?.content ?: ""
            val cariesRisk = examObj?.get("cariesRisk")?.jsonPrimitive?.content ?: ""
            val oralHygiene = examObj?.get("oralHygiene")?.jsonPrimitive?.content ?: ""
            val diagnosis = examObj?.get("diagnosis")?.jsonPrimitive?.content ?: ""
            val prognosis = examObj?.get("prognosis")?.jsonPrimitive?.content ?: "Good"
            val extraNotes = examObj?.get("extraNotes")?.jsonArray?.mapNotNull { it.jsonPrimitive.content } ?: emptyList()

            val examFindings = ClinicalExamFindings(
                chiefComplaints = chiefComplaints,
                chiefComplaintOther = chiefComplaintOther,
                painSeverity = painSeverity,
                sensitivityTriggers = sensitivityTriggers,
                gingivalRecession = gingivalRecession,
                softTissue = softTissue,
                calculus = calculus,
                stains = stains,
                tmjAssessment = tmjAssessment,
                functionalHabits = functionalHabits,
                brushingFrequency = brushingFrequency,
                flossingFrequency = flossingFrequency,
                cariesRisk = cariesRisk,
                oralHygiene = oralHygiene,
                diagnosis = diagnosis,
                prognosis = prognosis,
                extraNotes = extraNotes
            )

            val warnings = mutableListOf<String>()
            if (perioMeasurements.any { it.depthMm > 12 }) {
                warnings.add("One or more probing depths exceed 12mm. Mandatory clinical review required.")
            }

            ClinicalFindings(
                rawTranscript = rawTranscript,
                toothConditions = toothConditions,
                perioMeasurements = perioMeasurements,
                treatmentPlanItems = treatmentPlans,
                examFindings = examFindings,
                demographics = demographics,
                confidence = 0.95f,
                isAiExtracted = true,
                warnings = warnings
            )
        } catch (e: Exception) {
            Log.w(TAG, "Error parsing Gemini JSON output: ${e.message}")
            null
        }
    }

    /**
     * High-precision offline deterministic extractor that maps findings into separate fields.
     */
    fun extractDeterministic(transcript: String): ClinicalFindings {
        val toothConditions = mutableListOf<ToothConditionFinding>()
        val perioMeasurements = mutableListOf<PerioMeasurementFinding>()
        val treatmentPlans = mutableListOf<TreatmentPlanFinding>()
        val chiefComplaints = mutableListOf<String>()
        var chiefComplaintOther = ""
        val sensitivityTriggers = mutableListOf<String>()
        val gingivalRecession = mutableListOf<String>()
        val softTissue = mutableListOf<String>()
        val calculusList = mutableListOf<String>()
        val stainsList = mutableListOf<String>()
        val tmjList = mutableListOf<String>()
        val functionalHabits = mutableListOf<String>()
        val extraNotes = mutableListOf<String>()
        var painSeverity = ""
        var brushingFrequency = ""
        var flossingFrequency = ""
        var cariesRisk = ""
        var oralHygiene = ""
        var diagnosis = ""
        var prognosis = "Good"
        val warnings = mutableListOf<String>()

        // Demographics extraction state
        var patientName = ""
        var patientDob = ""
        var patientAgeYears: Int? = null
        var patientPhone = ""
        var patientEmail = ""
        var patientAddress = ""
        var patientIsChild: Boolean? = null
        val patientAllergies = mutableListOf<AllergyFinding>()
        val patientMedicalAlerts = mutableListOf<String>()
        val medicalHistoryNotes = mutableListOf<String>()
        val familyHistoryNotes = mutableListOf<String>()
        val pastDentalHistoryNotes = mutableListOf<String>()

        var normalized = transcript
            .replace(Regex("(?i)\\btooth\\s+number\\b"), "tooth")
            .replace(Regex("(?i)\\bteeth\\b"), "tooth")
            .replace(Regex("(?i)\\b#\\s*(\\d+)"), "tooth $1")
            .replace(Regex("(?i)\\bm\\s*o\\s*d\\b|\\bmod\\b"), "MOD")
            .replace(Regex("(?i)\\bm\\s*o\\b"), "MO")
            .replace(Regex("(?i)\\bd\\s*o\\b"), "DO")
            .replace(Regex("(?i)\\bb\\s*o\\s*p\\b"), "bleeding")
            .trim()

        // Normalize spoken number words after "tooth"
        val wordToNum = mapOf(
            "one" to "1", "to" to "2", "too" to "2", "two" to "2", "three" to "3",
            "four" to "4", "for" to "4", "five" to "5", "six" to "6", "seven" to "7",
            "eight" to "8", "ate" to "8", "nine" to "9", "ten" to "10",
            "eleven" to "11", "twelve" to "12", "thirteen" to "13",
            "fourteen" to "14", "for teen" to "14", "fifteen" to "15",
            "sixteen" to "16", "seventeen" to "17", "eighteen" to "18", "nineteen" to "19",
            "twenty" to "20", "twenty one" to "21", "twenty two" to "22", "twenty three" to "23",
            "twenty four" to "24", "twenty five" to "25", "twenty six" to "26", "twenty seven" to "27",
            "twenty eight" to "28", "twenty nine" to "29", "thirty" to "30",
            "thirty one" to "31", "thirty two" to "32"
        )
        for ((word, num) in wordToNum) {
            normalized = normalized.replace(Regex("(?i)\\btooth\\s+$word\\b"), "tooth $num")
        }

        val clauses = normalized.split(Regex("(?<=[.!?])\\s+|(?<=[,;])\\s+(?=(?:tooth|teeth|#|recommend|plan|schedule|suggest|patient|complaint|pain|sensitive|gingivit|calculus|caries|plaque|hygiene|brush|floss|habit|bruxism|clench|stain|tmj|mucosa|ulcer|diagnosis|prognosis|name|dob|born|age|phone|mobile|email|address|allerg|alert|medical|history))|\\s+and\\s+(?=(?:tooth|teeth|#|recommend|plan|floss|brush|patient|name|dob|born|age|phone|mobile|email|address|allerg|alert|medical|history))")).map { it.trim() }.filter { it.isNotBlank() }

        for (clause in clauses) {
            var handled = false

            // 1. Periodontal Pocket Probing Patterns (e.g. "tooth 3 mesiobuccal 4mm with bleeding", "tooth 14 pocket 5mm")
            val perioPattern = Regex("(?i)tooth\\s+(\\d{1,2})\\s*(.*?)(?:pocket|depth|probing|mm)?\\s*(\\d{1,2})\\s*(?:mm|millimeter|millimeters)?\\s*(bleeding|bop)?")
            val perioMatches = perioPattern.findAll(clause).toList()
            if (perioMatches.isNotEmpty() && (clause.contains("pocket", ignoreCase = true) || clause.contains("depth", ignoreCase = true) || clause.contains("probing", ignoreCase = true) || clause.contains("mm", ignoreCase = true) || clause.contains("bleeding", ignoreCase = true))) {
                for (match in perioMatches) {
                    val toothNum = match.groupValues[1].toIntOrNull() ?: continue
                    val siteDesc = match.groupValues[2].trim()
                    val depthMm = match.groupValues[3].toIntOrNull() ?: continue
                    val isBleeding = match.groupValues[4].isNotBlank() || clause.contains("bleeding", ignoreCase = true) || clause.contains("bop", ignoreCase = true)

                    val site = when {
                        siteDesc.contains("mesiobuccal", ignoreCase = true) || siteDesc.contains("mb", ignoreCase = true) -> "MB"
                        siteDesc.contains("distobuccal", ignoreCase = true) || siteDesc.contains("db", ignoreCase = true) -> "DB"
                        siteDesc.contains("mesiolingual", ignoreCase = true) || siteDesc.contains("ml", ignoreCase = true) -> "ML"
                        siteDesc.contains("distolingual", ignoreCase = true) || siteDesc.contains("dl", ignoreCase = true) -> "DL"
                        siteDesc.contains("buccal", ignoreCase = true) || siteDesc.contains("facial", ignoreCase = true) -> "B"
                        siteDesc.contains("lingual", ignoreCase = true) || siteDesc.contains("palatal", ignoreCase = true) -> "L"
                        siteDesc.contains("mesial", ignoreCase = true) -> "Mesial"
                        siteDesc.contains("distal", ignoreCase = true) -> "Distal"
                        else -> "Site not stated"
                    }

                    perioMeasurements.add(PerioMeasurementFinding(toothNum, site, depthMm, isBleeding))
                    handled = true
                }
            }

            // 2. Multi-site continuous probing depths (e.g. "tooth 14 probing depths 3 4 3 3 2 4")
            val multiDepthsPattern = Regex("(?i)tooth\\s+(\\d{1,2})\\s*(?:probing|depths|pockets)?\\s*(\\d)\\s+(\\d)\\s+(\\d)(?:\\s+(\\d)\\s+(\\d)\\s+(\\d))?")
            val multiMatch = multiDepthsPattern.find(clause)
            if (multiMatch != null && (clause.contains("depth", ignoreCase = true) || clause.contains("probing", ignoreCase = true))) {
                val toothNum = multiMatch.groupValues[1].toIntOrNull()
                if (toothNum != null) {
                    val sites = listOf("DB", "B", "MB", "DL", "L", "ML")
                    val rawDepths = (2..7).mapNotNull { idx ->
                        multiMatch.groupValues.getOrNull(idx)?.toIntOrNull()
                    }
                    rawDepths.forEachIndexed { i, depth ->
                        val site = sites.getOrElse(i) { "Site ${i + 1}" }
                        perioMeasurements.add(PerioMeasurementFinding(toothNum, site, depth, clause.contains("bleeding", ignoreCase = true)))
                    }
                    handled = true
                }
            }

            // 3. Tooth Condition Patterns (e.g. "tooth 14 occlusal caries", "tooth 19 missing", "tooth 30 root canal")
            val conditionPattern = Regex("(?i)tooth\\s+(\\d{1,2})\\s*(.*?)\\s*(caries|decay|cavity|carious|crown|capped|missing|extracted|implant|root\\s*canal|rct|endodontic|filled|restored|amalgam|composite|sound|exfoliated)")
            val condMatch = conditionPattern.find(clause)
            if (condMatch != null && !clause.contains("pocket", ignoreCase = true) && !clause.contains("depth", ignoreCase = true) && !clause.contains("recession", ignoreCase = true)) {
                val toothNum = condMatch.groupValues[1].toIntOrNull()
                if (toothNum != null) {
                    val detail = condMatch.groupValues[2].trim()
                    val keyword = condMatch.groupValues[3].lowercase(Locale.US)

                    val condition = when {
                        keyword.contains("caries") || keyword.contains("decay") || keyword.contains("cavity") || keyword.contains("carious") -> ToothCondition.DECAY
                        keyword.contains("crown") || keyword.contains("capped") -> ToothCondition.CROWN
                        keyword.contains("missing") || keyword.contains("extracted") -> ToothCondition.MISSING
                        keyword.contains("implant") -> ToothCondition.IMPLANT
                        keyword.contains("root") || keyword.contains("rct") || keyword.contains("endodontic") -> ToothCondition.ROOT_CANAL
                        keyword.contains("fill") || keyword.contains("restor") || keyword.contains("amalgam") || keyword.contains("composite") -> ToothCondition.FILLED
                        keyword.contains("exfoliat") -> ToothCondition.EXFOLIATED
                        else -> ToothCondition.SOUND
                    }

                    val surface = when {
                        detail.contains("occlusal", ignoreCase = true) || detail.contains("mod", ignoreCase = true) -> "MOD"
                        detail.contains("mesio-occlusal", ignoreCase = true) || detail.contains("mo", ignoreCase = true) -> "MO"
                        detail.contains("disto-occlusal", ignoreCase = true) || detail.contains("do", ignoreCase = true) -> "DO"
                        detail.contains("mesial", ignoreCase = true) -> "M"
                        detail.contains("distal", ignoreCase = true) -> "D"
                        detail.contains("buccal", ignoreCase = true) -> "B"
                        detail.contains("lingual", ignoreCase = true) -> "L"
                        detail.contains("incisal", ignoreCase = true) -> "I"
                        else -> detail
                    }

                    toothConditions.add(ToothConditionFinding(toothNum, condition, surface, notes = clause))
                    handled = true
                }
            }

            // 4. Treatment Plan Recommendations (e.g. "recommend composite filling for tooth 14", "plan crown for tooth 30")
            val txPattern = Regex("(?i)(?:recommend|plan|schedule|suggest)\\s+(.*?)(?:for\\s+tooth\\s+(\\d{1,2}))?$")
            val txMatch = txPattern.find(clause)
            if (txMatch != null) {
                val procedure = txMatch.groupValues[1].trim()
                val toothNum = txMatch.groupValues[2].toIntOrNull()
                if (procedure.isNotBlank()) {
                    treatmentPlans.add(TreatmentPlanFinding(toothNumber = toothNum, procedure = procedure.capitalize(Locale.US)))
                    handled = true
                }
            }

            // 5. Specific Clinical Examination Fields
            if (!handled) {
                when {
                    // Chief Complaints
                    clause.contains("chief complaint", ignoreCase = true) || clause.contains("complaining of", ignoreCase = true) || clause.contains("complaint", ignoreCase = true) -> {
                        val cleaned = clause.replace(Regex("(?i)chief complaint:?|patient complains of:?|complaining of:?|complaint:?"), "").trim().capitalize(Locale.US)
                        if (cleaned.isNotBlank()) {
                            chiefComplaints.add(cleaned)
                        }
                        handled = true
                    }
                    // Pain Severity
                    clause.contains("pain", ignoreCase = true) && (clause.contains("/10") || clause.contains("severe") || clause.contains("mild") || clause.contains("moderate") || clause.contains("throbbing") || clause.contains("sharp") || clause.contains("dull")) -> {
                        painSeverity = clause.capitalize(Locale.US)
                        handled = true
                    }
                    // Sensitivity Triggers
                    clause.contains("sensitive", ignoreCase = true) || clause.contains("sensitivity", ignoreCase = true) -> {
                        if (clause.contains("cold", ignoreCase = true)) sensitivityTriggers.add("Cold")
                        if (clause.contains("hot", ignoreCase = true) || clause.contains("heat", ignoreCase = true)) sensitivityTriggers.add("Hot")
                        if (clause.contains("sweet", ignoreCase = true) || clause.contains("sugar", ignoreCase = true)) sensitivityTriggers.add("Sweet")
                        if (clause.contains("biting", ignoreCase = true) || clause.contains("chewing", ignoreCase = true) || clause.contains("pressure", ignoreCase = true)) sensitivityTriggers.add("Biting / Mastication")
                        if (sensitivityTriggers.isEmpty()) sensitivityTriggers.add(clause.capitalize(Locale.US))
                        handled = true
                    }
                    // Gingival Recession
                    clause.contains("recession", ignoreCase = true) -> {
                        gingivalRecession.add(clause.capitalize(Locale.US))
                        handled = true
                    }
                    // Soft Tissue Findings
                    clause.contains("mucosa", ignoreCase = true) || clause.contains("ulcer", ignoreCase = true) || clause.contains("swelling", ignoreCase = true) || clause.contains("soft tissue", ignoreCase = true) || clause.contains("lichen planus", ignoreCase = true) || clause.contains("leukoplakia", ignoreCase = true) || clause.contains("erythema", ignoreCase = true) -> {
                        softTissue.add(clause.capitalize(Locale.US))
                        handled = true
                    }
                    // Calculus & Plaque
                    clause.contains("calculus", ignoreCase = true) || clause.contains("plaque", ignoreCase = true) || clause.contains("tartar", ignoreCase = true) || clause.contains("gingivit", ignoreCase = true) -> {
                        calculusList.add(clause.capitalize(Locale.US))
                        handled = true
                    }
                    // Stains
                    clause.contains("stain", ignoreCase = true) -> {
                        stainsList.add(clause.capitalize(Locale.US))
                        handled = true
                    }
                    // TMJ
                    clause.contains("tmj", ignoreCase = true) || clause.contains("clicking", ignoreCase = true) || clause.contains("jaw", ignoreCase = true) || clause.contains("crepitus", ignoreCase = true) -> {
                        tmjList.add(clause.capitalize(Locale.US))
                        handled = true
                    }
                    // Functional Habits
                    clause.contains("bruxism", ignoreCase = true) || clause.contains("grinding", ignoreCase = true) || clause.contains("clench", ignoreCase = true) || clause.contains("nail biting", ignoreCase = true) || clause.contains("mouth breath", ignoreCase = true) || clause.contains("thumb suck", ignoreCase = true) || clause.contains("smoking", ignoreCase = true) || clause.contains("tobacco", ignoreCase = true) || clause.contains("tongue thrust", ignoreCase = true) -> {
                        if (clause.contains("bruxism", ignoreCase = true) || clause.contains("grinding", ignoreCase = true)) {
                            functionalHabits.add("Bruxism / Night Grinding")
                        }
                        if (clause.contains("clench", ignoreCase = true)) {
                            functionalHabits.add("Clenching")
                        }
                        if (clause.contains("nail biting", ignoreCase = true)) {
                            functionalHabits.add("Nail Biting")
                        }
                        if (clause.contains("mouth breath", ignoreCase = true)) {
                            functionalHabits.add("Mouth Breathing")
                        }
                        if (clause.contains("thumb suck", ignoreCase = true)) {
                            functionalHabits.add("Thumb Sucking")
                        }
                        if (clause.contains("smoking", ignoreCase = true) || clause.contains("tobacco", ignoreCase = true)) {
                            functionalHabits.add("Tobacco / Smoking")
                        }
                        if (clause.contains("tongue thrust", ignoreCase = true)) {
                            functionalHabits.add("Tongue Thrust")
                        }
                        handled = true
                    }
                    // Flossing Frequency
                    clause.contains("floss", ignoreCase = true) -> {
                        flossingFrequency = when {
                            clause.contains("twice", ignoreCase = true) || clause.contains("2x", ignoreCase = true) -> "Twice daily"
                            clause.contains("once", ignoreCase = true) || clause.contains("1x", ignoreCase = true) || clause.contains("daily", ignoreCase = true) || clause.contains("every day", ignoreCase = true) -> "Daily"
                            clause.contains("week", ignoreCase = true) -> "Weekly"
                            clause.contains("irregular", ignoreCase = true) || clause.contains("rare", ignoreCase = true) || clause.contains("sometimes", ignoreCase = true) -> "Irregular"
                            clause.contains("never", ignoreCase = true) || clause.contains("no flossing", ignoreCase = true) -> "Never"
                            else -> clause.capitalize(Locale.US)
                        }
                        handled = true
                    }
                    // Brushing Frequency
                    clause.contains("brush", ignoreCase = true) -> {
                        brushingFrequency = when {
                            clause.contains("twice", ignoreCase = true) || clause.contains("2x", ignoreCase = true) -> "Twice daily (2x/day)"
                            clause.contains("three", ignoreCase = true) || clause.contains("3x", ignoreCase = true) -> "Three times daily (3x/day)"
                            clause.contains("once", ignoreCase = true) || clause.contains("1x", ignoreCase = true) || clause.contains("daily", ignoreCase = true) -> "Once daily (1x/day)"
                            else -> clause.capitalize(Locale.US)
                        }
                        oralHygiene = brushingFrequency
                        handled = true
                    }
                    // Caries Risk
                    clause.contains("caries risk", ignoreCase = true) || clause.contains("risk score", ignoreCase = true) -> {
                        cariesRisk = when {
                            clause.contains("high", ignoreCase = true) -> "High"
                            clause.contains("moderate", ignoreCase = true) -> "Moderate"
                            clause.contains("low", ignoreCase = true) -> "Low"
                            clause.contains("extreme", ignoreCase = true) -> "Extreme"
                            else -> clause.capitalize(Locale.US)
                        }
                        handled = true
                    }
                    // Prognosis
                    clause.contains("prognosis", ignoreCase = true) -> {
                        prognosis = when {
                            clause.contains("excellent", ignoreCase = true) -> "Excellent"
                            clause.contains("good", ignoreCase = true) -> "Good"
                            clause.contains("fair", ignoreCase = true) -> "Fair"
                            clause.contains("poor", ignoreCase = true) -> "Poor"
                            clause.contains("guarded", ignoreCase = true) || clause.contains("questionable", ignoreCase = true) -> "Guarded"
                            else -> clause.replace(Regex("(?i)prognosis:?"), "").trim().capitalize(Locale.US)
                        }
                        handled = true
                    }
                    // Diagnosis
                    clause.contains("diagnosis", ignoreCase = true) || clause.contains("diagnosed", ignoreCase = true) -> {
                        diagnosis = clause.replace(Regex("(?i)diagnosis:?|diagnosed with:?"), "").trim().trimEnd('.', ',', ';', ':').capitalize(Locale.US)
                        handled = true
                    }
                    // Demographics: Patient Name
                    (clause.contains(Regex("(?i)\\b(?:patient(?:'s)?\\s+name\\s+(?:is)?|name\\s+is)\\b")) ||
                     clause.matches(Regex("(?i)^patient\\s+([A-Z][a-z]+(?:\\s+[A-Z][a-z]+)+)$"))) -> {
                        val namePart = clause
                            .replace(Regex("(?i)^.*?\\b(?:patient(?:'s)?\\s+name\\s+(?:is)?|name\\s+is|patient)\\s*:?\\s*"), "")
                            .trim()
                            .trimEnd('.', ',', ';', ':')
                        if (namePart.isNotBlank()) {
                            patientName = namePart.split(" ")
                                .joinToString(" ") { it.replaceFirstChar { ch -> ch.uppercase(Locale.US) } }
                        }
                        handled = true
                    }
                    // Demographics: Date of Birth / Born on
                    clause.contains(Regex("(?i)\\b(?:date\\s+of\\s+birth|dob|born\\s+on)\\b")) -> {
                        val dobPart = clause
                            .replace(Regex("(?i)^.*?\\b(?:date\\s+of\\s+birth|dob|born\\s+on)\\s*:?\\s*(?:is\\s+)?"), "")
                            .trim()
                            .trimEnd('.', ',', ';', ':')
                        if (dobPart.isNotBlank()) {
                            patientDob = dobPart
                        }
                        handled = true
                    }
                    // Demographics: Age / Pediatric / Adult
                    clause.contains(Regex("(?i)\\b(?:pediatric|child|primary\\s+dentition|primary\\s+teeth)\\b")) -> {
                        patientIsChild = true
                        handled = true
                    }
                    clause.contains(Regex("(?i)\\b(?:adult\\s+patient|permanent\\s+dentition)\\b")) -> {
                        patientIsChild = false
                        handled = true
                    }
                    clause.contains(Regex("(?i)\\b(?:age\\s+(?:is\\s+)?\\d{1,3}|\\d{1,3}\\s+years\\s+old)\\b")) -> {
                        val ageMatch = Regex("(?i)\\b(?:age\\s+(?:is\\s+)?)?(\\d{1,3})(?:\\s+years\\s+old)?\\b").find(clause)
                        val ageVal = ageMatch?.groupValues?.get(1)?.toIntOrNull()
                        if (ageVal != null) {
                            patientAgeYears = ageVal
                            if (ageVal < 14) patientIsChild = true
                        }
                        handled = true
                    }
                    // Demographics: Phone / Mobile
                    clause.contains(Regex("(?i)\\b(?:phone|telephone|mobile|cell)\\b")) -> {
                        val phonePart = clause
                            .replace(Regex("(?i)^.*?\\b(?:phone(?:\\s+number)?|telephone|mobile|cell)\\s*:?\\s*(?:is\\s+)?"), "")
                            .trim()
                            .trimEnd('.', ',', ';', ':')
                        if (phonePart.isNotBlank()) {
                            patientPhone = phonePart
                        }
                        handled = true
                    }
                    // Demographics: Email
                    clause.contains(Regex("(?i)\\b(?:email|e-mail)\\b")) -> {
                        val emailPart = clause
                            .replace(Regex("(?i)^.*?\\b(?:email(?:\\s+address)?|e-mail)\\s*:?\\s*(?:is\\s+)?"), "")
                            .replace(" dot ", ".")
                            .replace(" at ", "@")
                            .replace(" ", "")
                            .trim()
                            .trimEnd('.', ',', ';', ':')
                        if (emailPart.isNotBlank()) {
                            patientEmail = emailPart
                        }
                        handled = true
                    }
                    // Demographics: Address
                    clause.contains(Regex("(?i)\\b(?:address|residing\\s+at|lives\\s+at)\\b")) -> {
                        val addrPart = clause
                            .replace(Regex("(?i)^.*?\\b(?:address|residing\\s+at|lives\\s+at)\\s*:?\\s*(?:is\\s+)?"), "")
                            .trim()
                            .trimEnd('.', ',', ';', ':')
                        if (addrPart.isNotBlank()) {
                            patientAddress = addrPart.capitalize(Locale.US)
                        }
                        handled = true
                    }
                    // Demographics: Allergies
                    clause.contains(Regex("(?i)\\b(?:allergic\\s+to|allergy\\s+to|allergies)\\b")) -> {
                        val rawAllergens = clause
                            .replace(Regex("(?i)^.*?\\b(?:allergic\\s+to|allergy\\s+to|allergies(?:\\s*:|\\s+include)?)\\s*"), "")
                            .split(Regex("(?i)\\s+(?:and|,)\\s+"))
                        for (raw in rawAllergens) {
                            val clean = raw.trim()
                            if (clean.isNotBlank()) {
                                val isSevere = clean.contains("severe", ignoreCase = true) || clean.contains("anaphylaxis", ignoreCase = true)
                                val isMild = clean.contains("mild", ignoreCase = true)
                                val severity = when {
                                    isSevere -> "Severe / Anaphylaxis"
                                    isMild -> "Mild"
                                    else -> "Moderate"
                                }
                                val allergenName = clean
                                    .replace(Regex("(?i)\\b(?:severe|mild|moderate|allergy|allergic)\\b"), "")
                                    .trim()
                                    .trimEnd('.', ',', ';', ':')
                                    .capitalize(Locale.US)
                                if (allergenName.isNotBlank()) {
                                    patientAllergies.add(AllergyFinding(allergen = allergenName, severity = severity))
                                }
                            }
                        }
                        handled = true
                    }
                    // Demographics: Medical Alerts
                    clause.contains(Regex("(?i)\\b(?:medical\\s+alert|pre-medication|blood\\s+thinner|bleeding\\s+disorder|pacemaker|diabetic\\s+alert|anticoagulant|bisphosphonate|mronj)\\b")) -> {
                        val alertText = when {
                            clause.contains("pre-med", ignoreCase = true) -> "Pre-medication required"
                            clause.contains("blood thinner", ignoreCase = true) || clause.contains("anticoagulant", ignoreCase = true) -> "Blood thinners / Anticoagulant therapy"
                            clause.contains("bleeding", ignoreCase = true) -> "Bleeding disorder"
                            clause.contains("pacemaker", ignoreCase = true) -> "Cardiac Pacemaker"
                            clause.contains("bisphosphonate", ignoreCase = true) || clause.contains("mronj", ignoreCase = true) -> "Bisphosphonates / MRONJ risk"
                            clause.contains("diabetic", ignoreCase = true) -> "Diabetic Alert"
                            else -> clause.replace(Regex("(?i)^.*?medical\\s+alert:?\\s*"), "").trim().capitalize(Locale.US)
                        }
                        if (alertText.isNotBlank()) {
                            patientMedicalAlerts.add(alertText)
                        }
                        handled = true
                    }
                    // Demographics: Family History
                    clause.contains(Regex("(?i)\\bfamily\\s+history\\b")) -> {
                        val fam = clause.replace(Regex("(?i)^.*?family\\s+history:?\\s*(?:of\\s+)?"), "").trim().capitalize(Locale.US)
                        if (fam.isNotBlank()) familyHistoryNotes.add(fam)
                        handled = true
                    }
                    // Demographics: Past Dental History
                    clause.contains(Regex("(?i)\\b(?:past\\s+dental\\s+history|dental\\s+history)\\b")) -> {
                        val dent = clause.replace(Regex("(?i)^.*?(?:past\\s+dental\\s+history|dental\\s+history):?\\s*"), "").trim().capitalize(Locale.US)
                        if (dent.isNotBlank()) pastDentalHistoryNotes.add(dent)
                        handled = true
                    }
                    // Demographics: Medical History
                    clause.contains(Regex("(?i)\\b(?:medical\\s+history|history\\s+of\\s+(?:hypertension|diabetes|asthma|cardiac|hepatitis|cancer|epilepsy|stroke))\\b")) -> {
                        val med = clause.replace(Regex("(?i)^.*?medical\\s+history:?\\s*"), "").trim().capitalize(Locale.US)
                        if (med.isNotBlank()) medicalHistoryNotes.add(med)
                        handled = true
                    }
                    // Extra / General Notes (Only true extra remarks go here!)
                    else -> {
                        if (clause.isNotBlank()) {
                            extraNotes.add(clause.capitalize(Locale.US))
                        }
                    }
                }
            }
        }

        if (perioMeasurements.any { it.depthMm > 12 }) {
            warnings.add("One or more probing depths exceed 12mm. Mandatory clinician review required.")
        }

        val examFindings = ClinicalExamFindings(
            chiefComplaints = chiefComplaints.distinct(),
            chiefComplaintOther = chiefComplaintOther,
            painSeverity = painSeverity,
            sensitivityTriggers = sensitivityTriggers.distinct(),
            gingivalRecession = gingivalRecession.distinct(),
            softTissue = softTissue.distinct(),
            calculus = calculusList.distinct(),
            stains = stainsList.distinct(),
            tmjAssessment = tmjList.distinct(),
            functionalHabits = functionalHabits.distinct(),
            brushingFrequency = brushingFrequency,
            flossingFrequency = flossingFrequency,
            cariesRisk = cariesRisk,
            oralHygiene = oralHygiene.ifBlank { brushingFrequency },
            diagnosis = diagnosis,
            prognosis = prognosis,
            extraNotes = extraNotes.distinct()
        )

        val demographicsFinding = DemographicsFinding(
            name = patientName,
            dob = patientDob,
            ageYears = patientAgeYears,
            phone = patientPhone,
            email = patientEmail,
            address = patientAddress,
            isChild = patientIsChild,
            medicalHistory = medicalHistoryNotes.joinToString(", "),
            familyHistory = familyHistoryNotes.joinToString(", "),
            pastDentalHistory = pastDentalHistoryNotes.joinToString(", "),
            allergies = patientAllergies.distinctBy { it.allergen.lowercase() },
            medicalAlerts = patientMedicalAlerts.distinct()
        )

        return ClinicalFindings(
            rawTranscript = transcript,
            toothConditions = toothConditions.distinctBy { it.toothNumber },
            perioMeasurements = perioMeasurements,
            clinicalNotes = extraNotes.map { ClinicalNoteFinding(category = "General Note", text = it) },
            treatmentPlanItems = treatmentPlans,
            examFindings = examFindings,
            demographics = if (demographicsFinding.hasAnyField) demographicsFinding else null,
            confidence = if (toothConditions.isNotEmpty() || perioMeasurements.isNotEmpty() || demographicsFinding.hasAnyField) 0.95f else 0.85f,
            isAiExtracted = false,
            warnings = warnings
        )
    }

    private fun String.capitalize(locale: Locale): String =
        replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
}
