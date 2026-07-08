package com.example.api

import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import android.util.Log

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    @Json(name = "contents") val contents: List<Content>,
    @Json(name = "generationConfig") val generationConfig: GenerationConfig? = null,
    @Json(name = "systemInstruction") val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    @Json(name = "parts") val parts: List<Part>
)

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
data class GenerationConfig(
    @Json(name = "temperature") val temperature: Float? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    @Json(name = "candidates") val candidates: List<Candidate>?
)

@JsonClass(generateAdapter = true)
data class Candidate(
    @Json(name = "content") val content: Content?
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
        retrofit.create(GeminiApiService::class.java)
    }
}

suspend fun callGeminiApi(
    systemPrompt: String,
    userPrompt: String,
    conversation: List<Content> = emptyList(),
    base64Image: String? = null,
    mimeType: String? = null
): String {
    val apiKey = BuildConfig.GEMINI_API_KEY
    if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
        Log.e("GeminiApiClient", "Gemini API Key is a placeholder or empty!")
        return "MODE HORS-LIGNE ACTIVÉ : L'expert agronome local est disponible mais sans connexion à l'IA distante. Pour utiliser l'IA Gemini avancée en direct, veuillez saisir votre clé d'API valide dans le panneau de configuration d'AI Studio."
    }

    val requestContents = if (conversation.isNotEmpty()) {
        conversation
    } else {
        if (base64Image != null && mimeType != null) {
            listOf(
                Content(
                    parts = listOf(
                        Part(text = userPrompt),
                        Part(inlineData = InlineData(mimeType = mimeType, data = base64Image))
                    )
                )
            )
        } else {
            listOf(Content(parts = listOf(Part(text = userPrompt))))
        }
    }

    val systemInstructionContent = if (systemPrompt.isNotEmpty()) {
        Content(parts = listOf(Part(text = systemPrompt)))
    } else {
        null
    }

    val request = GenerateContentRequest(
        contents = requestContents,
        generationConfig = GenerationConfig(temperature = 0.7f),
        systemInstruction = systemInstructionContent
    )

    return try {
        val response = RetrofitClient.service.generateContent(apiKey, request)
        response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
            ?: "Désolé, je n'ai pas pu générer de réponse agronomique. Veuillez réessayer."
    } catch (e: Exception) {
        Log.e("GeminiApiClient", "Error calling Gemini API: ${e.message}", e)
        "Erreur de connexion : impossible de joindre l'agronome virtuel distant. Mode de secours hors-ligne appliqué. Vérifiez votre réseau."
    }
}
