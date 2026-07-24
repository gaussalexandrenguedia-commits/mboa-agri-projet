package com.example.ui.screens

import android.content.Intent
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.MainViewModel
import com.example.ui.Translations
import com.example.ui.ZoneAlert
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val isEnglish by viewModel.currentLanguageIsEnglish.collectAsState()
    fun t(key: String) = Translations.translate(key, isEnglish)

    val activeAlerts by viewModel.activeAlerts.collectAsState()
    val currentLat by viewModel.currentLatitude.collectAsState()
    val currentLng by viewModel.currentLongitude.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0 = Map & Active Alerts, 1 = Prevention Guide, 2 = Report Outbreak
    var selectedAlertForReport by remember { mutableStateOf<ZoneAlert?>(null) }
    var showReportDialog by remember { mutableStateOf(false) }

    // Report Outbreak Form state
    var reportCrop by remember { mutableStateOf("") }
    var reportSymptom by remember { mutableStateOf("") }
    var reportSector by remember { mutableStateOf("Foumbot / Ouest Cameroun") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isEnglish) "Zone Phytosanitary Alerts" else "Alertes Phytosanitaires de Zone",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isEnglish) "Territorial Outbreak Monitoring & Prevention" else "Surveillance & Prévention des Foyers",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("alert_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val textToShare = buildString {
                                append("⚠️ ALERTE ROUGE PHYTOSANITAIRE MBOA AGRI ⚠️\n")
                                append("Secteur : Foumbot / Ouest Cameroun\n")
                                append("Alerte active : ${activeAlerts.size} foyers détectés.\n")
                                activeAlerts.forEach { a ->
                                    append("• Culture: ${a.plantName} | Maladie: ${a.diseaseName} (${a.casesCount} cas)\n")
                                }
                                append("\nProtocole de prévention :\n1. Arrachage immédiat des feuilles malades\n2. Traitement local au purin de neem/cendre\n3. Désinfection des outils agricoles\n\nPartagé via l'application MBOA AGRI.")
                            }
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Alerte Phytosanitaire MBOA AGRI")
                                putExtra(Intent.EXTRA_TEXT, textToShare)
                            }
                            context.startActivity(Intent.createChooser(intent, t("share_via")))
                        },
                        modifier = Modifier.testTag("alert_share_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Alert Report",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Category Navigation Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = if (isEnglish) "Foyers Actifs" else "Foyers Actifs",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = if (isEnglish) "Prévention" else "Prévention",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        Text(
                            text = if (isEnglish) "Signaler" else "Signaler",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.AddAlert,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }

            when (selectedTab) {
                0 -> ActiveAlertsTab(
                    alerts = activeAlerts,
                    currentLat = currentLat,
                    currentLng = currentLng,
                    isEnglish = isEnglish,
                    onSelectAlert = { alert ->
                        selectedAlertForReport = alert
                        showReportDialog = true
                    }
                )
                1 -> PreventionGuideTab(isEnglish = isEnglish)
                2 -> ReportOutbreakTab(
                    isEnglish = isEnglish,
                    crop = reportCrop,
                    onCropChange = { reportCrop = it },
                    symptom = reportSymptom,
                    onSymptomChange = { reportSymptom = it },
                    sector = reportSector,
                    onSectorChange = { reportSector = it },
                    onSubmit = {
                        if (reportCrop.isNotBlank() && reportSymptom.isNotBlank()) {
                            viewModel.runLiveDiagnostic(
                                cropName = reportCrop,
                                latitude = currentLat,
                                longitude = currentLng
                            )
                            Toast.makeText(
                                context,
                                if (isEnglish) "Alert report submitted & synchronized!" else "Alerte de foyer transmise et synchronisée !",
                                Toast.LENGTH_LONG
                            ).show()
                            reportCrop = ""
                            reportSymptom = ""
                            selectedTab = 0
                        } else {
                            Toast.makeText(
                                context,
                                if (isEnglish) "Please enter crop and symptom details" else "Veuillez renseigner la culture et les symptômes",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
            }
        }
    }

    // Modal Sheet / Dialog for Alert Details & Screenshot Card Preview
    if (showReportDialog && selectedAlertForReport != null) {
        val alert = selectedAlertForReport!!
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        val shareText = "⚠️ FICHE D'ALERTE OFFICELLE MBOA AGRI ⚠️\nPlante : ${alert.plantName}\nMaladie : ${alert.diseaseName}\nCas recensés : ${alert.casesCount}\nCoordonnées : Lat ${alert.latitude}, Lng ${alert.longitude}\nSecteur : Foumbot\n\nConsignes sanitaires : Mettre la parcelle en quarantaine et appliquer immédiatement le purin de neem/cendre de bois."
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(Intent.createChooser(intent, "Partager la Fiche d'Alerte"))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isEnglish) "Share Card / Capture" else "Partager / Capture d'écran",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) {
                    Text(text = if (isEnglish) "Close" else "Fermer")
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFD32F2F),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isEnglish) "Outbreak Report Badge" else "Fiche d'Alerte de Foyer",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            },
            text = {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFD32F2F))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "NIVEAU ROUGE • 3+ CAS DÉTECTÉS",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Plante : ${alert.plantName}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Maladie : ${alert.diseaseName}",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = Color(0xFFD32F2F)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Foyer : Coordonnées GPS (${((alert.latitude * 1000).toInt() / 1000.0)}, ${((alert.longitude * 1000).toInt() / 1000.0)})",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = alert.message,
                            fontSize = 12.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        )
    }
}

@Composable
fun ActiveAlertsTab(
    alerts: List<ZoneAlert>,
    currentLat: Double,
    currentLng: Double,
    isEnglish: Boolean,
    onSelectAlert: (ZoneAlert) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // High Impact Banner
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("alert_high_impact_banner"),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFB71C1C) // Deep Alert Red
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(Color.Yellow)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isEnglish) "ZONE EMERGENCY ALERT" else "URGENCE PHYTOSANITAIRE",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.25f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "SECTEUR FOUMBOT",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isEnglish) 
                            "3 or more identical disease scans detected within 10 km over the last 7 days. Automatic quarantine protocols activated."
                            else "3 cas identiques ou plus détectés dans un rayon de 10 km au cours des 7 derniers jours. Protocole de précaution activé.",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Live Google Map of Outbreak Epicenter
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = Color(0xFFD32F2F),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isEnglish) "Live Outbreak Epicenter Map" else "Carte en Direct du Foyer Epidémique",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "Radius 10 km",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.DarkGray)
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    webViewClient = WebViewClient()
                                    loadUrl("https://maps.google.com/maps?q=$currentLat,$currentLng&z=13&output=embed")
                                }
                            },
                            update = { webView ->
                                webView.loadUrl("https://maps.google.com/maps?q=$currentLat,$currentLng&z=13&output=embed")
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        // Active Alerts List Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEnglish) "Detected Active Outbreaks" else "Foyers d'Infection Recensés",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp
                )
                Text(
                    text = "${alerts.size} ${if (isEnglish) "active" else "actifs"}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD32F2F)
                )
            }
        }

        if (alerts.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isEnglish) "No active epidemic outbreaks in your sector." else "Aucune alerte majeure actuellement dans votre secteur.",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        } else {
            items(alerts) { alert ->
                Card(
                    onClick = { onSelectAlert(alert) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("alert_item_${alert.plantName}"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(Color(0xFFD32F2F), Color(0xFFFF9800))))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFD32F2F).copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = alert.plantName.uppercase(),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp,
                                        color = Color(0xFFD32F2F)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = alert.diseaseName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    maxLines = 1
                                )
                            }
                            IconButton(onClick = { onSelectAlert(alert) }) {
                                Icon(
                                    imageVector = Icons.Default.QrCode,
                                    contentDescription = "View Badge",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = alert.message,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                            lineHeight = 15.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                    contentDescription = null,
                                    tint = Color(0xFFE65100),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${alert.casesCount} ${if (isEnglish) "reported cases" else "cas signalés"}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE65100)
                                )
                            }
                            Text(
                                text = if (isEnglish) "Tap to view/share screenshot card" else "Toucher pour la fiche de capture",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PreventionGuideTab(isEnglish: Boolean) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isEnglish) "Phytosanitary Prevention Protocol" else "Protocole de Gestion & Prévention",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (isEnglish) "Recommended actions to protect your farm and stop epidemic spreads." else "Mesures recommandées pour protéger vos parcelles et stopper l'épidémie.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // Section 1: Biological & Organic
        item {
            PreventionCardItem(
                stepNumber = "01",
                title = if (isEnglish) "Biological & Organic Recipes" else "Traitements Bio & Écologiques Locaux",
                icon = Icons.Default.Eco,
                color = Color(0xFF2E7D32),
                content = if (isEnglish) 
                    "• Neem Oil Extract: Mix 50ml of crushed neem seed oil with 1L warm soapy water. Spray every 5 days.\n" +
                    "• Wood Ash & Black Soap: Dust dry kitchen wood ash on wet leaves to deter fungal spores and insect vectors.\n" +
                    "• Garlic & Chilli Extract: Crush 5 garlic cloves + 2 hot peppers in 1L water, filter and spray to repel whiteflies and aphids."
                    else 
                    "• Extrait d'Huile de Neem : Mélanger 50ml d'huile de neem avec 1L d'eau tiède savonneuse. Pulvériser tous les 5 jours.\n" +
                    "• Cendre de Bois de Cuisine : Saupoudrer de la cendre sèche sur les feuilles humides pour créer un milieu alcalin anti-fongique.\n" +
                    "• Purin d'Ail & Piment : Piler 5 gousses d'ail + 2 piments dans 1L d'eau, filtrer et pulvériser contre la mouche blanche du manioc."
            )
        }

        // Section 2: Quarantine & Sanitation
        item {
            PreventionCardItem(
                stepNumber = "02",
                title = if (isEnglish) "Quarantine & Field Sanitation" else "Quarantaine & Hygiène des Outils",
                icon = Icons.Default.CleaningServices,
                color = Color(0xFF1565C0),
                content = if (isEnglish) 
                    "• Sanitary Roguing: Immediately uproot infected plants showing symptoms, place in sealed bags and burn or bury at 50cm deep.\n" +
                    "• Tool Disinfection: Clean machetes, hoes, and shears with 70% alcohol or bleach solution between trees to avoid cross-contamination.\n" +
                    "• Crop Rotation: Rotate solanaceous crops (tomatoes, peppers) with legumes (beans, peanuts) every 2 seasons."
                    else 
                    "• Arrachage Sanitaire : Arracher immédiatement les plants malades dès les premiers symptômes, les enfouir à 50 cm hors du champ ou les brûler.\n" +
                    "• Désinfection des Machettes : Tremper les lames dans une solution d'eau de javel ou d'alcool à 70° entre chaque arbre pour éviter d'inoculer le virus.\n" +
                    "• Rotation des Cultures : Alterner les cultures sensibles (tomate, piment) avec des légumineuses (haricot, arachide) tous les 2 cycles."
            )
        }

        // Section 3: Chemical Safety Guidelines
        item {
            PreventionCardItem(
                stepNumber = "03",
                title = if (isEnglish) "Approved Chemical Guidelines" else "Consignes d'Intervention Chimique",
                icon = Icons.Default.Science,
                color = Color(0xFFD32F2F),
                content = if (isEnglish) 
                    "• Always wear full protective gear (mask, gloves, boots) when handling copper or metalaxyl fungicides.\n" +
                    "• Strict Dosage: Follow exact ratios (50g per 15L backpack sprayer) to avoid soil toxicity and resistance build-up.\n" +
                    "• Pre-harvest Interval (PHI): Respect the mandatory 14-day waiting period before harvesting crops treated with systemic fungicides."
                    else 
                    "• Équipement de Protection : Porter obligatoirement un masque, des gants et des bottes lors de l'application de fongicides à base de cuivre.\n" +
                    "• Respect Strict des Doses : Utiliser la dose exacte (ex: 50g pour 15L d'eau) pour éviter la résistance des champignons et préserver le sol.\n" +
                    "• Délai Avant Récolte (DAR) : Observer impérativement un délai d'au moins 14 jours entre le dernier traitement et la consommation des récoltes."
            )
        }

        // Section 4: Algorithmic Zone Threshold
        item {
            PreventionCardItem(
                stepNumber = "04",
                title = if (isEnglish) "Territorial Alert Threshold Rules" else "Seuil d'Alerte Algorithmique de Zone",
                icon = Icons.Default.AutoGraph,
                color = Color(0xFFE65100),
                content = if (isEnglish) 
                    "• 3+ Identical Scans: When 3 or more farmers within a 10 km radius scan the same disease within 7 days, a Red Zone Alert automatically broadcasts.\n" +
                    "• Community Protection: This early warning alerts nearby farmers to inspect their crops before symptoms appear."
                    else 
                    "• Règle des 3+ Scans : Dès que 3 agriculteurs ou plus scannent la même maladie dans un rayon de 10 km en 7 jours, une alerte de zone rouge est déclenchée.\n" +
                    "• Protection Communautaire : Ce mécanisme d'alerte précoce prévient les planteurs voisins d'inspecter leurs parcelles avant la propagation."
            )
        }
    }
}

@Composable
fun PreventionCardItem(
    stepNumber: String,
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    content: String
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(color.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = stepNumber,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            color = color
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = content,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun ReportOutbreakTab(
    isEnglish: Boolean,
    crop: String,
    onCropChange: (String) -> Unit,
    symptom: String,
    onSymptomChange: (String) -> Unit,
    sector: String,
    onSectorChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AddAlert,
                            contentDescription = null,
                            tint = Color(0xFFD32F2F),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isEnglish) "Report Suspected Outbreak" else "Signaler un Foyer Infectieux",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = if (isEnglish) "Participate in early territorial phytosanitary warning." else "Contribuez à la veille phytosanitaire de votre région.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = crop,
                        onValueChange = onCropChange,
                        label = { Text(if (isEnglish) "Crop Name (e.g., Cacao, Manioc, Tomate)" else "Culture (ex: Cacao, Manioc, Tomate)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("report_crop_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = symptom,
                        onValueChange = onSymptomChange,
                        label = { Text(if (isEnglish) "Observed Symptoms" else "Symptômes Observés") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .testTag("report_symptom_input"),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = sector,
                        onValueChange = onSectorChange,
                        label = { Text(if (isEnglish) "Sector / Region" else "Secteur / Région") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("report_sector_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onSubmit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("report_submit_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isEnglish) "SUBMIT OUTBREAK REPORT" else "TRANSMETTRE L'ALERTE DE FOYER",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
