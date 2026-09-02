package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ScanResultEntity::class, 
        SoilRecordEntity::class, 
        UserEntity::class, 
        ForumPostEntity::class, 
        ForumCommentEntity::class
    ], 
    version = 4, // Très important : on passe à la version 4
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scanResultDao(): ScanResultDao
    abstract fun soilRecordDao(): SoilRecordDao
    abstract fun userDao(): UserDao
    abstract fun forumPostDao(): ForumPostDao
    abstract fun forumCommentDao(): ForumCommentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mboa_agri_db"
                )
                .fallbackToDestructiveMigration(true) // Sécurité pour les mises à jour
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
