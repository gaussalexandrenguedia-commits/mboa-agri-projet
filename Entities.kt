package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// 1. TABLE DES SCANS (Pour le diagnostic au champ)
@Entity(tableName = "scan_results")
data class ScanResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val plantName: String,
    val pathologieId: Int? = null, // Lien avec ton catalogue SQL
    val confidence: Int,
    val symptoms: String,
    val treatmentApplied: Boolean = false, // Retour terrain (Lot 6)
    val timestamp: Long = System.currentTimeMillis(),
    val latitude: Double? = null,
    val longitude: Double? = null,
    
    // GESTION DE LA SYNCHRONISATION
    // PENDING = à envoyer, SYNCED = envoyé, FAILED = erreur
    val syncStatus: String = "PENDING", 
    val photoPath: String? = null // Chemin de la photo sur le téléphone
)

// 2. TABLE UTILISATEUR (Profil complet)
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val passwordHash: String,
    val commune: String = "", // Pour les alertes territoriales
    val village: String = "",
    val cultures: String = "", // Ex: "Tomate, Manioc"
    val consentementAlertes: Boolean = false,
    val createdAt: String = ""
)

// 3. TABLE DES RELEVÉS DE SOL
@Entity(tableName = "soil_records")
data class SoilRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nitrogen: String,
    val phosphorus: String,
    val potassium: String,
    val humidity: Float,
    val temperature: Float,
    val ph: Float,
    val timestamp: Long = System.currentTimeMillis()
)

// 4. TABLES DU FORUM (On les garde pour ne rien casser)
@Entity(tableName = "forum_posts")
data class ForumPostEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val author: String,
    val type: String,
    val content: String,
    val rating: Float = 0f,
    val ratingCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "forum_comments")
data class ForumCommentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val postId: Long,
    val author: String,
    val content: String,
    val date: String,
    val timestamp: Long = System.currentTimeMillis()
)
