package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.api.CommuneResponse

/**
 * Composants partagés du profil agriculteur (inscription + paramètres) :
 * commune, cultures pratiquées, langue et consentement aux alertes.
 */

@Composable
fun SelectablePill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Sélecteur de langue du profil : « fr » ou « en ». */
@Composable
fun LanguagePickerRow(
    selectedLangue: String,
    onSelect: (String) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SelectablePill(text = "Français", selected = selectedLangue == "fr") { onSelect("fr") }
        SelectablePill(text = "English", selected = selectedLangue == "en") { onSelect("en") }
    }
}

/** Interrupteur de consentement aux alertes phytosanitaires. */
@Composable
fun ConsentAlertsRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Sélecteur de commune alimenté par GET /api/communes (contrat mobile officiel).
 *
 * La liste officielle est la seule source fiable des `commune_code` envoyés au backend :
 * un code inventé provoque un 422 « Code commune inconnu » sur POST /api/scans.
 * Si la liste n'est pas disponible (hors ligne, ou avant authentification car la route
 * exige un JWT), la saisie manuelle du code reste possible.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunePickerField(
    communes: List<CommuneResponse>,
    communeName: String,
    communeCode: String,
    isEnglish: Boolean,
    isLoading: Boolean,
    onSelect: (CommuneResponse) -> Unit,
    onRefresh: () -> Unit,
    onManualCodeChange: (String) -> Unit,
    testTagPrefix: String = "commune"
) {
    var expanded by remember { mutableStateOf(false) }
    var manualMode by remember { mutableStateOf(false) }

    val summary = when {
        communeCode.isNotBlank() && communeName.isNotBlank() -> "$communeName — $communeCode"
        communeCode.isNotBlank() -> communeCode
        else -> ""
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ExposedDropdownMenuBox(
            expanded = expanded && communes.isNotEmpty(),
            onExpandedChange = { if (communes.isNotEmpty()) expanded = !expanded },
            modifier = Modifier.weight(1f)
        ) {
            OutlinedTextField(
                value = summary,
                onValueChange = {},
                readOnly = true,
                label = { Text(if (isEnglish) "Commune (official list)" else "Commune (liste officielle)") },
                placeholder = {
                    Text(
                        if (communes.isEmpty()) {
                            if (isEnglish) "No list loaded" else "Liste non chargée"
                        } else {
                            if (isEnglish) "Choose a commune" else "Choisir une commune"
                        },
                        fontSize = 13.sp
                    )
                },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .testTag("${testTagPrefix}_picker")
            )
            ExposedDropdownMenu(
                expanded = expanded && communes.isNotEmpty(),
                onDismissRequest = { expanded = false }
            ) {
                communes.forEach { commune ->
                    DropdownMenuItem(
                        text = { Text("${commune.name} — ${commune.code}", fontSize = 13.sp) },
                        onClick = {
                            onSelect(commune)
                            expanded = false
                            manualMode = false
                        }
                    )
                }
            }
        }

        IconButton(onClick = onRefresh, enabled = !isLoading) {
            Icon(
                imageVector = if (isLoading) Icons.Default.HourglassEmpty else Icons.Default.Refresh,
                contentDescription = if (isEnglish) "Reload communes" else "Recharger les communes",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }

    if (communes.isEmpty()) {
        Text(
            text = if (isEnglish) {
                "Official list unavailable (offline or not signed in): /api/communes requires a JWT."
            } else {
                "Liste officielle indisponible (hors ligne ou non connecté) : /api/communes exige un JWT."
            },
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 2.dp)
        )
    }

    TextButton(
        onClick = { manualMode = !manualMode },
        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)
    ) {
        Text(
            text = if (manualMode) {
                if (isEnglish) "Hide manual entry" else "Masquer la saisie manuelle"
            } else {
                if (isEnglish) "Enter the code manually" else "Saisir le code manuellement"
            },
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }

    if (manualMode) {
        OutlinedTextField(
            value = communeCode,
            onValueChange = { onManualCodeChange(it.uppercase()) },
            label = { Text(if (isEnglish) "Commune code (e.g. CM-001)" else "Code commune (ex. CM-001)") },
            placeholder = { Text("CM-001", fontSize = 13.sp) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("${testTagPrefix}_code_manual"),
            singleLine = true
        )
    }
}
