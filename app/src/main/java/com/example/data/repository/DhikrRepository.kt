package com.example.data.repository

import android.content.Context
import com.example.data.local.DhikrDao
import com.example.data.local.DhikrEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.random.Random

class DhikrRepository(
    private val context: Context,
    private val dhikrDao: DhikrDao
) {
    val dhikrListFlow: Flow<List<DhikrEntity>> = dhikrDao.observeAll()

    private data class CatalogSeed(
        val id: String,
        val titleAr: String,
        val textAr: String,
        val categoryAr: String
    )

    private val seedList = listOf(
        CatalogSeed(
            id = "dhikr_short_subhanallah",
            titleAr = "سبحان الله",
            textAr = "سُبْحَانَ اللَّهِ",
            categoryAr = "التسبيح"
        ),
        CatalogSeed(
            id = "dhikr_short_alhamdulillah",
            titleAr = "الحمد لله",
            textAr = "الْحَمْدُ لِلَّهِ",
            categoryAr = "التحميد"
        ),
        CatalogSeed(
            id = "dhikr_short_hawqalah",
            titleAr = "لا حول ولا قوة إلا بالله",
            textAr = "لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ",
            categoryAr = "الحوقلة"
        ),
        CatalogSeed(
            id = "dhikr_short_hasbunallah",
            titleAr = "حسبي الله ونعم الوكيل",
            textAr = "حَسْبِيَ اللَّهُ وَنِعْمَ الْوَكِيلُ",
            categoryAr = "التوكل"
        ),
        CatalogSeed(
            id = "dhikr_short_allahuakbar",
            titleAr = "الله أكبر",
            textAr = "اللَّهُ أَكْبَرُ",
            categoryAr = "التكبير"
        ),
        CatalogSeed(
            id = "dhikr_short_salawat",
            titleAr = "صلِّ على النبي ﷺ",
            textAr = "اللَّهُمَّ صَلِّ عَلَى مُحَمَّدٍ",
            categoryAr = "الصلاة على النبي ﷺ"
        ),
        CatalogSeed(
            id = "dhikr_short_astaghfirullah",
            titleAr = "أستغفر الله",
            textAr = "أَسْتَغْفِرُ اللَّهَ",
            categoryAr = "الاستغفار"
        ),
        CatalogSeed(
            id = "dhikr_short_tahlil",
            titleAr = "لا إله إلا الله",
            textAr = "لَا إِلَهَ إِلَّا اللَّهُ",
            categoryAr = "التهليل"
        )
    )

    suspend fun ensureSeeded() = withContext(Dispatchers.IO) {
        val entities = seedList.map { seed ->
            DhikrEntity(
                id = seed.id,
                titleAr = seed.titleAr,
                textAr = seed.textAr,
                categoryAr = seed.categoryAr,
                reciterAr = "",
                audioUrl = "",
                fallbackAudioUrl = "",
                localFilePath = null,
                isDownloaded = false,
                isDownloading = false,
                isEnabled = true,
                isCustom = false,
                durationMs = 0L
            )
        }
        dhikrDao.insertAllIgnore(entities)
        purgeAllHumanVoiceFiles()
    }

    private suspend fun purgeAllHumanVoiceFiles() {
        val dir = File(context.filesDir, "dhikr_audio")
        if (dir.exists()) {
            runCatching { dir.deleteRecursively() }
        }
        dhikrDao.clearAllAudioMetadata()
    }

    suspend fun addCustomDhikr(
        titleAr: String,
        textAr: String,
        categoryAr: String = "مخصص من المستخدم"
    ): Result<DhikrEntity> = withContext(Dispatchers.IO) {
        try {
            val id = "custom_dhikr_${System.currentTimeMillis()}"
            val entity = DhikrEntity(
                id = id,
                titleAr = titleAr.trim(),
                textAr = textAr.trim(),
                categoryAr = categoryAr,
                reciterAr = "",
                audioUrl = "",
                fallbackAudioUrl = "",
                localFilePath = null,
                isDownloaded = false,
                isDownloading = false,
                isEnabled = true,
                isCustom = true,
                durationMs = 0L
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
        dhikrDao.deleteCustomById(dhikr.id)
    }

    suspend fun pickRandomDhikr(excludeId: String?): DhikrEntity? = withContext(Dispatchers.IO) {
        ensureSeeded()
        val enabled = dhikrDao.getEnabledOnce()
        if (enabled.isEmpty()) return@withContext null

        val nonRepeating = if (enabled.size > 1 && excludeId != null) {
            enabled.filter { it.id != excludeId }
        } else {
            enabled
        }
        val chosen = nonRepeating.random(Random)
        dhikrDao.markLastPlayed(chosen.id, System.currentTimeMillis())
        chosen
    }
}
