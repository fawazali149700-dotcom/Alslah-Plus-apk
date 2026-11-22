package com.example.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.local.SettingsDataStore
import com.example.data.repository.WeatherRepository
import com.example.service.AdhanAlarmScheduler
import com.example.widget.WidgetUpdater
import java.util.concurrent.TimeUnit

class WidgetSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val settingsStore = SettingsDataStore(applicationContext)
            val settings = settingsStore.getSettingsOnce()

            // 1. Refresh Weather in background
            val weatherRepo = WeatherRepository(settingsStore)
            weatherRepo.fetchAndCacheWeather(settings.latitude, settings.longitude)

            // 2. Ensure prayer & reminder alarms are up to date
            AdhanAlarmScheduler(applicationContext).rescheduleAll(settings)

            // 3. Refresh all 4 Home Screen Glance Widgets
            WidgetUpdater.updateAllWidgets(applicationContext)

            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "noor_widget_weather_sync_30m"

        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()

            val request = PeriodicWorkRequestBuilder<WidgetSyncWorker>(
                30L,
                TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
