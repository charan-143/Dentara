package com.example.thornburydental.data.cds

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Response structure holding generated text and live web citations from Google Search grounding.
 */
data class GeminiMultimodalResponse(
    val responseText: String,
    val webCitations: List<WebSearchCitation> = emptyList(),
    val searchQueries: List<String> = emptyList(),
    val rawJson: String = ""
)

/**
 * Client for invoking Google Gemini Multimodal API with specialized
 * medical and dental prompts, structured outputs, Google Search Grounding, and fallback simulation.
 */
object GeminiMultimodalClient {

    private const val GEMINI_API_ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"
    private const val PREFS_NAME = "dentara_ai_config"
    private const val KEY_GEMINI_API_KEY = "gemini_api_key"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = false
        encodeDefaults = true
    }

    /**
     * Retrieves stored API key from user preferences if configured.
     */
    fun getApiKey(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_GEMINI_API_KEY, null)?.takeIf { it.isNotBlank() }
    }

    /**
     * Stores API key into user preferences.
     */
    fun setApiKey(context: Context, apiKey: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_GEMINI_API_KEY, apiKey.trim()).apply()
    }

    /**
     * Executes multimodal prompt to Gemini with optional Google Search Grounding.
     */
    suspend fun generateMultimodalClinicalResponse(
        context: Context,
        systemInstruction: String,
        promptText: String,
        mediaItems: List<MultimodalMediaItem>,
        enableWebSearch: Boolean = false
    ): Result<GeminiMultimodalResponse> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey(context)

        if (apiKey.isNullOrBlank()) {
            return@withContext Result.failure(IllegalStateException("No Gemini API Key configured. Use on-device medical reasoning."))
        }

        try {
            val url = URL("$GEMINI_API_ENDPOINT?key=$apiKey")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.connectTimeout = 15000
            connection.readTimeout = 35000
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.doOutput = true

            val parts = mutableListOf<JsonObject>()

            // 1. Text Prompt part
            parts.add(
                buildJsonObject {
                    put("text", promptText)
                }
            )

            // 2. Multimodal Image & Document parts
            for (item in mediaItems.filter { it.isSelected && it.base64Data != null }) {
                parts.add(
                    buildJsonObject {
                        putJsonObject("inlineData") {
                            put("mimeType", if (item.mimeType.startsWith("image/")) item.mimeType else "image/jpeg")
                            put("data", item.base64Data)
                        }
                    }
                )
            }

            val requestJson = buildJsonObject {
                putJsonObject("systemInstruction") {
                    putJsonArray("parts") {
                        add(buildJsonObject { put("text", systemInstruction) })
                    }
                }
                putJsonArray("contents") {
                    add(buildJsonObject {
                        put("role", "user")
                        put("parts", JsonArray(parts))
                    })
                }

                // Google Search Grounding tool injection
                if (enableWebSearch) {
                    putJsonArray("tools") {
                        add(buildJsonObject {
                            putJsonObject("google_search") {}
                        })
                    }
                }

                putJsonObject("generationConfig") {
                    put("temperature", 0.2)
                    put("maxOutputTokens", 2500)
                }
            }

            val inputBytes = requestJson.toString().toByteArray(Charsets.UTF_8)
            connection.outputStream.use { os ->
                os.write(inputBytes, 0, inputBytes.size)
            }

            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) connection.inputStream else (connection.errorStream ?: connection.inputStream)
            val responseText = stream?.bufferedReader()?.use { it.readText() } ?: ""

            if (responseCode in 200..299) {
                val parsed = parseGeminiResponse(responseText)
                Result.success(parsed)
            } else {
                Result.failure(IOException("Gemini API Error ($responseCode): $responseText"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseGeminiResponse(rawJson: String): GeminiMultimodalResponse {
        return try {
            val root = json.parseToJsonElement(rawJson).jsonObject
            val candidates = root["candidates"]?.jsonArray
            val firstCandidate = candidates?.firstOrNull()?.jsonObject
            val content = firstCandidate?.get("content")?.jsonObject
            val parts = content?.get("parts")?.jsonArray
            val textBuilder = StringBuilder()
            parts?.forEach { part ->
                part.jsonObject["text"]?.jsonPrimitive?.content?.let { textBuilder.append(it) }
            }
            val mainText = textBuilder.toString().ifEmpty { rawJson }

            // Extract Google Search Grounding metadata & citations
            val webCitations = mutableListOf<WebSearchCitation>()
            val searchQueries = mutableListOf<String>()

            val groundingMetadata = firstCandidate?.get("groundingMetadata")?.jsonObject
            if (groundingMetadata != null) {
                // 1. Search queries used
                groundingMetadata["webSearchQueries"]?.jsonArray?.forEach { qElem ->
                    qElem.jsonPrimitive.contentOrNull?.let { searchQueries.add(it) }
                }

                // 2. Web citations / grounding chunks
                groundingMetadata["groundingChunks"]?.jsonArray?.forEach { chunkElem ->
                    val webObj = chunkElem.jsonObject["web"]?.jsonObject
                    if (webObj != null) {
                        val uri = webObj["uri"]?.jsonPrimitive?.contentOrNull ?: ""
                        val title = webObj["title"]?.jsonPrimitive?.contentOrNull ?: uri
                        if (uri.isNotBlank()) {
                            val domain = try {
                                val host = URL(uri).host
                                host.removePrefix("www.")
                            } catch (_: Exception) {
                                ""
                            }
                            webCitations.add(
                                WebSearchCitation(
                                    title = title,
                                    url = uri,
                                    snippet = null,
                                    domain = domain
                                )
                            )
                        }
                    }
                }
            }

            GeminiMultimodalResponse(
                responseText = mainText,
                webCitations = webCitations.distinctBy { it.url },
                searchQueries = searchQueries.distinct(),
                rawJson = rawJson
            )
        } catch (_: Exception) {
            GeminiMultimodalResponse(
                responseText = rawJson,
                webCitations = emptyList(),
                searchQueries = emptyList(),
                rawJson = rawJson
            )
        }
    }
}
