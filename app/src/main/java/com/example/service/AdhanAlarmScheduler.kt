package com.example.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.model.UserSettings
import com.example.data.repository.PrayerRepository
import com.example.receiver.AdhanAlarmReceiver
import java.util.Calendar
import kotlin.math.roundToInt
import kotlin.random.Random

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

        // 4. Randomized Daily Dhikr Alarms
        scheduleRandomizedDhikrAlarms(settings)

        // 5. Daily Midnight Reschedule Alarm (00:05 AM)
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

    fun rescheduleDhikrOnly(settings: UserSettings): List<Long> {
        cancelRange(500..549, AdhanAlarmReceiver.ACTION_DHIKR_ALERT)
        return scheduleRandomizedDhikrAlarms(settings)
    }

    private fun scheduleRandomizedDhikrAlarms(settings: UserSettings): List<Long> {
        val dhikrCfg = settings.dhikrSettings
        if (!dhikrCfg.enabled || dhikrCfg.dailyCount <= 0) return emptyList()

        val dailyCount = dhikrCfg.dailyCount.coerceIn(1, 48)
        val startHour = dhikrCfg.activeStartHour.coerceIn(0, 22)
        val endHour = dhikrCfg.activeEndHour.coerceIn(startHour + 1, 23)
        val now = System.currentTimeMillis()
        val triggers = mutableListOf<Long>()

        // Day 0 (Today remaining) and Day 1 (Tomorrow full)
        val todayWindowStart = dayTimeMillis(0, startHour, 0)
        val todayWindowEnd = dayTimeMillis(0, endHour, 59)
        val fullWindowDuration = (todayWindowEnd - todayWindowStart).coerceAtLeast(60_000L)

        if (now + 120_000L < todayWindowEnd) {
            val effectiveStart = maxOf(now + 120_000L, todayWindowStart)
            val remainingFraction = ((todayWindowEnd - effectiveStart).toDouble() / fullWindowDuration.toDouble())
                .coerceIn(0.0, 1.0)
            val todaySlots = (dailyCount * remainingFraction).roundToInt().coerceAtLeast(1)
            triggers += generateStratifiedRandomTimes(effectiveStart, todayWindowEnd, todaySlots)
        }

        val tomorrowStart = dayTimeMillis(1, startHour, 0)
        val tomorrowEnd = dayTimeMillis(1, endHour, 59)
        val remainingCapacity = (48 - triggers.size).coerceAtLeast(0)
        if (remainingCapacity > 0) {
            val tomorrowSlots = minOf(dailyCount, remainingCapacity)
            triggers += generateStratifiedRandomTimes(tomorrowStart, tomorrowEnd, tomorrowSlots)
        }

        triggers.take(48).forEachIndexed { idx, triggerAt ->
            val intent = Intent(context, AdhanAlarmReceiver::class.java).apply {
                action = AdhanAlarmReceiver.ACTION_DHIKR_ALERT
            }
            scheduleExact(requestCode = 500 + idx, triggerAtMillis = triggerAt, intent = intent)
        }

        return triggers.sorted()
    }

    private fun generateStratifiedRandomTimes(startMillis: Long, endMillis: Long, count: Int): List<Long> {
        if (count <= 0 || endMillis <= startMillis + 60_000L) return emptyList()
        val span = endMillis - startMillis
        val bucketSize = span / count.toDouble()
        val list = mutableListOf<Long>()
        for (i in 0 until count) {
            val bucketStart = startMillis + (i * bucketSize).toLong()
            val bucketEnd = minOf(endMillis, startMillis + ((i + 1) * bucketSize).toLong())
            val picked = if (bucketEnd > bucketStart + 5_000L) {
                Random.nextLong(bucketStart, bucketEnd)
            } else {
                bucketStart
            }
            list.add(picked)
        }
        return list
    }

    private fun dayTimeMillis(daysOffset: Int, hour: Int, minute: Int): Long {
        return Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, daysOffset)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
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
        cancelRange(100..115, AdhanAlarmReceiver.ACTION_ADHAN)
        cancelRange(200..215, AdhanAlarmReceiver.ACTION_PRE_PRAYER)
        cancelRange(300..315, AdhanAlarmReceiver.ACTION_IQAMAH)
        cancelRange(500..549, AdhanAlarmReceiver.ACTION_DHIKR_ALERT)
    }

    private fun cancelRange(range: IntRange, actionStr: String) {
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
