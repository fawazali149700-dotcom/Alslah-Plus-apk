package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.CityPreset
import com.example.data.model.CountryEntry
import com.example.data.model.CustomAudioAttachment
import com.example.data.model.PrayerName
import com.example.data.model.WorldCitiesCatalog
import com.example.location.ResolvedLocation

@Composable
fun SettingsSectionCard(
    title: String,
    icon: ImageVector,
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PerPrayerAdhanIqamahCard(
    prayer: PrayerName,
    globalAdhanEnabled: Boolean,
    globalIqamahEnabled: Boolean,
    prayerAdhanEnabled: Boolean,
    prayerIqamahEnabled: Boolean,
    iqamahMinutes: Int,
    onAdhanToggle: (Boolean) -> Unit,
    onIqamahToggle: (Boolean) -> Unit,
    onIqamahMinutesChange: (Int) -> Unit,
    iqamahAudioAttachment: CustomAudioAttachment? = null,
    onPickIqamahAudio: () -> Unit = {},
    onClearIqamahAudio: () -> Unit = {},
    onTestIqamahAudio: () -> Unit = {}
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "صلاة ${prayer.arabicName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "الأذان",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(4.dp))
                        Switch(
                            checked = prayerAdhanEnabled,
                            onCheckedChange = onAdhanToggle,
                            enabled = globalAdhanEnabled,
                            modifier = Modifier.testTag("prayer_adhan_switch_${prayer.name}")
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "الإقامة",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(4.dp))
                        Switch(
                            checked = prayerIqamahEnabled,
                            onCheckedChange = onIqamahToggle,
                            enabled = globalIqamahEnabled,
                            modifier = Modifier.testTag("prayer_iqamah_switch_${prayer.name}")
                        )
                    }
                }
            }

            if (globalIqamahEnabled && prayerIqamahEnabled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "الفاصل بين الأذان والإقامة",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { onIqamahMinutesChange((iqamahMinutes - 1).coerceAtLeast(0)) },
                            modifier = Modifier.testTag("iqamah_minus_${prayer.name}")
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "إنقاص دقيقة")
                        }
                        Text(
                            text = "$iqamahMinutes د",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(
                            onClick = { onIqamahMinutesChange((iqamahMinutes + 1).coerceAtMost(60)) },
                            modifier = Modifier.testTag("iqamah_plus_${prayer.name}")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "زيادة دقيقة")
                        }
                    }
                }

                Text(
                    text = iqamahAudioAttachment?.displayName?.let { "صوت الإقامة: $it" }
                        ?: "صوت الإقامة: حسب الوضع العام المختار",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (!iqamahAudioAttachment?.path.isNullOrBlank()) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = onPickIqamahAudio,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("pick_prayer_iqamah_audio_${prayer.name}")
                    ) {
                        Icon(Icons.Default.AudioFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("صوت إقامة من الذاكرة")
                    }

                    OutlinedButton(
                        onClick = onTestIqamahAudio,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("test_prayer_iqamah_audio_${prayer.name}")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("تجربة")
                    }

                    if (!iqamahAudioAttachment?.path.isNullOrBlank()) {
                        TextButton(
                            onClick = onClearIqamahAudio,
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("clear_prayer_iqamah_audio_${prayer.name}")
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("حذف")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PerPrayerPreReminderCard(
    prayer: PrayerName,
    globalEnabled: Boolean,
    prayerEnabled: Boolean,
    attachment: CustomAudioAttachment?,
    onTogglePrayer: (Boolean) -> Unit,
    onPickAudio: () -> Unit,
    onClearAudio: () -> Unit,
    onTestAudio: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "تنبيه اقتراب صلاة ${prayer.arabicName}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = attachment?.displayName?.let { "الصوت: $it" }
                            ?: "الصوت: الافتراضي (نطق صوتي / نغمة النظام)",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (attachment?.path != null) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
                Switch(
                    checked = prayerEnabled,
                    onCheckedChange = onTogglePrayer,
                    enabled = globalEnabled,
                    modifier = Modifier.testTag("prayer_pre_switch_${prayer.name}")
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = onPickAudio,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("pick_prayer_pre_audio_${prayer.name}")
                ) {
                    Icon(Icons.Default.AudioFile, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("اختيار من الذاكرة")
                }

                OutlinedButton(
                    onClick = onTestAudio,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("test_prayer_pre_audio_${prayer.name}")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("تجربة")
                }

                if (!attachment?.path.isNullOrBlank()) {
                    TextButton(
                        onClick = onClearAudio,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("clear_prayer_pre_audio_${prayer.name}")
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("حذف")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DeveloperContactCard(
    onShowStatusMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val telegramHandle = "@sus_z"
    val telegramUrl = "https://t.me/sus_z"

    SettingsSectionCard(
        title = "تواصل مع المطور",
        icon = Icons.Default.SupportAgent
    ) {
        Text(
            text = "للاقتراحات أو الإبلاغ عن أي ملاحظة، يمكنك التواصل مباشرة مع مطور تطبيق نور عبر تليجرام:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "يوزر التليجرام",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = telegramHandle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    val launched = runCatching {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(telegramUrl)).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    }.isSuccess
                    if (!launched) {
                        clipboardManager.setText(AnnotatedString(telegramHandle))
                        onShowStatusMessage("تم نسخ المعرف $telegramHandle لعدم توفر متصفح أو تليجرام")
                    }
                },
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("contact_telegram_btn")
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("مراسلة المطور ($telegramHandle)")
            }

            OutlinedButton(
                onClick = {
                    clipboardManager.setText(AnnotatedString(telegramHandle))
                    onShowStatusMessage("تم نسخ معرف المطور $telegramHandle")
                },
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("copy_telegram_btn")
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("نسخ اليوزر")
            }
        }
    }
}

@Composable
fun CountryCityPickerDialog(
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
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
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
