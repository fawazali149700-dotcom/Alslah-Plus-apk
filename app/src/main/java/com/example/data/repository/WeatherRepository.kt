package com.example.data.repository

import com.example.BuildConfig
import com.example.data.local.SettingsDataStore
import com.example.data.model.WeatherData
import com.example.data.remote.OpenMeteoApiService
import com.example.data.remote.OpenWeatherApiService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class WeatherRepository(
    private val settingsDataStore: SettingsDataStore
) {
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    private val openWeatherApi: OpenWeatherApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OpenWeatherApiService::class.java)
    }

    private val openMeteoApi: OpenMeteoApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OpenMeteoApiService::class.java)
    }

    suspend fun fetchAndCacheWeather(lat: Double, lon: Double): Result<WeatherData> {
        val apiKey = BuildConfig.OPENWEATHER_API_KEY
        val hasOpenWeatherKey = apiKey.isNotBlank() && apiKey != "MY_OPENWEATHER_API_KEY"

        if (hasOpenWeatherKey) {
            try {
                val current = openWeatherApi.getCurrentWeather(lat, lon, apiKey)
                val forecast = runCatching { openWeatherApi.getForecast(lat, lon, apiKey) }.getOrNull()
                val minTemp = forecast?.list?.minOfOrNull { it.main.tempMin } ?: current.main.tempMin
                val maxTemp = forecast?.list?.maxOfOrNull { it.main.tempMax } ?: current.main.tempMax
                val condition = current.weather?.firstOrNull()
                val weather = WeatherData(
                    tempC = current.main.temp,
                    minTempC = minTemp,
                    maxTempC = maxTemp,
                    humidity = current.main.humidity,
                    windSpeedKmh = ((current.wind?.speed ?: 0.0) * 3.6 * 10.0).roundToInt() / 10.0,
                    conditionAr = condition?.description ?: "صافي",
                    weatherCode = condition?.id ?: 800,
                    sourceLabel = "OpenWeatherMap",
                    updatedAtMillis = System.currentTimeMillis()
                )
                settingsDataStore.saveCachedWeather(weather)
                return Result.success(weather)
            } catch (_: Exception) {
                // Fall through to Open-Meteo if OpenWeatherMap call fails
            }
        }

        return try {
            val meteo = openMeteoApi.getForecast(lat = lat, lon = lon)
            val current = meteo.current ?: throw IllegalStateException("بيانات الطقس غير متوفرة")
            val maxT = meteo.daily?.temperature2mMax?.firstOrNull() ?: current.temperature2m
            val minT = meteo.daily?.temperature2mMin?.firstOrNull() ?: current.temperature2m
            val weather = WeatherData(
                tempC = current.temperature2m,
                minTempC = minT,
                maxTempC = maxT,
                humidity = current.relativeHumidity2m,
                windSpeedKmh = current.windSpeed10m,
                conditionAr = wmoCodeToArabic(current.weatherCode),
                weatherCode = current.weatherCode,
                sourceLabel = "Open-Meteo",
                updatedAtMillis = System.currentTimeMillis()
            )
            settingsDataStore.saveCachedWeather(weather)
            Result.success(weather)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun wmoCodeToArabic(code: Int): String = when (code) {
        0 -> "سماء صافية"
        1, 2 -> "غائم جزئياً"
        3 -> "غائم كلياً"
        45, 48 -> "ضباب"
        51, 53, 55 -> "رذاذ خفيف"
        61, 63, 65 -> "أمطار"
        71, 73, 75 -> "ثلوج"
        80, 81, 82 -> "زخات مطرية"
        95, 96, 99 -> "عواصف رعدية"
        else -> "أجواء معتدلة"
    }
}
