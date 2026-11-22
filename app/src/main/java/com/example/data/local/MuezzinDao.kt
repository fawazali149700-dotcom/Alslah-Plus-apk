package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MuezzinDao {
    @Query("SELECT * FROM muezzins ORDER BY regionAr ASC, nameAr ASC")
    fun observeAll(): Flow<List<MuezzinEntity>>

    @Query("SELECT * FROM muezzins")
    suspend fun getAllOnce(): List<MuezzinEntity>

    @Query("SELECT * FROM muezzins WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): MuezzinEntity?

    @Query("SELECT * FROM muezzins WHERE downloadManagerId = :downloadId LIMIT 1")
    suspend fun getByDownloadId(downloadId: Long): MuezzinEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnore(items: List<MuezzinEntity>)

    @Query(
        "UPDATE muezzins SET isDownloading = :isDownloading, downloadProgress = :progress, downloadManagerId = :downloadId WHERE id = :id"
    )
    suspend fun updateDownloadStarted(
        id: String,
        isDownloading: Boolean,
        progress: Int,
        downloadId: Long
    )

    @Query("UPDATE muezzins SET downloadProgress = :progress WHERE id = :id")
    suspend fun updateProgress(id: String, progress: Int)

    @Query(
        "UPDATE muezzins SET isDownloaded = 1, isDownloading = 0, downloadProgress = 100, localFilePath = :localPath, fileSizeBytes = :fileSizeBytes, downloadManagerId = -1 WHERE id = :id"
    )
    suspend fun markDownloaded(id: String, localPath: String, fileSizeBytes: Long)

    @Query(
        "UPDATE muezzins SET isDownloading = 0, downloadProgress = 0, downloadManagerId = -1 WHERE id = :id"
    )
    suspend fun markDownloadFailed(id: String)

    @Query(
        "UPDATE muezzins SET isDownloaded = 0, isDownloading = 0, downloadProgress = 0, localFilePath = NULL, fileSizeBytes = 0, downloadManagerId = -1 WHERE id = :id"
    )
    suspend fun clearDownload(id: String)
}
