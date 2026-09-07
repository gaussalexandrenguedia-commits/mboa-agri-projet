package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.data.DiagnosticSeverity

/**
 * Code couleur STRICT de MBOA AGRI — une seule source de vérité pour toute l'application :
 *
 *  - VERT   = SAIN      (plante saine)
 *  - ORANGE = ATTENTION (maladie à surveiller)
 *  - ROUGE  = URGENT    (intervention immédiate)
 *
 * Toute interface représentant un état sanitaire doit utiliser ces couleurs.
 */
val SeverityGreen = Color(0xFF2E7D32)
val SeverityOrange = Color(0xFFEF6C00)
val SeverityRed = Color(0xFFD32F2F)

val DiagnosticSeverity.severityColor: Color
    get() = when (this) {
        DiagnosticSeverity.SAIN -> SeverityGreen
        DiagnosticSeverity.ATTENTION -> SeverityOrange
        DiagnosticSeverity.URGENT -> SeverityRed
    }

fun DiagnosticSeverity.label(isEnglish: Boolean): String =
    if (isEnglish) labelEn else labelFr
