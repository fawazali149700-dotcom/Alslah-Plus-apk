package com.example.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.DhikrScreenPosition
import com.example.data.model.PrePrayerToneOption
import com.example.receiver.AdhanAlarmReceiver

object NotificationHelper {

    const val CHANNEL_ADHAN_PLAYBACK = "adhan_playback_channel"
    const val CHANNEL_IQAMAH = "iqamah_alert_channel"
    const val CHANNEL_DHIKR_TOP = "dhikr_top_heads_up_v1"
    const val CHANNEL_DHIKR_STANDARD = "dhikr_standard_v1"
    const val CHANNEL_DHIKR_SILENT = "dhikr_silent_bg_v1"
    private const val CHANNEL_PRE_DEFAULT = "pre_prayer_default_v1"
    private const val CHANNEL_PRE_ALARM = "pre_prayer_alarm_v1"
    private const val CHANNEL_PRE_RINGTONE = "pre_prayer_ringtone_v1"
    private const val CHANNEL_PRE_VIBRATE = "pre_prayer_vibrate_v1"

    const val NOTIF_ID_ADHAN_SERVICE = 1001
    const val NOTIF_ID_PRE_PRAYER = 2001
    const val NOTIF_ID_IQAMAH = 3001
    const val NOTIF_ID_DHIKR = 4001

    private val vibrationPattern = longArrayOf(0L, 400L, 200L, 400L)

    fun createAllChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 1. Adhan Foreground Playback Channel (silent channel because MediaPlayer plays the local MP3)
        val adhanChannel = NotificationChannel(
            CHANNEL_ADHAN_PLAYBACK,
            context.getString(R.string.channel_adhan_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.channel_adhan_desc)
            setSound(null, null)
            enableVibration(true)
            vibrationPattern = this@NotificationHelper.vibrationPattern
        }
        manager.createNotificationChannel(adhanChannel)

        // 2. Iqamah Channel (with notification sound + vibration)
        val iqamahAudioAttrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val iqamahChannel = NotificationChannel(
            CHANNEL_IQAMAH,
            context.getString(R.string.channel_iqamah_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.channel_iqamah_desc)
            setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), iqamahAudioAttrs)
            enableVibration(true)
            vibrationPattern = this@NotificationHelper.vibrationPattern
        }
        manager.createNotificationChannel(iqamahChannel)

        // 3. Pre-Prayer Channels per tone option (so changing tone in settings changes channel sound cleanly)
        PrePrayerToneOption.entries.forEach { option ->
            val channelId = channelIdForTone(option)
            val soundUri = soundUriForTone(option)
            val audioAttrs = AudioAttributes.Builder()
                .setUsage(
                    if (option == PrePrayerToneOption.GENTLE_ALARM) AudioAttributes.USAGE_ALARM
                    else AudioAttributes.USAGE_NOTIFICATION
                )
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val channel = NotificationChannel(
                channelId,
                "${context.getString(R.string.channel_pre_prayer_name)} — ${option.titleAr}",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.channel_pre_prayer_desc)
                setSound(soundUri, if (soundUri != null) audioAttrs else null)
                enableVibration(true)
                vibrationPattern = this@NotificationHelper.vibrationPattern
            }
            manager.createNotificationChannel(channel)
        }

        // 4. Dhikr Notification Channels (Top heads-up, Standard, and Silent/Hidden background audio)
        val dhikrTopChannel = NotificationChannel(
            CHANNEL_DHIKR_TOP,
            context.getString(R.string.channel_dhikr_top_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "إشعارات الأذكار اليومية أعلى الشاشة"
            setSound(null, null)
            enableVibration(true)
            vibrationPattern = longArrayOf(0L, 180L)
        }
        manager.createNotificationChannel(dhikrTopChannel)

        val dhikrStdChannel = NotificationChannel(
            CHANNEL_DHIKR_STANDARD,
            context.getString(R.string.channel_dhikr_standard_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "إشعارات الأذكار اليومية بشكل هادئ"
            setSound(null, null)
            enableVibration(false)
        }
        manager.createNotificationChannel(dhikrStdChannel)

        val dhikrSilentChannel = NotificationChannel(
            CHANNEL_DHIKR_SILENT,
            context.getString(R.string.channel_dhikr_silent_name),
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            description = "قناة خفية لتشغيل الصوت البشري للذكر عند إخفاء الإشعار"
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
        }
        manager.createNotificationChannel(dhikrSilentChannel)
    }

    private fun channelIdForTone(option: PrePrayerToneOption): String = when (option) {
        PrePrayerToneOption.DEFAULT_NOTIFICATION -> CHANNEL_PRE_DEFAULT
        PrePrayerToneOption.GENTLE_ALARM -> CHANNEL_PRE_ALARM
        PrePrayerToneOption.RINGTONE_CHIME -> CHANNEL_PRE_RINGTONE
        PrePrayerToneOption.VIBRATE_ONLY -> CHANNEL_PRE_VIBRATE
    }

    private fun soundUriForTone(option: PrePrayerToneOption): Uri? = when (option) {
        PrePrayerToneOption.DEFAULT_NOTIFICATION ->
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        PrePrayerToneOption.GENTLE_ALARM ->
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        PrePrayerToneOption.RINGTONE_CHIME ->
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        PrePrayerToneOption.VIBRATE_ONLY -> null
    }

    fun buildAdhanForegroundNotification(
        context: Context,
        prayerNameAr: String,
        muezzinNameAr: String
    ): Notification {
        createAllChannels(context)
        val openIntent = PendingIntent.getActivity(
            context,
            10,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getBroadcast(
            context,
            11,
            Intent(context, AdhanAlarmReceiver::class.java).apply {
                action = AdhanAlarmReceiver.ACTION_STOP_ADHAN
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ADHAN_PLAYBACK)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("حان الآن موعد صلاة $prayerNameAr")
            .setContentText("الأذان بصوت: $muezzinNameAr")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setContentIntent(openIntent)
            .addAction(
                android.R.drawable.ic_media_pause,
                context.getString(R.string.action_stop_adhan),
                stopIntent
            )
            .build()
    }

    fun showPrePrayerNotification(
        context: Context,
        prayerNameAr: String,
        minutesBefore: Int,
        toneOption: PrePrayerToneOption
    ) {
        if (!canPostNotifications(context)) return
        createAllChannels(context)

        val openIntent = PendingIntent.getActivity(
            context,
            20,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = channelIdForTone(toneOption)
        val soundUri = soundUriForTone(toneOption)
        val message = "اقترب موعد صلاة $prayerNameAr بعد $minutesBefore دقيقة، حان وقت الاستعداد للصلاة"

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("تذكير باقتراب صلاة $prayerNameAr")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setVibrate(vibrationPattern)
            .setContentIntent(openIntent)

        if (soundUri != null) {
            builder.setSound(soundUri)
        } else {
            builder.setSilent(false)
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIF_ID_PRE_PRAYER, builder.build())
    }

    fun showIqamahNotification(context: Context, prayerNameAr: String) {
        if (!canPostNotifications(context)) return
        createAllChannels(context)

        val openIntent = PendingIntent.getActivity(
            context,
            30,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val message = "حان الآن وقت إقامة صلاة $prayerNameAr — أقم الصلاة"

        val notification = NotificationCompat.Builder(context, CHANNEL_IQAMAH)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("إقامة صلاة $prayerNameAr")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setSound(soundUri)
            .setVibrate(vibrationPattern)
            .setContentIntent(openIntent)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIF_ID_IQAMAH, notification)
    }

    fun showDhikrNotification(
        context: Context,
        dhikrTitle: String,
        dhikrText: String,
        position: DhikrScreenPosition,
        includeToneSound: Boolean
    ) {
        if (position == DhikrScreenPosition.HIDDEN || !canPostNotifications(context)) return
        createAllChannels(context)

        val openIntent = PendingIntent.getActivity(
            context,
            40,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = if (position == DhikrScreenPosition.TOP) {
            CHANNEL_DHIKR_TOP
        } else {
            CHANNEL_DHIKR_STANDARD
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("ذكر الله — $dhikrTitle")
            .setContentText(dhikrText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(dhikrText))
            .setPriority(
                if (position == DhikrScreenPosition.TOP) NotificationCompat.PRIORITY_HIGH
                else NotificationCompat.PRIORITY_DEFAULT
            )
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openIntent)

        if (includeToneSound) {
            val tone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            if (tone != null) builder.setSound(tone)
        } else {
            builder.setSilent(true)
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIF_ID_DHIKR, builder.build())
    }

    fun buildDhikrPlaybackForegroundNotification(
        context: Context,
        dhikrTitle: String,
        dhikrText: String,
        silentMinimal: Boolean
    ): Notification {
        createAllChannels(context)
        val openIntent = PendingIntent.getActivity(
            context,
            41,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getBroadcast(
            context,
            42,
            Intent(context, AdhanAlarmReceiver::class.java).apply {
                action = AdhanAlarmReceiver.ACTION_STOP_ADHAN
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = if (silentMinimal) CHANNEL_DHIKR_SILENT else CHANNEL_DHIKR_STANDARD
        val title = if (silentMinimal) "تشغيل ذكر الله" else "ذكر الله — $dhikrTitle"
        val body = if (silentMinimal) "صوت بشري خاشع" else dhikrText

        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode_off)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(if (silentMinimal) NotificationCompat.PRIORITY_MIN else NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .setOngoing(true)
            .setContentIntent(openIntent)
            .addAction(
                android.R.drawable.ic_media_pause,
                "إيقاف",
                stopIntent
            )
            .build()
    }

    fun canPostNotifications(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}
