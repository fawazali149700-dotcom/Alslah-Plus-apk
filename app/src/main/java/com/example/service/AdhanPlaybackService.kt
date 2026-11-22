package com.example.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.content.ContextCompat
import com.example.data.local.AppDatabase
import com.example.data.local.SettingsDataStore
import com.example.data.model.IqamahSoundMode
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
import java.util.Locale

class AdhanPlaybackService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var mediaPlayer: MediaPlayer? = null
    private var textToSpeech: TextToSpeech? = null
    private var playbackJob: Job? = null
    private var audioFocusRequest: AudioFocusRequest? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        if (action == ACTION_STOP) {
            stopPlaybackAndSelf()
            return START_NOT_STICKY
        }

        val prayerNameAr = intent?.getStringExtra(EXTRA_PRAYER_NAME) ?: "الصلاة"
        val customAudioPath = intent?.getStringExtra(EXTRA_LOCAL_PATH)
        val minutesBefore = intent?.getIntExtra(EXTRA_MINUTES_BEFORE, 15) ?: 15

        val initialSubtitle = when (action) {
            ACTION_START_IQAMAH -> "إقامة صلاة $prayerNameAr"
            ACTION_SPEAK_PRE_PRAYER -> "اقتربت صلاة $prayerNameAr"
            else -> "المؤذن المختار"
        }
        val notification = NotificationHelper.buildAdhanForegroundNotification(
            context = this,
            prayerNameAr = prayerNameAr,
            muezzinNameAr = initialSubtitle
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
            val settings = settingsStore.getSettingsOnce()

            when (action) {
                ACTION_START_IQAMAH -> {
                    val downloadedPath = settings.downloadedIqamahPath?.takeIf { File(it).exists() && File(it).length() > 512L }
                    val customPath = customAudioPath?.takeIf { File(it).exists() && File(it).length() > 512L }
                        ?: settings.customIqamahAudioPath?.takeIf { File(it).exists() && File(it).length() > 512L }

                    when (settings.iqamahSoundMode) {
                        IqamahSoundMode.DOWNLOADED_HUMAN -> {
                            if (customAudioPath != null && File(customAudioPath).exists()) {
                                startAudio(customAudioPath)
                            } else if (downloadedPath != null) {
                                startAudio(downloadedPath)
                            } else {
                                // Try downloading selected Iqamah preset on the fly
                                val db = AppDatabase.getInstance(applicationContext)
                                val muezzinRepo = MuezzinRepository(applicationContext, db.muezzinDao())
                                val preset = muezzinRepo.iqamahPresets.firstOrNull {
                                    it.id == settings.selectedIqamahPresetId
                                } ?: muezzinRepo.iqamahPresets.first()
                                val fetched = muezzinRepo.downloadIqamahPreset(preset).getOrNull()
                                if (fetched != null) {
                                    settingsStore.updateDownloadedIqamahAudio(fetched.first, fetched.second)
                                    startAudio(fetched.first)
                                } else if (customPath != null) {
                                    startAudio(customPath)
                                } else {
                                    playGentleNotificationChime()
                                }
                            }
                        }
                        IqamahSoundMode.VOICE_ANNOUNCEMENT -> {
                            // If a specific per-prayer custom file exists, play that clean human voice instead of TTS
                            val perPrayerOrCustom = customAudioPath?.takeIf { File(it).exists() && File(it).length() > 512L }
                            if (perPrayerOrCustom != null) {
                                startAudio(perPrayerOrCustom)
                            } else {
                                val spokenText = "اقتربت صلاة $prayerNameAr، حان الآن وقت إقامة الصلاة"
                                speakArabicAnnouncement(
                                    text = spokenText,
                                    fallbackAudioPath = downloadedPath ?: customPath
                                )
                            }
                        }
                        IqamahSoundMode.CUSTOM_AUDIO -> {
                            val resolved = customPath ?: downloadedPath
                            if (resolved != null) {
                                startAudio(resolved)
                            } else {
                                playGentleNotificationChime()
                            }
                        }
                        IqamahSoundMode.SYSTEM_TONE -> playGentleNotificationChime()
                    }
                }
                ACTION_SPEAK_PRE_PRAYER -> {
                    if (!customAudioPath.isNullOrBlank() && File(customAudioPath).exists()) {
                        startAudio(customAudioPath)
                    } else {
                        val spokenText = "اقتربت صلاة $prayerNameAr بعد $minutesBefore دقيقة، حان وقت الاستعداد للصلاة"
                        speakArabicAnnouncement(spokenText)
                    }
                }
                else -> {
                    val db = AppDatabase.getInstance(applicationContext)
                    val muezzinRepo = MuezzinRepository(applicationContext, db.muezzinDao())
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
            }
        }

        return START_NOT_STICKY
    }

    private fun speakArabicAnnouncement(
        text: String,
        fallbackAudioPath: String? = null
    ) {
        releasePlayer()
        releaseTts()
        requestAudioFocusCompat()
        _isPlaying.value = true

        textToSpeech = TextToSpeech(applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val tts = textToSpeech ?: return@TextToSpeech
                val arLocale = Locale("ar")
                val arAvailability = tts.isLanguageAvailable(arLocale)
                val arResult = tts.setLanguage(arLocale)
                val isArabicSupported = arAvailability >= TextToSpeech.LANG_AVAILABLE &&
                    arResult != TextToSpeech.LANG_MISSING_DATA &&
                    arResult != TextToSpeech.LANG_NOT_SUPPORTED

                if (!isArabicSupported) {
                    serviceScope.launch {
                        val validFallback = fallbackAudioPath?.takeIf { File(it).exists() && File(it).length() > 512L }
                        if (validFallback != null) startAudio(validFallback)
                        else playGentleNotificationChime()
                    }
                    return@TextToSpeech
                }

                tts.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                tts.setSpeechRate(0.95f)
                tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {
                        serviceScope.launch { stopPlaybackAndSelf() }
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        serviceScope.launch {
                            val validFallback = fallbackAudioPath?.takeIf { File(it).exists() && File(it).length() > 512L }
                            if (validFallback != null) startAudio(validFallback)
                            else playGentleNotificationChime()
                        }
                    }
                })
                val params = android.os.Bundle().apply {
                    putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "noor_voice_alert")
                }
                val speakStatus = tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, "noor_voice_alert")
                if (speakStatus == TextToSpeech.ERROR) {
                    val validFallback = fallbackAudioPath?.takeIf { File(it).exists() && File(it).length() > 512L }
                    if (validFallback != null) startAudio(validFallback)
                    else playGentleNotificationChime()
                }
            } else {
                serviceScope.launch {
                    val validFallback = fallbackAudioPath?.takeIf { File(it).exists() && File(it).length() > 512L }
                    if (validFallback != null) startAudio(validFallback)
                    else playGentleNotificationChime()
                }
            }
        }

        // Safety timeout in case TTS engine stalls
        serviceScope.launch {
            delay(18_000L)
            if (_isPlaying.value) stopPlaybackAndSelf()
        }
    }

    private fun startAudio(localFilePath: String?) {
        releasePlayer()
        releaseTts()
        requestAudioFocusCompat()
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
            serviceScope.launch {
                delay(15_000L)
                stopPlaybackAndSelf()
            }
        } catch (_: Exception) {
            stopPlaybackAndSelf()
        }
    }

    private fun playGentleNotificationChime() {
        releasePlayer()
        requestAudioFocusCompat()
        _isPlaying.value = true
        val uri: Uri? = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        if (uri == null) {
            stopPlaybackAndSelf()
            return
        }
        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(applicationContext, uri)
                isLooping = false
                setOnCompletionListener { stopPlaybackAndSelf() }
                prepare()
                start()
            }
            mediaPlayer = player
            serviceScope.launch {
                delay(6_000L)
                if (_isPlaying.value) stopPlaybackAndSelf()
            }
        } catch (_: Exception) {
            stopPlaybackAndSelf()
        }
    }

    private fun requestAudioFocusCompat() {
        val am = getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .build()
            audioFocusRequest = req
            am.requestAudioFocus(req)
        } else {
            @Suppress("DEPRECATION")
            am.requestAudioFocus(null, AudioManager.STREAM_ALARM, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        }
    }

    private fun abandonAudioFocusCompat() {
        val am = getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { am.abandonAudioFocusRequest(it) }
            audioFocusRequest = null
        } else {
            @Suppress("DEPRECATION")
            am.abandonAudioFocus(null)
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

    private fun releaseTts() {
        runCatching {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        }
        textToSpeech = null
    }

    private fun stopPlaybackAndSelf() {
        releasePlayer()
        releaseTts()
        abandonAudioFocusCompat()
        _isPlaying.value = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        releasePlayer()
        releaseTts()
        abandonAudioFocusCompat()
        _isPlaying.value = false
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.example.service.ACTION_START_ADHAN"
        const val ACTION_START_IQAMAH = "com.example.service.ACTION_START_IQAMAH"
        const val ACTION_SPEAK_PRE_PRAYER = "com.example.service.ACTION_SPEAK_PRE_PRAYER"
        const val ACTION_STOP = "com.example.service.ACTION_STOP_ADHAN"
        const val EXTRA_PRAYER_NAME = "extra_prayer_name"
        const val EXTRA_LOCAL_PATH = "extra_local_path"
        const val EXTRA_MINUTES_BEFORE = "extra_minutes_before"

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

        fun startIqamahAlert(context: Context, prayerNameAr: String, customPath: String? = null) {
            val intent = Intent(context, AdhanPlaybackService::class.java).apply {
                action = ACTION_START_IQAMAH
                putExtra(EXTRA_PRAYER_NAME, prayerNameAr)
                if (customPath != null) putExtra(EXTRA_LOCAL_PATH, customPath)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun speakPrePrayer(
            context: Context,
            prayerNameAr: String,
            minutesBefore: Int,
            customPath: String? = null
        ) {
            val intent = Intent(context, AdhanPlaybackService::class.java).apply {
                action = ACTION_SPEAK_PRE_PRAYER
                putExtra(EXTRA_PRAYER_NAME, prayerNameAr)
                putExtra(EXTRA_MINUTES_BEFORE, minutesBefore)
                if (customPath != null) putExtra(EXTRA_LOCAL_PATH, customPath)
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
