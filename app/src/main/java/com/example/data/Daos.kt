package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanResultDao {
    @Query("SELECT * FROM scan_results ORDER BY timestamp DESC")
    fun getAllScans(): Flow<List<ScanResultEntity>>

    @Query("SELECT * FROM scan_results WHERE id = :id LIMIT 1")
    suspend fun getScanById(id: Long): ScanResultEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(scan: ScanResultEntity): Long

    @Update
    suspend fun updateScan(scan: ScanResultEntity)

    @Query("DELETE FROM scan_results WHERE id = :id")
    suspend fun deleteScanById(id: Long)
}

@Dao
interface SoilRecordDao {
    @Query("SELECT * FROM soil_records ORDER BY timestamp DESC")
    fun getAllSoilRecords(): Flow<List<SoilRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSoilRecord(record: SoilRecordEntity): Long

    @Query("DELETE FROM soil_records WHERE id = :id")
    suspend fun deleteSoilRecordById(id: Long)
}

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUser(user: UserEntity): Long
}

@Dao
interface ForumPostDao {
    @Query("SELECT * FROM forum_posts ORDER BY id DESC")
    fun getAllPosts(): Flow<List<ForumPostEntity>>

    @Query("SELECT * FROM forum_posts WHERE id = :id LIMIT 1")
    suspend fun getPostById(id: Long): ForumPostEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: ForumPostEntity): Long

    @Update
    suspend fun updatePost(post: ForumPostEntity)
}

@Dao
interface ForumCommentDao {
    @Query("SELECT * FROM forum_comments WHERE postId = :postId ORDER BY id ASC")
    fun getCommentsByPostId(postId: Long): Flow<List<ForumCommentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: ForumCommentEntity): Long
}
