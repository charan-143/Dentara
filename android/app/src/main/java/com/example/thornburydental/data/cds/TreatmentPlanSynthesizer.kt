package com.example.thornburydental.data.cds

/**
 * Intelligent synthesizer for phased, evidence-grounded Dental Treatment Plans.
 *
 * Organizes care pathways into 3 standard clinical phases:
 * Phase 1: Emergency, Diagnostics & Symptom Relief
 * Phase 2: Definitive Restorative, Endodontic, Surgical & Periodontal Care
 * Phase 3: Prevention, Maintenance & Supportive Recall
 *
 * Fully incorporates verified clinician overrides and custom recommendations.
 */
object TreatmentPlanSynthesizer {

    fun synthesizePlan(
        differentials: List<DifferentialDiagnosisRecommendation>,
        radiologyObservations: List<DentalRadiologyObservation>,
        patientContext: PatientChartContext?,
        appliedSuggestions: List<ClinicianSuggestionRecord>
    ): SuggestedTreatmentPlan {
        val topDiff = differentials.firstOrNull()?.conditionName ?: "Diagnostic Evaluation"
        val targetTooth = radiologyObservations.firstOrNull { it.toothNumber != null }?.toothNumber 
            ?: patientContext?.decayedTeethNumbers?.firstOrNull() 
            ?: 19

        val phase1Procedures = mutableListOf<SuggestedTreatmentProcedure>()
        val phase2Procedures = mutableListOf<SuggestedTreatmentProcedure>()
        val phase3Procedures = mutableListOf<SuggestedTreatmentProcedure>()

        // ---------------------------------------------------------------------
        // Phase 1: Diagnostics & Emergency Stabilization
        // ---------------------------------------------------------------------
        phase1Procedures.add(
            SuggestedTreatmentProcedure(
                procedureName = "Limited Oral Evaluation - Problem Focused",
                toothNumber = targetTooth,
                toothLabel = "Tooth #$targetTooth",
                cdtCode = "D0140",
                priority = "Urgent / Emergency",
                clinicalRationale = "Diagnostic baseline, vitality testing, and symptom localization.",
                estimatedVisits = 1
            )
        )
        phase1Procedures.add(
            SuggestedTreatmentProcedure(
                procedureName = "Intraoral Periapical Radiographic Image",
                toothNumber = targetTooth,
                toothLabel = "Tooth #$targetTooth",
                cdtCode = "D0220",
                priority = "Urgent / Emergency",
                clinicalRationale = "High-resolution apical verification and lamina dura assessment.",
                estimatedVisits = 1
            )
        )

        val isAbscessOrSwelling = topDiff.contains("Abscess", true) || differentials.any { it.conditionName.contains("Abscess", true) }
        if (isAbscessOrSwelling) {
            phase1Procedures.add(
                SuggestedTreatmentProcedure(
                    procedureName = "Incision and Drainage of Abscess - Intraoral Soft Tissue",
                    toothNumber = targetTooth,
                    toothLabel = "Tooth #$targetTooth",
                    cdtCode = "D7510",
                    priority = "Urgent / Emergency",
                    clinicalRationale = "Surgical decompression to relieve hydrostatic pressure and establish drainage.",
                    estimatedVisits = 1
                )
            )
        }

        // ---------------------------------------------------------------------
        // Phase 2: Definitive Restorative / Endodontic / Surgical Care
        // ---------------------------------------------------------------------
        val isPulpitisOrEndo = topDiff.contains("Pulpitis", true) || topDiff.contains("Apical Periodontitis", true)
        if (isPulpitisOrEndo) {
            val rctCode = when {
                targetTooth in listOf(6, 7, 8, 9, 10, 11, 22, 23, 24, 25, 26, 27) -> "D3310"
                targetTooth in listOf(4, 5, 12, 13, 20, 21, 28, 29) -> "D3320"
                else -> "D3330"
            }
            phase2Procedures.add(
                SuggestedTreatmentProcedure(
                    procedureName = "Endodontic Therapy - Complete Canal Instrumentation & Obturation",
                    toothNumber = targetTooth,
                    toothLabel = "Tooth #$targetTooth",
                    cdtCode = rctCode,
                    priority = "High",
                    clinicalRationale = "Complete removal of inflamed/necrotic pulpal tissue and 3D hermetic canal sealing.",
                    estimatedVisits = 1
                )
            )
            phase2Procedures.add(
                SuggestedTreatmentProcedure(
                    procedureName = "Core Buildup, Including Any Pins When Required",
                    toothNumber = targetTooth,
                    toothLabel = "Tooth #$targetTooth",
                    cdtCode = "D2950",
                    priority = "High",
                    clinicalRationale = "Retention foundation and coronal seal prior to definitive indirect restoration.",
                    estimatedVisits = 1
                )
            )
            phase2Procedures.add(
                SuggestedTreatmentProcedure(
                    procedureName = "Crown - Monolithic High-Translucency Zirconia / Ceramic",
                    toothNumber = targetTooth,
                    toothLabel = "Tooth #$targetTooth",
                    cdtCode = "D2740",
                    priority = "High",
                    clinicalRationale = "Full coronal coverage to prevent catastrophic biomechanical cusp fracture under occlusal loads.",
                    estimatedVisits = 2
                )
            )
        } else if (patientContext?.decayedTeethNumbers?.isNotEmpty() == true) {
            patientContext.decayedTeethNumbers.forEach { tNum ->
                phase2Procedures.add(
                    SuggestedTreatmentProcedure(
                        procedureName = "Resin-Based Composite - 2 Surfaces, Posterior",
                        toothNumber = tNum,
                        toothLabel = "Tooth #$tNum",
                        cdtCode = "D2392",
                        priority = "Moderate",
                        clinicalRationale = "Direct adhesive composite restoration following complete caries excavation.",
                        estimatedVisits = 1
                    )
                )
            }
        } else {
            phase2Procedures.add(
                SuggestedTreatmentProcedure(
                    procedureName = "Direct Resin Restoration / Desensitization Protocol",
                    toothNumber = targetTooth,
                    toothLabel = "Tooth #$targetTooth",
                    cdtCode = "D2391",
                    priority = "Moderate",
                    clinicalRationale = "Conservative dentinal tubule occlusion and protective restoration.",
                    estimatedVisits = 1
                )
            )
        }

        // Periodontal treatment if indicated
        val isPeriodontal = topDiff.contains("Periodontitis", true) || differentials.any { it.conditionName.contains("Periodontitis", true) }
        if (isPeriodontal) {
            phase2Procedures.add(
                SuggestedTreatmentProcedure(
                    procedureName = "Periodontal Scaling and Root Planing - Four or More Teeth per Quadrant",
                    toothNumber = null,
                    toothLabel = "Mandibular / Maxillary Quadrants",
                    cdtCode = "D4341",
                    priority = "High",
                    clinicalRationale = "Subgingival debridement to reduce pocket depths and eliminate bacterial biofilm.",
                    estimatedVisits = 2
                )
            )
        }

        // ---------------------------------------------------------------------
        // Phase 3: Maintenance & Prevention
        // ---------------------------------------------------------------------
        phase3Procedures.add(
            SuggestedTreatmentProcedure(
                procedureName = if (isPeriodontal) "Periodontal Maintenance Recall" else "Periodic Oral Evaluation & Prophylaxis",
                toothNumber = null,
                toothLabel = "Full Mouth",
                cdtCode = if (isPeriodontal) "D4910" else "D0120",
                priority = "Preventive / Maintenance",
                clinicalRationale = if (isPeriodontal) "3-month supportive periodontal therapy recall." else "6-month preventive checkup and hygiene recall.",
                estimatedVisits = 1
            )
        )
        phase3Procedures.add(
            SuggestedTreatmentProcedure(
                procedureName = "Topical Fluoride Varnish Application",
                toothNumber = null,
                toothLabel = "Full Dentition",
                cdtCode = "D1206",
                priority = "Preventive / Maintenance",
                clinicalRationale = "Remineralization and secondary caries prevention.",
                estimatedVisits = 1
            )
        )

        // ---------------------------------------------------------------------
        // Integrate Applied Clinician Suggestions
        // ---------------------------------------------------------------------
        val customizedNotes = mutableListOf<String>()
        appliedSuggestions.filter { it.isApplied }.forEach { suggestion ->
            customizedNotes.add(suggestion.suggestionText)
            val sTeeth = suggestion.affectedTeeth.ifEmpty { listOf(targetTooth) }
            val cdt = suggestion.detectedCdtCodes.firstOrNull() ?: "D9999"

            val targetPhase = when {
                cdt in listOf("D0140", "D0220", "D7510", "D9930") -> phase1Procedures
                cdt in listOf("D0120", "D1110", "D1206", "D4910") -> phase3Procedures
                else -> phase2Procedures
            }

            sTeeth.forEach { tNum ->
                targetPhase.add(
                    SuggestedTreatmentProcedure(
                        procedureName = "Customized: ${suggestion.suggestionText.take(50)}",
                        toothNumber = tNum,
                        toothLabel = "Tooth #$tNum",
                        cdtCode = cdt,
                        priority = "High (Clinician Override)",
                        clinicalRationale = suggestion.feedbackMessage,
                        estimatedVisits = 1,
                        isClinicianCustomization = true
                    )
                )
            }
        }

        val phases = listOf(
            SuggestedTreatmentPhase(
                phaseNumber = 1,
                phaseTitle = "Phase 1: Emergency, Diagnostics & Stabilization",
                targetTimeline = "Immediate (Visit 1)",
                procedures = phase1Procedures
            ),
            SuggestedTreatmentPhase(
                phaseNumber = 2,
                phaseTitle = "Phase 2: Definitive Restorative, Endodontic & Periodontal Care",
                targetTimeline = "1 - 3 Weeks Post-Stabilization",
                procedures = phase2Procedures
            ),
            SuggestedTreatmentPhase(
                phaseNumber = 3,
                phaseTitle = "Phase 3: Prevention, Supportive Therapy & Maintenance",
                targetTimeline = "3 - 6 Month Recall",
                procedures = phase3Procedures
            )
        )

        val totalProcedures = phases.sumOf { it.procedures.size }

        return SuggestedTreatmentPlan(
            planTitle = "Care Pathway: $topDiff",
            clinicalSummary = "Phased evidence-grounded treatment pathway formulated for ${patientContext?.fullName ?: "Ad-hoc Consultation"} ($totalProcedures total procedural steps across 3 clinical phases).",
            primaryDiagnosis = topDiff,
            phases = phases,
            totalEstimatedProcedures = totalProcedures,
            clinicianCustomizations = customizedNotes
        )
    }
}
