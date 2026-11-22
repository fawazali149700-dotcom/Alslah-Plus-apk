package com.example.data.repository

import android.app.DownloadManager
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import com.example.data.local.MuezzinDao
import com.example.data.local.MuezzinEntity
import com.example.data.model.IqamahPreset
import com.example.data.model.PrayerName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream

data class IqamahZipBundleResult(
    val iqamahGlobal: Pair<String, String>?,
    val iqamahPerPrayer: Map<PrayerName, Pair<String, String>>
)

class MuezzinRepository(
    private val context: Context,
    private val muezzinDao: MuezzinDao
) {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    val iqamahPresets: List<IqamahPreset> = listOf(
        IqamahPreset(
            id = "iqamah_makkah_basnawy",
            titleAr = "إقامة المسجد الحرام بمكة المكرمة",
            subtitleAr = "الشيخ أحمد عبد الله بصنوي — تسجيل حقيقي",
            metadataUrl = "https://archive.org/metadata/Iqamah-Makkah",
            fallbackDirectUrl = "https://archive.org/metadata/Iqamah-makkah"
        ),
        IqamahPreset(
            id = "iqamah_makkah_haram",
            titleAr = "إقامة الحرم المكي المبارك (صوت خاشع)",
            subtitleAr = "تسجيل إقامة الصلاة من مكة المكرمة",
            metadataUrl = "https://archive.org/metadata/Iqamah-makkah",
            fallbackDirectUrl = "https://archive.org/metadata/Iqamah-Makkah"
        ),
        IqamahPreset(
            id = "iqamah_madinah_nabawi",
            titleAr = "إقامة المسجد النبوي بالمدينة المنورة",
            subtitleAr = "نداء الإقامة الخاشع — الحرم النبوي",
            metadataUrl = "https://archive.org/metadata/IQAMATRAIWINDIJTEMA",
            fallbackDirectUrl = "https://archive.org/metadata/Iqamah-makkah"
        )
    )
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
            // Remove older global custom iqamah files to save space
            dir.listFiles()?.filter { it.name.startsWith("iqamah_global_") }?.forEach { it.delete() }

            val outFile = File(dir, "iqamah_global_${System.currentTimeMillis()}.audio")
            context.contentResolver.openInputStream(uri)?.use { input ->
                outFile.outputStream().use { output -> input.copyTo(output) }
            } ?: return@withContext Result.failure(IllegalStateException("تعذر قراءة ملف الإقامة"))

            Result.success(outFile.absolutePath to displayName)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importCustomPrayerIqamahFromUri(prayer: PrayerName, uri: Uri): Result<Pair<String, String>> =
        withContext(Dispatchers.IO) {
            try {
                val displayName = resolveDisplayName(uri) ?: "إقامة ${prayer.arabicName}"
                val dir = File(context.filesDir, "custom_iqamah").apply { if (!exists()) mkdirs() }
                val prefix = "iqamah_${prayer.name}_"
                dir.listFiles()?.filter { it.name.startsWith(prefix) }?.forEach { it.delete() }

                val outFile = File(dir, "$prefix${System.currentTimeMillis()}.audio")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    outFile.outputStream().use { output -> input.copyTo(output) }
                } ?: return@withContext Result.failure(IllegalStateException("تعذر قراءة ملف الإقامة"))

                Result.success(outFile.absolutePath to displayName)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun importIqamahBundleZip(uri: Uri): Result<IqamahZipBundleResult> = withContext(Dispatchers.IO) {
        try {
            val iqamahDir = File(context.filesDir, "custom_iqamah").apply { if (!exists()) mkdirs() }
            var iqamahGlobal: Pair<String, String>? = null
            val iqamahPerPrayer = mutableMapOf<PrayerName, Pair<String, String>>()
            val supportedExts = setOf("mp3", "ogg", "oga", "wav", "m4a", "opus")

            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(IllegalStateException("تعذر فتح ملف الحزمة المضغوطة (ZIP)"))

            inputStream.use { raw ->
                ZipInputStream(raw.buffered()).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory) {
                            val fileName = entry.name.substringAfterLast('/')
                            val stem = fileName.substringBeforeLast('.').lowercase()
                            val ext = fileName.substringAfterLast('.', "").lowercase()
                            if (ext in supportedExts && !fileName.startsWith("._")) {
                                val matchedPrayer = matchIqamahPrayerFromStem(stem)
                                if (matchedPrayer != null) {
                                    val prefix = "iqamah_${matchedPrayer.name}_"
                                    iqamahDir.listFiles()?.filter { it.name.startsWith(prefix) }?.forEach { it.delete() }
                                    val outFile = File(iqamahDir, "$prefix${System.currentTimeMillis()}.$ext")
                                    outFile.outputStream().use { zis.copyTo(it) }
                                    if (outFile.length() > 512L) {
                                        iqamahPerPrayer[matchedPrayer] = outFile.absolutePath to fileName
                                    } else {
                                        outFile.delete()
                                    }
                                } else if (stem == "iqamah" || stem == "iqama" || stem.startsWith("iqamah_")) {
                                    iqamahDir.listFiles()?.filter { it.name.startsWith("iqamah_global_") }?.forEach { it.delete() }
                                    val outFile = File(iqamahDir, "iqamah_global_${System.currentTimeMillis()}.$ext")
                                    outFile.outputStream().use { zis.copyTo(it) }
                                    if (outFile.length() > 512L) {
                                        iqamahGlobal = outFile.absolutePath to fileName
                                    } else {
                                        outFile.delete()
                                    }
                                }
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            }

            if (iqamahGlobal == null && iqamahPerPrayer.isEmpty()) {
                return@withContext Result.failure(
                    IllegalStateException("لم يُعثر داخل ملف ZIP على ملفات صوتية لإقامة الصلاة")
                )
            }
            Result.success(
                IqamahZipBundleResult(
                    iqamahGlobal = iqamahGlobal,
                    iqamahPerPrayer = iqamahPerPrayer
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun matchIqamahPrayerFromStem(stem: String): PrayerName? {
        if (!stem.contains("iqamah") && !stem.contains("iqama") && !stem.contains("اقامه")) return null
        return when {
            stem.contains("fajr") || stem.contains("فجر") -> PrayerName.FAJR
            stem.contains("dhuhr") || stem.contains("zuhr") || stem.contains("ظهر") -> PrayerName.DHUHR
            stem.contains("asr") || stem.contains("عصر") -> PrayerName.ASR
            stem.contains("maghrib") || stem.contains("مغرب") -> PrayerName.MAGHRIB
            stem.contains("isha") || stem.contains("عشاء") -> PrayerName.ISHA
            else -> null
        }
    }

    suspend fun downloadIqamahPreset(preset: IqamahPreset): Result<Pair<String, String>> = withContext(Dispatchers.IO) {
        try {
            val urlsToTry = linkedSetOf<String>().apply {
                resolveArchiveAudioUrls(preset.metadataUrl).forEach { add(it) }
                if (preset.fallbackDirectUrl.isNotBlank()) {
                    resolveArchiveAudioUrls(preset.fallbackDirectUrl).forEach { add(it) }
                }
            }.toList()

            if (urlsToTry.isEmpty()) {
                return@withContext Result.failure(IllegalStateException("تعذر العثور على رابط صوت الإقامة، تحقق من الإنترنت"))
            }

            val dir = File(context.filesDir, "downloaded_iqamah").apply { if (!exists()) mkdirs() }
            val outFile = File(dir, "${preset.id}.mp3")
            val tempFile = File(dir, "${preset.id}.tmp")

            for (url in urlsToTry) {
                val ok = runCatching {
                    val req = Request.Builder()
                        .url(url)
                        .header("User-Agent", "NoorIslamicApp/1.0")
                        .build()
                    httpClient.newCall(req).execute().use { resp ->
                        if (!resp.isSuccessful) return@use false
                        val ct = resp.header("Content-Type").orEmpty()
                        if (ct.contains("text/html", ignoreCase = true)) return@use false
                        val body = resp.body ?: return@use false
                        body.byteStream().use { input ->
                            tempFile.outputStream().use { output -> input.copyTo(output) }
                        }
                        if (tempFile.length() <= 2048L) return@use false
                        val durationMs = extractDurationMs(tempFile)
                        durationMs > 500L
                    }
                }.getOrDefault(false)

                if (ok) {
                    if (outFile.exists()) outFile.delete()
                    if (tempFile.renameTo(outFile)) {
                        return@withContext Result.success(outFile.absolutePath to preset.titleAr)
                    }
                } else {
                    tempFile.delete()
                }
            }

            Result.failure(IllegalStateException("تعذر تنزيل صوت الإقامة، تحقق من اتصالك بالإنترنت"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun resolveArchiveAudioUrls(metadataUrl: String): List<String> {
        if (!metadataUrl.contains("archive.org/metadata/")) return listOf(metadataUrl)
        val identifier = metadataUrl.substringAfter("archive.org/metadata/").substringBefore('/').trim()
        if (identifier.isBlank()) return emptyList()
        val body = runCatching {
            val req = Request.Builder()
                .url(metadataUrl)
                .header("User-Agent", "NoorIslamicApp/1.0")
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) resp.body?.string() else null
            }
        }.getOrNull() ?: return emptyList()

        val found = mutableListOf<String>()
        runCatching {
            val json = JSONObject(body)
            val files = json.optJSONArray("files") ?: return@runCatching
            for (i in 0 until files.length()) {
                val obj = files.optJSONObject(i) ?: continue
                val name = obj.optString("name", "")
                if (name.isBlank()) continue
                val lower = name.lowercase()
                if (lower.endsWith(".mp3") || lower.endsWith(".ogg") || lower.endsWith(".m4a")) {
                    val encodedName = name.split('/').joinToString("/") { part ->
                        URLEncoder.encode(part, "UTF-8").replace("+", "%20")
                    }
                    found.add("https://archive.org/download/$identifier/$encodedName")
                }
            }
        }
        return found.sortedByDescending { it.lowercase().endsWith(".mp3") }
    }

    private fun extractDurationMs(file: File): Long {
        return runCatching {
            val mmr = MediaMetadataRetriever()
            try {
                mmr.setDataSource(file.absolutePath)
                mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            } finally {
                mmr.release()
            }
        }.getOrDefault(0L)
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
