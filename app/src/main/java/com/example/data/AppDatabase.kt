package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@androidx.room.TypeConverters(Converters::class)
@Database(
    entities = [
        ScanResultEntity::class,
        SoilRecordEntity::class,
        UserEntity::class,
        ForumPostEntity::class,
        ForumCommentEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scanResultDao(): ScanResultDao
    abstract fun soilRecordDao(): SoilRecordDao
    abstract fun userDao(): UserDao
    abstract fun forumPostDao(): ForumPostDao
    abstract fun forumCommentDao(): ForumCommentDao

    companion object {
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE scan_results ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'PENDING'")
                database.execSQL("ALTER TABLE users ADD COLUMN commune TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE users ADD COLUMN cultures TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE users ADD COLUMN langue TEXT NOT NULL DEFAULT 'fr'")
                database.execSQL("ALTER TABLE users ADD COLUMN consentementAlertes INTEGER NOT NULL DEFAULT 0")
            }
        }

        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "mboa_agri_db"
            ).addMigrations(MIGRATION_3_4).fallbackToDestructiveMigration(true).build()
                .also { INSTANCE = it }
        }
    }
}
