package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.api.ApiClient
import com.example.api.TokenManager
import com.example.config.BackendConfig
import com.example.ui.MainViewModel
import com.example.ui.Translations
import com.example.ui.theme.SeverityGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val isEnglish by viewModel.currentLanguageIsEnglish.collectAsState()
    val isExpert by viewModel.currentAiModeExpert.collectAsState()
    val profile by viewModel.currentProfile.collectAsState()

    var commune by remember { mutableStateOf("") }
    var communeCode by remember { mutableStateOf("") }
    var cultures by remember { mutableStateOf("") }
    var langue by remember { mutableStateOf("fr") }
    var consentementAlertes by remember { mutableStateOf(false) }
    var profileLoaded by remember { mutableStateOf(false) }
    var profileSavedTick by remember { mutableStateOf(false) }

    // Backend config
    var customBackendUrl by remember { mutableStateOf(BackendConfig.getBaseUrl(context)) }
    var backendSavedTick by remember { mutableStateOf(false) }
    var tokenInfo by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadCurrentUserProfile()
        val tm = TokenManager.getInstance(context)
        tokenInfo = if (tm.hasToken()) {
            "JWT présent (user: ${tm.getUsername()}, phone: ${tm.getPhone()}, commune_code: ${tm.getCommuneCode()})"
        } else {
            "Aucun JWT - authentifiez-vous"
        }
    }
    LaunchedEffect(profile) {
        val loadedProfile = profile
        if (!profileLoaded && loadedProfile != null) {
            commune = loadedProfile.commune
            communeCode = loadedProfile.communeCode
            cultures = loadedProfile.cultures
            langue = loadedProfile.langue.ifBlank { "fr" }
            consentementAlertes = loadedProfile.consentementAlertes
            profileLoaded = true
        }
    }
    LaunchedEffect(profileSavedTick) {
        if (profileSavedTick) {
            kotlinx.coroutines.delay(2000)
            profileSavedTick = false
        }
    }
    LaunchedEffect(backendSavedTick) {
        if (backendSavedTick) {
            kotlinx.coroutines.delay(2000)
            backendSavedTick = false
        }
    }

    fun t(key: String): String = Translations.translate(key, isEnglish)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t("settings_screen"), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("settings_back_button")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Backend Production URL Configuration
            SettingsSectionCard(
                title = if (isEnglish) "Backend Configuration (Production)" else "Configuration Backend (Production)",
                icon = Icons.Default.Settings
            ) {
                Text(
                    text = if (isEnglish) "Default (build): ${com.example.BuildConfig.BACKEND_BASE_URL}" else "Défaut (build): ${com.example.BuildConfig.BACKEND_BASE_URL}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = BackendConfig.getConfigSummary(context),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = customBackendUrl,
                    onValueChange = { customBackendUrl = it },
                    label = { Text(if (isEnglish) "Production URL (https://.../ with trailing /)" else "URL de production (https://.../ avec / final)") },
                    placeholder = { Text("https://api-mboa-agri.railway.app/", fontSize = 11.sp) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("backend_url_input"),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            if (customBackendUrl.isNotBlank()) {
                                BackendConfig.setCustomBaseUrl(context, customBackendUrl)
                                ApiClient.init(context)
                                ApiClient.rebuild()
                                backendSavedTick = true
                                tokenInfo = "URL mise à jour: ${BackendConfig.getBaseUrl(context)} - reconnectez-vous pour nouveau JWT"
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isEnglish) "Save URL" else "Sauvegarder URL", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = {
                            BackendConfig.clearCustomUrl(context)
                            customBackendUrl = BackendConfig.getBaseUrl(context)
                            ApiClient.init(context)
                            ApiClient.rebuild()
                            backendSavedTick = true
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isEnglish) "Reset" else "Réinitialiser", fontSize = 12.sp)
                    }
                }
                if (backendSavedTick) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "✓ " + if (isEnglish) "Backend URL saved, rebuild ApiClient" else "URL backend sauvegardée, ApiClient reconstruit",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SeverityGreen
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isEnglish) "Instructions: set BACKEND_BASE_URL env at build OR configure here at runtime. Example: export BACKEND_BASE_URL=\"https://your-production-url/\" && ./gradlew assembleDebug"
                    else "Instructions: définir BACKEND_BASE_URL à la compilation OU configurer ici à l'exécution. Exemple: export BACKEND_BASE_URL=\"https://votre-url-prod/\" && ./gradlew assembleDebug",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = tokenInfo,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            // Profil agriculteur
            SettingsSectionCard(
                title = t("profile_section"),
                icon = Icons.Default.Agriculture
            ) {
                OutlinedTextField(
                    value = commune,
                    onValueChange = { commune = it },
                    label = { Text(t("commune")) },
                    placeholder = { Text(t("commune_hint"), fontSize = 13.sp) },
                    prefix = {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_commune_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = communeCode,
                    onValueChange = { communeCode = it.uppercase() },
                    label = { Text(if (isEnglish) "Commune code (e.g. CM-BFS-02)" else "Code commune (ex: CM-BFS-02)") },
                    placeholder = { Text("CM-BFS-02", fontSize = 13.sp) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_commune_code_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = cultures,
                    onValueChange = { cultures = it },
                    label = { Text(t("cultures")) },
                    placeholder = { Text(t("cultures_hint"), fontSize = 13.sp) },
                    prefix = {
                        Icon(
                            imageVector = Icons.Default.Grass,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_cultures_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = t("langue"),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(6.dp))
                LanguagePickerRow(selectedLangue = langue, onSelect = { langue = it })

                Spacer(modifier = Modifier.height(12.dp))

                ConsentAlertsRow(
                    checked = consentementAlertes,
                    onCheckedChange = { consentementAlertes = it },
                    label = t("consentement_alertes")
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        viewModel.updateUserProfileWithCode(
                            commune = commune.trim(),
                            communeCode = communeCode.trim(),
                            cultures = cultures.trim(),
                            langue = langue,
                            consentementAlertes = consentementAlertes
                        )
                        viewModel.currentLanguageIsEnglish.value = langue == "en"
                        profileSavedTick = true
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_profile_button")
                ) {
                    Text(t("save_profile"), fontWeight = FontWeight.Bold)
                }

                if (profileSavedTick) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "✓ " + t("profile_saved"),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SeverityGreen
                    )
                }
            }

            SettingsSectionCard(
                title = t("language"),
                icon = Icons.Default.Language
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { viewModel.currentLanguageIsEnglish.value = false },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isEnglish) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.4f),
                            contentColor = if (!isEnglish) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("lang_fr_button")
                    ) {
                        Text("Français", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { viewModel.currentLanguageIsEnglish.value = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isEnglish) MaterialTheme.colorScheme.secondary else Color.LightGray.copy(alpha = 0.4f),
                            contentColor = if (isEnglish) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("lang_en_button")
                    ) {
                        Text("English", fontWeight = FontWeight.Bold)
                    }
                }
            }

            SettingsSectionCard(
                title = t("ai_mode"),
                icon = Icons.Default.Psychology
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { viewModel.currentAiModeExpert.value = false },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isExpert) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.4f),
                            contentColor = if (!isExpert) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ai_quick_button")
                    ) {
                        Text(t("quick"), fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { viewModel.currentAiModeExpert.value = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isExpert) MaterialTheme.colorScheme.secondary else Color.LightGray.copy(alpha = 0.4f),
                            contentColor = if (isExpert) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ai_expert_button")
                    ) {
                        Text(t("expert"), fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (isExpert) t("expert_desc") else t("quick_desc"),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            SettingsSectionCard(
                title = t("about"),
                icon = Icons.Default.Info
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "🛡️ " + t("about_app"), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(text = "🇨🇲 " + t("about_team"), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                    Text(text = "⚡ " + t("about_ai"), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                    Text(text = "🎓 École Nationale Supérieure Polytechnique de Douala (ENSPD) / Yaoundé", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "Backend: ${BackendConfig.getBaseUrl(context)}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    Text(text = "Build: ${com.example.BuildConfig.BACKEND_BASE_URL}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                }
            }

            SettingsSectionCard(
                title = t("share_via"),
                icon = Icons.Default.Share
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SocialIconBadge(letter = "W", bgColor = Color(0xFF25D366))
                        Text(text = "WhatsApp", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SocialIconBadge(letter = "F", bgColor = Color(0xFF1877F2))
                        Text(text = "Facebook", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SocialIconBadge(letter = "I", bgColor = Color(0xFF0077B5))
                        Text(text = "LinkedIn", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SocialIconBadge(letter = "T", bgColor = Color(0xFF1DA1F2))
                        Text(text = "Twitter / X", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}
