package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.MenuBook
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
import android.webkit.WebView
import android.webkit.WebViewClient
import android.util.Log
import com.example.data.ScanResultEntity
import com.example.ui.MainViewModel
import com.example.ui.Translations
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import java.text.SimpleDateFormat
import java.util.*

fun getDashboardLocation(context: android.content.Context): Pair<Double, Double>? {
    val locationManager = context.getSystemService(android.content.Context.LOCATION_SERVICE) as? android.location.LocationManager
        ?: return null
    try {
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            val gpsLocation = locationManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
            val networkLocation = locationManager.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
            val bestLocation = when {
                gpsLocation != null && networkLocation != null -> {
                    if (gpsLocation.time > networkLocation.time) gpsLocation else networkLocation
                }
                gpsLocation != null -> gpsLocation
                else -> networkLocation
            }
            if (bestLocation != null) {
                return Pair(bestLocation.latitude, bestLocation.longitude)
            }
        }
    } catch (e: Exception) {
        Log.e("DashboardLocation", "Error getting location", e)
    }
    return Pair(5.683 + (Math.random() - 0.5) * 0.01, 10.633 + (Math.random() - 0.5) * 0.01)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateToScan: () -> Unit,
    onNavigateToTutorat: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToDetail: (ScanResultEntity) -> Unit,
    onNavigateToChat: (ScanResultEntity) -> Unit,
    onLogout: () -> Unit,
    onNavigateToAlert: () -> Unit = {}
) {
    val scans by viewModel.allScans.collectAsState()
    val activeAlerts by viewModel.activeAlerts.collectAsState()
    val isEnglish by viewModel.currentLanguageIsEnglish.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    fun t(key: String): String = Translations.translate(key, isEnglish)

    val dateFormatter = remember(isEnglish) {
        if (isEnglish) SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US)
        else SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.FRENCH)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = t("app_title"),
                            fontWeight = FontWeight.Black,
                            fontSize = 19.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = t("subtitle"),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                },
                actions = {
                    BadgedBox(
                        badge = {
                            if (activeAlerts.isNotEmpty()) {
                                Badge(
                                    containerColor = Color(0xFFD32F2F),
                                    contentColor = Color.White
                                ) {
                                    Text(text = "${activeAlerts.size}")
                                }
                            }
                        },
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        IconButton(
                            onClick = onNavigateToAlert,
                            modifier = Modifier.testTag("dashboard_alert_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = "Alerts",
                                tint = if (activeAlerts.isNotEmpty()) Color(0xFFD32F2F) else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("dashboard_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings Icon",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onLogout,
                        modifier = Modifier.testTag("dashboard_logout_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Logout",
                            tint = Color.Red.copy(alpha = 0.8f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        val context = LocalContext.current
        val locationPermissionState = rememberMultiplePermissionsState(
            listOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )

        val weatherState by viewModel.weatherState.collectAsState()
        val isFetchingWeather by viewModel.isFetchingWeather.collectAsState()
        val lat by viewModel.currentLatitude.collectAsState()
        val lng by viewModel.currentLongitude.collectAsState()

        var isWeatherExpanded by remember { mutableStateOf(false) }
        var isGuideExpanded by remember { mutableStateOf(false) }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Item 1: Stat / Connection Banner
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isEnglish) "Mbolo, $currentUser!" else "Mbolo, $currentUser !",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = t("what_do"),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF2E7D32).copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "● Online • AI Actif",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }
                }
            }

            // Item 2: Dynamic Zone Alerts Section
            if (activeAlerts.isNotEmpty()) {
                item {
                    Card(
                        onClick = onNavigateToAlert,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .testTag("dashboard_alert_banner_card"),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFD32F2F) // Bold warning Red
                        )
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
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Warning Icon",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = t("alert_banner"),
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.25f))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "ACTIF",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = t("alerts_desc"),
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            activeAlerts.forEach { alert ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.15f))
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                        contentDescription = null,
                                        tint = Color(0xFFFFB74D), // Light Orange
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${alert.plantName} : ${alert.diseaseName} (${alert.casesCount} cas signalés)",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isEnglish) "Tap for map & prevention guide ➔" else "Ouvrir la carte & guide de prévention ➔",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Item 3: Grid Modules Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Focus Module 1: SCAN DISEASE
                    Card(
                        onClick = onNavigateToScan,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(115.dp)
                            .testTag("onboarding_scan_card"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.secondary
                                        )
                                    )
                                )
                                .padding(12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Column {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = t("scan_title"),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = t("scan_sub"),
                                    fontSize = 9.sp,
                                    color = Color.White.copy(alpha = 0.85f),
                                    maxLines = 1,
                                    lineHeight = 11.sp
                                )
                            }
                        }
                    }

                    // Focus Module 2: AI TUTORAT
                    Card(
                        onClick = onNavigateToTutorat,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(115.dp)
                            .testTag("onboarding_tutorat_card"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = t("tutorat_title"),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = t("tutorat_sub"),
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                    maxLines = 1,
                                    lineHeight = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            // Item 4: Real-time Weather & Location Map Card (Expandable)
            item {
                Card(
                    onClick = { isWeatherExpanded = !isWeatherExpanded },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .testTag("weather_map_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
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
                                Icon(
                                    imageVector = Icons.Default.WbSunny,
                                    contentDescription = "Weather Icon",
                                    tint = Color(0xFFFBC02D),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = t("weather_title"),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = if (isWeatherExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Expand details",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Weather mini-preview when collapsed
                        if (!isWeatherExpanded) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (weatherState != null) "${weatherState?.temperature}°C • ${weatherState?.description}" else t("weather_loading"),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        AnimatedVisibility(visible = isWeatherExpanded) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                // Weather Info Block
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = if (weatherState != null) "${weatherState?.temperature}°C" else "--°C",
                                            fontSize = 28.sp,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = weatherState?.description ?: t("weather_loading"),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    
                                    Column(horizontalAlignment = Alignment.End) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Air,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (weatherState != null) "${weatherState?.windspeed} km/h" else "-- km/h",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Lat: ${((lat * 10000).toInt() / 10000.0)} | Lng: ${((lng * 10000).toInt() / 10000.0)}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = t("map_title"),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                // Real-time WebView interactive Google Maps iframe API
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.DarkGray)
                                ) {
                                    AndroidView(
                                        factory = { ctx ->
                                            WebView(ctx).apply {
                                                settings.javaScriptEnabled = true
                                                settings.domStorageEnabled = true
                                                webViewClient = WebViewClient()
                                                loadUrl("https://maps.google.com/maps?q=$lat,$lng&z=14&output=embed")
                                            }
                                        },
                                        update = { webView ->
                                            webView.loadUrl("https://maps.google.com/maps?q=$lat,$lng&z=14&output=embed")
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Refresh GPS and fetch weather button
                                Button(
                                    onClick = {
                                        if (locationPermissionState.allPermissionsGranted) {
                                            val coords = getDashboardLocation(context)
                                            if (coords != null) {
                                                viewModel.updateLocation(coords.first, coords.second)
                                            }
                                        } else {
                                            locationPermissionState.launchMultiplePermissionRequest()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MyLocation,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = t("map_refresh"),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Item 5: Complete User Guide Card (Expandable Manual)
            item {
                Card(
                    onClick = { isGuideExpanded = !isGuideExpanded },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .testTag("user_guide_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
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
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = "Guide Icon",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = t("guide_title"),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = if (isGuideExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Expand details",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (!isGuideExpanded) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = t("guide_desc"),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }

                        AnimatedVisibility(visible = isGuideExpanded) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Spacer(modifier = Modifier.height(12.dp))

                                // Step 1
                                GuideStepItem(
                                    num = "1",
                                    icon = Icons.Default.CameraAlt,
                                    title = if (isEnglish) "Intelligent Disease Scanner (AI)" else "Scanner de Maladies Intelligent (IA)",
                                    desc = if (isEnglish) 
                                        "Click SCAN on the dashboard, snap a clear picture of any crop's leaves, and let Gemini 3.5 Flash identify symptoms and provide targeted organic & chemical remedies instantly." 
                                        else "Appuyez sur SCAN, prenez une photo nette des feuilles infectées de votre culture et laissez l'IA diagnostiquer la maladie et proposer des traitements biologiques et chimiques précis."
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Step 2
                                GuideStepItem(
                                    num = "2",
                                    icon = Icons.Default.Psychology,
                                    title = if (isEnglish) "Personal Agricultural Tutoring" else "Conseils Agricoles & Mentorat IA",
                                    desc = if (isEnglish) 
                                        "Use the TUTORING module to chat with our expert agronomist. Toggle the AI Mode in Settings: Quick for immediate clear actions, or Expert for highly detailed scientific guides." 
                                        else "Utilisez le module TUTORAT pour converser avec notre expert agronome. Choisissez le Mode de l'IA (Rapide pour des conseils directs, ou Scientifique pour des rapports d'experts)."
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Step 3
                                GuideStepItem(
                                    num = "3",
                                    icon = Icons.Default.WbSunny,
                                    title = if (isEnglish) "Real-Time Weather & Google Maps" else "Météo en Temps Réel & Cartographie GPS",
                                    desc = if (isEnglish) 
                                        "Get up-to-date regional wind & temperature telemetry. Click 'Refresh Location' to update your GPS and automatically center the live Google Map on your current farm location." 
                                        else "Obtenez les prévisions de température et de vent en direct. Cliquez sur 'Actualiser la Localisation' pour centrer instantanément la Google Map sur vos coordonnées réelles."
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Step 4
                                GuideStepItem(
                                    num = "4",
                                    icon = Icons.Default.Forum,
                                    title = if (isEnglish) "Interactive Community Forum" else "Forum Communautaire Interactif",
                                    desc = if (isEnglish) 
                                        "Connect with other Cameroonian farmers. Share PDF tutorials, publish crop demands or product offers, comment on posts, and rate contributions." 
                                        else "Collaborez avec les agriculteurs camerounais. Partagez des tutoriels PDF, publiez des offres/demandes d'achats, commentez et notez les publications de la communauté."
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Step 5
                                GuideStepItem(
                                    num = "5",
                                    icon = Icons.Default.Warning,
                                    title = if (isEnglish) "Phytosanitary Zone Alert System" else "Alerte Rouge Phytosanitaire de Zone",
                                    desc = if (isEnglish) 
                                        "If 3+ users in Foumbot or nearby sectors scan the same crop disease within a 7-day period, a prominent warning alert automatically broadcasts on everyone's dashboard to prevent catastrophic outbreaks." 
                                        else "Si au moins 3 scans identiques sont signalés dans la région sur une période de 7 jours, l'application déclenche automatiquement une Alerte de zone rouge pour prévenir les épidémies."
                                )
                            }
                        }
                    }
                }
            }

            // Item 6: Diagnostic History Section Header
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = t("history_screen"),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${scans.size} entries",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Item 7: History List items or empty placeholders
            if (scans.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.FilterVintage,
                                contentDescription = "Empty leaf history",
                                modifier = Modifier.size(50.dp),
                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = t("no_history"),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = t("do_scan"),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            } else {
                items(scans, key = { it.id }) { scan ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToDetail(scan) }
                            .testTag("scan_item_${scan.id}"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (scan.plantName == "TUTORAT") MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                                        else if (scan.plantName.contains("Cacao", true)) Color(0xFF5D4037)
                                        else if (scan.plantName.contains("Maïs", true)) Color(0xFFFFEB3B).copy(alpha = 0.5f)
                                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (scan.plantName == "TUTORAT") Icons.Default.School
                                    else if (scan.plantName.contains("Cacao", true)) Icons.Default.Forest
                                    else Icons.Default.Grass,
                                    contentDescription = "CropIcon",
                                    tint = if (scan.plantName == "TUTORAT") MaterialTheme.colorScheme.secondary
                                    else if (scan.plantName.contains("Maïs", true)) Color(0xFFE5A61C)
                                    else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (scan.plantName == "TUTORAT") t("tutorat_title") else "${scan.plantName} • ${scan.confidence}%",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (scan.plantName == "TUTORAT") MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(1.dp))
                                Text(
                                    text = scan.diseaseName,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = dateFormatter.format(Date(scan.timestamp)),
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }

                            Row {
                                if (scan.plantName != "TUTORAT") {
                                    IconButton(
                                        onClick = { onNavigateToChat(scan) },
                                        modifier = Modifier.testTag("chat_shortcut_${scan.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Forum,
                                            contentDescription = "Chat with Gemini shortcut",
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { viewModel.deleteScan(scan.id) },
                                    modifier = Modifier.testTag("delete_scan_${scan.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Delete scan log",
                                        tint = Color.Red.copy(alpha = 0.7f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Beautiful helper Composable for the User Manual steps
@Composable
fun GuideStepItem(
    num: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.6f))
            .padding(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = num,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = desc,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}
