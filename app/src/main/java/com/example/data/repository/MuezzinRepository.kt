package com.example.data.repository

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import com.example.data.local.MuezzinDao
import com.example.data.local.MuezzinEntity
import com.example.data.model.PrayerName
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
            // الحرم المكي والمدني والأقصى
            MuezzinEntity(
                id = "makkah_mullah",
                nameAr = "الشيخ علي أحمد ملا",
                regionAr = "الحرمين والأقصى — مكة المكرمة",
                mosqueAr = "المسجد الحرام بمكة المكرمة",
                audioUrl = "https://www.islamcan.com/audio/adhan/azan1.mp3",
                estimatedSizeLabel = "1.4 MB"
            ),
            MuezzinEntity(
                id = "makkah_fajr",
                nameAr = "أذان الفجر المكي (الصلاة خير من النوم)",
                regionAr = "الحرمين والأقصى — مكة المكرمة",
                mosqueAr = "المسجد الحرام — أذان الفجر",
                audioUrl = "https://www.islamcan.com/audio/adhan/azan10.mp3",
                estimatedSizeLabel = "1.5 MB"
            ),
            MuezzinEntity(
                id = "makkah_farooq",
                nameAr = "الشيخ فاروق حضراوي",
                regionAr = "الحرمين والأقصى — مكة المكرمة",
                mosqueAr = "المسجد الحرام — مقام حجازي",
                audioUrl = "https://www.islamcan.com/audio/adhan/azan11.mp3",
                estimatedSizeLabel = "1.3 MB"
            ),
            MuezzinEntity(
                id = "madinah_bukhari",
                nameAr = "الشيخ عصام بخاري",
                regionAr = "الحرمين والأقصى — المدينة المنورة",
                mosqueAr = "المسجد النبوي بالمدينة المنورة",
                audioUrl = "https://www.islamcan.com/audio/adhan/azan2.mp3",
                estimatedSizeLabel = "1.2 MB"
            ),
            MuezzinEntity(
                id = "madinah_sharif",
                nameAr = "الشيخ عبد الرحمن خاشقجي",
                regionAr = "الحرمين والأقصى — المدينة المنورة",
                mosqueAr = "المسجد النبوي — أذان مدني عتيق",
                audioUrl = "https://www.islamcan.com/audio/adhan/azan12.mp3",
                estimatedSizeLabel = "1.3 MB"
            ),
            MuezzinEntity(
                id = "alaqsa_quds",
                nameAr = "أذان المسجد الأقصى المبارك",
                regionAr = "الحرمين والأقصى — القدس",
                mosqueAr = "المسجد الأقصى بالقدس الشريف",
                audioUrl = "https://www.islamcan.com/audio/adhan/azan3.mp3",
                estimatedSizeLabel = "1.1 MB"
            ),
            MuezzinEntity(
                id = "alaqsa_fajr",
                nameAr = "أذان الفجر في المسجد الأقصى",
                regionAr = "الحرمين والأقصى — القدس",
                mosqueAr = "المسجد الأقصى — نداء الفجر",
                audioUrl = "https://www.islamcan.com/audio/adhan/azan13.mp3",
                estimatedSizeLabel = "1.2 MB"
            ),

            // العراق والخليج والسعودية
            MuezzinEntity(
                id = "iraq_raad_alkurdi",
                nameAr = "الشيخ رعد محمد الكردي",
                regionAr = "العراق والخليج — كردستان",
                mosqueAr = "أذان خاشع بصوت الشيخ رعد الكردي",
                audioUrl = "https://www.islamcan.com/audio/adhan/azan9.mp3",
                estimatedSizeLabel = "1.4 MB"
            ),
            MuezzinEntity(
                id = "kuwait_alafasy",
                nameAr = "الشيخ مشاري راشد العفاسي",
                regionAr = "العراق والخليج — الكويت",
                mosqueAr = "المسجد الكبير بالكويت",
                audioUrl = "https://www.islamcan.com/audio/adhan/azan8.mp3",
                estimatedSizeLabel = "1.6 MB"
            ),
            MuezzinEntity(
                id = "saudi_qatami",
                nameAr = "الشيخ ناصر القطامي",
                regionAr = "العراق والخليج — الرياض",
                mosqueAr = "أذان نجدي هادئ",
                audioUrl = "https://www.islamcan.com/audio/adhan/azan6.mp3",
                estimatedSizeLabel = "1.0 MB"
            ),
            MuezzinEntity(
                id = "saudi_dosari",
                nameAr = "الشيخ ياسر الدوسري",
                regionAr = "العراق والخليج — السعودية",
                mosqueAr = "أذان مؤثر بصوت الشيخ ياسر الدوسري",
                audioUrl = "https://www.islamcan.com/audio/adhan/azan14.mp3",
                estimatedSizeLabel = "1.4 MB"
            ),
            MuezzinEntity(
                id = "saudi_ajmi",
                nameAr = "الشيخ أحمد العجمي",
                regionAr = "العراق والخليج — الشرقية",
                mosqueAr = "أذان عذب بصوت الشيخ أحمد العجمي",
                audioUrl = "https://www.islamcan.com/audio/adhan/azan15.mp3",
                estimatedSizeLabel = "1.3 MB"
            ),

            // مصر والشام
            MuezzinEntity(
                id = "egypt_rifaat",
                nameAr = "الشيخ محمد رفعت",
                regionAr = "مصر والشام — القاهرة",
                mosqueAr = "أذان مصري كلاسيكي خاشع",
                audioUrl = "https://www.islamcan.com/audio/adhan/azan4.mp3",
                estimatedSizeLabel = "1.5 MB"
            ),
            MuezzinEntity(
                id = "egypt_abdulbasit",
                nameAr = "الشيخ عبد الباسط عبد الصمد",
                regionAr = "مصر والشام — صعيد مصر",
                mosqueAr = "أذان خاشع بصوت الشيخ عبد الباسط",
                audioUrl = "https://www.islamcan.com/audio/adhan/azan5.mp3",
                estimatedSizeLabel = "1.3 MB"
            ),
            MuezzinEntity(
                id = "egypt_minshawi",
                nameAr = "الشيخ محمد صديق المنشاوي",
                regionAr = "مصر والشام — مصر",
                mosqueAr = "أذان روحاني بصوت المنشاوي",
                audioUrl = "https://www.islamcan.com/audio/adhan/azan16.mp3",
                estimatedSizeLabel = "1.4 MB"
            ),
            MuezzinEntity(
                id = "egypt_husary",
                nameAr = "الشيخ محمود خليل الحصري",
                regionAr = "مصر والشام — مصر",
                mosqueAr = "أذان متقن بصوت الشيخ الحصري",
                audioUrl = "https://www.islamcan.com/audio/adhan/azan17.mp3",
                estimatedSizeLabel = "1.2 MB"
            ),
            MuezzinEntity(
                id = "sham_hijazi",
                nameAr = "أذان بلاد الشام (المقام الحجازي)",
                regionAr = "مصر والشام — دمشق",
                mosqueAr = "الجامع الأموي بدمشق",
                audioUrl = "https://www.islamcan.com/audio/adhan/azan7.mp3",
                estimatedSizeLabel = "1.2 MB"
            ),
            MuezzinEntity(
                id = "sham_halabi",
                nameAr = "أذان المقام الصبا الحلبي",
                regionAr = "مصر والشام — حلب",
                mosqueAr = "الجامع الكبير بحلب",
                audioUrl = "https://www.islamcan.com/audio/adhan/azan18.mp3",
                estimatedSizeLabel = "1.3 MB"
            ),

            // العالم الإسلامي
            MuezzinEntity(
                id = "turkey_ottoman",
                nameAr = "الأذان التركي (جامع السلطان أحمد)",
                regionAr = "العالم الإسلامي — تركيا",
                mosqueAr = "إسطنبول — مقام عجم",
                audioUrl = "https://www.islamcan.com/audio/adhan/azan19.mp3",
                estimatedSizeLabel = "1.4 MB"
            ),
            MuezzinEntity(
                id = "morocco_andalusi",
                nameAr = "الأذان المغربي الأندلسي",
                regionAr = "العالم الإسلامي — المغرب",
                mosqueAr = "جامع القرويين بفاس",
                audioUrl = "https://www.islamcan.com/audio/adhan/azan20.mp3",
                estimatedSizeLabel = "1.1 MB"
            ),
            MuezzinEntity(
                id = "indonesia_nusantara",
                nameAr = "أذان مسجد الاستقلال",
                regionAr = "العالم الإسلامي — إندونيسيا",
                mosqueAr = "مسجد الاستقلال بجاكرتا",
                audioUrl = "https://www.islamcan.com/audio/adhan/azan21.mp3",
                estimatedSizeLabel = "1.2 MB"
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
                if (item.id.startsWith("custom_local_")) {
                    muezzinDao.deleteById(item.id)
                } else {
                    muezzinDao.clearDownload(item.id)
                }
            }
        }
    }

    suspend fun getPlayableLocalPath(muezzinId: String): String? = withContext(Dispatchers.IO) {
        val entity = muezzinDao.getById(muezzinId)
        val path = entity?.localFilePath
        if (!path.isNullOrBlank() && File(path).exists()) {
            path
        } else {
            muezzinDao.getAllOnce().firstOrNull {
                it.isDownloaded && !it.localFilePath.isNullOrBlank() && File(it.localFilePath).exists()
            }?.localFilePath
        }
    }

    suspend fun importCustomAdhanFromUri(uri: Uri): Result<MuezzinEntity> = withContext(Dispatchers.IO) {
        try {
            val displayName = resolveDisplayName(uri) ?: "أذان مخصص"
            val cleanTitle = displayName.substringBeforeLast('.').ifBlank { displayName }
            val dir = File(context.filesDir, "custom_adhan").apply { if (!exists()) mkdirs() }
            val id = "custom_local_${System.currentTimeMillis()}"
            val outFile = File(dir, "$id.audio")

            context.contentResolver.openInputStream(uri)?.use { input ->
                outFile.outputStream().use { output -> input.copyTo(output) }
            } ?: return@withContext Result.failure(IllegalStateException("تعذر قراءة الملف الصوتي"))

            val sizeBytes = outFile.length()
            val entity = MuezzinEntity(
                id = id,
                nameAr = cleanTitle,
                regionAr = "ملف من ذاكرة الهاتف",
                mosqueAr = "صوت مضاف من جهازك ($displayName)",
                audioUrl = "",
                estimatedSizeLabel = formatBytes(sizeBytes, "ملف محلي"),
                fileSizeBytes = sizeBytes,
                localFilePath = outFile.absolutePath,
                isDownloaded = true,
                isDownloading = false,
                downloadProgress = 100
            )
            muezzinDao.upsert(entity)
            Result.success(entity)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importCustomIqamahFromUri(uri: Uri): Result<Pair<String, String>> = withContext(Dispatchers.IO) {
        try {
            val displayName = resolveDisplayName(uri) ?: "صوت الإقامة المخصص"
            val dir = File(context.filesDir, "custom_iqamah").apply { if (!exists()) mkdirs() }
            // Remove older custom iqamah files to save space
            dir.listFiles()?.forEach { it.delete() }

            val outFile = File(dir, "iqamah_${System.currentTimeMillis()}.audio")
            context.contentResolver.openInputStream(uri)?.use { input ->
                outFile.outputStream().use { output -> input.copyTo(output) }
            } ?: return@withContext Result.failure(IllegalStateException("تعذر قراءة ملف الإقامة"))

            Result.success(outFile.absolutePath to displayName)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importCustomPrePrayerFromUri(prayer: PrayerName, uri: Uri): Result<Pair<String, String>> =
        withContext(Dispatchers.IO) {
            try {
                val displayName = resolveDisplayName(uri) ?: "تنبيه ${prayer.arabicName}"
                val dir = File(context.filesDir, "custom_pre_prayer").apply { if (!exists()) mkdirs() }
                val prefix = "pre_${prayer.name}_"
                dir.listFiles()?.filter { it.name.startsWith(prefix) }?.forEach { it.delete() }

                val outFile = File(dir, "$prefix${System.currentTimeMillis()}.audio")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    outFile.outputStream().use { output -> input.copyTo(output) }
                } ?: return@withContext Result.failure(IllegalStateException("تعذر قراءة الملف الصوتي"))

                Result.success(outFile.absolutePath to displayName)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    fun deleteLocalFileIfExists(path: String?) {
        if (!path.isNullOrBlank()) {
            runCatching { File(path).takeIf { it.exists() }?.delete() }
        }
    }

    private fun resolveDisplayName(uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) cursor.getString(idx) else null
                    } else null
                }
        } catch (_: Exception) {
            null
        }
    }

    fun startDownload(muezzin: MuezzinEntity) {
        if (muezzin.audioUrl.isBlank()) return
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
        if (muezzin.id.startsWith("custom_local_")) {
            muezzinDao.deleteById(muezzin.id)
        } else {
            muezzinDao.clearDownload(muezzin.id)
        }
    }

    fun formatBytes(bytes: Long, fallback: String): String {
        if (bytes <= 0L) return fallback
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        return if (mb >= 1.0) String.format("%.2f MB", mb) else String.format("%.0f KB", kb)
    }
}
