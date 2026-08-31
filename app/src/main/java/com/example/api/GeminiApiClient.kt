package com.example.api

import android.util.Log
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    @Json(name = "contents") val contents: List<Content>,
    @Json(name = "generationConfig") val generationConfig: GenerationConfig? = null,
    @Json(name = "systemInstruction") val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(@Json(name = "parts") val parts: List<Part>)

@JsonClass(generateAdapter = true)
data class InlineData(
    @Json(name = "mimeType") val mimeType: String,
    @Json(name = "data") val data: String
)

@JsonClass(generateAdapter = true)
data class Part(
    @Json(name = "text") val text: String? = null,
    @Json(name = "inlineData") val inlineData: InlineData? = null
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(@Json(name = "temperature") val temperature: Float? = null)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(@Json(name = "candidates") val candidates: List<Candidate>?)

@JsonClass(generateAdapter = true)
data class Candidate(@Json(name = "content") val content: Content?)

suspend fun callGeminiApi(
    systemPrompt: String,
    userPrompt: String,
    conversation: List<Content> = emptyList(),
    base64Image: String? = null,
    mimeType: String? = null
): String {
    val requestContents = if (conversation.isNotEmpty()) conversation else listOf(
        Content(
            parts = buildList {
                add(Part(text = userPrompt))
                if (base64Image != null && mimeType != null) {
                    add(Part(inlineData = InlineData(mimeType, base64Image)))
                }
            }
        )
    )
    val request = GenerateContentRequest(
        contents = requestContents,
        generationConfig = GenerationConfig(temperature = 0.7f),
        systemInstruction = systemPrompt.takeIf { it.isNotBlank() }?.let { Content(listOf(Part(text = it))) }
    )
    return try {
        ApiClient.service.generate(request).candidates?.firstOrNull()?.content?.parts
            ?.firstOrNull { it.text != null }?.text
            ?: "Désolé, je n'ai pas pu générer de réponse agronomique."
    } catch (e: Exception) {
        Log.e("GeminiApiClient", "Backend AI unavailable: ${e.message}", e)
        "Mode hors connexion : l’agronome virtuel sera disponible dès que le réseau reviendra."
    }
}
