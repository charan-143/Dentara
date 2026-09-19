package com.example.thornburydental.data.cds

import java.util.Locale
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.sqrt

/**
 * On-device hybrid semantic retrieval & BM25 embedding engine with clinical synonym expansion.
 * Maps symptom queries and clinical narratives to top-k relevant practice guideline passages.
 */
class ClinicalEmbeddingEngine(
    private val corpusRepository: ClinicalCorpusRepository = ClinicalCorpusRepository
) {

    /**
     * Executes top-k hybrid semantic retrieval over the clinical corpus.
     */
    fun retrieveRelevantPassages(query: SymptomInputQuery, topK: Int = 3): List<RetrievedPassage> {
        val rawQueryText = "${query.selectedSymptoms.joinToString(" ") { it.name }} ${query.freeTextDescription}"
        val expandedQueryText = expandSynonyms(rawQueryText.lowercase(Locale.ROOT))
        val queryTokens = extractTokens(expandedQueryText)
        val passages = corpusRepository.getAllPassages()

        if (queryTokens.isEmpty() || passages.isEmpty()) {
            return passages.take(topK).map { it.copy(relevanceScore = 0.5f) }
        }

        // Compute Inverse Document Frequency (IDF) across corpus
        val totalDocs = passages.size.toDouble()
        val docFreqMap = mutableMapOf<String, Int>()
        val tokenizedPassages = passages.map { passage ->
            val pText = expandSynonyms("${passage.guidelineTitle} ${passage.sectionHeader} ${passage.passageContent}".lowercase(Locale.ROOT))
            val pTokens = extractTokens(pText)
            pTokens.distinct().forEach { token ->
                docFreqMap[token] = (docFreqMap[token] ?: 0) + 1
            }
            pTokens
        }

        val idfMap = docFreqMap.mapValues { (_, count) ->
            ln((totalDocs - count + 0.5) / (count + 0.5) + 1.0).coerceAtLeast(0.1)
        }

        val scoredPassages = passages.mapIndexed { index, passage ->
            val passageTokens = tokenizedPassages[index]
            val bm25Score = computeBm25Score(queryTokens, passageTokens, idfMap)
            val cosineScore = computeCosineSimilarity(queryTokens, passageTokens)
            val hybridScore = (bm25Score * 0.6f + cosineScore * 0.4f).coerceIn(0f, 1f)
            passage.copy(relevanceScore = hybridScore)
        }.sortedByDescending { it.relevanceScore }

        return scoredPassages.take(topK)
    }

    private fun expandSynonyms(text: String): String {
        var expanded = text
        for ((term, synonyms) in SYNONYM_DICTIONARY) {
            if (expanded.contains(term)) {
                expanded = "$expanded ${synonyms.joinToString(" ")}"
            }
        }
        return expanded
    }

    private fun extractTokens(text: String): List<String> {
        return text.lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length > 2 && it !in STOP_WORDS }
    }

    private fun computeBm25Score(
        queryTokens: List<String>,
        passageTokens: List<String>,
        idfMap: Map<String, Double>,
        k1: Double = 1.2,
        b: Double = 0.75,
        avgDocLen: Double = 40.0
    ): Float {
        if (passageTokens.isEmpty()) return 0f

        val docLen = passageTokens.size.toDouble()
        val passageFreq = passageTokens.groupingBy { it }.eachCount()
        var score = 0.0

        for (token in queryTokens.distinct()) {
            val count = passageFreq[token]?.toDouble() ?: 0.0
            if (count > 0.0) {
                val idf = idfMap[token] ?: 0.5
                val numerator = count * (k1 + 1.0)
                val denominator = count + k1 * (1.0 - b + b * (docLen / max(avgDocLen, 1.0)))
                score += idf * (numerator / denominator)
            }
        }

        // Normalize BM25 score approximately into [0.0, 1.0] range
        val normalized = (score / (score + 5.0)).toFloat()
        return normalized.coerceIn(0f, 1f)
    }

    private fun computeCosineSimilarity(queryTokens: List<String>, passageTokens: List<String>): Float {
        val vocabulary = (queryTokens + passageTokens).distinct()
        if (vocabulary.isEmpty()) return 0f

        val queryFreq = queryTokens.groupingBy { it }.eachCount()
        val passageFreq = passageTokens.groupingBy { it }.eachCount()

        var dotProduct = 0.0
        var normQuery = 0.0
        var normPassage = 0.0

        for (word in vocabulary) {
            val q = queryFreq[word]?.toDouble() ?: 0.0
            val p = passageFreq[word]?.toDouble() ?: 0.0
            dotProduct += q * p
            normQuery += q * q
            normPassage += p * p
        }

        if (normQuery == 0.0 || normPassage == 0.0) return 0f
        return (dotProduct / (sqrt(normQuery) * sqrt(normPassage))).toFloat()
    }

    companion object {
        private val STOP_WORDS = setOf(
            "the", "and", "for", "with", "that", "this", "from", "are", "was", "were",
            "has", "have", "had", "been", "not", "but", "can", "could", "should", "will",
            "about", "into", "over", "after"
        )

        private val SYNONYM_DICTIONARY = mapOf(
            "dry socket" to listOf("alveolar", "osteitis", "socket", "extraction"),
            "pus" to listOf("purulent", "exudate", "suppuration", "abscess"),
            "abscess" to listOf("odontogenic", "infection", "periapical", "cellulitis"),
            "locked jaw" to listOf("trismus", "mouth", "opening", "mastication"),
            "trismus" to listOf("opening", "spasm", "deep", "space", "infection"),
            "numbness" to listOf("paresthesia", "anesthesia", "toxicity", "last"),
            "knocked out" to listOf("avulsion", "trauma", "reimplantation", "splint"),
            "avulsed" to listOf("avulsion", "trauma", "permanent", "milk"),
            "blood thinner" to listOf("anticoagulant", "warfarin", "doac", "inr", "hemostasis"),
            "anticoagulated" to listOf("warfarin", "doac", "bleeding", "tranexamic", "txa"),
            "bisphosphonate" to listOf("mronj", "osteonecrosis", "denosumab", "bone"),
            "mronj" to listOf("bisphosphonate", "antiresorptive", "necrotic", "bone"),
            "pericoronitis" to listOf("operculum", "wisdom", "molar", "infection"),
            "child" to listOf("pediatric", "primary", "dentition", "germ")
        )
    }
}
