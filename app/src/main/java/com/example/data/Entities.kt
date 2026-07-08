package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_results")
data class ScanResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val plantName: String,
    val diseaseName: String,
    val confidence: Int,
    val symptoms: String,
    val treatmentLocal: String,
    val treatmentChemical: String,
    val timestamp: Long = System.currentTimeMillis(),
    val chatHistoryJson: String = "", // Stores conversation history for this scan
    val latitude: Double? = null,
    val longitude: Double? = null
)

@Entity(tableName = "soil_records")
data class SoilRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nitrogen: String, // "Bas" / "Moyen" / "Élevé"
    val phosphorus: String, // "Bas" / "Moyen" / "Élevé"
    val potassium: String, // "Bas" / "Moyen" / "Élevé"
    val humidity: Float, // 0 - 100%
    val temperature: Float, // °C
    val ph: Float, // 0 - 14
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val passwordHash: String,
    val createdAt: String = ""
)

@Entity(tableName = "forum_posts")
data class ForumPostEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val author: String,
    val type: String, // "post_pdf" / "post_offer" / "post_demand" / "post_question"
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
