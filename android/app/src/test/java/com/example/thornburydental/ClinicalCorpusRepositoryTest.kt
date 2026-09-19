package com.example.thornburydental

import com.example.thornburydental.data.cds.ClinicalCorpusRepository
import org.junit.Assert.*
import org.junit.Test

class ClinicalCorpusRepositoryTest {

    @Test
    fun testCorpusIntegrityVerification() {
        val isValid = ClinicalCorpusRepository.verifyCorpusIntegrity()
        assertTrue("Corpus SHA-256 integrity should be valid", isValid)
    }

    @Test
    fun testGetAllPassages_returnsPreloadedClinicalGuidelines() {
        val passages = ClinicalCorpusRepository.getAllPassages()
        assertTrue("Passages list should contain clinical guidelines", passages.size >= 5)

        val titles = passages.map { it.guidelineTitle }
        assertTrue(titles.any { it.contains("AAOMS") })
        assertTrue(titles.any { it.contains("AAP/EFP") })
        assertTrue(titles.any { it.contains("ADA") })
    }

    @Test
    fun testMetadataUpdate() {
        ClinicalCorpusRepository.updateCorpusMetadata("2026.09.2", 150, "abc123sha256")
        val meta = ClinicalCorpusRepository.metadata.value
        assertEquals("2026.09.2", meta.version)
        assertEquals(150, meta.passageCount)
        assertEquals("abc123sha256", meta.checksumSha256)
    }
}
