package com.example.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.example.data.local.AppDatabase
import com.example.data.local.SettingsDataStore
import com.example.data.repository.MuezzinRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class AdhanPlaybackService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var mediaPlayer: MediaPlayer? = null
    private var playbackJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopPlaybackAndSelf()
            return START_NOT_STICKY
        }

        val prayerNameAr = intent?.getStringExtra(EXTRA_PRAYER_NAME) ?: "الصلاة"
        val customAudioPath = intent?.getStringExtra(EXTRA_LOCAL_PATH)

        val notification = NotificationHelper.buildAdhanForegroundNotification(
            context = this,
            prayerNameAr = prayerNameAr,
            muezzinNameAr = "المؤذن المختار"
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NotificationHelper.NOTIF_ID_ADHAN_SERVICE,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NotificationHelper.NOTIF_ID_ADHAN_SERVICE, notification)
        }

        playbackJob?.cancel()
        playbackJob = serviceScope.launch {
            val settingsStore = SettingsDataStore(applicationContext)
            val db = AppDatabase.getInstance(applicationContext)
            val muezzinRepo = MuezzinRepository(applicationContext, db.muezzinDao())
            val settings = settingsStore.getSettingsOnce()
            val selectedEntity = db.muezzinDao().getById(settings.selectedMuezzinId)
            val localPath = customAudioPath ?: muezzinRepo.getPlayableLocalPath(settings.selectedMuezzinId)

            val updatedNotification = NotificationHelper.buildAdhanForegroundNotification(
                context = applicationContext,
                prayerNameAr = prayerNameAr,
                muezzinNameAr = selectedEntity?.nameAr ?: "المؤذن المختار"
            )
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            nm.notify(NotificationHelper.NOTIF_ID_ADHAN_SERVICE, updatedNotification)

            startAudio(localPath)
        }

        return START_NOT_STICKY
    }

    private fun startAudio(localFilePath: String?) {
        releasePlayer()
        _isPlaying.value = true

        val player = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            setOnCompletionListener {
                stopPlaybackAndSelf()
            }
            setOnErrorListener { _, _, _ ->
                playFallbackTone()
                true
            }
        }

        try {
            if (!localFilePath.isNullOrBlank() && File(localFilePath).exists()) {
                player.setDataSource(localFilePath)
                player.prepare()
                player.start()
                mediaPlayer = player
            } else {
                player.release()
                playFallbackTone()
            }
        } catch (_: Exception) {
            player.release()
            playFallbackTone()
        }
    }

    private fun playFallbackTone() {
        releasePlayer()
        val fallbackUri: Uri? = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        if (fallbackUri == null) {
            stopPlaybackAndSelf()
            return
        }

        try {
            val fallbackPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(applicationContext, fallbackUri)
                isLooping = false
                prepare()
                start()
            }
            mediaPlayer = fallbackPlayer
            // Cap fallback ringtone duration to 15 seconds
            serviceScope.launch {
                delay(15_000L)
                stopPlaybackAndSelf()
            }
        } catch (_: Exception) {
            stopPlaybackAndSelf()
        }
    }

    private fun releasePlayer() {
        runCatching {
            mediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
        }
        mediaPlayer = null
    }

    private fun stopPlaybackAndSelf() {
        releasePlayer()
        _isPlaying.value = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        releasePlayer()
        _isPlaying.value = false
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.example.service.ACTION_START_ADHAN"
        const val ACTION_STOP = "com.example.service.ACTION_STOP_ADHAN"
        const val EXTRA_PRAYER_NAME = "extra_prayer_name"
        const val EXTRA_LOCAL_PATH = "extra_local_path"

        private val _isPlaying = MutableStateFlow(false)
        val isPlayingFlow: StateFlow<Boolean> = _isPlaying.asStateFlow()

        fun startAdhan(context: Context, prayerNameAr: String, localPath: String? = null) {
            val intent = Intent(context, AdhanPlaybackService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_PRAYER_NAME, prayerNameAr)
                if (localPath != null) putExtra(EXTRA_LOCAL_PATH, localPath)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stopAdhan(context: Context) {
            val intent = Intent(context, AdhanPlaybackService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
