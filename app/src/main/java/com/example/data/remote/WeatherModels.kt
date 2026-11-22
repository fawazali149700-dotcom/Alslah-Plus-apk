package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// --- OpenWeatherMap DTOs ---
@JsonClass(generateAdapter = true)
data class OpenWeatherCurrentResponse(
    val main: OpenWeatherMain,
    val wind: OpenWeatherWind?,
    val weather: List<OpenWeatherCondition>?,
    val name: String?
)

@JsonClass(generateAdapter = true)
data class OpenWeatherMain(
    val temp: Double,
    @Json(name = "temp_min") val tempMin: Double,
    @Json(name = "temp_max") val tempMax: Double,
    val humidity: Int
)

@JsonClass(generateAdapter = true)
data class OpenWeatherWind(
    val speed: Double
)

@JsonClass(generateAdapter = true)
data class OpenWeatherCondition(
    val id: Int,
    val main: String,
    val description: String
)

@JsonClass(generateAdapter = true)
data class OpenWeatherForecastResponse(
    val list: List<OpenWeatherForecastItem>
)

@JsonClass(generateAdapter = true)
data class OpenWeatherForecastItem(
    val dt: Long,
    val main: OpenWeatherMain
)

// --- Open-Meteo Free Fallback DTOs (no API key needed) ---
@JsonClass(generateAdapter = true)
data class OpenMeteoResponse(
    val current: OpenMeteoCurrent?,
    val daily: OpenMeteoDaily?
)

@JsonClass(generateAdapter = true)
data class OpenMeteoCurrent(
    @Json(name = "temperature_2m") val temperature2m: Double,
    @Json(name = "relative_humidity_2m") val relativeHumidity2m: Int,
    @Json(name = "wind_speed_10m") val windSpeed10m: Double,
    @Json(name = "weather_code") val weatherCode: Int
)

@JsonClass(generateAdapter = true)
data class OpenMeteoDaily(
    @Json(name = "temperature_2m_max") val temperature2mMax: List<Double>,
    @Json(name = "temperature_2m_min") val temperature2mMin: List<Double>
)
