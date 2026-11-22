package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.SettingsDataStore
import com.example.service.AdhanAlarmScheduler
import com.example.service.AdhanPlaybackService
import com.example.service.NotificationHelper
import com.example.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AdhanAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settingsStore = SettingsDataStore(context.applicationContext)
                val settings = settingsStore.getSettingsOnce()

                when (action) {
                    ACTION_ADHAN -> {
                        val prayerName = intent.getStringExtra(EXTRA_PRAYER_NAME) ?: "الصلاة"
                        if (settings.adhanEnabled) {
                            AdhanPlaybackService.startAdhan(context.applicationContext, prayerName)
                        }
                        AdhanAlarmScheduler(context.applicationContext).rescheduleAll(settings)
                        WidgetUpdater.updateAllWidgets(context.applicationContext)
                    }
                    ACTION_PRE_PRAYER -> {
                        val prayerName = intent.getStringExtra(EXTRA_PRAYER_NAME) ?: "الصلاة"
                        val minutesBefore = intent.getIntExtra(
                            EXTRA_MINUTES_BEFORE,
                            settings.prePrayerMinutes
                        )
                        if (settings.prePrayerReminderEnabled) {
                            NotificationHelper.showPrePrayerNotification(
                                context = context.applicationContext,
                                prayerNameAr = prayerName,
                                minutesBefore = minutesBefore,
                                toneOption = settings.prePrayerTone
                            )
                        }
                    }
                    ACTION_IQAMAH -> {
                        val prayerName = intent.getStringExtra(EXTRA_PRAYER_NAME) ?: "الصلاة"
                        if (settings.iqamahAlertEnabled) {
                            NotificationHelper.showIqamahNotification(
                                context = context.applicationContext,
                                prayerNameAr = prayerName
                            )
                        }
                    }
                    ACTION_STOP_ADHAN -> {
                        AdhanPlaybackService.stopAdhan(context.applicationContext)
                    }
                    Intent.ACTION_BOOT_COMPLETED,
                    Intent.ACTION_TIME_CHANGED,
                    Intent.ACTION_TIMEZONE_CHANGED,
                    ACTION_RESCHEDULE -> {
                        AdhanAlarmScheduler(context.applicationContext).rescheduleAll(settings)
                        WidgetUpdater.updateAllWidgets(context.applicationContext)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_ADHAN = "com.example.ACTION_ADHAN"
        const val ACTION_PRE_PRAYER = "com.example.ACTION_PRE_PRAYER"
        const val ACTION_IQAMAH = "com.example.ACTION_IQAMAH"
        const val ACTION_STOP_ADHAN = "com.example.ACTION_STOP_ADHAN"
        const val ACTION_RESCHEDULE = "com.example.ACTION_RESCHEDULE"

        const val EXTRA_PRAYER_NAME = "extra_prayer_name"
        const val EXTRA_MINUTES_BEFORE = "extra_minutes_before"
    }
}
