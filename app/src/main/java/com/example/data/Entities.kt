package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class SyncStatus { PENDING, SYNCED, FAILED }

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
    val chatHistoryJson: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val syncStatus: SyncStatus = SyncStatus.PENDING
)

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

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val passwordHash: String,
    val createdAt: String = "",
    val commune: String = "",
    val cultures: String = "",
    val langue: String = "fr",
    val consentementAlertes: Boolean = false
)

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

fun ScanResultEntity.toSyncStatusValue(): String = syncStatus.name

fun String.toSyncStatus(): SyncStatus = runCatching { SyncStatus.valueOf(this) }
    .getOrDefault(SyncStatus.PENDING)
