package com.example.data.repository

import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import com.example.data.local.DhikrDao
import com.example.data.local.DhikrEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class DhikrRepository(
    private val context: Context,
    private val dhikrDao: DhikrDao
) {
    val dhikrListFlow: Flow<List<DhikrEntity>> = dhikrDao.observeAll()

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private data class CatalogSeed(
        val id: String,
        val titleAr: String,
        val textAr: String,
        val categoryAr: String,
        val searchQueries: List<String>,
        val filenameTokens: List<String>,
        val exactNormalizedMatches: List<String>,
        val defaultAudioUrl: String,
        val fallbackAudioUrl: String
    )

    private data class RemoteTrack(
        val normalizedText: String,
        val rawAudioUrl: String,
        val fallbackUrl: String
    )

    private val seedList = listOf(
        CatalogSeed(
            id = "dhikr_short_subhanallah",
            titleAr = "سبحان الله",
            textAr = "سُبْحَانَ اللَّهِ",
            categoryAr = "التسبيح",
            searchQueries = listOf(
                "\"سبحان الله\" filetype:audio",
                "subhanallah filetype:audio",
                "subhan allah filetype:audio"
            ),
            filenameTokens = listOf("سبحان", "subhan", "tasbih"),
            exactNormalizedMatches = listOf("سبحان الله"),
            defaultAudioUrl = "https://commons.wikimedia.org/wiki/Special:FilePath/Ar-%D8%B3%D8%A8%D8%AD%D8%A7%D9%86_%D8%A7%D9%84%D9%84%D9%87.ogg",
            fallbackAudioUrl = ""
        ),
        CatalogSeed(
            id = "dhikr_short_alhamdulillah",
            titleAr = "الحمد لله",
            textAr = "الْحَمْدُ لِلَّهِ",
            categoryAr = "التحميد",
            searchQueries = listOf(
                "\"الحمد لله\" filetype:audio",
                "alhamdulillah filetype:audio",
                "al-hamdulillah filetype:audio"
            ),
            filenameTokens = listOf("الحمد", "hamd"),
            exactNormalizedMatches = listOf("الحمد لله"),
            defaultAudioUrl = "https://commons.wikimedia.org/wiki/Special:FilePath/Ar-%D8%A7%D9%84%D8%AD%D9%85%D8%AF_%D9%84%D9%84%D9%87.ogg",
            fallbackAudioUrl = ""
        ),
        CatalogSeed(
            id = "dhikr_short_hawqalah",
            titleAr = "لا حول ولا قوة إلا بالله",
            textAr = "لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ",
            categoryAr = "الحوقلة",
            searchQueries = listOf(
                "\"لا حول ولا قوة الا بالله\" filetype:audio",
                "\"لا حول ولا قوة إلا بالله\" filetype:audio",
                "hawqalah filetype:audio"
            ),
            filenameTokens = listOf("حول", "قوه", "hawqal", "hawla"),
            exactNormalizedMatches = listOf("لا حول ولا قوه الا بالله"),
            defaultAudioUrl = "",
            fallbackAudioUrl = ""
        ),
        CatalogSeed(
            id = "dhikr_short_hasbunallah",
            titleAr = "حسبي الله ونعم الوكيل",
            textAr = "حَسْبِيَ اللَّهُ وَنِعْمَ الْوَكِيلُ",
            categoryAr = "التوكل",
            searchQueries = listOf(
                "\"حسبي الله ونعم الوكيل\" filetype:audio",
                "\"حسبنا الله ونعم الوكيل\" filetype:audio",
                "hasbunallah filetype:audio"
            ),
            filenameTokens = listOf("حسبي", "حسبنا", "hasbun", "hasbi"),
            exactNormalizedMatches = listOf("حسبي الله ونعم الوكيل", "حسبنا الله ونعم الوكيل"),
            defaultAudioUrl = "",
            fallbackAudioUrl = ""
        ),
        CatalogSeed(
            id = "dhikr_short_allahuakbar",
            titleAr = "الله أكبر",
            textAr = "اللَّهُ أَكْبَرُ",
            categoryAr = "التكبير",
            searchQueries = listOf(
                "\"الله أكبر\" filetype:audio",
                "allahuakbar filetype:audio",
                "allahu akbar filetype:audio"
            ),
            filenameTokens = listOf("اكبر", "akbar"),
            exactNormalizedMatches = listOf("الله اكبر"),
            defaultAudioUrl = "https://commons.wikimedia.org/wiki/Special:FilePath/Ar-eg-%D8%A7%D9%84%D9%84%D9%87_%D8%A3%D9%83%D8%A8%D8%B1.oga",
            fallbackAudioUrl = "https://commons.wikimedia.org/wiki/Special:FilePath/Allahuakbar.opus"
        ),
        CatalogSeed(
            id = "dhikr_short_salawat",
            titleAr = "صلِّ على النبي ﷺ",
            textAr = "اللَّهُمَّ صَلِّ عَلَى مُحَمَّدٍ",
            categoryAr = "الصلاة على النبي ﷺ",
            searchQueries = listOf(
                "\"اللهم صل على محمد\" filetype:audio",
                "\"صلى الله عليه وسلم\" filetype:audio",
                "salawat filetype:audio"
            ),
            filenameTokens = listOf("صلى", "صل", "محمد", "salawat", "salla"),
            exactNormalizedMatches = listOf("اللهم صل على محمد", "صلى الله عليه وسلم"),
            defaultAudioUrl = "",
            fallbackAudioUrl = ""
        ),
        CatalogSeed(
            id = "dhikr_short_astaghfirullah",
            titleAr = "أستغفر الله",
            textAr = "أَسْتَغْفِرُ اللَّهَ",
            categoryAr = "الاستغفار",
            searchQueries = listOf(
                "\"أستغفر الله\" filetype:audio",
                "astaghfirullah filetype:audio"
            ),
            filenameTokens = listOf("استغفر", "astaghfir"),
            exactNormalizedMatches = listOf("استغفر الله"),
            defaultAudioUrl = "",
            fallbackAudioUrl = ""
        ),
        CatalogSeed(
            id = "dhikr_short_tahlil",
            titleAr = "لا إله إلا الله",
            textAr = "لَا إِلَهَ إِلَّا اللَّهُ",
            categoryAr = "التهليل",
            searchQueries = listOf(
                "\"لا إله إلا الله\" filetype:audio",
                "\"لا اله الا الله\" filetype:audio",
                "tahlil filetype:audio"
            ),
            filenameTokens = listOf("اله", "shahada", "ilaha", "tahlil"),
            exactNormalizedMatches = listOf("لا اله الا الله"),
            defaultAudioUrl = "",
            fallbackAudioUrl = ""
        )
    )

    suspend fun ensureSeeded() = withContext(Dispatchers.IO) {
        val entities = seedList.map { seed ->
            DhikrEntity(
                id = seed.id,
                titleAr = seed.titleAr,
                textAr = seed.textAr,
                categoryAr = seed.categoryAr,
                reciterAr = if (seed.defaultAudioUrl.isNotBlank()) "نطق بشري قصير (بدون زيادة)" else "أضف تسجيلاً بشرياً قصيراً أو جرّب التحديث",
                audioUrl = seed.defaultAudioUrl,
                fallbackAudioUrl = seed.fallbackAudioUrl,
                isDownloaded = false,
                isDownloading = false,
                isEnabled = true,
                isCustom = false,
                durationMs = 0L
            )
        }
        dhikrDao.insertAllIgnore(entities)
        reconcileLocalFiles()
    }

    private suspend fun reconcileLocalFiles() {
        val all = dhikrDao.getAllOnce()
        val validIds = all.map { it.id }.toSet()

        // Purge leftover long-dua files from earlier app versions
        val dir = File(context.filesDir, "dhikr_audio")
        if (dir.exists()) {
            dir.listFiles()?.forEach { file ->
                val stem = file.nameWithoutExtension
                val belongsToCurrent = validIds.any { id ->
                    stem == id || stem.startsWith("custom_${id}_")
                }
                if (!belongsToCurrent) {
                    runCatching { file.delete() }
                }
            }
        }

        for (item in all) {
            val path = item.localFilePath
            if (item.isDownloaded) {
                val file = path?.let { File(it) }
                val existsValid = file != null && file.exists() && file.length() > 512L
                if (!existsValid) {
                    dhikrDao.clearDownload(item.id)
                } else if (!item.isCustom) {
                    val dur = extractPlayableDurationMs(file!!)
                    if (dur !in MIN_SHORT_DURATION_MS..MAX_SHORT_DURATION_MS) {
                        runCatching { file.delete() }
                        dhikrDao.clearDownload(item.id)
                    }
                }
            }
        }
    }

    /**
     * Resolves verified short, pure human pronunciation recordings (no long hadith narrations, no extra words)
     * using Wikimedia Commons Audio Search and exact-phrase-only entries from adhkar.json.
     */
    suspend fun syncCatalogFromOfficialJson(): Boolean = withContext(Dispatchers.IO) {
        ensureSeeded()
        val shortJsonTracks = fetchExactShortTracksFromAdhkarJson()

        var anyResolved = false
        for (seed in seedList) {
            val wikimediaUrls = resolveFromWikimediaCommons(seed)
            val jsonTrack = shortJsonTracks.firstOrNull { track ->
                seed.exactNormalizedMatches.any { exact -> track.normalizedText == exact }
            }

            val candidates = linkedSetOf<String>().apply {
                wikimediaUrls.forEach { if (it.isNotBlank()) add(it) }
                if (seed.defaultAudioUrl.isNotBlank()) add(seed.defaultAudioUrl)
                if (seed.fallbackAudioUrl.isNotBlank()) add(seed.fallbackAudioUrl)
                if (jsonTrack != null) {
                    add(jsonTrack.rawAudioUrl)
                    add(jsonTrack.fallbackUrl)
                }
            }.toList()

            val primary = candidates.getOrNull(0).orEmpty()
            val fallback = candidates.getOrNull(1).orEmpty()
            if (primary.isNotBlank()) {
                dhikrDao.updateAudioUrls(seed.id, primary, fallback)
                anyResolved = true
            }
        }
        anyResolved
    }

    private fun resolveFromWikimediaCommons(seed: CatalogSeed): List<String> {
        val found = mutableListOf<String>()
        for (query in seed.searchQueries) {
            if (found.size >= 2) break
            val encoded = runCatching { URLEncoder.encode(query, "UTF-8") }.getOrNull() ?: continue
            val apiUrl =
                "https://commons.wikimedia.org/w/api.php?action=query&format=json&generator=search" +
                    "&gsrnamespace=6&gsrlimit=8&gsrsearch=$encoded&prop=imageinfo&iiprop=url|mime|size"
            val body = fetchText(apiUrl) ?: continue
            runCatching {
                val pages = JSONObject(body).optJSONObject("query")?.optJSONObject("pages") ?: return@runCatching
                val keys = pages.keys()
                while (keys.hasNext()) {
                    val page = pages.optJSONObject(keys.next()) ?: continue
                    val title = normalizeArabic(page.optString("title", "")).lowercase()
                    val imageInfo = page.optJSONArray("imageinfo")?.optJSONObject(0) ?: continue
                    val url = imageInfo.optString("url", "")
                    val size = imageInfo.optLong("size", 0L)
                    if (url.isBlank() || size <= 1024L || size > 400_000L) continue

                    val lowerUrl = url.lowercase()
                    val supportedExt = lowerUrl.endsWith(".ogg") ||
                        lowerUrl.endsWith(".oga") ||
                        lowerUrl.endsWith(".mp3") ||
                        lowerUrl.endsWith(".wav") ||
                        lowerUrl.endsWith(".m4a") ||
                        (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && lowerUrl.endsWith(".opus"))
                    if (!supportedExt) continue

                    val tokenMatch = seed.filenameTokens.any { token ->
                        title.contains(token.lowercase())
                    }
                    if (tokenMatch && url !in found) {
                        found.add(url)
                    }
                }
            }
        }
        return found
    }

    private fun fetchExactShortTracksFromAdhkarJson(): List<RemoteTrack> {
        val jsonBody = fetchText(GITHUB_CATALOG_URL) ?: return emptyList()
        val parsedTracks = mutableListOf<RemoteTrack>()
        runCatching {
            val rootArray = JSONArray(jsonBody)
            for (i in 0 until rootArray.length()) {
                val chapter = rootArray.optJSONObject(i) ?: continue
                val items = chapter.optJSONArray("array") ?: continue
                for (j in 0 until items.length()) {
                    val entry = items.optJSONObject(j) ?: continue
                    val rawText = entry.optString("text", "")
                    val audioRel = entry.optString("audio", "").trim()
                    val filename = entry.optString("filename", "").trim()
                    if (rawText.isBlank() || audioRel.isBlank()) continue

                    val norm = normalizeArabic(rawText)
                        .replace(Regex("[^\\p{L}\\s]"), "")
                        .replace(Regex("\\s+"), " ")
                        .trim()
                    // Reject any long entry or commentary-containing entry
                    if (norm.length > 42) continue

                    val cleanRel = audioRel.removePrefix("/")
                    val githubUrl = if (cleanRel.startsWith("http")) cleanRel
                    else "https://raw.githubusercontent.com/rn0x/Adhkar-json/main/$cleanRel"

                    val filePart = if (filename.isNotBlank()) "$filename.mp3" else cleanRel.substringAfterLast('/')
                    val mirrorUrl = "$HISNMUSLIM_AUDIO_BASE$filePart"

                    parsedTracks.add(
                        RemoteTrack(
                            normalizedText = norm,
                            rawAudioUrl = githubUrl,
                            fallbackUrl = mirrorUrl
                        )
                    )
                }
            }
        }
        return parsedTracks
    }

    suspend fun downloadAllHumanVoices(): Pair<Int, Int> = withContext(Dispatchers.IO) {
        syncCatalogFromOfficialJson()
        val items = dhikrDao.getAllOnce().filter { !it.isCustom }
        var successCount = 0
        for (item in items) {
            val existing = item.localFilePath
            if (item.isDownloaded && !existing.isNullOrBlank() && File(existing).exists() && File(existing).length() > 512L) {
                successCount++
                continue
            }
            if (item.audioUrl.isBlank() && item.fallbackAudioUrl.isBlank()) continue
            val result = downloadSingleHumanVoice(item)
            if (result.isSuccess) {
                successCount++
            }
        }
        successCount to items.size
    }

    suspend fun ensureCoreVoicesReady(): Int = withContext(Dispatchers.IO) {
        ensureSeeded()
        val all = dhikrDao.getAllOnce()
        val alreadyReady = all.count {
            it.isDownloaded && !it.localFilePath.isNullOrBlank() && File(it.localFilePath).exists()
        }
        if (alreadyReady >= 4) return@withContext alreadyReady

        syncCatalogFromOfficialJson()
        val pending = dhikrDao.getEnabledOnce().filter {
            !it.isDownloaded && (it.audioUrl.isNotBlank() || it.fallbackAudioUrl.isNotBlank())
        }
        var downloaded = alreadyReady
        for (item in pending) {
            if (downloadSingleHumanVoice(item).isSuccess) {
                downloaded++
            }
        }
        downloaded
    }

    suspend fun downloadSingleHumanVoice(dhikr: DhikrEntity): Result<String> = withContext(Dispatchers.IO) {
        val latest = dhikrDao.getById(dhikr.id) ?: dhikr
        if (latest.audioUrl.isBlank() && latest.fallbackAudioUrl.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("لا يتوفر ملف قصير بدون زيادة عبر الإنترنت لهذا الذكر؛ يمكنك إرفاق صوتك البشري القصير من الذاكرة")
            )
        }
        dhikrDao.setDownloading(latest.id, true)
        try {
            val dir = File(context.filesDir, "dhikr_audio").apply { if (!exists()) mkdirs() }
            val ext = guessExtension(latest.audioUrl)
            val outFile = File(dir, "${latest.id}.$ext")

            var durationMs = downloadAndVerifyShortAudio(latest.audioUrl, outFile, enforceShort = !latest.isCustom)
            if (durationMs <= 0L && latest.fallbackAudioUrl.isNotBlank()) {
                val fbExt = guessExtension(latest.fallbackAudioUrl)
                val fbFile = File(dir, "${latest.id}.$fbExt")
                durationMs = downloadAndVerifyShortAudio(latest.fallbackAudioUrl, fbFile, enforceShort = !latest.isCustom)
                if (durationMs > 0L) {
                    val reciter = if (latest.isCustom) latest.reciterAr else "نطق بشري قصير (بدون زيادة)"
                    dhikrDao.markDownloaded(latest.id, fbFile.absolutePath, reciter, durationMs)
                    return@withContext Result.success(fbFile.absolutePath)
                }
            }

            if (durationMs > 0L && outFile.exists() && outFile.length() > 512L) {
                val reciter = if (latest.isCustom) latest.reciterAr else "نطق بشري قصير (بدون زيادة)"
                dhikrDao.markDownloaded(latest.id, outFile.absolutePath, reciter, durationMs)
                Result.success(outFile.absolutePath)
            } else {
                outFile.delete()
                dhikrDao.setDownloading(latest.id, false)
                Result.failure(
                    IllegalStateException("لم يُعثر على تسجيل بشري قصير مطابق بدون زيادة؛ يمكنك إرفاق تسجيل قصير من الذاكرة")
                )
            }
        } catch (e: Exception) {
            dhikrDao.setDownloading(latest.id, false)
            Result.failure(e)
        }
    }

    suspend fun importCustomHumanAudioForDhikr(dhikr: DhikrEntity, uri: Uri): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val dir = File(context.filesDir, "dhikr_audio").apply { if (!exists()) mkdirs() }
                val fileName = "custom_${dhikr.id}_${System.currentTimeMillis()}.audio"
                val outFile = File(dir, fileName)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    outFile.outputStream().use { output -> input.copyTo(output) }
                } ?: return@withContext Result.failure(IllegalStateException("تعذر قراءة الملف الصوتي"))

                if (outFile.length() < 512L) {
                    outFile.delete()
                    return@withContext Result.failure(IllegalStateException("الملف الصوتي غير صالح"))
                }

                val durMs = extractPlayableDurationMs(outFile)
                dhikr.localFilePath?.let { old ->
                    runCatching {
                        val f = File(old)
                        if (f.exists() && f.parentFile?.name == "dhikr_audio") f.delete()
                    }
                }

                val label = resolveDisplayName(uri)?.substringBeforeLast('.') ?: "تسجيل بشري قصير"
                dhikrDao.markDownloaded(
                    id = dhikr.id,
                    localPath = outFile.absolutePath,
                    reciterAr = "صوت بشري من الذاكرة ($label)",
                    durationMs = durMs.coerceAtLeast(0L)
                )
                Result.success(outFile.absolutePath)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun addCustomDhikr(
        titleAr: String,
        textAr: String,
        categoryAr: String = "مخصص من المستخدم",
        audioUri: Uri?
    ): Result<DhikrEntity> = withContext(Dispatchers.IO) {
        try {
            val id = "custom_dhikr_${System.currentTimeMillis()}"
            var localPath: String? = null
            var reciterLabel = "ذكر نصي مخصص"
            var durMs = 0L

            if (audioUri != null) {
                val dir = File(context.filesDir, "dhikr_audio").apply { if (!exists()) mkdirs() }
                val outFile = File(dir, "$id.audio")
                context.contentResolver.openInputStream(audioUri)?.use { input ->
                    outFile.outputStream().use { output -> input.copyTo(output) }
                } ?: return@withContext Result.failure(IllegalStateException("تعذر قراءة الملف الصوتي"))
                localPath = outFile.absolutePath
                durMs = extractPlayableDurationMs(outFile).coerceAtLeast(0L)
                val audioName = resolveDisplayName(audioUri)?.substringBeforeLast('.') ?: "تسجيل بشري"
                reciterLabel = "صوت بشري من الذاكرة ($audioName)"
            }

            val entity = DhikrEntity(
                id = id,
                titleAr = titleAr.trim(),
                textAr = textAr.trim(),
                categoryAr = categoryAr,
                reciterAr = reciterLabel,
                audioUrl = "",
                fallbackAudioUrl = "",
                localFilePath = localPath,
                isDownloaded = localPath != null,
                isDownloading = false,
                isEnabled = true,
                isCustom = true,
                durationMs = durMs
            )
            dhikrDao.upsert(entity)
            Result.success(entity)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setDhikrEnabled(id: String, enabled: Boolean) {
        dhikrDao.updateEnabled(id, enabled)
    }

    suspend fun deleteCustomDhikr(dhikr: DhikrEntity) = withContext(Dispatchers.IO) {
        if (!dhikr.isCustom) return@withContext
        dhikr.localFilePath?.let { path ->
            runCatching { File(path).takeIf { it.exists() }?.delete() }
        }
        dhikrDao.deleteCustomById(dhikr.id)
    }

    suspend fun pickRandomPlayableDhikr(
        excludeId: String?,
        preferDownloadedAudio: Boolean
    ): DhikrEntity? = withContext(Dispatchers.IO) {
        ensureSeeded()
        val enabled = dhikrDao.getEnabledOnce()
        if (enabled.isEmpty()) return@withContext null

        val pool = if (preferDownloadedAudio) {
            val downloaded = enabled.filter {
                it.isDownloaded && !it.localFilePath.isNullOrBlank() && File(it.localFilePath).exists()
            }
            if (downloaded.isNotEmpty()) {
                downloaded
            } else {
                syncCatalogFromOfficialJson()
                val candidates = enabled.filter { it.audioUrl.isNotBlank() || it.fallbackAudioUrl.isNotBlank() }
                val candidate = candidates.filter { it.id != excludeId }.ifEmpty { candidates }.randomOrNull()
                if (candidate != null) {
                    downloadSingleHumanVoice(candidate)
                    val refreshed = dhikrDao.getById(candidate.id)
                    if (refreshed?.isDownloaded == true) listOf(refreshed) else enabled
                } else {
                    enabled
                }
            }
        } else {
            enabled
        }

        val nonRepeating = if (pool.size > 1 && excludeId != null) {
            pool.filter { it.id != excludeId }
        } else {
            pool
        }
        val chosen = nonRepeating.random(Random)
        dhikrDao.markLastPlayed(chosen.id, System.currentTimeMillis())
        chosen
    }

    private fun fetchText(url: String): String? {
        return runCatching {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", WIKIMEDIA_USER_AGENT)
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                resp.body?.string()
            }
        }.getOrNull()
    }

    private fun downloadAndVerifyShortAudio(
        url: String,
        target: File,
        enforceShort: Boolean
    ): Long {
        if (url.isBlank()) return 0L
        return runCatching {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", WIKIMEDIA_USER_AGENT)
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use 0L
                val contentType = resp.header("Content-Type").orEmpty()
                if (contentType.contains("text/html", ignoreCase = true) ||
                    contentType.contains("application/json", ignoreCase = true)
                ) {
                    return@use 0L
                }
                val body = resp.body ?: return@use 0L
                val temp = File(target.parentFile, "${target.name}.tmp")
                body.byteStream().use { input ->
                    temp.outputStream().use { output -> input.copyTo(output) }
                }
                if (temp.length() <= 512L) {
                    temp.delete()
                    return@use 0L
                }

                val durMs = extractPlayableDurationMs(temp)
                val valid = if (enforceShort) {
                    durMs in MIN_SHORT_DURATION_MS..MAX_SHORT_DURATION_MS
                } else {
                    durMs > 0L
                }
                if (valid) {
                    if (target.exists()) target.delete()
                    if (temp.renameTo(target)) durMs else 0L
                } else {
                    temp.delete()
                    0L
                }
            }
        }.getOrDefault(0L)
    }

    private fun extractPlayableDurationMs(file: File): Long {
        val mmrDuration = runCatching {
            val mmr = MediaMetadataRetriever()
            try {
                mmr.setDataSource(file.absolutePath)
                mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            } finally {
                mmr.release()
            }
        }.getOrDefault(0L)

        if (mmrDuration > 0L) return mmrDuration

        return runCatching {
            val mp = MediaPlayer()
            try {
                mp.setDataSource(file.absolutePath)
                mp.prepare()
                mp.duration.toLong().coerceAtLeast(0L)
            } finally {
                mp.release()
            }
        }.getOrDefault(0L)
    }

    private fun guessExtension(url: String): String {
        val clean = url.substringBefore('?').lowercase()
        return when {
            clean.endsWith(".ogg") -> "ogg"
            clean.endsWith(".oga") -> "oga"
            clean.endsWith(".opus") -> "opus"
            clean.endsWith(".wav") -> "wav"
            clean.endsWith(".m4a") -> "m4a"
            else -> "mp3"
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

    private fun normalizeArabic(input: String): String {
        return input
            .replace(Regex("[\u0610-\u061A\u064B-\u065F\u0670\u06D6-\u06ED]"), "")
            .replace('أ', 'ا')
            .replace('إ', 'ا')
            .replace('آ', 'ا')
            .replace('ى', 'ي')
            .replace('ة', 'ه')
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    companion object {
        private const val MIN_SHORT_DURATION_MS = 250L
        private const val MAX_SHORT_DURATION_MS = 5500L
        private const val WIKIMEDIA_USER_AGENT = "NoorIslamicApp/1.0 (https://github.com/noor-app)"
        private const val GITHUB_CATALOG_URL = "https://raw.githubusercontent.com/rn0x/Adhkar-json/main/adhkar.json"
        private const val HISNMUSLIM_AUDIO_BASE = "https://www.hisnmuslim.com/audio/ar/"
    }
}
