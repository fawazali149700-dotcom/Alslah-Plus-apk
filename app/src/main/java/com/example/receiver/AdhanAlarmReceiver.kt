package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.AppDatabase
import com.example.data.local.SettingsDataStore
import com.example.data.model.ActiveDhikrBanner
import com.example.data.model.DhikrAlertMode
import com.example.data.model.DhikrScreenPosition
import com.example.data.model.IqamahSoundMode
import com.example.data.model.PrayerName
import com.example.data.repository.DhikrRepository
import com.example.service.AdhanAlarmScheduler
import com.example.service.AdhanPlaybackService
import com.example.service.DhikrBannerBus
import com.example.service.NotificationHelper
import com.example.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class AdhanAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val appContext = context.applicationContext
                val settingsStore = SettingsDataStore(appContext)
                val settings = settingsStore.getSettingsOnce()
                val prayerEnum = intent.getStringExtra(EXTRA_PRAYER_ENUM)?.let { raw ->
                    runCatching { PrayerName.valueOf(raw) }.getOrNull()
                }

                when (action) {
                    ACTION_ADHAN -> {
                        val prayerName = intent.getStringExtra(EXTRA_PRAYER_NAME) ?: "الصلاة"
                        val allowed = settings.adhanEnabled &&
                            (prayerEnum == null || settings.perPrayerAdhanEnabled.isEnabled(prayerEnum))
                        if (allowed) {
                            AdhanPlaybackService.startAdhan(appContext, prayerName)
                        }
                        AdhanAlarmScheduler(appContext).rescheduleAll(settings)
                        WidgetUpdater.updateAllWidgets(appContext)
                    }
                    ACTION_PRE_PRAYER -> {
                        val prayerName = intent.getStringExtra(EXTRA_PRAYER_NAME) ?: "الصلاة"
                        val minutesBefore = intent.getIntExtra(
                            EXTRA_MINUTES_BEFORE,
                            settings.prePrayerMinutes
                        )
                        val allowed = settings.prePrayerReminderEnabled &&
                            (prayerEnum == null || settings.perPrayerPrePrayerEnabled.isEnabled(prayerEnum))
                        if (allowed) {
                            NotificationHelper.showPrePrayerNotification(
                                context = appContext,
                                prayerNameAr = prayerName,
                                minutesBefore = minutesBefore,
                                toneOption = settings.prePrayerTone
                            )
                            val customPrePath = prayerEnum?.let {
                                settings.perPrayerPrePrayerAudio[it]?.path
                            }?.takeIf { File(it).exists() }

                            if (customPrePath != null) {
                                AdhanPlaybackService.speakPrePrayer(
                                    context = appContext,
                                    prayerNameAr = prayerName,
                                    minutesBefore = minutesBefore,
                                    customPath = customPrePath
                                )
                            } else if (settings.prePrayerSpeakAnnouncement) {
                                AdhanPlaybackService.speakPrePrayer(
                                    context = appContext,
                                    prayerNameAr = prayerName,
                                    minutesBefore = minutesBefore
                                )
                            }
                        }
                    }
                    ACTION_IQAMAH -> {
                        val prayerName = intent.getStringExtra(EXTRA_PRAYER_NAME) ?: "الصلاة"
                        val allowed = settings.iqamahAlertEnabled &&
                            (prayerEnum == null || settings.perPrayerIqamahEnabled.isEnabled(prayerEnum))
                        if (allowed) {
                            val serviceHandlesAudio = settings.iqamahSoundMode != IqamahSoundMode.SYSTEM_TONE
                            NotificationHelper.showIqamahNotification(
                                context = appContext,
                                prayerNameAr = prayerName,
                                silentWhenServicePlaysAudio = serviceHandlesAudio
                            )
                            val perPrayerIqamahPath = prayerEnum?.let {
                                settings.perPrayerIqamahAudio[it]?.path
                            }?.takeIf { File(it).exists() }
                            val resolvedIqamahPath = perPrayerIqamahPath
                                ?: settings.customIqamahAudioPath?.takeIf { File(it).exists() }
                                ?: settings.downloadedIqamahPath?.takeIf { File(it).exists() }

                            if (serviceHandlesAudio) {
                                AdhanPlaybackService.startIqamahAlert(
                                    context = appContext,
                                    prayerNameAr = prayerName,
                                    customPath = resolvedIqamahPath
                                )
                            }
                        }
                    }
                    ACTION_DHIKR_ALERT -> {
                        val dhikrCfg = settings.dhikrSettings
                        if (dhikrCfg.enabled && dhikrCfg.screenPosition != DhikrScreenPosition.HIDDEN) {
                            val db = AppDatabase.getInstance(appContext)
                            val dhikrRepo = DhikrRepository(appContext, db.dhikrDao())
                            val chosen = dhikrRepo.pickRandomDhikr(excludeId = dhikrCfg.lastTriggeredDhikrId)
                            if (chosen != null) {
                                settingsStore.updateLastTriggeredDhikrId(chosen.id)

                                DhikrBannerBus.post(
                                    ActiveDhikrBanner(
                                        id = chosen.id,
                                        title = chosen.titleAr,
                                        text = chosen.textAr,
                                        categoryLabel = chosen.categoryAr,
                                        position = dhikrCfg.screenPosition
                                    )
                                )
                                NotificationHelper.showDhikrNotification(
                                    context = appContext,
                                    dhikrTitle = chosen.titleAr,
                                    dhikrText = chosen.textAr,
                                    position = dhikrCfg.screenPosition,
                                    includeToneSound = dhikrCfg.mode == DhikrAlertMode.TEXT_WITH_TONE
                                )
                            }
                        }
                    }
                    ACTION_STOP_ADHAN -> {
                        AdhanPlaybackService.stopAdhan(appContext)
                    }
                    Intent.ACTION_BOOT_COMPLETED,
                    Intent.ACTION_TIME_CHANGED,
                    Intent.ACTION_TIMEZONE_CHANGED,
                    ACTION_RESCHEDULE -> {
                        AdhanAlarmScheduler(appContext).rescheduleAll(settings)
                        WidgetUpdater.updateAllWidgets(appContext)
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
        const val ACTION_DHIKR_ALERT = "com.example.ACTION_DHIKR_ALERT"
        const val ACTION_STOP_ADHAN = "com.example.ACTION_STOP_ADHAN"
        const val ACTION_RESCHEDULE = "com.example.ACTION_RESCHEDULE"

        const val EXTRA_PRAYER_NAME = "extra_prayer_name"
        const val EXTRA_PRAYER_ENUM = "extra_prayer_enum"
        const val EXTRA_MINUTES_BEFORE = "extra_minutes_before"
    }
}
