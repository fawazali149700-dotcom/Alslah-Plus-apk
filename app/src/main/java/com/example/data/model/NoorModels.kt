package com.example.data.model

import com.batoulapps.adhan.CalculationMethod
import com.batoulapps.adhan.Madhab

enum class PrayerName(val arabicName: String, val isObligatory: Boolean) {
    FAJR("الفجر", true),
    SUNRISE("الشروق", false),
    DHUHR("الظهر", true),
    ASR("العصر", true),
    MAGHRIB("المغرب", true),
    ISHA("العشاء", true)
}

data class PrayerItem(
    val prayer: PrayerName,
    val adhanTimeMillis: Long,
    val iqamahTimeMillis: Long?,
    val iqamahOffsetMinutes: Int,
    val formattedAdhanTime: String,
    val formattedIqamahTime: String?,
    val isNext: Boolean,
    val isPassed: Boolean
)

data class HijriDateModel(
    val day: Int,
    val month: Int,
    val year: Int,
    val monthNameAr: String,
    val weekdayAr: String
) {
    val formattedAr: String
        get() = "$weekdayAr، $day $monthNameAr $year هـ"

    val shortFormattedAr: String
        get() = "$day $monthNameAr $year هـ"
}

data class DailyPrayerSchedule(
    val prayers: List<PrayerItem>,
    val nextPrayer: PrayerItem?,
    val remainingMillis: Long,
    val qiblaDegrees: Double,
    val hijriDate: HijriDateModel,
    val gregorianDateAr: String,
    val cityName: String,
    val countryName: String,
    val midnightTimeMillis: Long,
    val formattedMidnightTime: String,
    val lastThirdNightMillis: Long,
    val formattedLastThirdNight: String
)

data class WeatherData(
    val tempC: Double,
    val minTempC: Double,
    val maxTempC: Double,
    val humidity: Int,
    val windSpeedKmh: Double,
    val conditionAr: String,
    val weatherCode: Int,
    val sourceLabel: String,
    val updatedAtMillis: Long
)

enum class CalculationMethodOption(
    val titleAr: String,
    val adhanMethod: CalculationMethod
) {
    UMM_AL_QURA("أم القرى (مكة المكرمة)", CalculationMethod.UMM_AL_QURA),
    MUSLIM_WORLD_LEAGUE("رابطة العالم الإسلامي", CalculationMethod.MUSLIM_WORLD_LEAGUE),
    EGYPTIAN("الهيئة المصرية العامة للمساحة", CalculationMethod.EGYPTIAN),
    KARACHI("جامعة العلوم الإسلامية بكراتشي", CalculationMethod.KARACHI),
    NORTH_AMERICA("الجمعية الإسلامية بأمريكا الشمالية (ISNA)", CalculationMethod.NORTH_AMERICA),
    DUBAI("دبي", CalculationMethod.DUBAI),
    KUWAIT("الكويت", CalculationMethod.KUWAIT),
    QATAR("قطر", CalculationMethod.QATAR),
    SINGAPORE("سنغافورة", CalculationMethod.SINGAPORE)
}

enum class MadhabOption(val titleAr: String, val adhanMadhab: Madhab) {
    SHAFI("الجمهور (الشافعي والمالكي والحنبلي)", Madhab.SHAFI),
    HANAFI("المذهب الحنفي", Madhab.HANAFI)
}

enum class PrePrayerToneOption(val titleAr: String) {
    DEFAULT_NOTIFICATION("نغمة الإشعار الافتراضية"),
    GENTLE_ALARM("نغمة تنبيه هادئة"),
    RINGTONE_CHIME("رنين تنبيه واضح"),
    VIBRATE_ONLY("اهتزاز فقط")
}

enum class IqamahSoundMode(val titleAr: String) {
    DOWNLOADED_HUMAN("صوت إقامة بشري منزّل (يعمل بدون إنترنت)"),
    VOICE_ANNOUNCEMENT("إعلان صوتي: اقتربت إقامة صلاة ..."),
    CUSTOM_AUDIO("ملف صوتي من ذاكرة الهاتف / ZIP"),
    SYSTEM_TONE("نغمة تنبيه النظام")
}

data class IqamahPreset(
    val id: String,
    val titleAr: String,
    val subtitleAr: String,
    val metadataUrl: String,
    val fallbackDirectUrl: String
)

enum class TemperatureUnit(val symbol: String, val titleAr: String) {
    CELSIUS("°C", "مئوية (°C)"),
    FAHRENHEIT("°F", "فهرنهايت (°F)")
}

enum class WidgetTypeKey(val titleAr: String) {
    COMPACT_4X1("شريط شامل (1×4)"),
    PRAYER_4X2("أوقات الصلاة (2×4)"),
    COMPREHENSIVE_4X2("الودجت الشامل (2×4)"),
    DASHBOARD_4X3("لوحة المواقيت (3×4)"),
    GRAND_4X4("اللوحة الكبرى (4×4)"),
    QIYAM_2X2("قيام الليل والصلوات (2×2)"),
    HIJRI_3X1("التاريخ الهجري (1×3)"),
    WEATHER_3X2("حالة الطقس (2×3)")
}

data class WidgetAppearanceConfig(
    val sizeScale: Float = 1.0f,
    val fontScale: Float = 1.0f,
    val showClock: Boolean = false,
    val showGregorianDate: Boolean = false
) {
    companion object {
        fun defaultFor(key: WidgetTypeKey): WidgetAppearanceConfig = when (key) {
            WidgetTypeKey.COMPREHENSIVE_4X2 -> WidgetAppearanceConfig(showClock = true, showGregorianDate = false)
            WidgetTypeKey.DASHBOARD_4X3 -> WidgetAppearanceConfig(showClock = false, showGregorianDate = true)
            WidgetTypeKey.GRAND_4X4 -> WidgetAppearanceConfig(showClock = true, showGregorianDate = true)
            WidgetTypeKey.HIJRI_3X1 -> WidgetAppearanceConfig(showClock = false, showGregorianDate = true)
            else -> WidgetAppearanceConfig(showClock = false, showGregorianDate = false)
        }
    }
}

data class PerPrayerIqamahOffsets(
    val fajrMinutes: Int = 20,
    val dhuhrMinutes: Int = 10,
    val asrMinutes: Int = 10,
    val maghribMinutes: Int = 5,
    val ishaMinutes: Int = 10
) {
    fun offsetFor(prayer: PrayerName): Int = when (prayer) {
        PrayerName.FAJR -> fajrMinutes
        PrayerName.DHUHR -> dhuhrMinutes
        PrayerName.ASR -> asrMinutes
        PrayerName.MAGHRIB -> maghribMinutes
        PrayerName.ISHA -> ishaMinutes
        PrayerName.SUNRISE -> 0
    }
}

data class PerPrayerToggleConfig(
    val fajr: Boolean = true,
    val dhuhr: Boolean = true,
    val asr: Boolean = true,
    val maghrib: Boolean = true,
    val isha: Boolean = true
) {
    fun isEnabled(prayer: PrayerName): Boolean = when (prayer) {
        PrayerName.FAJR -> fajr
        PrayerName.DHUHR -> dhuhr
        PrayerName.ASR -> asr
        PrayerName.MAGHRIB -> maghrib
        PrayerName.ISHA -> isha
        PrayerName.SUNRISE -> false
    }

    fun withPrayer(prayer: PrayerName, enabled: Boolean): PerPrayerToggleConfig = when (prayer) {
        PrayerName.FAJR -> copy(fajr = enabled)
        PrayerName.DHUHR -> copy(dhuhr = enabled)
        PrayerName.ASR -> copy(asr = enabled)
        PrayerName.MAGHRIB -> copy(maghrib = enabled)
        PrayerName.ISHA -> copy(isha = enabled)
        PrayerName.SUNRISE -> this
    }
}

data class CustomAudioAttachment(
    val path: String? = null,
    val displayName: String? = null
)

data class CityPreset(
    val nameAr: String,
    val latitude: Double,
    val longitude: Double
)

data class CountryEntry(
    val nameAr: String,
    val cities: List<CityPreset>
)

enum class DhikrAlertMode(val titleAr: String, val descAr: String) {
    TEXT_WITH_TONE("كتابي مع نغمة تنبيه", "عرض الذكر على الشاشة مع نغمة إشعار لطيفة"),
    TEXT_ONLY("كتابي صامت (بدون صوت)", "عرض الذكر كتابياً على الشاشة بدون أي صوت")
}

enum class DhikrScreenPosition(val titleAr: String, val descAr: String) {
    TOP("أعلى الشاشة", "يظهر الإشعار أعلى الشاشة بوضوح"),
    CENTER("وسط الشاشة", "يظهر في منتصف الشاشة بتصميم أنيق"),
    BOTTOM("أسفل الشاشة", "يظهر أسفل الشاشة دون حجب المحتوى"),
    HIDDEN("إخفاء من الشاشة", "إخفاء الإشعار المرئي عن الشاشة")
}

data class DhikrSettings(
    val enabled: Boolean = true,
    val mode: DhikrAlertMode = DhikrAlertMode.TEXT_WITH_TONE,
    val dailyCount: Int = 5,
    val screenPosition: DhikrScreenPosition = DhikrScreenPosition.TOP,
    val activeStartHour: Int = 7,
    val activeEndHour: Int = 22,
    val lastTriggeredDhikrId: String? = null
)

data class ActiveDhikrBanner(
    val id: String,
    val title: String,
    val text: String,
    val categoryLabel: String,
    val position: DhikrScreenPosition,
    val triggerTimestamp: Long = System.currentTimeMillis()
)

data class UserSettings(
    val calculationMethod: CalculationMethodOption = CalculationMethodOption.UMM_AL_QURA,
    val madhab: MadhabOption = MadhabOption.SHAFI,
    val adhanEnabled: Boolean = true,
    val perPrayerAdhanEnabled: PerPrayerToggleConfig = PerPrayerToggleConfig(),
    val selectedMuezzinId: String = "makkah_mullah",
    val iqamahAlertEnabled: Boolean = true,
    val perPrayerIqamahEnabled: PerPrayerToggleConfig = PerPrayerToggleConfig(),
    val iqamahSoundMode: IqamahSoundMode = IqamahSoundMode.VOICE_ANNOUNCEMENT,
    val selectedIqamahPresetId: String = "iqamah_makkah_basnawy",
    val downloadedIqamahPath: String? = null,
    val downloadedIqamahName: String? = null,
    val customIqamahAudioPath: String? = null,
    val customIqamahAudioName: String? = null,
    val perPrayerIqamahAudio: Map<PrayerName, CustomAudioAttachment> = emptyMap(),
    val iqamahOffsets: PerPrayerIqamahOffsets = PerPrayerIqamahOffsets(),
    val prePrayerReminderEnabled: Boolean = true,
    val perPrayerPrePrayerEnabled: PerPrayerToggleConfig = PerPrayerToggleConfig(),
    val perPrayerPrePrayerAudio: Map<PrayerName, CustomAudioAttachment> = emptyMap(),
    val prePrayerSpeakAnnouncement: Boolean = true,
    val prePrayerMinutes: Int = 15,
    val prePrayerTone: PrePrayerToneOption = PrePrayerToneOption.DEFAULT_NOTIFICATION,
    val useGpsLocation: Boolean = true,
    val latitude: Double = 21.4225,
    val longitude: Double = 39.8262,
    val cityName: String = "مكة المكرمة",
    val countryName: String = "المملكة العربية السعودية",
    val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    val widgetConfigs: Map<WidgetTypeKey, WidgetAppearanceConfig> =
        WidgetTypeKey.entries.associateWith { WidgetAppearanceConfig.defaultFor(it) },
    val dhikrSettings: DhikrSettings = DhikrSettings(),
    val hourlyAnnouncerEnabled: Boolean = false,
    val silencedHours: Set<Int> = setOf(0, 1, 2, 3, 4, 5)
)
