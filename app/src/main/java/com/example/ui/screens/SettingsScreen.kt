package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.CalculationMethodOption
import com.example.data.model.CityPreset
import com.example.data.model.CountryEntry
import com.example.data.model.MadhabOption
import com.example.data.model.PrayerName
import com.example.data.model.PrePrayerToneOption
import com.example.data.model.TemperatureUnit
import com.example.data.model.WorldCitiesCatalog
import com.example.location.ResolvedLocation
import com.example.ui.viewmodel.NoorUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    uiState: NoorUiState,
    onCalculationMethodChange: (CalculationMethodOption) -> Unit,
    onMadhabChange: (MadhabOption) -> Unit,
    onAdhanEnabledChange: (Boolean) -> Unit,
    onIqamahEnabledChange: (Boolean) -> Unit,
    onPrayerIqamahOffsetChange: (PrayerName, Int) -> Unit,
    onPrePrayerEnabledChange: (Boolean) -> Unit,
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
    onForceUpdateWidgets: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings = uiState.settings
    var showCountryCityDialog by remember { mutableStateOf(false) }
    var showMethodDialog by remember { mutableStateOf(false) }

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

            // 2. Adhan & Per-Prayer Iqamah Settings
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
                            onCheckedChange = onAdhanEnabledChange
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                "اضبط المدة بين الأذان والإقامة لكل صلاة على حدة",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.iqamahAlertEnabled,
                            onCheckedChange = onIqamahEnabledChange
                        )
                    }

                    if (settings.iqamahAlertEnabled) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            obligatoryPrayers.forEach { prayer ->
                                val currentMins = settings.iqamahOffsets.offsetFor(prayer)
                                PerPrayerIqamahRow(
                                    prayer = prayer,
                                    minutes = currentMins,
                                    onMinutesChange = { newMins ->
                                        onPrayerIqamahOffsetChange(prayer, newMins)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 3. Pre-Prayer Reminder Notification
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
                            onCheckedChange = onPrePrayerEnabledChange
                        )
                    }

                    if (settings.prePrayerReminderEnabled) {
                        Text("مدة التنبيه قبل الأذان", style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(10, 15, 20, 30).forEach { mins ->
                                FilterChip(
                                    selected = settings.prePrayerMinutes == mins,
                                    onClick = { onPrePrayerMinutesChange(mins) },
                                    label = { Text("قبل $mins دقيقة") }
                                )
                            }
                        }

                        Spacer(Modifier.height(6.dp))
                        Text("نغمة تنبيه الاقتراب", style = MaterialTheme.typography.labelLarge)
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

                        OutlinedButton(
                            onClick = onTestPrePrayerNotification,
                            modifier = Modifier.testTag("test_pre_prayer_btn")
                        ) {
                            Text("تجربة إشعار اقتراب الصلاة بالصوت")
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

            // 5. Home Screen Widgets Info
            item {
                SettingsSectionCard(title = "ودجات الشاشة الرئيسية", icon = Icons.Default.Widgets) {
                    Text(
                        text = "يتوفر 8 ودجات بأحجام متنوعة (أقلها 1×4 شريط شامل، و2×2، و2×4، و3×4، و4×4) تعرض أوقات جميع الصلوات والتاريخ الهجري ودرجة الحرارة الفعلية ومنتصف الليل والثلث الأخير، وتتحدث تلقائياً كل 30 دقيقة.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(onClick = onForceUpdateWidgets) {
                        Text("تحديث جميع الودجات الآن")
                    }
                }
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

@Composable
private fun PerPrayerIqamahRow(
    prayer: PrayerName,
    minutes: Int,
    onMinutesChange: (Int) -> Unit
) {
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
                text = "إقامة ${prayer.arabicName}",
                style = MaterialTheme.typography.titleMedium
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onMinutesChange((minutes - 1).coerceAtLeast(0)) },
                    modifier = Modifier.testTag("iqamah_minus_${prayer.name}")
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "إنقاص دقيقة")
                }
                Text(
                    text = "$minutes د",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(
                    onClick = { onMinutesChange((minutes + 1).coerceAtMost(60)) },
                    modifier = Modifier.testTag("iqamah_plus_${prayer.name}")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "زيادة دقيقة")
                }
            }
        }
    }
}

@Composable
private fun CountryCityPickerDialog(
    globalResults: List<ResolvedLocation>,
    isSearchingGlobal: Boolean,
    onSearchGlobal: (String) -> Unit,
    onClearGlobalSearch: () -> Unit,
    onSelectCity: (CityPreset, String) -> Unit,
    onSelectResolved: (ResolvedLocation) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCountry by remember { mutableStateOf<CountryEntry?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredCountries = remember(searchQuery) {
        if (searchQuery.isBlank()) WorldCitiesCatalog.countries
        else WorldCitiesCatalog.countries.filter { country ->
            country.nameAr.contains(searchQuery.trim(), ignoreCase = true) ||
                country.cities.any { it.nameAr.contains(searchQuery.trim(), ignoreCase = true) }
        }
    }

    val filteredCities = remember(selectedCountry, searchQuery) {
        val list = selectedCountry?.cities.orEmpty()
        if (searchQuery.isBlank()) list
        else list.filter { it.nameAr.contains(searchQuery.trim(), ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (selectedCountry != null) {
                    IconButton(onClick = {
                        selectedCountry = null
                        searchQuery = ""
                        onClearGlobalSearch()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "العودة لقائمة الدول"
                        )
                    }
                }
                Text(
                    text = selectedCountry?.let { "مدن ${it.nameAr}" } ?: "اختر الدولة أو ابحث عن أي مدينة",
                    style = MaterialTheme.typography.titleLarge
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { q ->
                        searchQuery = q
                        if (selectedCountry == null && q.trim().length >= 2) {
                            onSearchGlobal(q)
                        } else if (q.isBlank()) {
                            onClearGlobalSearch()
                        }
                    },
                    placeholder = {
                        Text(
                            if (selectedCountry == null) "ابحث عن دولة أو مدينة..."
                            else "ابحث داخل مدن ${selectedCountry?.nameAr}..."
                        )
                    },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (isSearchingGlobal) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.height(18.dp).width(18.dp), strokeWidth = 2.dp)
                        Text("جاري البحث العالمي...", style = MaterialTheme.typography.labelMedium)
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (selectedCountry == null) {
                        if (globalResults.isNotEmpty()) {
                            item {
                                Text(
                                    text = "نتائج البحث العالمي:",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            items(globalResults) { resolved ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelectResolved(resolved) }
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${resolved.cityNameAr}${if (resolved.countryNameAr.isNotBlank()) " — ${resolved.countryNameAr}" else ""}",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                            item { HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp)) }
                        }

                        items(filteredCountries, key = { it.nameAr }) { country ->
                            Surface(
                                onClick = {
                                    selectedCountry = country
                                    searchQuery = ""
                                    onClearGlobalSearch()
                                },
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = country.nameAr,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "${country.cities.size} مدينة",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    } else {
                        val countryName = selectedCountry?.nameAr.orEmpty()
                        items(filteredCities, key = { "${countryName}_${it.nameAr}" }) { city ->
                            Surface(
                                onClick = { onSelectCity(city, countryName) },
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = city.nameAr,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("إغلاق") }
        }
    )
}

@Composable
private fun SettingsSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleLarge)
            }
            content()
        }
    }
}
