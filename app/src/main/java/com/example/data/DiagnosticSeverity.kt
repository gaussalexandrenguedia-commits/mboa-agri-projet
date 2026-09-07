package com.example.data

import java.text.Normalizer

/**
 * Code couleur STRICT de MBOA AGRI, appliqué partout dans l'application :
 *
 *  - VERT   (SAIN)      : plante saine, aucune maladie détectée.
 *  - ORANGE (ATTENTION) : maladie détectée, à surveiller et traiter.
 *  - ROUGE  (URGENT)    : maladie grave ou très contagieuse, intervention immédiate.
 */
enum class DiagnosticSeverity(val labelFr: String, val labelEn: String) {
    SAIN("SAIN", "HEALTHY"),
    ATTENTION("ATTENTION", "ATTENTION"),
    URGENT("URGENT", "URGENT")
}

/** Minuscules + suppression des accents pour une classification robuste en FR/EN. */
private fun normalizeForMatching(text: String): String =
    Normalizer.normalize(text, Normalizer.Form.NFD)
        .replace("\\p{Mn}+".toRegex(), "")
        .lowercase()

private val healthyKeywords = listOf(
    "saine", "sain", "healthy",
    "aucune maladie", "pas de maladie", "no disease",
    "aucun signe", "no symptoms", "bonne sante"
)

private val urgentKeywords = listOf(
    "pourriture", "fletrissement", "foudroy", "mortel", "mortalite", "epidemie",
    "grave", "severe", "urgent", "devastat", "necrose", "chancre",
    "virus", "viral", "mosaique", "bacterien", "bacteriose", "anthracnose"
)

/**
 * Classifie un scan selon le code couleur strict, à partir du nom de la maladie
 * et des symptômes détectés.
 *
 * Renvoie `null` pour les enregistrements qui ne sont pas des diagnostics
 * (par exemple les questions du TUTORAT) afin que l'interface n'affiche
 * jamais de couleur trompeuse pour ces entrées.
 */
fun ScanResultEntity.diagnosticSeverity(): DiagnosticSeverity? {
    if (plantName.equals("TUTORAT", ignoreCase = true)) return null
    val haystack = normalizeForMatching("$diseaseName $symptoms")
    if (healthyKeywords.any { haystack.contains(it) }) return DiagnosticSeverity.SAIN
    if (urgentKeywords.any { haystack.contains(it) }) return DiagnosticSeverity.URGENT
    return DiagnosticSeverity.ATTENTION
}
