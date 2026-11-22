package com.example.ui.viewmodel

import android.app.Application
import android.media.MediaPlayer
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.DhikrEntity
import com.example.data.local.MuezzinEntity
import com.example.data.local.SettingsDataStore
import com.example.data.model.ActiveDhikrBanner
import com.example.data.model.CalculationMethodOption
import com.example.data.model.CityPreset
import com.example.data.model.DailyPrayerSchedule
import com.example.data.model.DhikrAlertMode
import com.example.data.model.DhikrScreenPosition
import com.example.data.model.IqamahSoundMode
import com.example.data.model.MadhabOption
import com.example.data.model.PrayerName
import com.example.data.model.PrePrayerToneOption
import com.example.data.model.TemperatureUnit
import com.example.data.model.UserSettings
import com.example.data.model.WeatherData
import com.example.data.model.WidgetTypeKey
import com.example.data.repository.DhikrRepository
import com.example.data.repository.MuezzinRepository
import com.example.data.repository.PrayerRepository
import com.example.data.repository.WeatherRepository
import com.example.location.LocationHelper
import com.example.location.ResolvedLocation
import com.example.sensor.CompassReading
import com.example.sensor.QiblaCompassManager
import com.example.service.AdhanAlarmScheduler
import com.example.service.AdhanPlaybackService
import com.example.service.DhikrBannerBus
import com.example.service.NotificationHelper
import com.example.widget.WidgetUpdater
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

data class NoorUiState(
    val settings: UserSettings = UserSettings(),
    val schedule: DailyPrayerSchedule? = null,
    val weather: WeatherData? = null,
    val isWeatherLoading: Boolean = false,
    val weatherError: String? = null,
    val isLocationLoading: Boolean = false,
    val globalLocationResults: List<ResolvedLocation> = emptyList(),
    val isSearchingLocations: Boolean = false,
    val statusMessage: String? = null,
    val muezzins: List<MuezzinEntity> = emptyList(),
    val previewingMuezzinId: String? = null,
    val isAdhanServicePlaying: Boolean = false,
    val canScheduleExactAlarms: Boolean = true,
    val dhikrList: List<DhikrEntity> = emptyList(),
    val previewingDhikrId: String? = null,
    val isDownloadingAllDhikr: Boolean = false
)

class NoorViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val settingsStore = SettingsDataStore(context)
    private val db = AppDatabase.getInstance(context)
    private val prayerRepo = PrayerRepository()
    private val weatherRepo = WeatherRepository(settingsStore)
    private val muezzinRepo = MuezzinRepository(context, db.muezzinDao())
    private val dhikrRepo = DhikrRepository(context, db.dhikrDao())
    private val locationHelper = LocationHelper(context)
    private val compassManager = QiblaCompassManager(context)
    private val alarmScheduler = AdhanAlarmScheduler(context, prayerRepo)

    private val _isWeatherLoading = MutableStateFlow(false)
    private val _weatherError = MutableStateFlow<String?>(null)
    private val _isLocationLoading = MutableStateFlow(false)
    private val _globalLocationResults = MutableStateFlow<List<ResolvedLocation>>(emptyList())
    private val _isSearchingLocations = MutableStateFlow(false)
    private val _statusMessage = MutableStateFlow<String?>(null)
    private val _previewingMuezzinId = MutableStateFlow<String?>(null)
    private val _previewingDhikrId = MutableStateFlow<String?>(null)
    private val _isDownloadingAllDhikr = MutableStateFlow(false)
    private val _canExactAlarms = MutableStateFlow(alarmScheduler.canScheduleExactAlarms())

    private var previewPlayer: MediaPlayer? = null

    private val tickerFlow = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1_000L)
        }
    }

    val compassReading: StateFlow<CompassReading> = settingsStore.settingsFlow
        .map { it.latitude to it.longitude }
        .distinctUntilChanged()
        .flatMapLatest { (lat, lon) -> compassManager.observeAzimuth(lat, lon) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = CompassReading(0f, hasSensor = true, accuracyLow = false)
        )

    private val statusTupleFlow = combine(
        _isWeatherLoading,
        _weatherError,
        _isLocationLoading,
        _statusMessage,
        _previewingMuezzinId
    ) { wLoad, wErr, locLoad, msg, prevId ->
        StatusTuple(wLoad, wErr, locLoad, msg, prevId)
    }

    private val searchTupleFlow = combine(
        _globalLocationResults,
        _isSearchingLocations
    ) { results, searching ->
        results to searching
    }

    private val dhikrTupleFlow = combine(
        dhikrRepo.dhikrListFlow,
        _previewingDhikrId,
        _isDownloadingAllDhikr
    ) { list, previewId, downloading ->
        DhikrTuple(list, previewId, downloading)
    }

    val uiState: StateFlow<NoorUiState> = combine(
        settingsStore.settingsFlow,
        tickerFlow,
        settingsStore.cachedWeatherFlow,
        muezzinRepo.muezzinsFlow,
        statusTupleFlow,
        searchTupleFlow,
        AdhanPlaybackService.isPlayingFlow,
        _canExactAlarms,
        dhikrTupleFlow
    ) { values ->
        val settings = values[0] as UserSettings
        val nowMillis = values[1] as Long
        val weather = values[2] as WeatherData?
        @Suppress("UNCHECKED_CAST")
        val muezzins = values[3] as List<MuezzinEntity>
        val status = values[4] as StatusTuple
        @Suppress("UNCHECKED_CAST")
        val searchPair = values[5] as Pair<List<ResolvedLocation>, Boolean>
        val isServicePlaying = values[6] as Boolean
        val exactAlarms = values[7] as Boolean
        val dhikrTuple = values[8] as DhikrTuple

        val schedule = prayerRepo.computeSchedule(settings, nowMillis)
        NoorUiState(
            settings = settings,
            schedule = schedule,
            weather = weather,
            isWeatherLoading = status.isWeatherLoading,
            weatherError = status.weatherError,
            isLocationLoading = status.isLocationLoading,
            globalLocationResults = searchPair.first,
            isSearchingLocations = searchPair.second,
            statusMessage = status.statusMessage,
            muezzins = muezzins,
            previewingMuezzinId = status.previewingMuezzinId,
            isAdhanServicePlaying = isServicePlaying,
            canScheduleExactAlarms = exactAlarms,
            dhikrList = dhikrTuple.dhikrList,
            previewingDhikrId = dhikrTuple.previewingDhikrId,
            isDownloadingAllDhikr = dhikrTuple.isDownloadingAll
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = NoorUiState()
    )

    init {
        viewModelScope.launch {
            muezzinRepo.ensureSeeded()
            dhikrRepo.ensureSeeded()
            refreshWeather(silent = true)
            dhikrRepo.ensureCoreVoicesReady()
        }
    }

    fun refreshExactAlarmStatus() {
        _canExactAlarms.value = alarmScheduler.canScheduleExactAlarms()
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun showStatusMessage(message: String) {
        _statusMessage.value = message
    }

    fun refreshWeather(silent: Boolean = false) {
        viewModelScope.launch {
            _isWeatherLoading.value = true
            _weatherError.value = null
            val settings = settingsStore.getSettingsOnce()
            val result = weatherRepo.fetchAndCacheWeather(settings.latitude, settings.longitude)
            _isWeatherLoading.value = false
            result.onSuccess {
                WidgetUpdater.updateAllWidgets(context)
                if (!silent) _statusMessage.value = "تم تحديث حالة الطقس بنجاح"
            }.onFailure { err ->
                if (!silent) {
                    _weatherError.value = "تعذر تحديث الطقس: ${err.localizedMessage ?: "تحقق من الاتصال"}"
                }
            }
        }
    }

    fun refreshGpsLocation() {
        viewModelScope.launch {
            _isLocationLoading.value = true
            val result = locationHelper.fetchCurrentLocation()
            _isLocationLoading.value = false
            result.onSuccess { resolved ->
                settingsStore.updateUseGpsLocation(true)
                settingsStore.updateLocation(
                    resolved.latitude,
                    resolved.longitude,
                    resolved.cityNameAr,
                    resolved.countryNameAr
                )
                onScheduleInputsChanged()
                refreshWeather(silent = true)
                _statusMessage.value = "تم تحديث الموقع: ${resolved.cityNameAr}"
            }.onFailure {
                _statusMessage.value = "تعذر جلب الموقع التلقائي، يمكنك اختيار الدولة والمدينة يدوياً"
            }
        }
    }

    fun selectManualCity(city: CityPreset, countryName: String) {
        viewModelScope.launch {
            settingsStore.updateUseGpsLocation(false)
            settingsStore.updateLocation(city.latitude, city.longitude, city.nameAr, countryName)
            onScheduleInputsChanged()
            refreshWeather(silent = true)
            _statusMessage.value = "تم ضبط الموقع على ${city.nameAr} — $countryName"
        }
    }

    fun selectResolvedLocation(resolved: ResolvedLocation) {
        viewModelScope.launch {
            settingsStore.updateUseGpsLocation(false)
            settingsStore.updateLocation(
                resolved.latitude,
                resolved.longitude,
                resolved.cityNameAr,
                resolved.countryNameAr
            )
            onScheduleInputsChanged()
            refreshWeather(silent = true)
            val suffix = if (resolved.countryNameAr.isNotBlank()) " — ${resolved.countryNameAr}" else ""
            _statusMessage.value = "تم ضبط الموقع على ${resolved.cityNameAr}$suffix"
        }
    }

    fun searchGlobalCities(query: String) {
        viewModelScope.launch {
            if (query.trim().length < 2) {
                _globalLocationResults.value = emptyList()
                return@launch
            }
            _isSearchingLocations.value = true
            _globalLocationResults.value = locationHelper.searchCityByName(query.trim())
            _isSearchingLocations.value = false
        }
    }

    fun clearGlobalSearch() {
        _globalLocationResults.value = emptyList()
    }

    fun updateCalculationMethod(method: CalculationMethodOption) {
        viewModelScope.launch {
            settingsStore.updateCalculationMethod(method)
            onScheduleInputsChanged()
        }
    }

    fun updateMadhab(madhab: MadhabOption) {
        viewModelScope.launch {
            settingsStore.updateMadhab(madhab)
            onScheduleInputsChanged()
        }
    }

    fun setAdhanEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.updateAdhanEnabled(enabled)
            onScheduleInputsChanged()
        }
    }

    fun setIqamahEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.updateIqamahAlertEnabled(enabled)
            onScheduleInputsChanged()
        }
    }

    fun setIqamahSoundMode(mode: IqamahSoundMode) {
        viewModelScope.launch {
            settingsStore.updateIqamahSoundMode(mode)
            _statusMessage.value = "تم ضبط صوت الإقامة: ${mode.titleAr}"
        }
    }

    fun setPrayerIqamahOffset(prayer: PrayerName, minutes: Int) {
        viewModelScope.launch {
            settingsStore.updatePrayerIqamahOffset(prayer, minutes)
            onScheduleInputsChanged()
        }
    }

    fun setPrePrayerEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.updatePrePrayerReminderEnabled(enabled)
            onScheduleInputsChanged()
        }
    }

    fun setPrayerAdhanEnabled(prayer: PrayerName, enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.updatePrayerAdhanEnabled(prayer, enabled)
            onScheduleInputsChanged()
        }
    }

    fun setPrayerIqamahEnabled(prayer: PrayerName, enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.updatePrayerIqamahEnabled(prayer, enabled)
            onScheduleInputsChanged()
        }
    }

    fun setPrayerPrePrayerEnabled(prayer: PrayerName, enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.updatePrayerPrePrayerEnabled(prayer, enabled)
            onScheduleInputsChanged()
        }
    }

    fun setPrePrayerSpeakAnnouncement(speak: Boolean) {
        viewModelScope.launch {
            settingsStore.updatePrePrayerSpeakAnnouncement(speak)
        }
    }

    fun setPrePrayerMinutes(minutes: Int) {
        viewModelScope.launch {
            settingsStore.updatePrePrayerMinutes(minutes.coerceIn(1, 60))
            onScheduleInputsChanged()
        }
    }

    fun setPrePrayerTone(tone: PrePrayerToneOption) {
        viewModelScope.launch {
            settingsStore.updatePrePrayerTone(tone)
            onScheduleInputsChanged()
        }
    }

    fun setTemperatureUnit(unit: TemperatureUnit) {
        viewModelScope.launch {
            settingsStore.updateTemperatureUnit(unit)
            WidgetUpdater.updateAllWidgets(context)
        }
    }

    fun selectMuezzin(muezzin: MuezzinEntity) {
        viewModelScope.launch {
            settingsStore.updateSelectedMuezzin(muezzin.id)
            _statusMessage.value = "تم اختيار ${muezzin.nameAr} للأذان"
        }
    }

    fun importCustomAdhanAudio(uri: Uri) {
        viewModelScope.launch {
            val result = muezzinRepo.importCustomAdhanFromUri(uri)
            result.onSuccess { entity ->
                settingsStore.updateSelectedMuezzin(entity.id)
                _statusMessage.value = "تمت إضافة (${entity.nameAr}) من ذاكرة الهاتف وتعيينه للأذان"
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "تعذر استيراد الملف الصوتي"
            }
        }
    }

    fun importCustomIqamahAudio(uri: Uri) {
        viewModelScope.launch {
            val result = muezzinRepo.importCustomIqamahFromUri(uri)
            result.onSuccess { (path, name) ->
                settingsStore.updateCustomIqamahAudio(path, name)
                settingsStore.updateIqamahSoundMode(IqamahSoundMode.CUSTOM_AUDIO)
                _statusMessage.value = "تم تعيين صوت الإقامة من الذاكرة: $name"
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "تعذر استيراد ملف الإقامة"
            }
        }
    }

    fun importCustomPrePrayerAudio(prayer: PrayerName, uri: Uri) {
        viewModelScope.launch {
            val existingPath = uiState.value.settings.perPrayerPrePrayerAudio[prayer]?.path
            val result = muezzinRepo.importCustomPrePrayerFromUri(prayer, uri)
            result.onSuccess { (path, name) ->
                if (!existingPath.isNullOrBlank() && existingPath != path) {
                    muezzinRepo.deleteLocalFileIfExists(existingPath)
                }
                settingsStore.updatePrayerPrePrayerAudio(prayer, path, name)
                settingsStore.updatePrayerPrePrayerEnabled(prayer, true)
                _statusMessage.value = "تم تعيين صوت تنبيه ${prayer.arabicName}: $name"
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "تعذر استيراد صوت التنبيه"
            }
        }
    }

    fun clearCustomPrePrayerAudio(prayer: PrayerName) {
        viewModelScope.launch {
            val existingPath = uiState.value.settings.perPrayerPrePrayerAudio[prayer]?.path
            muezzinRepo.deleteLocalFileIfExists(existingPath)
            settingsStore.updatePrayerPrePrayerAudio(prayer, null, null)
            _statusMessage.value = "تمت إزالة الصوت المخصص لتنبيه ${prayer.arabicName}"
        }
    }

    fun downloadMuezzin(muezzin: MuezzinEntity) {
        muezzinRepo.startDownload(muezzin)
        _statusMessage.value = "بدأ تنزيل أذان ${muezzin.nameAr}..."
    }

    fun deleteMuezzinAudio(muezzin: MuezzinEntity) {
        viewModelScope.launch {
            if (_previewingMuezzinId.value == muezzin.id) stopPreview()
            muezzinRepo.deleteDownload(muezzin)
            _statusMessage.value = "تم حذف الملف الصوتي وتوفير المساحة"
        }
    }

    fun togglePreviewMuezzin(muezzin: MuezzinEntity) {
        if (_previewingMuezzinId.value == muezzin.id) {
            stopPreview()
            return
        }
        stopPreview()
        val path = muezzin.localFilePath
        if (path.isNullOrBlank() || !File(path).exists()) {
            _statusMessage.value = "يرجى تنزيل الصوت أولاً للاستماع بدون إنترنت"
            return
        }
        try {
            val player = MediaPlayer().apply {
                setDataSource(path)
                setOnCompletionListener { stopPreview() }
                prepare()
                start()
            }
            previewPlayer = player
            _previewingMuezzinId.value = muezzin.id
        } catch (_: Exception) {
            _statusMessage.value = "تعذر تشغيل الملف الصوتي"
            stopPreview()
        }
    }

    fun stopPreview() {
        runCatching {
            previewPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
        }
        previewPlayer = null
        _previewingMuezzinId.value = null
        _previewingDhikrId.value = null
    }

    fun triggerTestAdhan() {
        val nextName = uiState.value.schedule?.nextPrayer?.prayer?.arabicName ?: "الظهر"
        AdhanPlaybackService.startAdhan(context, nextName)
    }

    fun triggerTestIqamahAlert() {
        val settings = uiState.value.settings
        val nextName = uiState.value.schedule?.nextPrayer?.prayer?.arabicName ?: "الظهر"
        AdhanPlaybackService.startIqamahAlert(context, nextName, settings.customIqamahAudioPath)
    }

    fun stopActiveAdhan() {
        AdhanPlaybackService.stopAdhan(context)
    }

    fun triggerTestPrePrayerNotification() {
        val nextPrayer = uiState.value.schedule?.nextPrayer?.prayer ?: PrayerName.ASR
        triggerTestPrePrayerForPrayer(nextPrayer)
    }

    fun triggerTestPrePrayerForPrayer(prayer: PrayerName) {
        val settings = uiState.value.settings
        val prayerNameAr = prayer.arabicName
        NotificationHelper.showPrePrayerNotification(
            context = context,
            prayerNameAr = prayerNameAr,
            minutesBefore = settings.prePrayerMinutes,
            toneOption = settings.prePrayerTone
        )
        val customPath = settings.perPrayerPrePrayerAudio[prayer]?.path
            ?.takeIf { File(it).exists() }
        if (customPath != null) {
            AdhanPlaybackService.speakPrePrayer(
                context = context,
                prayerNameAr = prayerNameAr,
                minutesBefore = settings.prePrayerMinutes,
                customPath = customPath
            )
        } else if (settings.prePrayerSpeakAnnouncement) {
            AdhanPlaybackService.speakPrePrayer(
                context = context,
                prayerNameAr = prayerNameAr,
                minutesBefore = settings.prePrayerMinutes
            )
        }
    }

    fun updateWidgetAppearance(
        widgetKey: WidgetTypeKey,
        sizeScale: Float,
        fontScale: Float,
        showClock: Boolean,
        showGregorianDate: Boolean
    ) {
        viewModelScope.launch {
            settingsStore.updateWidgetAppearance(widgetKey, sizeScale, fontScale, showClock, showGregorianDate)
            WidgetUpdater.updateAllWidgets(context)
        }
    }

    fun resetWidgetAppearances() {
        viewModelScope.launch {
            settingsStore.resetWidgetAppearances()
            WidgetUpdater.updateAllWidgets(context)
            _statusMessage.value = "تمت إعادة تعيين حجم وخط جميع الودجات"
        }
    }

    fun forceUpdateWidgets() {
        viewModelScope.launch {
            WidgetUpdater.updateAllWidgets(context)
            _statusMessage.value = "تم تحديث جميع الودجات"
        }
    }

    fun formatFileSize(muezzin: MuezzinEntity): String {
        return muezzinRepo.formatBytes(muezzin.fileSizeBytes, muezzin.estimatedSizeLabel)
    }

    private suspend fun onScheduleInputsChanged() {
        val updated = settingsStore.getSettingsOnce()
        alarmScheduler.rescheduleAll(updated)
        WidgetUpdater.updateAllWidgets(context)
    }

    // ---- Dhikr Human-Voice Notifications & Random Daily Schedule ----

    fun setDhikrEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.updateDhikrEnabled(enabled)
            val updated = settingsStore.getSettingsOnce()
            alarmScheduler.rescheduleDhikrOnly(updated)
            if (enabled) {
                dhikrRepo.ensureCoreVoicesReady()
            }
        }
    }

    fun setDhikrMode(mode: DhikrAlertMode) {
        viewModelScope.launch {
            settingsStore.updateDhikrMode(mode)
            _statusMessage.value = "تم ضبط نوع إشعار الأذكار: ${mode.titleAr}"
        }
    }

    fun setDhikrDailyCount(count: Int) {
        viewModelScope.launch {
            settingsStore.updateDhikrDailyCount(count)
            val updated = settingsStore.getSettingsOnce()
            alarmScheduler.rescheduleDhikrOnly(updated)
        }
    }

    fun setDhikrScreenPosition(position: DhikrScreenPosition) {
        viewModelScope.launch {
            settingsStore.updateDhikrScreenPosition(position)
        }
    }

    fun setDhikrActiveHours(startHour: Int, endHour: Int) {
        viewModelScope.launch {
            settingsStore.updateDhikrActiveHours(startHour, endHour)
            val updated = settingsStore.getSettingsOnce()
            alarmScheduler.rescheduleDhikrOnly(updated)
        }
    }

    fun reshuffleDhikrToday() {
        viewModelScope.launch {
            val updated = settingsStore.getSettingsOnce()
            val times = alarmScheduler.rescheduleDhikrOnly(updated)
            _statusMessage.value = if (updated.dhikrSettings.enabled && times.isNotEmpty()) {
                "تم إعادة توزيع ${updated.dhikrSettings.dailyCount} أذكار عشوائياً على مدار اليوم"
            } else {
                "يرجى تفعيل إشعارات الأذكار أولاً"
            }
        }
    }

    fun downloadAllDhikrHumanVoices() {
        if (_isDownloadingAllDhikr.value) return
        viewModelScope.launch {
            _isDownloadingAllDhikr.value = true
            val (ok, total) = dhikrRepo.downloadAllHumanVoices()
            _isDownloadingAllDhikr.value = false
            _statusMessage.value = if (ok > 0) {
                "تم تجهيز $ok من أصل $total صوت بشري حقيقي للأذكار"
            } else {
                "تعذر تنزيل الأصوات، تحقق من اتصالك بالإنترنت"
            }
        }
    }

    fun downloadSingleDhikrVoice(dhikr: DhikrEntity) {
        viewModelScope.launch {
            val res = dhikrRepo.downloadSingleHumanVoice(dhikr)
            res.onSuccess {
                _statusMessage.value = "تم تنزيل الصوت البشري: ${dhikr.titleAr}"
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "تعذر تنزيل الصوت البشري"
            }
        }
    }

    fun toggleDhikrItemEnabled(dhikr: DhikrEntity, enabled: Boolean) {
        viewModelScope.launch {
            dhikrRepo.setDhikrEnabled(dhikr.id, enabled)
        }
    }

    fun importCustomHumanAudioForDhikr(dhikr: DhikrEntity, uri: Uri) {
        viewModelScope.launch {
            val res = dhikrRepo.importCustomHumanAudioForDhikr(dhikr, uri)
            res.onSuccess {
                _statusMessage.value = "تم تعيين صوت بشري من الذاكرة لـ: ${dhikr.titleAr}"
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "تعذر استيراد الملف الصوتي"
            }
        }
    }

    fun addCustomDhikr(titleAr: String, textAr: String, audioUri: Uri?) {
        viewModelScope.launch {
            val res = dhikrRepo.addCustomDhikr(titleAr = titleAr, textAr = textAr, audioUri = audioUri)
            res.onSuccess {
                _statusMessage.value = "تمت إضافة الذكر إلى المكتبة: ${it.titleAr}"
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "تعذر إضافة الذكر"
            }
        }
    }

    fun deleteCustomDhikr(dhikr: DhikrEntity) {
        viewModelScope.launch {
            if (_previewingDhikrId.value == dhikr.id) stopPreview()
            dhikrRepo.deleteCustomDhikr(dhikr)
            _statusMessage.value = "تم حذف الذكر المخصص"
        }
    }

    fun togglePreviewDhikrVoice(dhikr: DhikrEntity) {
        if (_previewingDhikrId.value == dhikr.id) {
            stopPreview()
            return
        }
        stopPreview()
        viewModelScope.launch {
            var path = dhikr.localFilePath?.takeIf { File(it).exists() && File(it).length() > 512L }
            if (path == null) {
                _statusMessage.value = "جاري تنزيل الصوت البشري لـ ${dhikr.titleAr}..."
                dhikrRepo.syncCatalogFromOfficialJson()
                path = dhikrRepo.downloadSingleHumanVoice(dhikr).getOrNull()
            }
            if (path.isNullOrBlank() || !File(path).exists()) {
                _statusMessage.value = "تعذر العثور على الصوت البشري؛ يمكنك تنزيله بالإنترنت أو اختياره من الذاكرة"
                return@launch
            }
            try {
                val player = MediaPlayer().apply {
                    setDataSource(path)
                    setOnCompletionListener { stopPreview() }
                    prepare()
                    start()
                }
                previewPlayer = player
                _previewingDhikrId.value = dhikr.id
            } catch (_: Exception) {
                _statusMessage.value = "تعذر تشغيل التسجيل الصوتي"
                stopPreview()
            }
        }
    }

    fun previewBannerPositionOnScreen() {
        val cfg = uiState.value.settings.dhikrSettings
        if (cfg.screenPosition == DhikrScreenPosition.HIDDEN) {
            DhikrBannerBus.dismiss()
            _statusMessage.value = "الإشعار مضبوط على الإخفاء — لن يظهر على الشاشة"
            return
        }
        val sample = uiState.value.dhikrList.firstOrNull { it.isEnabled }
        DhikrBannerBus.post(
            ActiveDhikrBanner(
                id = sample?.id ?: "preview",
                title = sample?.titleAr ?: "سبحان الله وبحمده",
                text = sample?.textAr ?: "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ، سُبْحَانَ اللَّهِ الْعَظِيمِ",
                reciterLabel = sample?.reciterAr ?: "الشيخ حمد الدريهم — حصن المسلم",
                position = cfg.screenPosition
            )
        )
    }

    fun triggerTestRandomDhikrNow() {
        viewModelScope.launch {
            val settings = settingsStore.getSettingsOnce()
            val cfg = settings.dhikrSettings
            val chosen = dhikrRepo.pickRandomPlayableDhikr(
                excludeId = cfg.lastTriggeredDhikrId,
                preferDownloadedAudio = cfg.mode != DhikrAlertMode.TEXT_ONLY
            )
            if (chosen == null) {
                _statusMessage.value = "يرجى تفعيل ذكر واحد على الأقل من القائمة"
                return@launch
            }
            settingsStore.updateLastTriggeredDhikrId(chosen.id)

            val showVisual = cfg.mode != DhikrAlertMode.VOICE_ONLY &&
                cfg.screenPosition != DhikrScreenPosition.HIDDEN

            if (showVisual) {
                DhikrBannerBus.post(
                    ActiveDhikrBanner(
                        id = chosen.id,
                        title = chosen.titleAr,
                        text = chosen.textAr,
                        reciterLabel = chosen.reciterAr,
                        position = cfg.screenPosition
                    )
                )
                NotificationHelper.showDhikrNotification(
                    context = context,
                    dhikrTitle = chosen.titleAr,
                    dhikrText = chosen.textAr,
                    position = cfg.screenPosition,
                    includeToneSound = cfg.mode == DhikrAlertMode.TEXT_ONLY
                )
            } else {
                DhikrBannerBus.dismiss()
            }

            if (cfg.mode != DhikrAlertMode.TEXT_ONLY) {
                AdhanPlaybackService.playDhikrAudio(
                    context = context,
                    dhikrId = chosen.id,
                    dhikrTitle = chosen.titleAr,
                    dhikrText = chosen.textAr,
                    localPath = chosen.localFilePath,
                    silentNotification = !showVisual
                )
            }
        }
    }

    override fun onCleared() {
        stopPreview()
        super.onCleared()
    }

    private data class StatusTuple(
        val isWeatherLoading: Boolean,
        val weatherError: String?,
        val isLocationLoading: Boolean,
        val statusMessage: String?,
        val previewingMuezzinId: String?
    )

    private data class DhikrTuple(
        val dhikrList: List<DhikrEntity>,
        val previewingDhikrId: String?,
        val isDownloadingAll: Boolean
    )
}
