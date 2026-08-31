package com.example.api

import com.example.BuildConfig
import com.example.data.ScanResultEntity
import com.example.data.UserEntity
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class ScanSyncPayload(
    @Json(name = "local_id") val localId: Long,
    @Json(name = "plant_name") val plantName: String,
    @Json(name = "disease_name") val diseaseName: String,
    val confidence: Int,
    val symptoms: String,
    @Json(name = "treatment_local") val treatmentLocal: String,
    @Json(name = "treatment_chemical") val treatmentChemical: String,
    val timestamp: Long,
    val latitude: Double?,
    val longitude: Double?
) {
    companion object {
        fun from(entity: ScanResultEntity) = ScanSyncPayload(
            localId = entity.id,
            plantName = entity.plantName,
            diseaseName = entity.diseaseName,
            confidence = entity.confidence,
            symptoms = entity.symptoms,
            treatmentLocal = entity.treatmentLocal,
            treatmentChemical = entity.treatmentChemical,
            timestamp = entity.timestamp,
            latitude = entity.latitude,
            longitude = entity.longitude
        )
    }
}

@JsonClass(generateAdapter = true)
data class ProfileSyncPayload(
    val username: String,
    val commune: String,
    val cultures: String,
    val langue: String,
    @Json(name = "consentement_alertes") val consentementAlertes: Boolean
) {
    companion object {
        fun from(entity: UserEntity) = ProfileSyncPayload(
            username = entity.username,
            commune = entity.commune,
            cultures = entity.cultures,
            langue = entity.langue,
            consentementAlertes = entity.consentementAlertes
        )
    }
}

interface BackendApiService {
    @POST("api/scans")
    suspend fun uploadScan(@Body scan: ScanSyncPayload)

    @POST("api/profiles")
    suspend fun uploadProfile(@Body profile: ProfileSyncPayload)

    @POST("api/ai/generate")
    suspend fun generate(@Body request: GenerateContentRequest): GenerateContentResponse
}

object ApiClient {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: BackendApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BACKEND_BASE_URL)
            .client(httpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(BackendApiService::class.java)
    }
}
