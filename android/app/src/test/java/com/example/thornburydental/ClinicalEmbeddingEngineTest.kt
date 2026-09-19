package com.example.thornburydental

import com.example.thornburydental.data.cds.ClinicalEmbeddingEngine
import com.example.thornburydental.data.cds.SymptomInputQuery
import com.example.thornburydental.data.cds.SymptomItem
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ClinicalEmbeddingEngineTest {

    private lateinit var engine: ClinicalEmbeddingEngine

    @Before
    fun setUp() {
        engine = ClinicalEmbeddingEngine()
    }

    @Test
    fun testVectorSearch_retrievesRelevantPassages() {
        val query = SymptomInputQuery(
            selectedSymptoms = listOf(
                SymptomItem("s1", "Severe Spontaneous Toothache", "Maxillofacial"),
                SymptomItem("s2", "Lingering Thermal Sensitivity", "Maxillofacial")
            ),
            freeTextDescription = "Pulpitis thermal sensitivity spontaneously lingering pain"
        )

        val passages = engine.retrieveRelevantPassages(query, topK = 3)
        assertFalse(passages.isEmpty())
        assertTrue(passages.size <= 3)

        val topPassage = passages.first()
        assertTrue("Top passage should be pulpitis or infection related", topPassage.passageContent.lowercase().contains("pulpitis") || topPassage.passageContent.lowercase().contains("infection"))
    }

    @Test
    fun testSynonymExpansion_drySocketRetrieval() {
        val query = SymptomInputQuery(
            freeTextDescription = "Severe throbbing dry socket pain 3 days after lower molar extraction."
        )

        val passages = engine.retrieveRelevantPassages(query, topK = 2)
        assertFalse(passages.isEmpty())
        val top = passages.first()
        assertTrue("Dry socket synonym expansion must retrieve alveolar osteitis guideline", top.id == "kb-alveolar-01" || top.passageContent.lowercase().contains("osteitis"))
    }

    @Test
    fun testSynonymExpansion_anticoagulationRetrieval() {
        val query = SymptomInputQuery(
            freeTextDescription = "Patient on blood thinner taking Warfarin needing urgent extraction."
        )

        val passages = engine.retrieveRelevantPassages(query, topK = 2)
        assertFalse(passages.isEmpty())
        val top = passages.first()
        assertTrue("Blood thinner query must retrieve anticoagulation guideline", top.id == "kb-anticoag-01" || top.passageContent.lowercase().contains("anticoagulant") || top.passageContent.lowercase().contains("warfarin"))
    }

    @Test
    fun testVectorSearch_emptyQueryFallback() {
        val query = SymptomInputQuery()
        val passages = engine.retrieveRelevantPassages(query, topK = 2)
        assertEquals(2, passages.size)
    }
}
