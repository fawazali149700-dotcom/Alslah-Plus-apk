package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "muezzins")
data class MuezzinEntity(
    @PrimaryKey val id: String,
    val nameAr: String,
    val regionAr: String,
    val mosqueAr: String,
    val audioUrl: String,
    val estimatedSizeLabel: String,
    val fileSizeBytes: Long = 0L,
    val localFilePath: String? = null,
    val isDownloaded: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadProgress: Int = 0,
    val downloadManagerId: Long = -1L
)
