package com.example.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll
import com.example.data.local.SettingsDataStore
import com.example.data.model.DailyPrayerSchedule
import com.example.data.model.TemperatureUnit
import com.example.data.model.WeatherData
import com.example.data.repository.PrayerRepository
import com.example.data.repository.WeatherRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

data class WidgetSnapshot(
    val schedule: DailyPrayerSchedule,
    val weather: WeatherData?,
    val tempUnit: TemperatureUnit,
    val currentTimeText: String
) {
    fun formatTemp(tempC: Double): String {
        val value = if (tempUnit == TemperatureUnit.FAHRENHEIT) {
            (tempC * 9.0 / 5.0) + 32.0
        } else {
            tempC
        }
        return "${value.roundToInt()}${tempUnit.symbol}"
    }

    fun actualTempOrDash(): String = weather?.let { formatTemp(it.tempC) } ?: "--°"

    fun compactTime(formattedTime: String): String {
        return formattedTime.replace(" ص", "").replace(" م", "").trim()
    }

    fun formatRemainingTime(): String {
        val totalMin = (schedule.remainingMillis / 60_000L).coerceAtLeast(1L)
        val hours = totalMin / 60L
        val mins = totalMin % 60L
        return if (hours > 0) "بعد $hours س $mins د" else "بعد $mins د"
    }
}

object WidgetColors {
    val EmeraldBg = Color(0xFF0B3B2E)
    val MidnightCard = Color(0xFF102A3A)
    val DeepPanel = Color(0xFF0A1D28)
    val GoldAccent = Color(0xFFD4AF37)
    val CreamText = Color(0xFFFAF7F0)
    val MutedText = Color(0xFFC8D5CE)
}

object WidgetDataLoader {
    suspend fun load(context: Context): WidgetSnapshot {
        val store = SettingsDataStore(context.applicationContext)
        val settings = store.getSettingsOnce()
        val schedule = PrayerRepository().computeSchedule(settings)
        val weather = store.getCachedWeatherOnce()
        val timeFmt = SimpleDateFormat("hh:mm a", Locale("ar"))
        return WidgetSnapshot(
            schedule = schedule,
            weather = weather,
            tempUnit = settings.temperatureUnit,
            currentTimeText = timeFmt.format(Date())
        )
    }
}

object WidgetUpdater {
    suspend fun updateAllWidgets(context: Context) {
        val appCtx = context.applicationContext
        runCatching { Compact4x1AllInOneWidget().updateAll(appCtx) }
        runCatching { PrayerTimesWidget().updateAll(appCtx) }
        runCatching { HijriDateWidget().updateAll(appCtx) }
        runCatching { WeatherWidget().updateAll(appCtx) }
        runCatching { ComprehensiveWidget().updateAll(appCtx) }
        runCatching { Dashboard4x3Widget().updateAll(appCtx) }
        runCatching { Grand4x4Widget().updateAll(appCtx) }
        runCatching { Qiyam2x2Widget().updateAll(appCtx) }
    }
}

class RefreshWidgetsActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val store = SettingsDataStore(context.applicationContext)
        val settings = store.getSettingsOnce()
        WeatherRepository(store).fetchAndCacheWeather(settings.latitude, settings.longitude)
        WidgetUpdater.updateAllWidgets(context.applicationContext)
    }
}
