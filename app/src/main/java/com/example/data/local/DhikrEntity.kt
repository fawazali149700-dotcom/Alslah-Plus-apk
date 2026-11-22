package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dhikr_items")
data class DhikrEntity(
    @PrimaryKey val id: String,
    val titleAr: String,
    val textAr: String,
    val categoryAr: String,
    val reciterAr: String,
    val audioUrl: String,
    val fallbackAudioUrl: String,
    val localFilePath: String? = null,
    val isDownloaded: Boolean = false,
    val isDownloading: Boolean = false,
    val isEnabled: Boolean = true,
    val isCustom: Boolean = false,
    val lastPlayedAt: Long = 0L
)
