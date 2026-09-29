package com.example.thornburydental

import com.example.thornburydental.data.Patient
import com.example.thornburydental.data.Prescription
import com.example.thornburydental.data.ToothCondition
import com.example.thornburydental.data.ToothRecord
import com.example.thornburydental.export.FhirExportManager
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class FhirExportTest {

    @Test
    fun testPatientToFhirSerialization() {
        val patient = Patient(
            id = "patient_123",
            opNo = "OP-9876",
            name = "Eleanor Vance",
            dob = "1985-04-12",
            phone = "+1-555-0199",
            email = "eleanor@example.com"
        )

        val json = FhirExportManager.exportPatientToFhir(patient)
        assertEquals("Patient", json["resourceType"]?.jsonPrimitive?.content)
        assertEquals("patient_123", json["id"]?.jsonPrimitive?.content)

        val nameArray = json["name"]?.jsonArray
        assertNotNull(nameArray)
        assertEquals("Eleanor Vance", nameArray?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.content)
    }

    @Test
    fun testPrescriptionToFhirSerialization() {
        val rx = Prescription(
            id = "rx_456",
            patientId = "patient_123",
            patientName = "Eleanor Vance",
            clinicianName = "Dr. Ingrid Halvorsen",
            drugName = "Amoxicillin 500mg",
            dosage = "500mg",
            frequency = "TID",
            duration = "7 days",
            instructions = "Take with food",
            issueDate = "2026-09-23"
        )

        val json = FhirExportManager.exportPrescriptionToFhir(rx)
        assertEquals("MedicationRequest", json["resourceType"]?.jsonPrimitive?.content)
        assertEquals("rx_456", json["id"]?.jsonPrimitive?.content)
        assertEquals("Amoxicillin 500mg", json["medicationCodeableConcept"]?.jsonObject?.get("text")?.jsonPrimitive?.content)
    }

    @Test
    fun testToothConditionToFhirSerialization() {
        val json = FhirExportManager.exportToothConditionToFhir(
            patientId = "patient_123",
            toothFdi = 16,
            condition = ToothCondition.DECAY,
            toothName = "Maxillary Right First Molar",
            notes = "Deep occlusal caries"
        )

        assertEquals("Condition", json["resourceType"]?.jsonPrimitive?.content)
        assertEquals("Patient/patient_123", json["subject"]?.jsonObject?.get("reference")?.jsonPrimitive?.content)
    }
}
