package com.example.thornburydental

import com.example.thornburydental.data.DentalTerminologyHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class DentalTerminologyTest {

    @Test
    fun testCdtCodeResolution() {
        assertEquals("D0120", DentalTerminologyHelper.getCdtCode("Periodic Oral Evaluation"))
        assertEquals("D2391", DentalTerminologyHelper.getCdtCode("Resin Composite - 1 Surface Posterior"))
        assertEquals("D7140", DentalTerminologyHelper.getCdtCode("Extraction - Erupted Tooth"))
        assertEquals("D4341", DentalTerminologyHelper.getCdtCode("Periodontal Scaling & Root Planing - 4+ Teeth"))
    }

    @Test
    fun testIcd10CodeResolution() {
        assertEquals("Z01.20", DentalTerminologyHelper.getIcd10Code("Periodic Oral Evaluation"))
        assertEquals("K02.9", DentalTerminologyHelper.getIcd10Code("Resin Composite - 1 Surface Posterior"))
        assertEquals("K04.7", DentalTerminologyHelper.getIcd10Code("Extraction - Erupted Tooth"))
        assertEquals("K05.3", DentalTerminologyHelper.getIcd10Code("Periodontal Scaling & Root Planing - 4+ Teeth"))
    }
}
