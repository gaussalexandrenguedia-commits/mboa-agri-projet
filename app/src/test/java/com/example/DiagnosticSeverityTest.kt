package com.example

import com.example.data.DiagnosticSeverity
import com.example.data.ScanResultEntity
import com.example.data.diagnosticSeverity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Tests du code couleur strict : VERT = sain, ORANGE = attention, ROUGE = urgent.
 */
class DiagnosticSeverityTest {

    private fun scan(disease: String, symptoms: String = "") = ScanResultEntity(
        plantName = "Cacao",
        diseaseName = disease,
        confidence = 90,
        symptoms = symptoms,
        treatmentLocal = "",
        treatmentChemical = ""
    )

    @Test
    fun `plante saine donne le niveau vert SAIN`() {
        assertEquals(DiagnosticSeverity.SAIN, scan("Feuille saine", "Aucun signe de maladie").diagnosticSeverity())
        assertEquals(DiagnosticSeverity.SAIN, scan("Healthy plant", "No disease detected").diagnosticSeverity())
    }

    @Test
    fun `maladie ordinaire donne le niveau orange ATTENTION`() {
        assertEquals(
            DiagnosticSeverity.ATTENTION,
            scan("Taches foliaires", "Petites taches brunes isolees").diagnosticSeverity()
        )
    }

    @Test
    fun `maladie grave donne le niveau rouge URGENT`() {
        assertEquals(DiagnosticSeverity.URGENT, scan("Pourriture brune des cabosses (Phytophthora)").diagnosticSeverity())
        assertEquals(DiagnosticSeverity.URGENT, scan("Mosaïque du Manioc (CMD)").diagnosticSeverity())
    }

    @Test
    fun `les accents ne cassent pas la classification`() {
        assertEquals(DiagnosticSeverity.URGENT, scan("Flétrissement bactérien").diagnosticSeverity())
        assertEquals(DiagnosticSeverity.URGENT, scan("Maladie sévère").diagnosticSeverity())
    }

    @Test
    fun `un enregistrement TUTORAT n est pas un diagnostic`() {
        val tutorat = ScanResultEntity(
            plantName = "TUTORAT",
            diseaseName = "Conseils Agricoles",
            confidence = 100,
            symptoms = "Question générale",
            treatmentLocal = "",
            treatmentChemical = ""
        )
        assertNull(tutorat.diagnosticSeverity())
    }
}
