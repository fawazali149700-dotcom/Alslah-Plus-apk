package com.example.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.model.PrayerName
import com.example.data.model.UserSettings
import com.example.data.repository.PrayerRepository
import com.example.receiver.AdhanAlarmReceiver
import java.util.Calendar

class AdhanAlarmScheduler(
    private val context: Context,
    private val prayerRepository: PrayerRepository = PrayerRepository()
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun rescheduleAll(settings: UserSettings) {
        cancelAllScheduledAlarms()

        val upcoming = prayerRepository.getUpcomingAlarmsForTodayAndTomorrow(settings)
        val now = System.currentTimeMillis()

        upcoming.forEachIndexed { index, (prayer, adhanMillis) ->
            // 1. Exact Adhan Alarm (per-prayer toggle)
            if (settings.adhanEnabled && settings.perPrayerAdhanEnabled.isEnabled(prayer) && adhanMillis > now) {
                val intent = Intent(context, AdhanAlarmReceiver::class.java).apply {
                    action = AdhanAlarmReceiver.ACTION_ADHAN
                    putExtra(AdhanAlarmReceiver.EXTRA_PRAYER_NAME, prayer.arabicName)
                    putExtra(AdhanAlarmReceiver.EXTRA_PRAYER_ENUM, prayer.name)
                }
                scheduleExact(requestCode = 100 + index, triggerAtMillis = adhanMillis, intent = intent)
            }

            // 2. Pre-Prayer Reminder Alarm (per-prayer toggle)
            if (settings.prePrayerReminderEnabled && settings.perPrayerPrePrayerEnabled.isEnabled(prayer)) {
                val reminderMillis = adhanMillis - settings.prePrayerMinutes * 60_000L
                if (reminderMillis > now) {
                    val intent = Intent(context, AdhanAlarmReceiver::class.java).apply {
                        action = AdhanAlarmReceiver.ACTION_PRE_PRAYER
                        putExtra(AdhanAlarmReceiver.EXTRA_PRAYER_NAME, prayer.arabicName)
                        putExtra(AdhanAlarmReceiver.EXTRA_PRAYER_ENUM, prayer.name)
                        putExtra(AdhanAlarmReceiver.EXTRA_MINUTES_BEFORE, settings.prePrayerMinutes)
                    }
                    scheduleExact(requestCode = 200 + index, triggerAtMillis = reminderMillis, intent = intent)
                }
            }

            // 3. Iqamah Alarm (per-prayer toggle & customizable offset)
            val iqamahOffset = settings.iqamahOffsets.offsetFor(prayer)
            if (settings.iqamahAlertEnabled &&
                settings.perPrayerIqamahEnabled.isEnabled(prayer) &&
                iqamahOffset > 0
            ) {
                val iqamahMillis = adhanMillis + iqamahOffset * 60_000L
                if (iqamahMillis > now) {
                    val intent = Intent(context, AdhanAlarmReceiver::class.java).apply {
                        action = AdhanAlarmReceiver.ACTION_IQAMAH
                        putExtra(AdhanAlarmReceiver.EXTRA_PRAYER_NAME, prayer.arabicName)
                        putExtra(AdhanAlarmReceiver.EXTRA_PRAYER_ENUM, prayer.name)
                    }
                    scheduleExact(requestCode = 300 + index, triggerAtMillis = iqamahMillis, intent = intent)
                }
            }
        }

        // 4. Daily Midnight Reschedule Alarm (00:05 AM)
        val midnight = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 5)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val rescheduleIntent = Intent(context, AdhanAlarmReceiver::class.java).apply {
            action = AdhanAlarmReceiver.ACTION_RESCHEDULE
        }
        scheduleExact(requestCode = 999, triggerAtMillis = midnight, intent = rescheduleIntent)
    }

    private fun scheduleExact(requestCode: Int, triggerAtMillis: Long, intent: Intent) {
        val pending = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
        }
    }

    private fun cancelAllScheduledAlarms() {
        val actions = listOf(
            100..115 to AdhanAlarmReceiver.ACTION_ADHAN,
            200..215 to AdhanAlarmReceiver.ACTION_PRE_PRAYER,
            300..315 to AdhanAlarmReceiver.ACTION_IQAMAH
        )
        actions.forEach { (range, actionStr) ->
            for (code in range) {
                val intent = Intent(context, AdhanAlarmReceiver::class.java).apply {
                    action = actionStr
                }
                val pi = PendingIntent.getBroadcast(
                    context,
                    code,
                    intent,
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                )
                if (pi != null) {
                    alarmManager.cancel(pi)
                    pi.cancel()
                }
            }
        }
    }
}
