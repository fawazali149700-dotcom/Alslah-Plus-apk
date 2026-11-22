package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.local.DhikrDao
import com.example.data.local.DhikrEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.File
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
        val matchKeywords: List<String>,
        val defaultAudioPath: String
    )

    private data class RemoteTrack(
        val normalizedText: String,
        val rawAudioUrl: String,
        val fallbackUrl: String
    )

    private val seedList = listOf(
        CatalogSeed(
            id = "dhikr_subhanallah_wabihamdihi",
            titleAr = "سبحان الله وبحمده",
            textAr = "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ، سُبْحَانَ اللَّهِ الْعَظِيمِ",
            categoryAr = "التسبيح والتهليل",
            matchKeywords = listOf("سبحان الله وبحمده"),
            defaultAudioPath = "91.mp3"
        ),
        CatalogSeed(
            id = "dhikr_remember_allah",
            titleAr = "اذكر الله يذكرك",
            textAr = "اذْكُرِ اللَّهَ يَذْكُرْكَ — أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ",
            categoryAr = "التسبيح والتهليل",
            matchKeywords = listOf("لا اله الا الله وحده لا شريك له"),
            defaultAudioPath = "90.mp3"
        ),
        CatalogSeed(
            id = "dhikr_salawat",
            titleAr = "صلِّ على النبي ﷺ",
            textAr = "اللَّهُمَّ صَلِّ وَسَلِّمْ وَبَارِكْ عَلَى نَبِيِّنَا مُحَمَّدٍ",
            categoryAr = "الصلاة على النبي ﷺ",
            matchKeywords = listOf("اللهم صل وسلم على نبينا محمد", "اللهم صل على محمد"),
            defaultAudioPath = "99.mp3"
        ),
        CatalogSeed(
            id = "dhikr_istighfar",
            titleAr = "أستغفر الله وأتوب إليه",
            textAr = "أَسْتَغْفِرُ اللَّهَ الْعَظِيمَ وَأَتُوبُ إِلَيْهِ",
            categoryAr = "الاستغفار",
            matchKeywords = listOf("استغفر الله واتوب اليه", "استغفر الله"),
            defaultAudioPath = "97.mp3"
        ),
        CatalogSeed(
            id = "dhikr_hawqalah",
            titleAr = "لا حول ولا قوة إلا بالله",
            textAr = "لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ الْعَلِيِّ الْعَظِيمِ",
            categoryAr = "التسبيح والتهليل",
            matchKeywords = listOf("لا حول ولا قوة الا بالله"),
            defaultAudioPath = "255.mp3"
        ),
        CatalogSeed(
            id = "dhikr_tahlil_complete",
            titleAr = "لا إله إلا الله وحده لا شريك له",
            textAr = "لَا إِلَهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ",
            categoryAr = "التسبيح والتهليل",
            matchKeywords = listOf("له الملك وله الحمد وهو على كل شيء قدير"),
            defaultAudioPath = "92.mp3"
        ),
        CatalogSeed(
            id = "dhikr_baqiyat_salihat",
            titleAr = "الباقيات الصالحات",
            textAr = "سُبْحَانَ اللَّهِ، وَالْحَمْدُ لِلَّهِ، وَلَا إِلَهَ إِلَّا اللَّهُ، وَاللَّهُ أَكْبَرُ",
            categoryAr = "التسبيح والتهليل",
            matchKeywords = listOf("سبحان الله والحمد لله ولا اله الا الله"),
            defaultAudioPath = "83.mp3"
        ),
        CatalogSeed(
            id = "dhikr_sayyid_istighfar",
            titleAr = "سيد الاستغفار",
            textAr = "اللَّهُمَّ أَنْتَ رَبِّي لَا إِلَهَ إِلَّا أَنْتَ، خَلَقْتَنِي وَأَنَا عَبْدُكَ، أَبُوءُ لَكَ بِنِعْمَتِكَ عَلَيَّ وَأَبُوءُ بِذَنْبِي فَاغْفِرْ لِي",
            categoryAr = "الاستغفار",
            matchKeywords = listOf("اللهم انت ربي لا اله الا انت خلقتني"),
            defaultAudioPath = "79.mp3"
        ),
        CatalogSeed(
            id = "dhikr_hasbi_allah",
            titleAr = "حسبي الله لا إله إلا هو",
            textAr = "حَسْبِيَ اللَّهُ لَا إِلَهَ إِلَّا هُوَ عَلَيْهِ تَوَكَّلْتُ وَهُوَ رَبُّ الْعَرْشِ الْعَظِيمِ",
            categoryAr = "التحصين والذكر",
            matchKeywords = listOf("حسبي الله لا اله الا هو عليه توكلت"),
            defaultAudioPath = "86.mp3"
        ),
        CatalogSeed(
            id = "dhikr_yunus_dua",
            titleAr = "دعاء ذي النون",
            textAr = "لَا إِلَهَ إِلَّا أَنْتَ سُبْحَانَكَ إِنِّي كُنْتُ مِنَ الظَّالِمِينَ",
            categoryAr = "الاستغفار",
            matchKeywords = listOf("لا اله الا انت سبحانك اني كنت من الظالمين"),
            defaultAudioPath = "122.mp3"
        ),
        CatalogSeed(
            id = "dhikr_ya_hayyu_ya_qayyum",
            titleAr = "يا حي يا قيوم برحمتك أستغيث",
            textAr = "يَا حَيُّ يَا قَيُّومُ بِرَحْمَتِكَ أَسْتَغِيثُ أَصْلِحْ لِي شَأْنِي كُلَّهُ وَلَا تَكِلْنِي إِلَى نَفْسِي طَرْفَةَ عَيْنٍ",
            categoryAr = "التحصين والذكر",
            matchKeywords = listOf("يا حي يا قيوم برحمتك استغيث"),
            defaultAudioPath = "93.mp3"
        ),
        CatalogSeed(
            id = "dhikr_radeetu_billahi",
            titleAr = "رضيت بالله رباً",
            textAr = "رَضِيتُ بِاللَّهِ رَبًّا، وَبِالْإِسْلَامِ دِينًا، وَبِمُحَمَّدٍ صَلَّى اللَّهُ عَلَيْهِ وَسَلَّمَ نَبِيًّا",
            categoryAr = "التحصين والذكر",
            matchKeywords = listOf("رضيت بالله ربا وبالاسلام دينا"),
            defaultAudioPath = "89.mp3"
        ),
        CatalogSeed(
            id = "dhikr_tasbeeh_khalqihi",
            titleAr = "سبحان الله عدد خلقه",
            textAr = "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ عَدَدَ خَلْقِهِ، وَرِضَا نَفْسِهِ، وَزِنَةَ عَرْشِهِ، وَمِدَادَ كَلِمَاتِهِ",
            categoryAr = "التسبيح والتهليل",
            matchKeywords = listOf("عدد خلقه ورضا نفسه وزنة عرشه"),
            defaultAudioPath = "95.mp3"
        ),
        CatalogSeed(
            id = "dhikr_ainni_ala_dhikrika",
            titleAr = "اللهم أعنّي على ذكرك",
            textAr = "اللَّهُمَّ أَعِنِّي عَلَى ذِكْرِكَ، وَشُكْرِكَ، وَحُسْنِ عِبَادَتِكَ",
            categoryAr = "التحصين والذكر",
            matchKeywords = listOf("اللهم اعني على ذكرك وشكرك"),
            defaultAudioPath = "72.mp3"
        ),
        CatalogSeed(
            id = "dhikr_afuw",
            titleAr = "اللهم إنك عفو تحب العفو",
            textAr = "اللَّهُمَّ إِنَّكَ عَفُوٌّ تُحِبُّ الْعَفْوَ فَاعْفُ عَنِّي",
            categoryAr = "الاستغفار",
            matchKeywords = listOf("اللهم انك عفو تحب العفو"),
            defaultAudioPath = "194.mp3"
        ),
        CatalogSeed(
            id = "dhikr_bismillah_layadurr",
            titleAr = "بسم الله الذي لا يضر مع اسمه شيء",
            textAr = "بِسْمِ اللَّهِ الَّذِي لَا يَضُرُّ مَعَ اسْمِهِ شَيْءٌ فِي الْأَرْضِ وَلَا فِي السَّمَاءِ وَهُوَ السَّمِيعُ الْعَلِيمُ",
            categoryAr = "التحصين والذكر",
            matchKeywords = listOf("بسم الله الذي لا يضر مع اسمه شيء"),
            defaultAudioPath = "88.mp3"
        ),
        CatalogSeed(
            id = "dhikr_rabbi_ighfirli",
            titleAr = "رب اغفر لي وتب علي",
            textAr = "رَبِّ اغْفِرْ لِي وَتُبْ عَلَيَّ إِنَّكَ أَنْتَ التَّوَّابُ الرَّحِيمُ",
            categoryAr = "الاستغفار",
            matchKeywords = listOf("رب اغفر لي وتب علي"),
            defaultAudioPath = "250.mp3"
        ),
        CatalogSeed(
            id = "dhikr_ilman_nafia",
            titleAr = "أسألك علماً نافعاً ورزقاً طيباً",
            textAr = "اللَّهُمَّ إِنِّي أَسْأَلُكَ عِلْمًا نَافِعًا، وَرِزْقًا طَيِّبًا، وَعَمَلًا مُتَقَبَّلًا",
            categoryAr = "التحصين والذكر",
            matchKeywords = listOf("اللهم اني اسالك علما نافعا"),
            defaultAudioPath = "96.mp3"
        ),
        CatalogSeed(
            id = "dhikr_salawat_ibrahimiyyah",
            titleAr = "الصلاة الإبراهيمية على النبي ﷺ",
            textAr = "اللَّهُمَّ صَلِّ عَلَى مُحَمَّدٍ وَعَلَى آلِ مُحَمَّدٍ كَمَا صَلَّيْتَ عَلَى إِبْرَاهِيمَ وَعَلَى آلِ إِبْرَاهِيمَ إِنَّكَ حَمِيدٌ مَجِيدٌ",
            categoryAr = "الصلاة على النبي ﷺ",
            matchKeywords = listOf("كما صليت على ابراهيم"),
            defaultAudioPath = "53.mp3"
        ),
        CatalogSeed(
            id = "dhikr_audhu_bikalimatillah",
            titleAr = "أعوذ بكلمات الله التامات",
            textAr = "أَعُوذُ بِكَلِمَاتِ اللَّهِ التَّامَّاتِ مِنْ شَرِّ مَا خَلَقَ",
            categoryAr = "التحصين والذكر",
            matchKeywords = listOf("اعوذ بكلمات الله التامات من شر ما خلق"),
            defaultAudioPath = "98.mp3"
        ),
        CatalogSeed(
            id = "dhikr_ayat_alkursi",
            titleAr = "آية الكرسي",
            textAr = "اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ ۚ لَا تَأْخُذُهُ سِنَةٌ وَلَا نَوْمٌ...",
            categoryAr = "التحصين والذكر",
            matchKeywords = listOf("الله لا اله الا هو الحي القيوم لا تاخذه سنة"),
            defaultAudioPath = "75.mp3"
        ),
        CatalogSeed(
            id = "dhikr_alhamdulillah_hamdan",
            titleAr = "الحمد لله حمداً كثيراً طيباً",
            textAr = "الْحَمْدُ لِلَّهِ حَمْدًا كَثِيرًا طَيِّبًا مُبَارَكًا فِيهِ",
            categoryAr = "التسبيح والتهليل",
            matchKeywords = listOf("حمدا كثيرا طيبا مباركا فيه", "اصبحنا واصبح الملك لله"),
            defaultAudioPath = "78.mp3"
        ),
        CatalogSeed(
            id = "dhikr_afiyah",
            titleAr = "اللهم إني أسألك العافية",
            textAr = "اللَّهُمَّ إِنِّي أَسْأَلُكَ الْعَفْوَ وَالْعَافِيَةَ فِي الدُّنْيَا وَالْآخِرَةِ",
            categoryAr = "التحصين والذكر",
            matchKeywords = listOf("اللهم اني اسالك العفو والعافية"),
            defaultAudioPath = "84.mp3"
        ),
        CatalogSeed(
            id = "dhikr_ashadu_kafa",
            titleAr = "اللهم بك أصبحنا وبك أمسينا",
            textAr = "اللَّهُمَّ بِكَ أَصْبَحْنَا، وَبِكَ أَمْسَيْنَا، وَبِكَ نَحْيَا، وَبِكَ نَمُوتُ، وَإِلَيْكَ النُّشُورُ",
            categoryAr = "التحصين والذكر",
            matchKeywords = listOf("اللهم بك اصبحنا وبك امسينا"),
            defaultAudioPath = "80.mp3"
        )
    )

    suspend fun ensureSeeded() = withContext(Dispatchers.IO) {
        val entities = seedList.map { seed ->
            DhikrEntity(
                id = seed.id,
                titleAr = seed.titleAr,
                textAr = seed.textAr,
                categoryAr = seed.categoryAr,
                reciterAr = "الشيخ حمد الدريهم — حصن المسلم",
                audioUrl = "$GITHUB_AUDIO_BASE${seed.defaultAudioPath}",
                fallbackAudioUrl = "$HISNMUSLIM_AUDIO_BASE${seed.defaultAudioPath}",
                isDownloaded = false,
                isDownloading = false,
                isEnabled = true,
                isCustom = false
            )
        }
        dhikrDao.insertAllIgnore(entities)
        reconcileLocalFiles()
    }

    private suspend fun reconcileLocalFiles() {
        val all = dhikrDao.getAllOnce()
        for (item in all) {
            val path = item.localFilePath
            if (item.isDownloaded && (path.isNullOrBlank() || !File(path).exists() || File(path).length() <= 2048L)) {
                dhikrDao.clearDownload(item.id)
            }
        }
    }

    /**
     * Fetches the live open-source Hisn Al-Muslim JSON catalog (human voice MP3 tracks by Sheikh Hamad Al-Duraikhim)
     * and maps exact verified MP3 tracks onto each seeded Dhikr so all links resolve cleanly.
     */
    suspend fun syncCatalogFromOfficialJson(): Boolean = withContext(Dispatchers.IO) {
        ensureSeeded()
        val jsonBody = fetchText(GITHUB_CATALOG_URL) ?: return@withContext false

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

                    val cleanRel = audioRel.removePrefix("/")
                    val githubUrl = if (cleanRel.startsWith("http")) cleanRel
                    else "https://raw.githubusercontent.com/rn0x/Adhkar-json/main/$cleanRel"

                    val filePart = if (filename.isNotBlank()) "$filename.mp3" else cleanRel.substringAfterLast('/')
                    val mirrorUrl = "$HISNMUSLIM_AUDIO_BASE$filePart"

                    parsedTracks.add(
                        RemoteTrack(
                            normalizedText = normalizeArabic(rawText),
                            rawAudioUrl = githubUrl,
                            fallbackUrl = mirrorUrl
                        )
                    )
                }
            }
        }

        if (parsedTracks.isEmpty()) return@withContext false

        // Match each seed against exact verified tracks from adhkar.json
        for (seed in seedList) {
            val matched = parsedTracks.firstOrNull { track ->
                seed.matchKeywords.any { kw ->
                    track.normalizedText.contains(normalizeArabic(kw))
                }
            }
            if (matched != null) {
                dhikrDao.updateAudioUrls(
                    id = seed.id,
                    audioUrl = matched.rawAudioUrl,
                    fallbackUrl = matched.fallbackUrl
                )
            }
        }
        true
    }

    suspend fun downloadAllHumanVoices(): Pair<Int, Int> = withContext(Dispatchers.IO) {
        syncCatalogFromOfficialJson()
        val items = dhikrDao.getAllOnce().filter { !it.isCustom && it.audioUrl.isNotBlank() }
        var successCount = 0
        for (item in items) {
            val existing = item.localFilePath
            if (item.isDownloaded && !existing.isNullOrBlank() && File(existing).exists() && File(existing).length() > 2048L) {
                successCount++
                continue
            }
            val result = downloadSingleHumanVoice(item)
            if (result.isSuccess) {
                successCount++
            }
        }
        successCount to items.size
    }

    suspend fun ensureCoreVoicesReady(): Int = withContext(Dispatchers.IO) {
        ensureSeeded()
        val alreadyReady = dhikrDao.getAllOnce().count {
            it.isDownloaded && !it.localFilePath.isNullOrBlank() && File(it.localFilePath).exists()
        }
        if (alreadyReady >= 6) return@withContext alreadyReady

        syncCatalogFromOfficialJson()
        val pending = dhikrDao.getEnabledOnce().filter { !it.isDownloaded && it.audioUrl.isNotBlank() }.take(8)
        var downloaded = alreadyReady
        for (item in pending) {
            if (downloadSingleHumanVoice(item).isSuccess) {
                downloaded++
            }
        }
        downloaded
    }

    suspend fun downloadSingleHumanVoice(dhikr: DhikrEntity): Result<String> = withContext(Dispatchers.IO) {
        if (dhikr.audioUrl.isBlank() && dhikr.fallbackAudioUrl.isBlank()) {
            return@withContext Result.failure(IllegalStateException("لا يوجد رابط تنزيل لهذا الذكر"))
        }
        dhikrDao.setDownloading(dhikr.id, true)
        try {
            val dir = File(context.filesDir, "dhikr_audio").apply { if (!exists()) mkdirs() }
            val outFile = File(dir, "${dhikr.id}.mp3")

            val downloadedOk = downloadToFile(dhikr.audioUrl, outFile) ||
                (dhikr.fallbackAudioUrl.isNotBlank() && downloadToFile(dhikr.fallbackAudioUrl, outFile))

            if (downloadedOk && outFile.exists() && outFile.length() > 2048L) {
                val reciter = if (dhikr.isCustom) dhikr.reciterAr else "الشيخ حمد الدريهم — حصن المسلم"
                dhikrDao.markDownloaded(dhikr.id, outFile.absolutePath, reciter)
                Result.success(outFile.absolutePath)
            } else {
                outFile.delete()
                dhikrDao.setDownloading(dhikr.id, false)
                Result.failure(IllegalStateException("تعذر تنزيل الصوت البشري، تحقق من الاتصال بالإنترنت"))
            }
        } catch (e: Exception) {
            dhikrDao.setDownloading(dhikr.id, false)
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

                // Remove old local file if owned by dhikr_audio
                dhikr.localFilePath?.let { old ->
                    runCatching {
                        val f = File(old)
                        if (f.exists() && f.parentFile?.name == "dhikr_audio") f.delete()
                    }
                }

                val label = resolveDisplayName(uri)?.substringBeforeLast('.') ?: "تسجيل بشري من الهاتف"
                dhikrDao.markDownloaded(
                    id = dhikr.id,
                    localPath = outFile.absolutePath,
                    reciterAr = "صوت بشري من الذاكرة ($label)"
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

            if (audioUri != null) {
                val dir = File(context.filesDir, "dhikr_audio").apply { if (!exists()) mkdirs() }
                val outFile = File(dir, "$id.audio")
                context.contentResolver.openInputStream(audioUri)?.use { input ->
                    outFile.outputStream().use { output -> input.copyTo(output) }
                } ?: return@withContext Result.failure(IllegalStateException("تعذر قراءة الملف الصوتي"))
                localPath = outFile.absolutePath
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
                isCustom = true
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

    /**
     * Picks a random enabled Dhikr from the database.
     * When [preferDownloadedAudio] is true, prioritizes entries that already have a real human MP3 locally,
     * and attempts an on-the-fly download if none are cached yet. Never falls back to TTS.
     */
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
                // Attempt quick catalog sync & single download for a random candidate
                syncCatalogFromOfficialJson()
                val candidate = enabled.filter { it.id != excludeId }.ifEmpty { enabled }.random(Random)
                downloadSingleHumanVoice(candidate)
                val refreshed = dhikrDao.getById(candidate.id)
                if (refreshed?.isDownloaded == true) listOf(refreshed) else enabled
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
            val req = Request.Builder().url(url).build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                resp.body?.string()
            }
        }.getOrNull()
    }

    private fun downloadToFile(url: String, target: File): Boolean {
        if (url.isBlank()) return false
        return runCatching {
            val req = Request.Builder().url(url).build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use false
                val contentType = resp.header("Content-Type").orEmpty()
                if (contentType.contains("text/html", ignoreCase = true)) return@use false
                val body = resp.body ?: return@use false
                val temp = File(target.parentFile, "${target.name}.tmp")
                body.byteStream().use { input ->
                    temp.outputStream().use { output -> input.copyTo(output) }
                }
                if (temp.length() > 2048L) {
                    if (target.exists()) target.delete()
                    temp.renameTo(target)
                } else {
                    temp.delete()
                    false
                }
            }
        }.getOrDefault(false)
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
        private const val GITHUB_CATALOG_URL = "https://raw.githubusercontent.com/rn0x/Adhkar-json/main/adhkar.json"
        private const val GITHUB_AUDIO_BASE = "https://raw.githubusercontent.com/rn0x/Adhkar-json/main/audio/"
        private const val HISNMUSLIM_AUDIO_BASE = "https://www.hisnmuslim.com/audio/ar/"
    }
}
