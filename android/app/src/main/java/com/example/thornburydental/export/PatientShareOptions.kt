package com.example.thornburydental.export

/**
 * Configuration options allowing the clinician/user to selectively choose
 * which clinical sections of a patient's health record to export and share.
 */
data class PatientShareOptions(
    val includeDemographics: Boolean = true,
    val includeMedicalHistory: Boolean = true,
    val includeExamination: Boolean = true,
    val includeDiagnosis: Boolean = true,
    val includeTreatmentPlans: Boolean = true,
    val includePrescriptions: Boolean = true,
    val includeReports: Boolean = true,
    val includeAppointments: Boolean = true,
    val anonymizePatientData: Boolean = false
) {
    /**
     * Returns true if at least one section is selected for inclusion.
     */
    val hasAnySelected: Boolean
        get() = includeDemographics ||
                includeMedicalHistory ||
                includeExamination ||
                includeDiagnosis ||
                includeTreatmentPlans ||
                includePrescriptions ||
                includeReports ||
                includeAppointments

    /**
     * Count of selected sections.
     */
    val selectedCount: Int
        get() = listOf(
            includeDemographics,
            includeMedicalHistory,
            includeExamination,
            includeDiagnosis,
            includeTreatmentPlans,
            includePrescriptions,
            includeReports,
            includeAppointments
        ).count { it }
}
