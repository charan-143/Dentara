package com.example.thornburydental.export

import com.example.thornburydental.data.DiagnosticReport
import com.example.thornburydental.data.Patient
import com.example.thornburydental.data.Prescription
import com.example.thornburydental.data.ToothCondition
import com.example.thornburydental.data.TreatmentPlan
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.util.UUID

/**
 * Serializer for HL7 FHIR R4 standard dental resources.
 * Transforms Dentara clinical entities into interoperable FHIR JSON Bundles.
 */
object FhirExportManager {

    /**
     * Builds a comprehensive FHIR R4 Transaction/Document Bundle for a patient's complete clinical record.
     */
    fun exportPatientRecordToFhirBundle(
        patient: Patient,
        prescriptions: List<Prescription> = emptyList(),
        treatmentPlans: List<TreatmentPlan> = emptyList(),
        diagnosticReports: List<DiagnosticReport> = emptyList()
    ): JsonObject {
        val bundleId = UUID.randomUUID().toString()

        return buildJsonObject {
            put("resourceType", "Bundle")
            put("id", bundleId)
            put("type", "collection")
            put("timestamp", java.time.Instant.now().toString())

            putJsonArray("entry") {
                // 1. Patient Resource
                add(buildJsonObject {
                    put("fullUrl", "urn:uuid:${patient.id}")
                    put("resource", exportPatientToFhir(patient))
                })

                // 2. Dental Condition Resources (Teeth with non-sound status)
                patient.teeth.values.filter { it.condition != ToothCondition.SOUND }.forEach { tooth ->
                    add(buildJsonObject {
                        put("fullUrl", "urn:uuid:${patient.id}-tooth-${tooth.fdiNumber}")
                        put("resource", exportToothConditionToFhir(patient.id, tooth.fdiNumber, tooth.condition, tooth.name, tooth.notes))
                    })
                }

                // 3. MedicationRequest Resources
                prescriptions.forEach { rx ->
                    add(buildJsonObject {
                        put("fullUrl", "urn:uuid:${rx.id}")
                        put("resource", exportPrescriptionToFhir(rx))
                    })
                }

                // 4. Treatment Plan / Procedure Resources
                treatmentPlans.forEach { plan ->
                    plan.steps.forEach { step ->
                        add(buildJsonObject {
                            put("fullUrl", "urn:uuid:${step.id}")
                            put("resource", exportPlanStepToFhirProcedure(patient.id, step, plan.clinicianName))
                        })
                    }
                }

                // 5. DiagnosticReport Resources
                diagnosticReports.forEach { report ->
                    add(buildJsonObject {
                        put("fullUrl", "urn:uuid:${report.id}")
                        put("resource", exportDiagnosticReportToFhir(report))
                    })
                }
            }
        }
    }

    /**
     * Serializes Patient to FHIR R4 Patient resource.
     */
    fun exportPatientToFhir(patient: Patient): JsonObject {
        return buildJsonObject {
            put("resourceType", "Patient")
            put("id", patient.id)
            putJsonArray("identifier") {
                add(buildJsonObject {
                    put("system", "https://dentara.dental/mrn")
                    put("value", patient.opNo)
                })
            }
            putJsonArray("name") {
                add(buildJsonObject {
                    put("text", patient.name)
                })
            }
            put("birthDate", patient.dob)
            if (patient.phone.isNotBlank()) {
                putJsonArray("telecom") {
                    add(buildJsonObject {
                        put("system", "phone")
                        put("value", patient.phone)
                        put("use", "mobile")
                    })
                    if (patient.email.isNotBlank()) {
                        add(buildJsonObject {
                            put("system", "email")
                            put("value", patient.email)
                        })
                    }
                }
            }
        }
    }

    /**
     * Serializes a tooth clinical finding to FHIR R4 Condition resource.
     */
    fun exportToothConditionToFhir(
        patientId: String,
        toothFdi: Int,
        condition: ToothCondition,
        toothName: String,
        notes: String
    ): JsonObject {
        val (snomedCode, snomedDisplay) = when (condition) {
            ToothCondition.DECAY -> "80967001" to "Dental caries (disorder)"
            ToothCondition.FILLED -> "275298007" to "Restoration of tooth (procedure)"
            ToothCondition.CROWN -> "36830001" to "Crown (physical object)"
            ToothCondition.MISSING -> "234947008" to "Missing tooth (finding)"
            ToothCondition.IMPLANT -> "36631000" to "Dental implant (physical object)"
            ToothCondition.ROOT_CANAL -> "48387007" to "Root canal therapy (procedure)"
            ToothCondition.EXFOLIATED -> "245598003" to "Deciduous tooth exfoliated (finding)"
            ToothCondition.UNERUPTED -> "278644002" to "Unerupted tooth (finding)"
            ToothCondition.SOUND -> "162005007" to "Normal tooth (finding)"
        }

        return buildJsonObject {
            put("resourceType", "Condition")
            put("id", "$patientId-tooth-$toothFdi")
            putJsonObject("subject") {
                put("reference", "Patient/$patientId")
            }
            putJsonObject("code") {
                putJsonArray("coding") {
                    add(buildJsonObject {
                        put("system", "http://snomed.info/sct")
                        put("code", snomedCode)
                        put("display", snomedDisplay)
                    })
                }
                put("text", "${condition.label} on Tooth $toothFdi ($toothName)")
            }
            putJsonArray("bodySite") {
                add(buildJsonObject {
                    putJsonObject("coding") {
                        put("system", "http://fdi.org/tooth")
                        put("code", toothFdi.toString())
                        put("display", toothName)
                    }
                })
            }
            if (notes.isNotBlank()) {
                putJsonArray("note") {
                    add(buildJsonObject {
                        put("text", notes)
                    })
                }
            }
        }
    }

    /**
     * Serializes a prescription to FHIR R4 MedicationRequest.
     */
    fun exportPrescriptionToFhir(prescription: Prescription): JsonObject {
        return buildJsonObject {
            put("resourceType", "MedicationRequest")
            put("id", prescription.id)
            put("status", if (prescription.isDispensed) "completed" else "active")
            put("intent", "order")
            putJsonObject("subject") {
                put("reference", "Patient/${prescription.patientId}")
                put("display", prescription.patientName)
            }
            putJsonObject("medicationCodeableConcept") {
                put("text", prescription.drugName)
            }
            put("authoredOn", prescription.issueDate)
            putJsonObject("requester") {
                put("display", prescription.clinicianName)
            }
            putJsonArray("dosageInstruction") {
                add(buildJsonObject {
                    put("text", "${prescription.dosage} ${prescription.frequency} for ${prescription.duration}. ${prescription.instructions}")
                })
            }
        }
    }

    /**
     * Serializes a plan step to FHIR R4 Procedure.
     */
    fun exportPlanStepToFhirProcedure(
        patientId: String,
        step: com.example.thornburydental.data.PlanStep,
        clinicianName: String
    ): JsonObject {
        return buildJsonObject {
            put("resourceType", "Procedure")
            put("id", step.id)
            put("status", if (step.completed) "completed" else "in-progress")
            putJsonObject("subject") {
                put("reference", "Patient/$patientId")
            }
            putJsonObject("code") {
                putJsonArray("coding") {
                    add(buildJsonObject {
                        put("system", "http://www.ada.org/cdt")
                        put("code", step.code)
                        put("display", step.procedure)
                    })
                }
                put("text", step.procedure)
            }
            putJsonObject("performer") {
                put("display", clinicianName)
            }
        }
    }

    /**
     * Serializes a diagnostic report to FHIR R4 DiagnosticReport.
     */
    fun exportDiagnosticReportToFhir(report: DiagnosticReport): JsonObject {
        return buildJsonObject {
            put("resourceType", "DiagnosticReport")
            put("id", report.id)
            put("status", "final")
            putJsonObject("category") {
                putJsonArray("coding") {
                    add(buildJsonObject {
                        put("system", "http://terminology.hl7.org/CodeSystem/v2-0074")
                        put("code", "RAD")
                        put("display", "Radiology")
                    })
                }
            }
            putJsonObject("code") {
                put("text", report.title)
            }
            putJsonObject("subject") {
                put("reference", "Patient/${report.patientId}")
            }
            put("effectiveDateTime", report.takenAt)
            put("conclusion", report.summary)
        }
    }
}
