package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.Translations
import com.example.ui.theme.MaizeYellow
import com.example.ui.theme.PlantationGreen
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    viewModel: MainViewModel,
    onAuthSuccess: () -> Unit
) {
    val isEnglish by viewModel.currentLanguageIsEnglish.collectAsState()
    var isRegisterMode by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var successMessage by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    fun t(key: String): String = Translations.translate(key, isEnglish)

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
            // Elegant top decorative curve with Cameroonian organic plantation gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
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
                        color = Color(0xFF2E7D32),
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
                        if (uName.isEmpty() || pWord.isEmpty()) {
                            errorMessage = if (isEnglish) "Please fill in all fields." else "Veuillez remplir tous les champs."
                            return@Button
                        }

                        coroutineScope.launch {
                            if (isRegisterMode) {
                                val registered = viewModel.registerUser(uName, pWord)
                                if (registered) {
                                    successMessage = t("register_ok")
                                    errorMessage = ""
                                    // Switch mode cleanly after delay
                                    kotlinx.coroutines.delay(1000)
                                    isRegisterMode = false
                                    password = ""
                                    successMessage = ""
                                } else {
                                    errorMessage = t("register_err")
                                }
                            } else {
                                val loggedIn = viewModel.loginUser(uName, pWord)
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

                Spacer(modifier = Modifier.height(40.dp))

                // Sharing options inside
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
