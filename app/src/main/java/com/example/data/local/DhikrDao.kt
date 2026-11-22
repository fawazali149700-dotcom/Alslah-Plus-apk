package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DhikrDao {
    @Query("SELECT * FROM dhikr_items ORDER BY isCustom DESC, categoryAr ASC, titleAr ASC")
    fun observeAll(): Flow<List<DhikrEntity>>

    @Query("SELECT * FROM dhikr_items")
    suspend fun getAllOnce(): List<DhikrEntity>

    @Query("SELECT * FROM dhikr_items WHERE isEnabled = 1")
    suspend fun getEnabledOnce(): List<DhikrEntity>

    @Query("SELECT * FROM dhikr_items WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): DhikrEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnore(items: List<DhikrEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: DhikrEntity)

    @Query("UPDATE dhikr_items SET isEnabled = :enabled WHERE id = :id")
    suspend fun updateEnabled(id: String, enabled: Boolean)

    @Query("UPDATE dhikr_items SET audioUrl = :audioUrl, fallbackAudioUrl = :fallbackUrl WHERE id = :id")
    suspend fun updateAudioUrls(id: String, audioUrl: String, fallbackUrl: String)

    @Query("UPDATE dhikr_items SET isDownloading = :downloading WHERE id = :id")
    suspend fun setDownloading(id: String, downloading: Boolean)

    @Query(
        "UPDATE dhikr_items SET isDownloaded = 1, isDownloading = 0, localFilePath = :localPath, reciterAr = :reciterAr, durationMs = :durationMs WHERE id = :id"
    )
    suspend fun markDownloaded(id: String, localPath: String, reciterAr: String, durationMs: Long)

    @Query("UPDATE dhikr_items SET isDownloaded = 0, isDownloading = 0, localFilePath = NULL, durationMs = 0 WHERE id = :id")
    suspend fun clearDownload(id: String)

    @Query("UPDATE dhikr_items SET lastPlayedAt = :timestamp WHERE id = :id")
    suspend fun markLastPlayed(id: String, timestamp: Long)

    @Query("UPDATE dhikr_items SET isDownloaded = 0, isDownloading = 0, localFilePath = NULL, audioUrl = '', fallbackAudioUrl = '', reciterAr = '', durationMs = 0")
    suspend fun clearAllAudioMetadata()

    @Query("DELETE FROM dhikr_items WHERE id = :id AND isCustom = 1")
    suspend fun deleteCustomById(id: String)
}
