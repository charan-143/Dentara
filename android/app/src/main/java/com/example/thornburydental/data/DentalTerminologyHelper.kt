package com.example.thornburydental.data

/**
 * Standardized dental procedure and diagnostic mapping ontology.
 * Bridges clinical charting vernacular to ADA CDT, SNODENT, and ICD-10-CM vocabularies.
 */
object DentalTerminologyHelper {

    data class TerminologyEntry(
        val cdtCode: String,
        val cdtDescription: String,
        val snodentCode: String,
        val icd10Diagnosis: String,
        val standardFeeUsd: Double
    )

    private val PROCEDURE_MAP = mapOf(
        // Diagnostic & Preventative
        "Periodic Oral Evaluation" to TerminologyEntry("D0120", "Periodic oral evaluation - established patient", "101820D", "Z01.20", 65.0),
        "Comprehensive Oral Evaluation" to TerminologyEntry("D0150", "Comprehensive oral evaluation - new or established patient", "101830D", "Z01.20", 115.0),
        "Intraoral Full Mouth Series" to TerminologyEntry("D0210", "Intraoral - comprehensive series of radiographic images", "102450D", "Z01.20", 145.0),
        "Bitewing Radiographs - Four Images" to TerminologyEntry("D0274", "Bitewings - four radiographic images", "102460D", "Z01.20", 75.0),
        "Prophylaxis - Adult" to TerminologyEntry("D1110", "Prophylaxis - adult", "103210D", "Z01.20", 95.0),
        "Prophylaxis - Child" to TerminologyEntry("D1120", "Prophylaxis - child", "103220D", "Z01.20", 70.0),

        // Restorative
        "Resin Composite - 1 Surface Anterior" to TerminologyEntry("D2330", "Resin-based composite - one surface, anterior", "158930D", "K02.9", 175.0),
        "Resin Composite - 2 Surface Anterior" to TerminologyEntry("D2331", "Resin-based composite - two surfaces, anterior", "158931D", "K02.9", 225.0),
        "Resin Composite - 1 Surface Posterior" to TerminologyEntry("D2391", "Resin-based composite - one surface, posterior", "158940D", "K02.9", 195.0),
        "Resin Composite - 2 Surface Posterior" to TerminologyEntry("D2392", "Resin-based composite - two surfaces, posterior", "158941D", "K02.9", 265.0),
        "Resin Composite - 3 Surface Posterior" to TerminologyEntry("D2393", "Resin-based composite - three surfaces, posterior", "158942D", "K02.9", 325.0),
        "Porcelain / Ceramic Crown" to TerminologyEntry("D2740", "Crown - porcelain/ceramic substrate", "104920D", "K02.9", 1250.0),

        // Endodontics
        "Root Canal Therapy - Anterior" to TerminologyEntry("D3310", "Endodontic therapy, anterior tooth", "107810D", "K04.0", 850.0),
        "Root Canal Therapy - Bicuspid" to TerminologyEntry("D3320", "Endodontic therapy, premolar tooth", "107820D", "K04.0", 950.0),
        "Root Canal Therapy - Molar" to TerminologyEntry("D3330", "Endodontic therapy, molar tooth", "107830D", "K04.0", 1150.0),

        // Periodontics
        "Periodontal Scaling & Root Planing - 4+ Teeth" to TerminologyEntry("D4341", "Periodontal scaling and root planing - four or more teeth per quadrant", "109840D", "K05.3", 280.0),
        "Periodontal Maintenance" to TerminologyEntry("D4910", "Periodontal maintenance", "109910D", "K05.3", 160.0),

        // Oral Surgery
        "Extraction - Erupted Tooth" to TerminologyEntry("D7140", "Extraction, erupted tooth or exposed root", "139420D", "K04.7", 195.0),
        "Surgical Extraction" to TerminologyEntry("D7210", "Extraction, erupted tooth requiring removal of bone and/or sectioning of tooth", "139430D", "K04.7", 345.0)
    )

    fun getEntryForProcedure(name: String): TerminologyEntry? {
        return PROCEDURE_MAP[name] ?: PROCEDURE_MAP.entries.firstOrNull { (k, _) ->
            k.contains(name, ignoreCase = true) || name.contains(k, ignoreCase = true)
        }?.value
    }

    fun getCdtCode(procedureName: String): String {
        return getEntryForProcedure(procedureName)?.cdtCode ?: "D9999"
    }

    fun getIcd10Code(procedureName: String): String {
        return getEntryForProcedure(procedureName)?.icd10Diagnosis ?: "K02.9"
    }

    fun getAllPredefinedProcedures(): List<Pair<String, TerminologyEntry>> {
        return PROCEDURE_MAP.toList()
    }
}
