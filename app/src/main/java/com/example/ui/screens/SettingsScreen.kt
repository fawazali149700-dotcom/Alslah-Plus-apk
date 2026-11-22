package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.CalculationMethodOption
import com.example.data.model.CityPreset
import com.example.data.model.IqamahSoundMode
import com.example.data.model.MadhabOption
import com.example.data.model.PrayerName
import com.example.data.model.PrePrayerToneOption
import com.example.data.model.TemperatureUnit
import com.example.data.model.WidgetAppearanceConfig
import com.example.data.model.WidgetTypeKey
import com.example.location.ResolvedLocation
import com.example.ui.components.CountryCityPickerDialog
import com.example.ui.components.DeveloperContactCard
import com.example.ui.components.PerPrayerAdhanIqamahCard
import com.example.ui.components.PerPrayerPreReminderCard
import com.example.ui.components.SettingsSectionCard
import com.example.ui.components.WidgetLivePreview
import com.example.ui.viewmodel.NoorUiState
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    uiState: NoorUiState,
    onCalculationMethodChange: (CalculationMethodOption) -> Unit,
    onMadhabChange: (MadhabOption) -> Unit,
    onAdhanEnabledChange: (Boolean) -> Unit,
    onPrayerAdhanEnabledChange: (PrayerName, Boolean) -> Unit,
    onIqamahEnabledChange: (Boolean) -> Unit,
    onPrayerIqamahEnabledChange: (PrayerName, Boolean) -> Unit,
    onIqamahSoundModeChange: (IqamahSoundMode) -> Unit,
    onPickCustomIqamahAudio: () -> Unit,
    onTestIqamahAlert: () -> Unit,
    onPrayerIqamahOffsetChange: (PrayerName, Int) -> Unit,
    onPrePrayerEnabledChange: (Boolean) -> Unit,
    onPrayerPrePrayerEnabledChange: (PrayerName, Boolean) -> Unit,
    onPickPrayerPreAudio: (PrayerName) -> Unit,
    onClearPrayerPreAudio: (PrayerName) -> Unit,
    onTestPrayerPreAudio: (PrayerName) -> Unit,
    onPrePrayerSpeakChange: (Boolean) -> Unit,
    onPrePrayerMinutesChange: (Int) -> Unit,
    onPrePrayerToneChange: (PrePrayerToneOption) -> Unit,
    onTemperatureUnitChange: (TemperatureUnit) -> Unit,
    onUseGpsClick: () -> Unit,
    onSelectManualCity: (CityPreset, String) -> Unit,
    onSearchGlobalCities: (String) -> Unit,
    onSelectResolvedLocation: (ResolvedLocation) -> Unit,
    onClearGlobalSearch: () -> Unit,
    onOpenMuezzinLibrary: () -> Unit,
    onTestAdhan: () -> Unit,
    onTestPrePrayerNotification: () -> Unit,
    onRequestExactAlarmPermission: () -> Unit,
    onWidgetAppearanceChange: (WidgetTypeKey, Float, Float, Boolean, Boolean) -> Unit,
    onResetWidgetAppearances: () -> Unit,
    onForceUpdateWidgets: () -> Unit,
    onShowStatusMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val settings = uiState.settings
    var showCountryCityDialog by remember { mutableStateOf(false) }
    var showMethodDialog by remember { mutableStateOf(false) }
    var selectedWidgetKey by remember { mutableStateOf(WidgetTypeKey.COMPACT_4X1) }

    val selectedMuezzinName = uiState.muezzins.firstOrNull {
        it.id == settings.selectedMuezzinId
    }?.nameAr ?: "المؤذن الافتراضي"

    val obligatoryPrayers = remember {
        listOf(
            PrayerName.FAJR,
            PrayerName.DHUHR,
            PrayerName.ASR,
            PrayerName.MAGHRIB,
            PrayerName.ISHA
        )
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 620.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "إعدادات تطبيق نور",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (!uiState.canScheduleExactAlarms) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "تنبيه مهم: إذن المنبهات الدقيقة مطلوب",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "لضمان رفع الأذان وإرسال تنبيه اقتراب الصلاة في الوقت المحدد بالضبط، يرجى تفعيل إذن المنبهات الدقيقة.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = onRequestExactAlarmPermission) {
                                Text("تفعيل المنبهات الدقيقة")
                            }
                        }
                    }
                }
            }

            // 1. Prayer Calculation Method & Madhab
            item {
                SettingsSectionCard(title = "حساب أوقات الصلاة", icon = Icons.Default.Alarm) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showMethodDialog = true }
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("طريقة الحساب", style = MaterialTheme.typography.titleMedium)
                            Text(
                                settings.calculationMethod.titleAr,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        TextButton(onClick = { showMethodDialog = true }) { Text("تغيير") }
                    }

                    Spacer(Modifier.height(6.dp))
                    Text("المذهب الفقهي (لصلاة العصر)", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MadhabOption.entries.forEach { option ->
                            FilterChip(
                                selected = settings.madhab == option,
                                onClick = { onMadhabChange(option) },
                                label = { Text(option.titleAr) }
                            )
                        }
                    }
                }
            }

            // 2. Adhan & Per-Prayer Adhan/Iqamah Controls
            item {
                SettingsSectionCard(title = "الأذان وإقامة كل صلاة", icon = Icons.Default.NotificationsActive) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("تشغيل صوت الأذان تلقائياً", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "المؤذن: $selectedMuezzinName",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.adhanEnabled,
                            onCheckedChange = onAdhanEnabledChange,
                            modifier = Modifier.testTag("global_adhan_switch")
                        )
                    }

                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onOpenMuezzinLibrary) {
                            Text("اختيار / تنزيل المؤذن")
                        }
                        OutlinedButton(
                            onClick = onTestAdhan,
                            modifier = Modifier.testTag("test_adhan_btn")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("تجربة الأذان")
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("تنبيه وقت الإقامة", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "فعّل الأذان والإقامة واضبط المدة لكل صلاة على حدة",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.iqamahAlertEnabled,
                            onCheckedChange = onIqamahEnabledChange,
                            modifier = Modifier.testTag("global_iqamah_switch")
                        )
                    }

                    Text(
                        text = "التحكم في تفعيل الأذان والإقامة لكل صلاة:",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        obligatoryPrayers.forEach { prayer ->
                            PerPrayerAdhanIqamahCard(
                                prayer = prayer,
                                globalAdhanEnabled = settings.adhanEnabled,
                                globalIqamahEnabled = settings.iqamahAlertEnabled,
                                prayerAdhanEnabled = settings.perPrayerAdhanEnabled.isEnabled(prayer),
                                prayerIqamahEnabled = settings.perPrayerIqamahEnabled.isEnabled(prayer),
                                iqamahMinutes = settings.iqamahOffsets.offsetFor(prayer),
                                onAdhanToggle = { onPrayerAdhanEnabledChange(prayer, it) },
                                onIqamahToggle = { onPrayerIqamahEnabledChange(prayer, it) },
                                onIqamahMinutesChange = { onPrayerIqamahOffsetChange(prayer, it) }
                            )
                        }
                    }

                    if (settings.iqamahAlertEnabled) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text("صوت تنبيه الإقامة", style = MaterialTheme.typography.labelLarge)
                        IqamahSoundMode.entries.forEach { mode ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onIqamahSoundModeChange(mode) }
                                    .padding(vertical = 4.dp)
                                    .testTag("iqamah_mode_${mode.name}"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = settings.iqamahSoundMode == mode,
                                    onClick = { onIqamahSoundModeChange(mode) }
                                )
                                Text(mode.titleAr, style = MaterialTheme.typography.bodyMedium)
                            }
                        }

                        if (settings.iqamahSoundMode == IqamahSoundMode.CUSTOM_AUDIO) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "ملف الإقامة المختار: ${settings.customIqamahAudioName ?: "لم يتم اختيار ملف بعد"}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    OutlinedButton(
                                        onClick = onPickCustomIqamahAudio,
                                        modifier = Modifier.testTag("pick_custom_iqamah_btn")
                                    ) {
                                        Icon(Icons.Default.AudioFile, contentDescription = null)
                                        Spacer(Modifier.width(6.dp))
                                        Text("اختيار صوت للإقامة من ذاكرة الهاتف")
                                    }
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = onTestIqamahAlert,
                            modifier = Modifier.testTag("test_iqamah_btn")
                        ) {
                            Icon(Icons.Default.RecordVoiceOver, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("تجربة صوت الإقامة")
                        }
                    }
                }
            }

            // 3. Pre-Prayer Reminder Notification (1..60 mins + Per-Prayer Custom Audio)
            item {
                SettingsSectionCard(title = "إشعار اقتراب موعد الصلاة", icon = Icons.Default.NotificationsActive) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("تفعيل تنبيه الاستعداد للصلاة", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "إرسال إشعار بالصوت والاهتزاز قبل دخول الوقت",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.prePrayerReminderEnabled,
                            onCheckedChange = onPrePrayerEnabledChange,
                            modifier = Modifier.testTag("global_pre_prayer_switch")
                        )
                    }

                    if (settings.prePrayerReminderEnabled) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("نطق صوتي افتراضي عند الاقتراب", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "ينطق تلقائياً عند عدم تعيين صوت مخصص من الذاكرة",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = settings.prePrayerSpeakAnnouncement,
                                onCheckedChange = onPrePrayerSpeakChange,
                                modifier = Modifier.testTag("pre_prayer_speak_switch")
                            )
                        }

                        Text("مدة التنبيه قبل الأذان (أقل من 10 دقائق أو أكثر)", style = MaterialTheme.typography.labelLarge)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(3, 5, 7, 10, 15, 20, 30).forEach { mins ->
                                FilterChip(
                                    selected = settings.prePrayerMinutes == mins,
                                    onClick = { onPrePrayerMinutesChange(mins) },
                                    label = { Text("قبل $mins د") },
                                    modifier = Modifier.testTag("pre_prayer_chip_$mins")
                                )
                            }
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "ضبط دقيق بالدقيقة (1 – 60 د)",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            onPrePrayerMinutesChange((settings.prePrayerMinutes - 1).coerceAtLeast(1))
                                        },
                                        modifier = Modifier.testTag("pre_prayer_minus_btn")
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "إنقاص دقيقة")
                                    }
                                    Text(
                                        text = "${settings.prePrayerMinutes} د",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    IconButton(
                                        onClick = {
                                            onPrePrayerMinutesChange((settings.prePrayerMinutes + 1).coerceAtMost(60))
                                        },
                                        modifier = Modifier.testTag("pre_prayer_plus_btn")
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "زيادة دقيقة")
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(4.dp))
                        Text("نغمة تنبيه الاقتراب (للإشعار)", style = MaterialTheme.typography.labelLarge)
                        PrePrayerToneOption.entries.forEach { tone ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onPrePrayerToneChange(tone) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = settings.prePrayerTone == tone,
                                    onClick = { onPrePrayerToneChange(tone) }
                                )
                                Text(tone.titleAr, style = MaterialTheme.typography.bodyMedium)
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text("صوت من الذاكرة وتفعيل التنبيه لكل صلاة على حدة:", style = MaterialTheme.typography.labelLarge)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            obligatoryPrayers.forEach { prayer ->
                                PerPrayerPreReminderCard(
                                    prayer = prayer,
                                    globalEnabled = settings.prePrayerReminderEnabled,
                                    prayerEnabled = settings.perPrayerPrePrayerEnabled.isEnabled(prayer),
                                    attachment = settings.perPrayerPrePrayerAudio[prayer],
                                    onTogglePrayer = { onPrayerPrePrayerEnabledChange(prayer, it) },
                                    onPickAudio = { onPickPrayerPreAudio(prayer) },
                                    onClearAudio = { onClearPrayerPreAudio(prayer) },
                                    onTestAudio = { onTestPrayerPreAudio(prayer) }
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = onTestPrePrayerNotification,
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("test_pre_prayer_btn")
                        ) {
                            Text("تجربة إشعار اقتراب الصلاة القادمة")
                        }
                    }
                }
            }

            // 4. Location (All Countries & Cities + Global Search) & Weather Units
            item {
                SettingsSectionCard(title = "الموقع ووحدة الحرارة", icon = Icons.Default.LocationOn) {
                    val countryLabel = if (settings.countryName.isNotBlank()) " (${settings.countryName})" else ""
                    Text(
                        text = "الموقع الحالي: ${settings.cityName}$countryLabel — ${if (settings.useGpsLocation) "تلقائي GPS" else "يدوي"}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onUseGpsClick) {
                            Text("تحديد تلقائي (GPS)")
                        }
                        OutlinedButton(
                            onClick = { showCountryCityDialog = true },
                            modifier = Modifier.testTag("open_country_city_picker_btn")
                        ) {
                            Text("اختيار الدولة والمدينة")
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Thermostat, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(6.dp))
                        Text("وحدة درجة الحرارة", style = MaterialTheme.typography.labelLarge)
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TemperatureUnit.entries.forEach { unit ->
                            FilterChip(
                                selected = settings.temperatureUnit == unit,
                                onClick = { onTemperatureUnitChange(unit) },
                                label = { Text(unit.titleAr) }
                            )
                        }
                    }
                }
            }

            // 5. Home Screen Widgets Customization (Size, Font, Clock & Gregorian per Widget)
            item {
                val activeConfig = settings.widgetConfigs[selectedWidgetKey]
                    ?: WidgetAppearanceConfig.defaultFor(selectedWidgetKey)
                var sliderSize by remember(selectedWidgetKey) { mutableFloatStateOf(activeConfig.sizeScale) }
                var sliderFont by remember(selectedWidgetKey) { mutableFloatStateOf(activeConfig.fontScale) }
                var showClock by remember(selectedWidgetKey) { mutableStateOf(activeConfig.showClock) }
                var showGregorian by remember(selectedWidgetKey) { mutableStateOf(activeConfig.showGregorianDate) }

                LaunchedEffect(
                    activeConfig.sizeScale,
                    activeConfig.fontScale,
                    activeConfig.showClock,
                    activeConfig.showGregorianDate,
                    selectedWidgetKey
                ) {
                    sliderSize = activeConfig.sizeScale
                    sliderFont = activeConfig.fontScale
                    showClock = activeConfig.showClock
                    showGregorian = activeConfig.showGregorianDate
                }

                SettingsSectionCard(title = "ودجات الشاشة الرئيسية وتخصيصها", icon = Icons.Default.Widgets) {
                    Text(
                        text = "يتوفر 8 ودجات بأحجام متنوعة (أقلها 1×4 شريط شامل، و2×2، و2×4، و3×4، و4×4). اختر أي ودجت لضبط حجمه وحجم الخط وإظهار الساعة والتاريخ الميلادي:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        WidgetTypeKey.entries.forEach { key ->
                            FilterChip(
                                selected = selectedWidgetKey == key,
                                onClick = { selectedWidgetKey = key },
                                label = { Text(key.titleAr) },
                                modifier = Modifier.testTag("widget_select_${key.name}")
                            )
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            WidgetLivePreview(
                                widgetKey = selectedWidgetKey,
                                sizeScale = sliderSize,
                                fontScale = sliderFont,
                                showClock = showClock,
                                showGregorianDate = showGregorian,
                                schedule = uiState.schedule,
                                weather = uiState.weather,
                                tempUnit = settings.temperatureUnit
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("حجم الودجت والهوامش", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "${(sliderSize * 100f).roundToInt()}%",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            Slider(
                                value = sliderSize,
                                onValueChange = { sliderSize = it },
                                onValueChangeFinished = {
                                    onWidgetAppearanceChange(
                                        selectedWidgetKey,
                                        sliderSize,
                                        sliderFont,
                                        showClock,
                                        showGregorian
                                    )
                                },
                                valueRange = 0.75f..1.35f,
                                modifier = Modifier.testTag("widget_size_slider")
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("حجم الخط داخل الودجت", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "${(sliderFont * 100f).roundToInt()}%",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            Slider(
                                value = sliderFont,
                                onValueChange = { sliderFont = it },
                                onValueChangeFinished = {
                                    onWidgetAppearanceChange(
                                        selectedWidgetKey,
                                        sliderSize,
                                        sliderFont,
                                        showClock,
                                        showGregorian
                                    )
                                },
                                valueRange = 0.75f..1.45f,
                                modifier = Modifier.testTag("widget_font_slider")
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("إظهار الساعة داخل الودجت", style = MaterialTheme.typography.bodyMedium)
                                Switch(
                                    checked = showClock,
                                    onCheckedChange = { checked ->
                                        showClock = checked
                                        onWidgetAppearanceChange(
                                            selectedWidgetKey,
                                            sliderSize,
                                            sliderFont,
                                            checked,
                                            showGregorian
                                        )
                                    },
                                    modifier = Modifier.testTag("widget_show_clock_switch")
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("إظهار التاريخ الميلادي داخل الودجت", style = MaterialTheme.typography.bodyMedium)
                                Switch(
                                    checked = showGregorian,
                                    onCheckedChange = { checked ->
                                        showGregorian = checked
                                        onWidgetAppearanceChange(
                                            selectedWidgetKey,
                                            sliderSize,
                                            sliderFont,
                                            showClock,
                                            checked
                                        )
                                    },
                                    modifier = Modifier.testTag("widget_show_gregorian_switch")
                                )
                            }
                        }
                    }

                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onForceUpdateWidgets,
                            modifier = Modifier.testTag("force_update_widgets_btn")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("تحديث جميع الودجات الآن")
                        }
                        TextButton(
                            onClick = onResetWidgetAppearances,
                            modifier = Modifier.testTag("reset_widgets_appearance_btn")
                        ) {
                            Text("إعادة تعيين أحجام الودجات")
                        }
                    }
                }
            }

            // 6. Developer Contact via Telegram (@sus_z)
            item {
                DeveloperContactCard(onShowStatusMessage = onShowStatusMessage)
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    if (showMethodDialog) {
        AlertDialog(
            onDismissRequest = { showMethodDialog = false },
            title = { Text("اختر طريقة الحساب") },
            text = {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(CalculationMethodOption.entries) { method ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onCalculationMethodChange(method)
                                    showMethodDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = settings.calculationMethod == method,
                                onClick = {
                                    onCalculationMethodChange(method)
                                    showMethodDialog = false
                                }
                            )
                            Text(method.titleAr, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMethodDialog = false }) { Text("إغلاق") }
            }
        )
    }

    if (showCountryCityDialog) {
        CountryCityPickerDialog(
            globalResults = uiState.globalLocationResults,
            isSearchingGlobal = uiState.isSearchingLocations,
            onSearchGlobal = onSearchGlobalCities,
            onClearGlobalSearch = onClearGlobalSearch,
            onSelectCity = { city, country ->
                onSelectManualCity(city, country)
                showCountryCityDialog = false
            },
            onSelectResolved = { resolved ->
                onSelectResolvedLocation(resolved)
                showCountryCityDialog = false
            },
            onDismiss = {
                onClearGlobalSearch()
                showCountryCityDialog = false
            }
        )
    }
}
