package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.CalculationMethodOption
import com.example.data.model.MadhabOption
import com.example.data.model.PerPrayerIqamahOffsets
import com.example.data.model.PrayerName
import com.example.data.model.PrePrayerToneOption
import com.example.data.model.TemperatureUnit
import com.example.data.model.UserSettings
import com.example.data.model.WeatherData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.noorDataStore: DataStore<Preferences> by preferencesDataStore(name = "noor_prefs")

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val CALCULATION_METHOD = stringPreferencesKey("calculation_method")
        val MADHAB = stringPreferencesKey("madhab")
        val ADHAN_ENABLED = booleanPreferencesKey("adhan_enabled")
        val SELECTED_MUEZZIN_ID = stringPreferencesKey("selected_muezzin_id")
        val IQAMAH_ALERT_ENABLED = booleanPreferencesKey("iqamah_alert_enabled")
        val IQAMAH_FAJR_MINUTES = intPreferencesKey("iqamah_fajr_minutes")
        val IQAMAH_DHUHR_MINUTES = intPreferencesKey("iqamah_dhuhr_minutes")
        val IQAMAH_ASR_MINUTES = intPreferencesKey("iqamah_asr_minutes")
        val IQAMAH_MAGHRIB_MINUTES = intPreferencesKey("iqamah_maghrib_minutes")
        val IQAMAH_ISHA_MINUTES = intPreferencesKey("iqamah_isha_minutes")
        val PRE_PRAYER_ENABLED = booleanPreferencesKey("pre_prayer_enabled")
        val PRE_PRAYER_MINUTES = intPreferencesKey("pre_prayer_minutes")
        val PRE_PRAYER_TONE = stringPreferencesKey("pre_prayer_tone")
        val USE_GPS_LOCATION = booleanPreferencesKey("use_gps_location")
        val LATITUDE = doublePreferencesKey("latitude")
        val LONGITUDE = doublePreferencesKey("longitude")
        val CITY_NAME = stringPreferencesKey("city_name")
        val COUNTRY_NAME = stringPreferencesKey("country_name")
        val TEMPERATURE_UNIT = stringPreferencesKey("temperature_unit")

        val WEATHER_TEMP_C = doublePreferencesKey("weather_temp_c")
        val WEATHER_MIN_C = doublePreferencesKey("weather_min_c")
        val WEATHER_MAX_C = doublePreferencesKey("weather_max_c")
        val WEATHER_HUMIDITY = intPreferencesKey("weather_humidity")
        val WEATHER_WIND_KMH = doublePreferencesKey("weather_wind_kmh")
        val WEATHER_CONDITION_AR = stringPreferencesKey("weather_condition_ar")
        val WEATHER_CODE = intPreferencesKey("weather_code")
        val WEATHER_SOURCE = stringPreferencesKey("weather_source")
        val WEATHER_UPDATED_AT = longPreferencesKey("weather_updated_at")
    }

    val settingsFlow: Flow<UserSettings> = context.noorDataStore.data.map { prefs ->
        UserSettings(
            calculationMethod = prefs[Keys.CALCULATION_METHOD]?.let { name ->
                CalculationMethodOption.entries.firstOrNull { it.name == name }
            } ?: CalculationMethodOption.UMM_AL_QURA,
            madhab = prefs[Keys.MADHAB]?.let { name ->
                MadhabOption.entries.firstOrNull { it.name == name }
            } ?: MadhabOption.SHAFI,
            adhanEnabled = prefs[Keys.ADHAN_ENABLED] ?: true,
            selectedMuezzinId = prefs[Keys.SELECTED_MUEZZIN_ID] ?: "makkah_mullah",
            iqamahAlertEnabled = prefs[Keys.IQAMAH_ALERT_ENABLED] ?: true,
            iqamahOffsets = PerPrayerIqamahOffsets(
                fajrMinutes = prefs[Keys.IQAMAH_FAJR_MINUTES] ?: 20,
                dhuhrMinutes = prefs[Keys.IQAMAH_DHUHR_MINUTES] ?: 10,
                asrMinutes = prefs[Keys.IQAMAH_ASR_MINUTES] ?: 10,
                maghribMinutes = prefs[Keys.IQAMAH_MAGHRIB_MINUTES] ?: 5,
                ishaMinutes = prefs[Keys.IQAMAH_ISHA_MINUTES] ?: 10
            ),
            prePrayerReminderEnabled = prefs[Keys.PRE_PRAYER_ENABLED] ?: true,
            prePrayerMinutes = prefs[Keys.PRE_PRAYER_MINUTES] ?: 15,
            prePrayerTone = prefs[Keys.PRE_PRAYER_TONE]?.let { name ->
                PrePrayerToneOption.entries.firstOrNull { it.name == name }
            } ?: PrePrayerToneOption.DEFAULT_NOTIFICATION,
            useGpsLocation = prefs[Keys.USE_GPS_LOCATION] ?: true,
            latitude = prefs[Keys.LATITUDE] ?: 21.4225,
            longitude = prefs[Keys.LONGITUDE] ?: 39.8262,
            cityName = prefs[Keys.CITY_NAME] ?: "مكة المكرمة",
            countryName = prefs[Keys.COUNTRY_NAME] ?: "المملكة العربية السعودية",
            temperatureUnit = prefs[Keys.TEMPERATURE_UNIT]?.let { name ->
                TemperatureUnit.entries.firstOrNull { it.name == name }
            } ?: TemperatureUnit.CELSIUS
        )
    }

    val cachedWeatherFlow: Flow<WeatherData?> = context.noorDataStore.data.map { prefs ->
        val temp = prefs[Keys.WEATHER_TEMP_C] ?: return@map null
        WeatherData(
            tempC = temp,
            minTempC = prefs[Keys.WEATHER_MIN_C] ?: temp,
            maxTempC = prefs[Keys.WEATHER_MAX_C] ?: temp,
            humidity = prefs[Keys.WEATHER_HUMIDITY] ?: 0,
            windSpeedKmh = prefs[Keys.WEATHER_WIND_KMH] ?: 0.0,
            conditionAr = prefs[Keys.WEATHER_CONDITION_AR] ?: "صافي",
            weatherCode = prefs[Keys.WEATHER_CODE] ?: 0,
            sourceLabel = prefs[Keys.WEATHER_SOURCE] ?: "Open-Meteo",
            updatedAtMillis = prefs[Keys.WEATHER_UPDATED_AT] ?: 0L
        )
    }

    suspend fun getSettingsOnce(): UserSettings = settingsFlow.first()

    suspend fun getCachedWeatherOnce(): WeatherData? = cachedWeatherFlow.first()

    suspend fun updateCalculationMethod(method: CalculationMethodOption) {
        context.noorDataStore.edit { it[Keys.CALCULATION_METHOD] = method.name }
    }

    suspend fun updateMadhab(madhab: MadhabOption) {
        context.noorDataStore.edit { it[Keys.MADHAB] = madhab.name }
    }

    suspend fun updateAdhanEnabled(enabled: Boolean) {
        context.noorDataStore.edit { it[Keys.ADHAN_ENABLED] = enabled }
    }

    suspend fun updateSelectedMuezzin(muezzinId: String) {
        context.noorDataStore.edit { it[Keys.SELECTED_MUEZZIN_ID] = muezzinId }
    }

    suspend fun updateIqamahAlertEnabled(enabled: Boolean) {
        context.noorDataStore.edit { it[Keys.IQAMAH_ALERT_ENABLED] = enabled }
    }

    suspend fun updatePrayerIqamahOffset(prayer: PrayerName, minutes: Int) {
        val safeMinutes = minutes.coerceIn(0, 60)
        context.noorDataStore.edit { prefs ->
            when (prayer) {
                PrayerName.FAJR -> prefs[Keys.IQAMAH_FAJR_MINUTES] = safeMinutes
                PrayerName.DHUHR -> prefs[Keys.IQAMAH_DHUHR_MINUTES] = safeMinutes
                PrayerName.ASR -> prefs[Keys.IQAMAH_ASR_MINUTES] = safeMinutes
                PrayerName.MAGHRIB -> prefs[Keys.IQAMAH_MAGHRIB_MINUTES] = safeMinutes
                PrayerName.ISHA -> prefs[Keys.IQAMAH_ISHA_MINUTES] = safeMinutes
                PrayerName.SUNRISE -> Unit
            }
        }
    }

    suspend fun updatePrePrayerReminderEnabled(enabled: Boolean) {
        context.noorDataStore.edit { it[Keys.PRE_PRAYER_ENABLED] = enabled }
    }

    suspend fun updatePrePrayerMinutes(minutes: Int) {
        context.noorDataStore.edit { it[Keys.PRE_PRAYER_MINUTES] = minutes }
    }

    suspend fun updatePrePrayerTone(tone: PrePrayerToneOption) {
        context.noorDataStore.edit { it[Keys.PRE_PRAYER_TONE] = tone.name }
    }

    suspend fun updateUseGpsLocation(useGps: Boolean) {
        context.noorDataStore.edit { it[Keys.USE_GPS_LOCATION] = useGps }
    }

    suspend fun updateLocation(
        latitude: Double,
        longitude: Double,
        cityName: String,
        countryName: String = ""
    ) {
        context.noorDataStore.edit {
            it[Keys.LATITUDE] = latitude
            it[Keys.LONGITUDE] = longitude
            it[Keys.CITY_NAME] = cityName
            if (countryName.isNotBlank()) it[Keys.COUNTRY_NAME] = countryName
        }
    }

    suspend fun updateTemperatureUnit(unit: TemperatureUnit) {
        context.noorDataStore.edit { it[Keys.TEMPERATURE_UNIT] = unit.name }
    }

    suspend fun saveCachedWeather(weather: WeatherData) {
        context.noorDataStore.edit {
            it[Keys.WEATHER_TEMP_C] = weather.tempC
            it[Keys.WEATHER_MIN_C] = weather.minTempC
            it[Keys.WEATHER_MAX_C] = weather.maxTempC
            it[Keys.WEATHER_HUMIDITY] = weather.humidity
            it[Keys.WEATHER_WIND_KMH] = weather.windSpeedKmh
            it[Keys.WEATHER_CONDITION_AR] = weather.conditionAr
            it[Keys.WEATHER_CODE] = weather.weatherCode
            it[Keys.WEATHER_SOURCE] = weather.sourceLabel
            it[Keys.WEATHER_UPDATED_AT] = weather.updatedAtMillis
        }
    }
}
