package com.example.ui.viewmodel

import android.app.Application
import android.media.MediaPlayer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.MuezzinEntity
import com.example.data.local.SettingsDataStore
import com.example.data.model.CalculationMethodOption
import com.example.data.model.CityPreset
import com.example.data.model.DailyPrayerSchedule
import com.example.data.model.MadhabOption
import com.example.data.model.PrayerName
import com.example.data.model.PrePrayerToneOption
import com.example.data.model.TemperatureUnit
import com.example.data.model.UserSettings
import com.example.data.model.WeatherData
import com.example.data.repository.MuezzinRepository
import com.example.data.repository.PrayerRepository
import com.example.data.repository.WeatherRepository
import com.example.location.LocationHelper
import com.example.location.ResolvedLocation
import com.example.sensor.CompassReading
import com.example.sensor.QiblaCompassManager
import com.example.service.AdhanAlarmScheduler
import com.example.service.AdhanPlaybackService
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
    val canScheduleExactAlarms: Boolean = true
)

class NoorViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val settingsStore = SettingsDataStore(context)
    private val db = AppDatabase.getInstance(context)
    private val prayerRepo = PrayerRepository()
    private val weatherRepo = WeatherRepository(settingsStore)
    private val muezzinRepo = MuezzinRepository(context, db.muezzinDao())
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

    val uiState: StateFlow<NoorUiState> = combine(
        settingsStore.settingsFlow,
        tickerFlow,
        settingsStore.cachedWeatherFlow,
        muezzinRepo.muezzinsFlow,
        statusTupleFlow,
        searchTupleFlow,
        AdhanPlaybackService.isPlayingFlow,
        _canExactAlarms
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
            canScheduleExactAlarms = exactAlarms
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = NoorUiState()
    )

    init {
        viewModelScope.launch {
            muezzinRepo.ensureSeeded()
            refreshWeather(silent = true)
        }
    }

    fun refreshExactAlarmStatus() {
        _canExactAlarms.value = alarmScheduler.canScheduleExactAlarms()
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
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

    fun setPrePrayerMinutes(minutes: Int) {
        viewModelScope.launch {
            settingsStore.updatePrePrayerMinutes(minutes)
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
    }

    fun triggerTestAdhan() {
        val nextName = uiState.value.schedule?.nextPrayer?.prayer?.arabicName ?: "الظهر"
        AdhanPlaybackService.startAdhan(context, nextName)
    }

    fun stopActiveAdhan() {
        AdhanPlaybackService.stopAdhan(context)
    }

    fun triggerTestPrePrayerNotification() {
        val settings = uiState.value.settings
        val nextName = uiState.value.schedule?.nextPrayer?.prayer?.arabicName ?: "العصر"
        NotificationHelper.showPrePrayerNotification(
            context = context,
            prayerNameAr = nextName,
            minutesBefore = settings.prePrayerMinutes,
            toneOption = settings.prePrayerTone
        )
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
}
