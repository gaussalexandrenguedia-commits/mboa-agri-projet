package com.example.api

import android.content.Context
import com.example.BuildConfig
import com.example.config.BackendConfig
import com.example.data.ScanResultEntity
import com.example.data.UserEntity
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.Interceptor
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

// --- Payloads compatibles avec le backend FastAPI réel ---

@JsonClass(generateAdapter = true)
data class RegisterRequest(
    val username: String,
    @Json(name = "phone_number") val phoneNumber: String,
    val password: String
)

@JsonClass(generateAdapter = true)
data class LoginRequest(
    @Json(name = "phone_number") val phoneNumber: String,
    val password: String
)

@JsonClass(generateAdapter = true)
data class UserResponse(
    val id: Int,
    val username: String,
    @Json(name = "phone_number") val phoneNumber: String,
    @Json(name = "is_active") val isActive: Boolean = true
)

@JsonClass(generateAdapter = true)
data class TokenResponse(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "token_type") val tokenType: String = "bearer"
)

@JsonClass(generateAdapter = true)
data class ScanSyncPayload(
    @Json(name = "local_id") val localId: Long,
    @Json(name = "user_id") val userId: Int? = null, // compat Room, ignoré côté serveur
    @Json(name = "pathology_id") val pathologyId: Int? = null,
    @Json(name = "commune_code") val communeCode: String? = null,
    @Json(name = "commune_id") val communeId: Int? = null, // compat Room
    @Json(name = "hors_ligne") val horsLigne: Boolean = true,
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
        fun from(entity: ScanResultEntity, communeCode: String? = null): ScanSyncPayload {
            // Détection commune_code depuis la commune locale si possible
            val resolvedCode = communeCode ?: mapCommuneNameToCode(entity.plantName) // fallback, sera remplacé par le vrai profil
            return ScanSyncPayload(
                localId = entity.id,
                plantName = entity.plantName,
                diseaseName = entity.diseaseName,
                confidence = entity.confidence,
                symptoms = entity.symptoms,
                treatmentLocal = entity.treatmentLocal,
                treatmentChemical = entity.treatmentChemical,
                timestamp = entity.timestamp,
                latitude = entity.latitude,
                longitude = entity.longitude,
                horsLigne = true,
                communeCode = resolvedCode
            )
        }

        // Mapping minimal des communes camerounaises vers codes stables
        // Exemple : Bafoussam II -> CM-BFS-02 (à compléter avec les vraies communes de l'IRAD)
        private fun mapCommuneNameToCode(name: String?): String? {
            if (name.isNullOrBlank()) return null
            // Laisser null si pas de mapping, le backend accepte commune_id null
            return null
        }
    }
}

@JsonClass(generateAdapter = true)
data class ScanResponse(
    val id: Int,
    @Json(name = "local_id") val localId: Int,
    @Json(name = "user_id") val userId: Int?,
    @Json(name = "pathology_id") val pathologyId: Int?,
    @Json(name = "commune_id") val communeId: Int?,
    @Json(name = "hors_ligne") val horsLigne: Boolean,
    @Json(name = "plant_name") val plantName: String,
    @Json(name = "disease_name") val diseaseName: String,
    val confidence: Int,
    val symptoms: String,
    @Json(name = "treatment_local") val treatmentLocal: String,
    @Json(name = "treatment_chemical") val treatmentChemical: String,
    val timestamp: Long,
    val latitude: Double?,
    val longitude: Double?,
    @Json(name = "sync_status") val syncStatus: String = "created",
    val message: String? = null
)

@JsonClass(generateAdapter = true)
data class DiagnosticResponse(
    val id: Int? = null,
    @Json(name = "local_id") val localId: Int? = null,
    @Json(name = "plant_name") val plantName: String,
    @Json(name = "disease_name") val diseaseName: String,
    val confidence: Int,
    val symptoms: String? = null,
    @Json(name = "treatment_local") val treatmentLocal: String? = null,
    @Json(name = "treatment_chemical") val treatmentChemical: String? = null,
    @Json(name = "pathology_id") val pathologyId: Int? = null,
    @Json(name = "pathology_code") val pathologyCode: String? = null,
    @Json(name = "commune_id") val communeId: Int? = null,
    @Json(name = "commune_code") val communeCode: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @Json(name = "image_url") val imageUrl: String? = null,
    @Json(name = "hors_ligne") val horsLigne: Boolean = false,
    @Json(name = "severity_detected") val severityDetected: String? = null,
    @Json(name = "severity_default") val severityDefault: String? = null,
    @Json(name = "information_source") val informationSource: String = "CATALOG",
    @Json(name = "validation_status") val validationStatus: String = "PENDING_REVIEW"
)

@JsonClass(generateAdapter = true)
data class AlertResponse(
    val id: Int,
    @Json(name = "pathology_id") val pathologyId: Int,
    @Json(name = "commune_id") val communeId: Int,
    @Json(name = "scan_count") val scanCount: Int,
    @Json(name = "alert_level") val alertLevel: String,
    @Json(name = "created_at") val createdAt: String
)

@JsonClass(generateAdapter = true)
data class PathologyResponse(
    val id: Int,
    val code: String,
    @Json(name = "technical_name") val technicalName: String? = null,
    @Json(name = "common_name") val commonName: String? = null,
    @Json(name = "crop_name") val cropName: String,
    @Json(name = "key_symptoms") val keySymptoms: String? = null,
    @Json(name = "biological_treatment") val biologicalTreatment: String? = null,
    @Json(name = "chemical_treatment") val chemicalTreatment: String? = null,
    @Json(name = "default_severity") val defaultSeverity: String? = null,
    @Json(name = "is_active") val isActive: Boolean = true
)

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

// --- Services Retrofit ---

interface AuthApiService {
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): UserResponse

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): TokenResponse
}

interface BackendApiService {
    @POST("api/scans")
    suspend fun uploadScan(@Body scan: ScanSyncPayload): ScanResponse

    @GET("api/scans")
    suspend fun getMyScans(@Query("limit") limit: Int = 50): List<ScanResponse>

    @Multipart
    @POST("api/scans/diagnose")
    suspend fun diagnoseScan(
        @Part image: MultipartBody.Part,
        @Part("plant_name") plantName: RequestBody,
        @Part("symptoms") symptoms: RequestBody?,
        @Part("local_id") localId: RequestBody?,
        @Part("commune_code") communeCode: RequestBody?,
        @Part("latitude") latitude: RequestBody?,
        @Part("longitude") longitude: RequestBody?
    ): DiagnosticResponse

    @GET("alerts")
    suspend fun getAlerts(
        @Query("commune") commune: String? = null,
        @Query("commune_code") communeCode: String? = null,
        @Query("pathology_id") pathologyId: Int? = null,
        @Query("crop_name") cropName: String? = null,
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null
    ): List<AlertResponse>

    @GET("api/pathologies")
    suspend fun getPathologies(
        @Query("crop_name") cropName: String? = null,
        @Query("is_active") isActive: Boolean? = null
    ): List<PathologyResponse>

    // Legacy - conservé pour compatibilité si le backend de Martial expose encore /api/profiles et /api/ai/generate
    @POST("api/profiles")
    suspend fun uploadProfile(@Body profile: ProfileSyncPayload)

    @POST("api/ai/generate")
    suspend fun generate(@Body request: GenerateContentRequest): GenerateContentResponse
}

object ApiClient {
    private var appContext: Context? = null
    private var retrofitInstance: Retrofit? = null
    private var backendService: BackendApiService? = null
    private var authService: AuthApiService? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private fun getBaseUrl(): String {
        val ctx = appContext
        return if (ctx != null) {
            BackendConfig.getBaseUrl(ctx)
        } else {
            BuildConfig.BACKEND_BASE_URL
        }
    }

    private fun authInterceptor(): Interceptor = Interceptor { chain ->
        val original = chain.request()
        val token = appContext?.let { TokenManager.getInstance(it).getToken() }
        val request = if (!token.isNullOrBlank() && !original.url.encodedPath.contains("auth/")) {
            original.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            original
        }
        chain.proceed(request)
    }

    private fun buildOkHttp(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor())
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(
                        HttpLoggingInterceptor().apply {
                            level = HttpLoggingInterceptor.Level.BASIC
                        }
                    )
                }
            }
            .build()
    }

    private fun buildRetrofit(): Retrofit {
        val base = getBaseUrl()
        return Retrofit.Builder()
            .baseUrl(base)
            .client(buildOkHttp())
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
    }

    fun rebuild() {
        retrofitInstance = buildRetrofit()
        backendService = retrofitInstance!!.create(BackendApiService::class.java)
        authService = retrofitInstance!!.create(AuthApiService::class.java)
    }

    private fun getRetrofit(): Retrofit {
        if (retrofitInstance == null) {
            retrofitInstance = buildRetrofit()
        }
        return retrofitInstance!!
    }

    val service: BackendApiService by lazy {
        backendService ?: getRetrofit().create(BackendApiService::class.java).also { backendService = it }
    }

    val authServiceInstance: AuthApiService by lazy {
        authService ?: getRetrofit().create(AuthApiService::class.java).also { authService = it }
    }

    // Pour le diagnostic online avec image (multipart)
    fun getServiceWithContext(context: Context): BackendApiService {
        init(context)
        rebuild()
        return service
    }
}
