package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.api.TokenManager
import com.example.config.BackendConfig
import com.example.ui.MainViewModel
import com.example.ui.Translations
import com.example.ui.theme.MaizeYellow
import com.example.ui.theme.PlantationGreen
import com.example.ui.theme.SeverityGreen
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    viewModel: MainViewModel,
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val isEnglish by viewModel.currentLanguageIsEnglish.collectAsState()
    val authState by viewModel.authState.collectAsState()
    var isRegisterMode by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var successMessage by remember { mutableStateOf("") }

    // Profil agriculteur (inscription)
    var commune by remember { mutableStateOf("") }
    var communeCode by remember { mutableStateOf("") }
    var cultures by remember { mutableStateOf("") }
    var langue by remember { mutableStateOf("fr") }
    var consentementAlertes by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    fun t(key: String): String = Translations.translate(key, isEnglish)

    // Afficher l'URL backend active
    val backendUrl = remember { BackendConfig.getBaseUrl(context) }
    val tokenExists = remember { TokenManager.getInstance(context).hasToken() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(PlantationGreen, Color(0xFF133621))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "MBOA AGRI",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 1.sp,
                        modifier = Modifier.testTag("auth_title")
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "GCD4F 2026 — Team 59 — Cameroun",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaizeYellow.copy(alpha = 0.9f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = backendUrl,
                        fontSize = 9.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    if (tokenExists) {
                        Text(
                            text = "✓ JWT stocké",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaizeYellow
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isRegisterMode) t("register_title") else t("login_title"),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = {
                        username = it
                        errorMessage = ""
                        successMessage = ""
                    },
                    label = { Text(t("username")) },
                    prefix = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("username_input")
                        .padding(bottom = 12.dp),
                    singleLine = true
                )

                // Téléphone obligatoire pour backend JWT
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = {
                        phoneNumber = it
                        errorMessage = ""
                        successMessage = ""
                    },
                    label = { Text(if (isEnglish) "Phone number" else "Numéro de téléphone") },
                    placeholder = { Text("6XXXXXXXX", fontSize = 13.sp) },
                    prefix = {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("phone_input")
                        .padding(bottom = 12.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorMessage = ""
                        successMessage = ""
                    },
                    label = { Text(t("password")) },
                    prefix = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("password_input")
                        .padding(bottom = 16.dp),
                    singleLine = true
                )

                // ===== Profil agriculteur (uniquement à l'inscription) =====
                if (isRegisterMode) {
                    OutlinedTextField(
                        value = commune,
                        onValueChange = {
                            commune = it
                            errorMessage = ""
                            successMessage = ""
                        },
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
                            .testTag("commune_input")
                            .padding(bottom = 12.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = communeCode,
                        onValueChange = {
                            communeCode = it.uppercase()
                            errorMessage = ""
                            successMessage = ""
                        },
                        label = { Text(if (isEnglish) "Commune code (e.g. CM-BFS-02)" else "Code commune (ex: CM-BFS-02)") },
                        placeholder = { Text("CM-BFS-02", fontSize = 13.sp) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("commune_code_input")
                            .padding(bottom = 12.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = cultures,
                        onValueChange = {
                            cultures = it
                            errorMessage = ""
                            successMessage = ""
                        },
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
                            .testTag("cultures_input")
                            .padding(bottom = 12.dp),
                        singleLine = true
                    )

                    Text(
                        text = t("langue"),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("langue_label")
                            .padding(bottom = 6.dp)
                    )
                    LanguagePickerRow(
                        selectedLangue = langue,
                        onSelect = { langue = it }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    ConsentAlertsRow(
                        checked = consentementAlertes,
                        onCheckedChange = { consentementAlertes = it },
                        label = t("consentement_alertes")
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                }

                if (authState is MainViewModel.AuthState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.padding(bottom = 12.dp))
                }

                if (errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    )
                }

                if (successMessage.isNotEmpty()) {
                    Text(
                        text = successMessage,
                        color = SeverityGreen,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    )
                }

                Button(
                    onClick = {
                        val uName = username.trim()
                        val pWord = password.trim()
                        val phone = phoneNumber.trim()
                        if (uName.isEmpty() || pWord.isEmpty()) {
                            errorMessage = if (isEnglish) "Please fill in all fields." else "Veuillez remplir tous les champs."
                            return@Button
                        }
                        if (phone.isEmpty() || phone.length < 8) {
                            errorMessage = if (isEnglish) "Phone number required (8+ digits) for JWT" else "Numéro de téléphone requis (8+ chiffres) pour JWT"
                            return@Button
                        }
                        if (isRegisterMode && (commune.isBlank() || cultures.isBlank())) {
                            errorMessage = t("fill_profile_fields")
                            return@Button
                        }

                        coroutineScope.launch {
                            if (isRegisterMode) {
                                val registered = viewModel.registerUser(
                                    username = uName,
                                    passwordRaw = pWord,
                                    phoneNumber = phone,
                                    commune = commune.trim(),
                                    communeCode = communeCode.trim(),
                                    cultures = cultures.trim(),
                                    langue = langue,
                                    consentementAlertes = consentementAlertes
                                )
                                if (registered) {
                                    viewModel.currentLanguageIsEnglish.value = langue == "en"
                                    successMessage = t("register_ok") + " JWT: " + if (TokenManager.getInstance(context).hasToken()) "OK" else "offline"
                                    errorMessage = ""
                                    kotlinx.coroutines.delay(1200)
                                    isRegisterMode = false
                                    password = ""
                                    successMessage = ""
                                } else {
                                    errorMessage = t("register_err")
                                }
                            } else {
                                val loggedIn = viewModel.loginUser(uName, pWord, phone)
                                if (loggedIn) {
                                    viewModel.currentUser.value = uName
                                    onAuthSuccess()
                                } else {
                                    errorMessage = t("login_err")
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("submit_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        text = if (isRegisterMode) t("register_btn") else t("login_btn"),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (isRegisterMode) t("has_account") else t("no_account"),
                    color = MaterialTheme.colorScheme.secondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            isRegisterMode = !isRegisterMode
                            errorMessage = ""
                            successMessage = ""
                        }
                        .padding(8.dp)
                        .testTag("toggle_auth_mode")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Info JWT pour tests terrain
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = if (isEnglish) "JWT Auth Test" else "Test Auth JWT",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isEnglish) "Flow: register -> login -> token stored -> /api/scans with Bearer" else "Flux: inscription -> connexion -> token stocké -> /api/scans avec Bearer",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Backend: $backendUrl",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                Text(
                    text = t("share_via") + " :",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SocialIconBadge(letter = "W", bgColor = Color(0xFF25D366))
                    SocialIconBadge(letter = "F", bgColor = Color(0xFF1877F2))
                    SocialIconBadge(letter = "I", bgColor = Color(0xFF0077B5))
                    SocialIconBadge(letter = "T", bgColor = Color(0xFF1DA1F2))
                }
            }
        }
    }
}

@Composable
fun SocialIconBadge(letter: String, bgColor: Color) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = letter,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}
