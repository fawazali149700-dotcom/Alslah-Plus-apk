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

enum class TemperatureUnit(val symbol: String, val titleAr: String) {
    CELSIUS("°C", "مئوية (°C)"),
    FAHRENHEIT("°F", "فهرنهايت (°F)")
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

data class CityPreset(
    val nameAr: String,
    val latitude: Double,
    val longitude: Double
)

data class CountryEntry(
    val nameAr: String,
    val cities: List<CityPreset>
)

data class UserSettings(
    val calculationMethod: CalculationMethodOption = CalculationMethodOption.UMM_AL_QURA,
    val madhab: MadhabOption = MadhabOption.SHAFI,
    val adhanEnabled: Boolean = true,
    val selectedMuezzinId: String = "makkah_mullah",
    val iqamahAlertEnabled: Boolean = true,
    val iqamahOffsets: PerPrayerIqamahOffsets = PerPrayerIqamahOffsets(),
    val prePrayerReminderEnabled: Boolean = true,
    val prePrayerMinutes: Int = 15,
    val prePrayerTone: PrePrayerToneOption = PrePrayerToneOption.DEFAULT_NOTIFICATION,
    val useGpsLocation: Boolean = true,
    val latitude: Double = 21.4225,
    val longitude: Double = 39.8262,
    val cityName: String = "مكة المكرمة",
    val countryName: String = "المملكة العربية السعودية",
    val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS
)
