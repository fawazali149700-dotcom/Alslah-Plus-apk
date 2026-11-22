package com.example.data.repository

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import com.example.data.local.MuezzinDao
import com.example.data.local.MuezzinEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MuezzinRepository(
    private val context: Context,
    private val muezzinDao: MuezzinDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    val muezzinsFlow: Flow<List<MuezzinEntity>> = muezzinDao.observeAll()

    suspend fun ensureSeeded() {
        val defaults = listOf(
            MuezzinEntity(
                id = "makkah_mullah",
                nameAr = "الشيخ علي أحمد ملا",
                regionAr = "الحرم المكي — السعودية",
                mosqueAr = "المسجد الحرام بمكة المكرمة",
                audioUrl = "https://www.islamcan.com/audio/adhan/azan1.mp3",
                estimatedSizeLabel = "1.4 MB"
            ),
            MuezzinEntity(
                id = "madinah_bukhari",
                nameAr = "الشيخ عصام بخاري",
                regionAr = "الحرم المدني — السعودية",
                mosqueAr = "المسجد النبوي بالمدينة المنورة",
                audioUrl = "https://www.islamcan.com/audio/adhan/azan2.mp3",
                estimatedSizeLabel = "1.2 MB"
            ),
            MuezzinEntity(
                id = "alaqsa_quds",
                nameAr = "أذان المسجد الأقصى المبارك",
                regionAr = "المسجد الأقصى — فلسطين",
                mosqueAr = "المسجد الأقصى بالقدس الشريف",
                audioUrl = "https://www.islamcan.com/audio/adhan/azan3.mp3",
                estimatedSizeLabel = "1.1 MB"
            ),
            MuezzinEntity(
                id = "egypt_rifaat",
                nameAr = "الشيخ محمد رفعت",
                regionAr = "مصر — التلاوة الكلاسيكية",
                mosqueAr = "أذان مصري عتيق",
                audioUrl = "https://www.islamcan.com/audio/adhan/azan4.mp3",
                estimatedSizeLabel = "1.5 MB"
            ),
            MuezzinEntity(
                id = "egypt_abdulbasit",
                nameAr = "الشيخ عبد الباسط عبد الصمد",
                regionAr = "مصر — صعيد مصر",
                mosqueAr = "أذان خاشع بصوت الشيخ عبد الباسط",
                audioUrl = "https://www.islamcan.com/audio/adhan/azan5.mp3",
                estimatedSizeLabel = "1.3 MB"
            ),
            MuezzinEntity(
                id = "saudi_qatami",
                nameAr = "الشيخ ناصر القطامي",
                regionAr = "السعودية — الرياض",
                mosqueAr = "أذان نجدي هادئ",
                audioUrl = "https://www.islamcan.com/audio/adhan/azan6.mp3",
                estimatedSizeLabel = "1.0 MB"
            ),
            MuezzinEntity(
                id = "sham_hijazi",
                nameAr = "أذان بلاد الشام (المقام الحجازي)",
                regionAr = "الشام — دمشق",
                mosqueAr = "الجامع الأموي بدمشق",
                audioUrl = "https://www.islamcan.com/audio/adhan/azan7.mp3",
                estimatedSizeLabel = "1.2 MB"
            ),
            MuezzinEntity(
                id = "kuwait_alafasy",
                nameAr = "الشيخ مشاري راشد العفاسي",
                regionAr = "الخليج — الكويت",
                mosqueAr = "المسجد الكبير بالكويت",
                audioUrl = "https://www.islamcan.com/audio/adhan/azan8.mp3",
                estimatedSizeLabel = "1.6 MB"
            ),
            MuezzinEntity(
                id = "iraq_raad_alkurdi",
                nameAr = "الشيخ رعد محمد الكردي",
                regionAr = "العراق — إقليم كردستان",
                mosqueAr = "أذان خاشع بصوت الشيخ رعد الكردي",
                audioUrl = "https://www.islamcan.com/audio/adhan/azan9.mp3",
                estimatedSizeLabel = "1.4 MB"
            )
        )
        muezzinDao.insertAllIgnore(defaults)
        reconcileExistingFiles()
    }

    private suspend fun reconcileExistingFiles() {
        val all = muezzinDao.getAllOnce()
        for (item in all) {
            val path = item.localFilePath
            if (item.isDownloaded && (path.isNullOrBlank() || !File(path).exists())) {
                muezzinDao.clearDownload(item.id)
            }
        }
    }

    suspend fun getPlayableLocalPath(muezzinId: String): String? = withContext(Dispatchers.IO) {
        val entity = muezzinDao.getById(muezzinId)
        val path = entity?.localFilePath
        if (!path.isNullOrBlank() && File(path).exists()) {
            path
        } else {
            // Fall back to any downloaded muezzin file if selected one isn't downloaded
            muezzinDao.getAllOnce().firstOrNull {
                it.isDownloaded && !it.localFilePath.isNullOrBlank() && File(it.localFilePath).exists()
            }?.localFilePath
        }
    }

    fun startDownload(muezzin: MuezzinEntity) {
        scope.launch {
            val adhanDir = File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "adhan")
            if (!adhanDir.exists()) adhanDir.mkdirs()

            val targetFile = File(adhanDir, "${muezzin.id}.mp3")
            if (targetFile.exists()) targetFile.delete()

            val request = DownloadManager.Request(Uri.parse(muezzin.audioUrl))
                .setTitle("أذان ${muezzin.nameAr}")
                .setDescription(muezzin.mosqueAr)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
                .setDestinationInExternalFilesDir(
                    context,
                    Environment.DIRECTORY_MUSIC,
                    "adhan/${muezzin.id}.mp3"
                )

            val downloadId = downloadManager.enqueue(request)
            muezzinDao.updateDownloadStarted(
                id = muezzin.id,
                isDownloading = true,
                progress = 5,
                downloadId = downloadId
            )

            trackDownloadProgress(muezzin.id, downloadId, targetFile)
        }
    }

    private fun trackDownloadProgress(muezzinId: String, downloadId: Long, targetFile: File) {
        scope.launch {
            var completed = false
            while (isActive && !completed) {
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor = downloadManager.query(query)
                if (cursor != null && cursor.moveToFirst()) {
                    val statusIdx = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    val downloadedIdx = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    val totalIdx = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)

                    val status = if (statusIdx >= 0) cursor.getInt(statusIdx) else DownloadManager.STATUS_FAILED
                    val downloaded = if (downloadedIdx >= 0) cursor.getLong(downloadedIdx) else 0L
                    val total = if (totalIdx >= 0) cursor.getLong(totalIdx) else -1L

                    when (status) {
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            val sizeBytes = if (targetFile.exists()) targetFile.length() else downloaded
                            muezzinDao.markDownloaded(
                                id = muezzinId,
                                localPath = targetFile.absolutePath,
                                fileSizeBytes = sizeBytes
                            )
                            completed = true
                        }
                        DownloadManager.STATUS_FAILED -> {
                            muezzinDao.markDownloadFailed(muezzinId)
                            completed = true
                        }
                        else -> {
                            if (total > 0) {
                                val pct = ((downloaded * 100L) / total).toInt().coerceIn(5, 99)
                                muezzinDao.updateProgress(muezzinId, pct)
                            }
                        }
                    }
                    cursor.close()
                } else {
                    cursor?.close()
                    muezzinDao.markDownloadFailed(muezzinId)
                    completed = true
                }
                if (!completed) delay(400L)
            }
        }
    }

    suspend fun deleteDownload(muezzin: MuezzinEntity) = withContext(Dispatchers.IO) {
        muezzin.localFilePath?.let { path ->
            val file = File(path)
            if (file.exists()) file.delete()
        }
        muezzinDao.clearDownload(muezzin.id)
    }

    fun formatBytes(bytes: Long, fallback: String): String {
        if (bytes <= 0L) return fallback
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        return if (mb >= 1.0) String.format("%.2f MB", mb) else String.format("%.0f KB", kb)
    }
}
