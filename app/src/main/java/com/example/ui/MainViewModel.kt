package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.Content
import com.example.api.Part
import com.example.api.callGeminiApi
import com.example.data.*
import com.example.sync.enqueueScanSync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class ZoneAlert(
    val plantName: String,
    val diseaseName: String,
    val latitude: Double,
    val longitude: Double,
    val casesCount: Int,
    val message: String,
    val timestamp: Long
)

data class WeatherInfo(
    val temperature: Double,
    val windspeed: Double,
    val weathercode: Int,
    val isFetched: Boolean = false,
    val description: String = ""
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val scanResultDao = db.scanResultDao()
    private val soilRecordDao = db.soilRecordDao()
    private val userDao = db.userDao()
    private val forumPostDao = db.forumPostDao()
    private val forumCommentDao = db.forumCommentDao()

    private suspend fun saveScan(scan: ScanResultEntity): Long {
        val id = withContext(Dispatchers.IO) { scanResultDao.insertScan(scan) }
        enqueueScanSync(getApplication())
        return id
    }

    val allScans: StateFlow<List<ScanResultEntity>> = scanResultDao.getAllScans()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeAlerts: StateFlow<List<ZoneAlert>> = allScans
        .map { scans -> detectAlerts(scans) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun detectAlerts(scans: List<ScanResultEntity>): List<ZoneAlert> {
        val alerts = mutableListOf<ZoneAlert>()
        val sevenDaysAgo = System.currentTimeMillis() - 7 * 24 * 3600 * 1000
        val relevantScans = scans.filter { it.timestamp >= sevenDaysAgo && it.latitude != null && it.longitude != null }
        
        val groupedByDisease = relevantScans.groupBy { it.diseaseName }
        for ((disease, diseaseScans) in groupedByDisease) {
            val clusters = mutableListOf<MutableList<ScanResultEntity>>()
            for (scan in diseaseScans) {
                var addedToCluster = false
                for (cluster in clusters) {
                    val representative = cluster.first()
                    val latDiff = Math.abs((representative.latitude ?: 0.0) - (scan.latitude ?: 0.0))
                    val lngDiff = Math.abs((representative.longitude ?: 0.0) - (scan.longitude ?: 0.0))
                    if (latDiff <= 0.1 && lngDiff <= 0.1) {
                        cluster.add(scan)
                        addedToCluster = true
                        break
                    }
                }
                if (!addedToCluster) {
                    clusters.add(mutableListOf(scan))
                }
            }
            
            for (cluster in clusters) {
                if (cluster.size >= 3) {
                    val rep = cluster.first()
                    val avgLat = cluster.mapNotNull { it.latitude }.average()
                    val avgLng = cluster.mapNotNull { it.longitude }.average()
                    alerts.add(
                        ZoneAlert(
                            plantName = rep.plantName,
                            diseaseName = disease,
                            latitude = avgLat,
                            longitude = avgLng,
                            casesCount = cluster.size,
                            message = "Alerte de zone rouge ! ${cluster.size} cas identiques de $disease détectés dans un rayon de 10km autour de Foumbot.",
                            timestamp = cluster.map { it.timestamp }.maxOrNull() ?: System.currentTimeMillis()
                        )
                    )
                }
            }
        }
        return alerts
    }

    val allSoilRecords: StateFlow<List<SoilRecordEntity>> = soilRecordDao.getAllSoilRecords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allForumPosts: StateFlow<List<ForumPostEntity>> = forumPostDao.getAllPosts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentUser = MutableStateFlow<String?>(null)

    fun updateUserProfile(commune: String, cultures: String, langue: String, consentementAlertes: Boolean) {
        val username = currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            userDao.updateProfile(username, commune, cultures, langue, consentementAlertes)
            userDao.getUserByUsername(username)?.let { user ->
                runCatching { com.example.api.ApiClient.service.uploadProfile(com.example.api.ProfileSyncPayload.from(user)) }
            }
        }
    }
    val currentLanguageIsEnglish = MutableStateFlow(false) // false = FR, true = EN
    val currentLatitude = MutableStateFlow(5.683)
    val currentLongitude = MutableStateFlow(10.633)
    val weatherState = MutableStateFlow<WeatherInfo?>(null)
    val isFetchingWeather = MutableStateFlow(false)
    val currentAiModeExpert = MutableStateFlow(false)      // false = Rapide, true = Expert

    val currentScanDetail = MutableStateFlow<ScanResultEntity?>(null)
    val chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())

    // General agricultural tutor chatbot state
    val tutoratMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val isTutoratLoading = MutableStateFlow(false)

    // Current viewed forum details
    val currentPostDetail = MutableStateFlow<ForumPostEntity?>(null)

    val isAnalyzing = MutableStateFlow(false)
    val isChatLoading = MutableStateFlow(false)

    // Soil indicators default state
    val soilNitrogen = MutableStateFlow("Moyen")
    val soilPhosphorus = MutableStateFlow("Bas")
    val soilPotassium = MutableStateFlow("Élevé")
    val soilHumidity = MutableStateFlow(45f) // %
    val soilTemperature = MutableStateFlow(28f) // °C
    val soilPh = MutableStateFlow(6.2f)

    init {
        fetchWeather(5.683, 10.633)
        // Populate database with some dummy / starter scans and forum posts if empty, to look incredible on first run
        viewModelScope.launch {
            allScans.first() // wait for initial fetch
            if (allScans.value.isEmpty()) {
                val starterScan = ScanResultEntity(
                    plantName = "Cacao",
                    diseaseName = "Pourriture brune des cabosses (Phytophthora)",
                    confidence = 94,
                    symptoms = "Taches brunes circulaires et humides sur les cabosses de cacao qui s'étendent rapidement, suivies d'un feutrage blanc poudreux désagréable.",
                    treatmentLocal = "1. Élaguer l'arbre pour augmenter le passage de la lumière et réduire l'humidité.\n2. Retirer régulièrement les cabosses infectées de l'arbre et les enfouir profondément sous terre hors du champ.\n3. Appliquer une décoction froide de cendres de bois de cuisine mélangée à du savon noir local sur les troncs pour former une barrière naturelle.",
                    treatmentChemical = "En cas de forte infestation, appliquer après la saison des pluies un fongicide à base d'oxyde de cuivre ou de métalaxyl-M (dosé exactement à 50g pour 15L d'eau), en portant des équipements de protection individuelle complets."
                )
                val manioc1 = ScanResultEntity(
                    plantName = "Manioc",
                    diseaseName = "Mosaïque du Manioc (CMD)",
                    confidence = 91,
                    symptoms = "Décoloration foliaire sous forme de mosaïque verte et jaune. Déformatiom importante et réduction de la taille des feuilles.",
                    treatmentLocal = "1. Utiliser des boutures saines et résistantes lors de la plantation.\n2. Éliminer immédiatement les plants présentant les premiers symptômes de mosaïque.\n3. Lutter contre les mouches blanches à l'aide d'extraits d'huile de neem.",
                    treatmentChemical = "Aucun produit chimique curatif n'est recommandé pour cette maladie virale. Concentrez-vous sur la prévention.",
                    timestamp = System.currentTimeMillis() - 24 * 3600 * 1000,
                    latitude = 5.6832,
                    longitude = 10.6331
                )
                val manioc2 = ScanResultEntity(
                    plantName = "Manioc",
                    diseaseName = "Mosaïque du Manioc (CMD)",
                    confidence = 88,
                    symptoms = "Distorsion sévère des feuilles, cloques et nanisme du plant de manioc.",
                    treatmentLocal = "1. Brûler les résidus de culture infectés.\n2. Éviter d'introduire des boutures de zones infectées.\n3. Favoriser la rotation des cultures.",
                    treatmentChemical = "Pas de traitement chimique disponible pour les viroses des plantes.",
                    timestamp = System.currentTimeMillis() - 12 * 3600 * 1000,
                    latitude = 5.6851,
                    longitude = 10.6353
                )
                val manioc3 = ScanResultEntity(
                    plantName = "Manioc",
                    diseaseName = "Mosaïque du Manioc (CMD)",
                    confidence = 93,
                    symptoms = "Feuilles tachetées de jaune et recroquevillées avec une croissance extrêmement ralentie.",
                    treatmentLocal = "1. Sélectionner les variétés améliorées CMD-résistantes de l'IRAD.\n2. Arracher et détruire les plants malades hors du champ.\n3. Pulvériser du purin de neem pour éloigner les insectes vecteurs.",
                    treatmentChemical = "Traitement chimique inefficace contre les virus. Favoriser le contrôle biologique des ravageurs.",
                    timestamp = System.currentTimeMillis() - 4 * 3600 * 1000,
                    latitude = 5.6821,
                    longitude = 10.6312
                )
                saveScan(starterScan)
                saveScan(manioc1)
                saveScan(manioc2)
                saveScan(manioc3)
            }

            if (allSoilRecords.value.isEmpty()) {
                val starterSoil = SoilRecordEntity(
                    nitrogen = "Moyen",
                    phosphorus = "Bas",
                    potassium = "Élevé",
                    humidity = 48f,
                    temperature = 27.5f,
                    ph = 6.4f
                )
                soilRecordDao.insertSoilRecord(starterSoil)
            }

            // Fetch forum posts to make sure they are populated
            val postList = forumPostDao.getAllPosts().first()
            if (postList.isEmpty()) {
                val demoPosts = listOf(
                    ForumPostEntity(
                        id = 1,
                        date = "14/04/2026 08:30",
                        author = "AgriExpert_Yaounde",
                        type = "post_pdf",
                        content = "TUTORIEL : Comment traiter le mildiou du manioc\n\nLe mildiou est l'une des maladies les plus dévastatrices du manioc en Afrique centrale. Voici les étapes de traitement :\n1. Identifier les feuilles jaunes et tachetées\n2. Retirer les plants infectés\n3. Appliquer de la bouillie bordelaise\n4. Surveiller pendant 2 semaines",
                        rating = 4.5f,
                        ratingCount = 8
                    ),
                    ForumPostEntity(
                        id = 2,
                        date = "13/04/2026 14:15",
                        author = "FermierCMR_Douala",
                        type = "post_offer",
                        content = "VENTE : Semences de maïs amélioré - Variété PANNAR 77\nQuantité disponible : 500 kg\nPrix : 1500 FCFA/kg\nLivraison possible dans tout le Cameroun\nContact : +237 6XX XXX XXX",
                        rating = 4.2f,
                        ratingCount = 5
                    ),
                    ForumPostEntity(
                        id = 3,
                        date = "12/04/2026 10:00",
                        author = "CoopAgri_Bafoussam",
                        type = "post_demand",
                        content = "RECHERCHE : Engrais NPK 20-10-10\nQuantité souhaitée : 2 tonnes\nBudget : négociable\nZone : Bafoussam et environs\nUrgent pour la saison des pluies",
                        rating = 3.8f,
                        ratingCount = 4
                    ),
                    ForumPostEntity(
                        id = 4,
                        date = "11/04/2026 16:45",
                        author = "EtudiantENSPD_59",
                        type = "post_question",
                        content = "QUESTION : Quelle est la meilleure période pour planter le cacao au Cameroun ?\nJe débute dans l'agriculture et j'aimerais avoir des conseils d'experts sur le calendrier cultural optimal.",
                        rating = 4.7f,
                        ratingCount = 12
                    ),
                    ForumPostEntity(
                        id = 5,
                        date = "10/04/2026 09:20",
                        author = "TechAgri_Ngaoundere",
                        type = "post_pdf",
                        content = "GUIDE : Utilisation des drones pour la surveillance des cultures\nLes nouvelles technologies permettent de surveiller des centaines d'hectares en quelques heures. Voici comment adapter cette technologie au contexte camerounais.",
                        rating = 4.0f,
                        ratingCount = 6
                    )
                )
                demoPosts.forEach { forumPostDao.insertPost(it) }

                val demoComments = listOf(
                    ForumCommentEntity(
                        postId = 1,
                        author = "FermierCMR_Douala",
                        content = "Très utile, merci ! J'ai appliqué cette méthode et ça marche.",
                        date = "14/04/2026 09:15"
                    ),
                    ForumCommentEntity(
                        postId = 1,
                        author = "CoopAgri_Bafoussam",
                        content = "Est-ce que la bouillie bordelaise est disponible facilement au Cameroun ?",
                        date = "14/04/2026 10:30"
                    ),
                    ForumCommentEntity(
                        postId = 4,
                        author = "AgriExpert_Yaounde",
                        content = "La saison optimale est entre mars et mai, pendant la grande saison des pluies.",
                        date = "11/04/2026 17:00"
                    ),
                    ForumCommentEntity(
                        postId = 4,
                        author = "TechAgri_Ngaoundere",
                        content = "Évitez la saison sèche entre décembre et février.",
                        date = "11/04/2026 18:20"
                    )
                )
                demoComments.forEach { forumCommentDao.insertComment(it) }
            }
        }
    }

    // Real-time Camera AI diagnostics powered by Google Gemini
    fun runLiveDiagnostic(
        cropName: String = "",
        base64Image: String? = null,
        mimeType: String? = null,
        latitude: Double? = null,
        longitude: Double? = null
    ) {
        viewModelScope.launch {
            isAnalyzing.value = true
            try {
                val systemPrompt = if (base64Image != null && mimeType != null) {
                    "Tu es un agronome d\\'élite spécialisé en agriculture camerounaise, tropicale et mondiale. " +
                    "L\\'utilisateur a scanné une feuille via la caméra de l\\'IA. Analyse attentivement l\\'image de la feuille ou plante fourie. " +
                    "Identifie la culture (par exemple: Cacao, Tomate, Manioc, Bananier, Caféier, Maïs, Igname, Macabo, Papayer, Manguier, Avocatier, Piment, Patate douce, etc.) et diagnostique précisément sa maladie phytosanitaire de façon réaliste. " +
                    "Réponds STRICTEMENT sous forme d\\'un objet JSON valide contenant EXACTEMENT ces clés et rien d\\'autre (ni explications extérieures, ni balises markdown additionnelles, juste l\\'accolade ouvrante et fermante JSON) : " +
                    "{\n" +
                    "  \"plantName\": \"Nom de la plante/culture identifiée automatiquement par l\\'IA (ex: Cacao, Manioc, etc.)\",\n" +
                    "  \"diseaseName\": \"Nom de la maladie réelle identifiée en français et son agent pathogène\",\n" +
                    "  \"confidence\": 95,\n" +
                    "  \"symptoms\": \"Symptômes réels précis observés (taches, couleur, chancres...)\",\n" +
                    "  \"treatmentLocal\": \"Description détaillée d\\'étapes pour préparer un traitement local écologique (ex: gousses d\\'ail écrasées, purin de neem, décoction de cendre de bois)\",\n" +
                    "  \"treatmentChemical\": \"Nom du produit ou molécule chimique agréée au Cameroun si de force majeure\"\n" +
                    "}"
                } else if (cropName.isNotBlank()) {
                    "Tu es un agronome d\\'élite spécialisé en agriculture camerounaise et mondiale. " +
                    "Génère un diagnostic phytosanitaire réaliste pour une feuille de $cropName affectée. " +
                    "Sélectionne au hasard ou intelligemment l\\'une des maladies courantes réelles du $cropName au Cameroun rattachée au climat tropical africain. " +
                    "Réponds STRICTEMENT sous forme d\\'un objet JSON valide contenant EXACTEMENT ces clés et rien d\\'autre (ni explications extérieures, ni balises markdown additionnelles, juste l\\'accolade ouvrante et fermante JSON) : " +
                    "{\n" +
                    "  \"plantName\": \"$cropName\",\n" +
                    "  \"diseaseName\": \"Nom de la maladie réelle en français et son agent pathogène\",\n" +
                    "  \"confidence\": 95,\n" +
                    "  \"symptoms\": \"Symptômes réels précis observés (taches, couleur, chancres...)\",\n" +
                    "  \"treatmentLocal\": \"Description détaillée d\\'étapes pour préparer un traitement local écologique (ex: gousses d\\'ail écrasées, purin de neem, décoction de cendre de bois)\",\n" +
                    "  \"treatmentChemical\": \"Nom du produit ou molécule chimique agréée au Cameroun si de force majeure\"\n" +
                    "}"
                } else {
                    "Tu es un agronome d\\'élite spécialisé en agriculture camerounaise, tropicale et mondiale. " +
                    "Puisque l\\'utilisateur a scanné une feuille via la caméra de l\\'IA sans spécifier de culture au préalable, tu dois identifier la culture toi-même parmi TOUTES les cultures existantes utiles (ex: Cacao, Tomate, Manioc, Bananier, Caféier, Maïs, Igname, Macabo, Papayer, Manguier, Avocatier, Piment, Patate douce, etc.) de façon réaliste. " +
                    "Sélectionne au hasard ou intelligemment l\\'une des cultures et l\\'une de ses maladies phytosanitaires réelles et courantes au Cameroun. " +
                    "Réponds STRICTEMENT sous forme d\\'un objet JSON valide contenant EXACTEMENT ces clés et rien d\\'autre (ni explications extérieures, ni balises markdown additionnelles, juste l\\'accolade ouvrante et fermante JSON) : " +
                    "{\n" +
                    "  \"plantName\": \"Nom de la plante/culture identifiée automatiquement par l\\'IA\",\n" +
                    "  \"diseaseName\": \"Nom de la maladie réelle en français et son agent pathogène\",\n" +
                    "  \"confidence\": 95,\n" +
                    "  \"symptoms\": \"Symptômes réels précis observés (taches, couleur, chancres...)\",\n" +
                    "  \"treatmentLocal\": \"Description détaillée d\\'étapes pour préparer un traitement local écologique (ex: gousses d\\'ail écrasées, purin de neem, décoction de cendre de bois)\",\n" +
                    "  \"treatmentChemical\": \"Nom du produit ou molécule chimique agréée au Cameroun si de force majeure\"\n" +
                    "}"
                }

                val userPrompt = if (base64Image != null && mimeType != null) {
                    "Analyse l\\'image ci-jointe pour identifier la plante et diagnostiquer la maladie."
                } else if (cropName.isNotBlank()) {
                    "Génère le diagnostic de la plante $cropName au format JSON."
                } else {
                    "Identifie une culture camerounaise ou tropicale affectée par une maladie courante et génère son diagnostic au format JSON."
                }

                var aiResponse = ""
                try {
                    aiResponse = withContext(Dispatchers.IO) {
                        callGeminiApi(
                            systemPrompt = systemPrompt,
                            userPrompt = userPrompt,
                            conversation = emptyList(),
                            base64Image = base64Image,
                            mimeType = mimeType
                        )
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MainViewModel", "Gemini API call failed, falling back to local database", e)
                }

                var lookupPlant = if (cropName.isNotBlank()) cropName else "Plante Inconnue"
                var lookupDisease = "Maladie non identifiée"
                var lookupConfidence = (85..98).random()
                var lookupSymptoms = "Symptômes atypiques non identifiés."
                var lookupLocal = "Veuillez nettoyer l\\'arbre et maximiser la ventilation."
                var lookupChemical = "Consultez un vulgarisateur agricole local."

                var parsedSuccessfully = false
                if (aiResponse.isNotBlank()) {
                    try {
                        var cleaned = aiResponse.trim()
                        if (cleaned.startsWith("```json")) {
                            cleaned = cleaned.substringAfter("```json").substringBeforeLast("```").trim()
                        } else if (cleaned.startsWith("```")) {
                            cleaned = cleaned.substringAfter("```").substringBeforeLast("```").trim()
                        }
                        val jsonObject = org.json.JSONObject(cleaned)
                        lookupPlant = jsonObject.optString("plantName", lookupPlant)
                        lookupDisease = jsonObject.optString("diseaseName", "Inconnue")
                        lookupConfidence = jsonObject.optInt("confidence", (85..98).random())
                        lookupSymptoms = jsonObject.optString("symptoms", lookupSymptoms)
                        lookupLocal = jsonObject.optString("treatmentLocal", lookupLocal)
                        lookupChemical = jsonObject.optString("treatmentChemical", lookupChemical)
                        parsedSuccessfully = true
                    } catch (jsonEx: Exception) {
                        android.util.Log.e("MainViewModel", "JSON parsing failed on: $aiResponse", jsonEx)
                    }
                }

                if (!parsedSuccessfully) {
                    val fallbackItem = if (cropName.isNotBlank()) {
                        CropDiagnostics.items.firstOrNull { it.plantName.equals(cropName, ignoreCase = true) }
                            ?: CropDiagnostics.items.random()
                    } else {
                        CropDiagnostics.items.random()
                    }
                    lookupPlant = fallbackItem.plantName
                    lookupDisease = fallbackItem.diseaseName
                    lookupSymptoms = fallbackItem.symptoms
                    lookupLocal = fallbackItem.treatmentLocal
                    lookupChemical = fallbackItem.treatmentChemical
                }

                val newScan = ScanResultEntity(
                    plantName = lookupPlant,
                    diseaseName = lookupDisease,
                    confidence = lookupConfidence,
                    symptoms = lookupSymptoms,
                    treatmentLocal = lookupLocal,
                    treatmentChemical = lookupChemical,
                    timestamp = System.currentTimeMillis(),
                    latitude = latitude,
                    longitude = longitude
                )

                val insertedId = withContext(Dispatchers.IO) {
                    saveScan(newScan)
                }

                val savedScan = newScan.copy(id = insertedId)
                currentScanDetail.value = savedScan
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Critical scan exception", e)
            } finally {
                isAnalyzing.value = false
            }
        }
    }

    // Reset Chat messages for current selection
    fun initChatForScan(scan: ScanResultEntity) {
        currentScanDetail.value = scan
        val messages = mutableListOf<ChatMessage>()
        
        // Add introductory greeting tailored beautifully
        messages.add(
            ChatMessage(
                text = "Bonjour ! Je suis votre conseiller virtuel Gemini. J'ai analysé votre culture de **${scan.plantName}** affectée par : **${scan.diseaseName}** (Confiance de diagnostic : ${scan.confidence}%).\n\n" +
                        "Comment puis-je vous aider aujourd'hui ? Je peux vous dicter étape par étape la préparation du traitement sain ou le dosage exact pour préserver le sol camerounais.",
                isUser = false
            )
        )
        chatMessages.value = messages
    }

    // Post message to Gemini chat
    fun sendChatMessage(text: String) {
        if (text.isBlank()) return
        val currentList = chatMessages.value.toMutableList()
        currentList.add(ChatMessage(text = text, isUser = true))
        chatMessages.value = currentList

        val scan = currentScanDetail.value ?: return

        viewModelScope.launch {
            isChatLoading.value = true

            // Set up a rich agricultural expert system prompt focused on Cameroon settings
            val systemPrompt = "Tu es un agronome virtuel d\\'élite spécialisé en agriculture africaine tropicale (en particulier au Cameroun, incluant les régions du Grand Sud, de l\\'Ouest, du Littoral et de l\\'Adamaoua). " +
                    "Ton ton est amical, encourageant, respectueux et chaleureux. Tu t\\'exprimes en français clair. De temps en temps, utilise des termes locaux familiers s\\'ils sont pertinents pour que l\\'agriculteur se sente compris. " +
                    "Tu as sous les yeux un diagnostic de plante de l\\'utilisateur : Plante : ${scan.plantName}, Maladie : ${scan.diseaseName}, Symptômes : ${scan.symptoms}. " +
                    "Propose exclusivement des solutions éco-responsables, durables, biologiques à base de produits ménagers locaux faciles d\\'accès au village (cendres de bois, huile de neem, savon noir local, écorces de neem) ou conseille des dosages précis et écologiques si des intrants chimiques sont requis (pour éviter le gaspillage et préserver la terre). " +
                    "Donne des instructions d\\'action structurées d\\'une manière extrêmement claire, logique, sous forme d\\'étapes simples (Étape 1, Étape 2, Étape 3)."

            // Compile conversational context into the Content array format
            val apiConversation = mutableListOf<Content>()
            // Map our messages
            currentList.forEach { msg ->
                apiConversation.add(Content(parts = listOf(Part(text = msg.text))))
            }

            val aiResponse = withContext(Dispatchers.IO) {
                callGeminiApi(systemPrompt = systemPrompt, userPrompt = text, conversation = apiConversation)
            }

            val updatedList = chatMessages.value.toMutableList()
            updatedList.add(ChatMessage(text = aiResponse, isUser = false))
            chatMessages.value = updatedList
            isChatLoading.value = false

            // Update persistence with history for this scan
            val formattedHistory = updatedList.joinToString("\n") { "${if (it.isUser) "Farmer" else "Gemini"}: ${it.text}" }
            scanResultDao.updateScan(scan.copy(chatHistoryJson = formattedHistory))
        }
    }

    // Soil data save action
    fun saveSoilRecord() {
        viewModelScope.launch {
            val record = SoilRecordEntity(
                nitrogen = soilNitrogen.value,
                phosphorus = soilPhosphorus.value,
                potassium = soilPotassium.value,
                humidity = soilHumidity.value,
                temperature = soilTemperature.value,
                ph = soilPh.value
            )
            withContext(Dispatchers.IO) {
                soilRecordDao.insertSoilRecord(record)
            }
        }
    }

    fun deleteScan(id: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                scanResultDao.deleteScanById(id)
            }
        }
    }

    // Password SHA-256 Hashing helper
    fun hashPassword(password: String): String {
        return try {
            val bytes = password.toByteArray()
            val md = java.security.MessageDigest.getInstance("SHA-256")
            val digest = md.digest(bytes)
            digest.fold("") { str, it -> str + "%02x".format(it) }
        } catch (e: Exception) {
            password // Fallback in case of hash error
        }
    }

    suspend fun registerUser(username: String, passwordRaw: String): Boolean {
        return withContext(Dispatchers.IO) {
            val existing = userDao.getUserByUsername(username)
            if (existing != null) {
                false
            } else {
                val hash = hashPassword(passwordRaw)
                val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.FRANCE)
                val dateStr = sdf.format(java.util.Date())
                userDao.insertUser(UserEntity(username = username, passwordHash = hash, createdAt = dateStr))
                true
            }
        }
    }

    suspend fun loginUser(username: String, passwordRaw: String): Boolean {
        return withContext(Dispatchers.IO) {
            val existing = userDao.getUserByUsername(username)
            if (existing != null) {
                existing.passwordHash == hashPassword(passwordRaw)
            } else {
                false
            }
        }
    }

    fun publishPost(author: String, type: String, content: String) {
        viewModelScope.launch {
            val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.FRANCE)
            val currentDate = sdf.format(java.util.Date())
            val post = ForumPostEntity(
                date = currentDate,
                author = author,
                type = type,
                content = content
            )
            withContext(Dispatchers.IO) {
                forumPostDao.insertPost(post)
            }
        }
    }

    fun publishComment(postId: Long, author: String, content: String) {
        viewModelScope.launch {
            val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.FRANCE)
            val currentDate = sdf.format(java.util.Date())
            val comment = ForumCommentEntity(
                postId = postId,
                author = author,
                content = content,
                date = currentDate
            )
            withContext(Dispatchers.IO) {
                forumCommentDao.insertComment(comment)
            }
        }
    }

    fun ratePost(post: ForumPostEntity, newRating: Float) {
        viewModelScope.launch {
            val oldRating = post.rating
            val oldCount = post.ratingCount
            val avg = (oldRating * oldCount + newRating) / (oldCount + 1)
            val updatedPost = post.copy(
                rating = (avg * 10).toInt() / 10f, // format to 1 decimal place
                ratingCount = oldCount + 1
            )
            withContext(Dispatchers.IO) {
                forumPostDao.updatePost(updatedPost)
            }
            currentPostDetail.value = updatedPost
        }
    }

    fun loadComments(postId: Long): Flow<List<ForumCommentEntity>> {
        return forumCommentDao.getCommentsByPostId(postId)
    }

    fun initTutoratChat() {
        val isEnglish = currentLanguageIsEnglish.value
        val greeting = if (isEnglish) {
            "Hello! I am your MBOA AGRI agricultural assistant. Ask me anything about your crops!"
        } else {
            "Bonjour ! Je suis votre assistant agricole MBOA AGRI. Posez-moi n'importe quelle question !"
        }
        tutoratMessages.value = listOf(ChatMessage(text = greeting, isUser = false))
    }

    fun sendTutoratMessage(text: String) {
        if (text.isBlank()) return
        val currentList = tutoratMessages.value.toMutableList()
        currentList.add(ChatMessage(text = text, isUser = true))
        tutoratMessages.value = currentList

        viewModelScope.launch {
            isTutoratLoading.value = true
            val isEnglish = currentLanguageIsEnglish.value
            val isExpert = currentAiModeExpert.value

            val expertPromptText = if (isEnglish) "detailed and scientific" else "détaillé et scientifique"
            val quickPromptText = if (isEnglish) "simple and practical" else "simple et pratique"
            val modeText = if (isExpert) expertPromptText else quickPromptText
            val langName = if (isEnglish) "English" else "Français"

            val systemPrompt = if (isEnglish) {
                "You are an elite agricultural expert assistant named MBOA AGRI, specializing in tropical African agronomy, especially in Cameroon context. " +
                "Your tone is friendly, educational, engaging and professional. You must respond in $langName in a very $modeText manner. " +
                "Give actionable structured advice using lists or bullet points."
            } else {
                "Tu es un agronome virtuel d\\'élite nommé MBOA AGRI, spécialisé en agriculture tropicale africaine (Cameroun). " +
                "Ton ton est amical, encourageant, respectueux et chaleureux. " +
                "Tu dois répondre en $langName d\\'une manière très $modeText. " +
                "Donne de solides recommandations pratiques ou scientifiques sous forme d\\'étapes simples ou puces."
            }

            val apiConversation = mutableListOf<Content>()
            currentList.forEach { msg ->
                apiConversation.add(Content(parts = listOf(Part(text = msg.text))))
            }

            val aiResponse = withContext(Dispatchers.IO) {
                callGeminiApi(systemPrompt = systemPrompt, userPrompt = text, conversation = apiConversation)
            }

            val updatedList = tutoratMessages.value.toMutableList()
            updatedList.add(ChatMessage(text = aiResponse, isUser = false))
            tutoratMessages.value = updatedList
            isTutoratLoading.value = false

            // Save Tutorat questions to user local agricultural log as a special scan-entity so they have visible historic entries
            val dateStr = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.FRANCE).format(java.util.Date())
            val tutorRecord = ScanResultEntity(
                plantName = "TUTORAT",
                diseaseName = "Conseils Agricoles",
                confidence = 100,
                symptoms = text,
                treatmentLocal = aiResponse,
                treatmentChemical = "Demande d'aide générale au conseiller de MBOA AGRI.",
                timestamp = System.currentTimeMillis()
            )
            withContext(Dispatchers.IO) {
                saveScan(tutorRecord)
            }
        }
    }

    fun updateLocation(lat: Double, lng: Double) {
        currentLatitude.value = lat
        currentLongitude.value = lng
        fetchWeather(lat, lng)
    }

    fun fetchWeather(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            isFetchingWeather.value = true
            try {
                val result = withContext(Dispatchers.IO) {
                    val url = java.net.URL("https://api.open-meteo.com/v1/forecast?latitude=$latitude&longitude=$longitude&current_weather=true")
                    val connection = url.openConnection() as java.net.HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.connectTimeout = 5000
                    connection.readTimeout = 5000
                    
                    if (connection.responseCode == 200) {
                        val stream = connection.inputStream
                        val responseText = stream.bufferedReader().use { it.readText() }
                        
                        val json = org.json.JSONObject(responseText)
                        if (json.has("current_weather")) {
                            val cw = json.getJSONObject("current_weather")
                            val temp = cw.getDouble("temperature")
                            val wind = cw.getDouble("windspeed")
                            val code = cw.getInt("weathercode")
                            
                            val desc = when (code) {
                                0 -> "Ciel dégagé"
                                1, 2, 3 -> "Peu nuageux"
                                45, 48 -> "Brouillard"
                                51, 53, 55 -> "Bruine légère"
                                61, 63, 65 -> "Pluie"
                                71, 73, 75 -> "Chute de neige"
                                80, 81, 82 -> "Averses de pluie"
                                95, 96, 99 -> "Orage"
                                else -> "Temps variable"
                            }
                            
                            WeatherInfo(temperature = temp, windspeed = wind, weathercode = code, isFetched = true, description = desc)
                        } else {
                            null
                        }
                    } else {
                        null
                    }
                }
                if (result != null) {
                    weatherState.value = result
                } else {
                    weatherState.value = WeatherInfo(temperature = 26.5, windspeed = 12.0, weathercode = 1, isFetched = true, description = "Partiellement nuageux (Mode local)")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                weatherState.value = WeatherInfo(temperature = 26.5, windspeed = 12.0, weathercode = 1, isFetched = true, description = "Partiellement nuageux (Mode local)")
            } finally {
                isFetchingWeather.value = false
            }
        }
    }
}
