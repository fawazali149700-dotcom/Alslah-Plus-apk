package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.PrayerItem
import com.example.data.model.PrayerName
import com.example.data.model.TemperatureUnit
import com.example.data.model.WeatherData
import com.example.data.repository.HijriCalendarHelper
import com.example.ui.viewmodel.NoorUiState
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    uiState: NoorUiState,
    onRefreshGps: () -> Unit,
    onRefreshWeather: () -> Unit,
    onStopAdhan: () -> Unit,
    onOpenMuezzins: () -> Unit,
    onOpenWidgetSize: () -> Unit = {},
    onForceUpdateWidgets: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showDateConverter by remember { mutableStateOf(false) }
    val schedule = uiState.schedule

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 620.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Live Adhan Playback Banner
            item {
                AnimatedVisibility(visible = uiState.isAdhanServicePlaying) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = "الأذان قيد التشغيل",
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = "يرفع الآن الأذان...",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            Button(
                                onClick = onStopAdhan,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                ),
                                modifier = Modifier.testTag("stop_adhan_btn")
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("إيقاف")
                            }
                        }
                    }
                }
            }

            // Hero Header Card (Next Prayer + Hijri & Gregorian Dates + Location)
            if (schedule != null) {
                item {
                    HeroPrayerCard(
                        cityName = schedule.cityName,
                        hijriFormatted = schedule.hijriDate.formattedAr,
                        gregorianFormatted = schedule.gregorianDateAr,
                        nextPrayer = schedule.nextPrayer,
                        remainingMillis = schedule.remainingMillis,
                        isLocationLoading = uiState.isLocationLoading,
                        onRefreshLocation = onRefreshGps,
                        onOpenDateConverter = { showDateConverter = true }
                    )
                }
            }

            // Weather Summary Card
            item {
                WeatherSummaryCard(
                    weather = uiState.weather,
                    unit = uiState.settings.temperatureUnit,
                    isLoading = uiState.isWeatherLoading,
                    error = uiState.weatherError,
                    onRefresh = onRefreshWeather
                )
            }

            // Prayer Times Section Title + Quick Shortcuts
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "مواقيت الصلاة اليوم",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = onForceUpdateWidgets,
                            modifier = Modifier.testTag("home_refresh_widgets_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "تحديث الودجات",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(2.dp))
                            Text(
                                text = "تحديث الودجات",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        TextButton(
                            onClick = onOpenWidgetSize,
                            modifier = Modifier.testTag("home_widget_size_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Widgets,
                                contentDescription = "التحكم بحجم الودجات",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(2.dp))
                            Text(
                                text = "الودجات",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }

            // Prayer List Cards
            if (schedule != null) {
                items(schedule.prayers, key = { it.prayer.name }) { item ->
                    PrayerRowCard(
                        item = item,
                        iqamahEnabled = uiState.settings.iqamahAlertEnabled
                    )
                }

                // Qiyam al-Layl Card: Midnight & Last Third of the Night
                item {
                    QiyamNightTimesCard(
                        midnightTime = schedule.formattedMidnightTime,
                        lastThirdTime = schedule.formattedLastThirdNight
                    )
                }
            }

            item { Spacer(Modifier.height(12.dp)) }
        }
    }

    if (showDateConverter) {
        HijriConverterDialog(onDismiss = { showDateConverter = false })
    }
}

@Composable
private fun HeroPrayerCard(
    cityName: String,
    hijriFormatted: String,
    gregorianFormatted: String,
    nextPrayer: PrayerItem?,
    remainingMillis: Long,
    isLocationLoading: Boolean,
    onRefreshLocation: () -> Unit,
    onOpenDateConverter: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(26.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.img_islamic_hero_banner),
                contentDescription = "خلفية المسجد والهلال",
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xCC0B3B2E),
                                Color(0xBB0D1B2A)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Top Row: City & Date Converter Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = onRefreshLocation,
                        color = Color.White.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.testTag("refresh_location_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isLocationLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFFE7C55D)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "تحديث الموقع",
                                    tint = Color(0xFFE7C55D),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = cityName,
                                style = MaterialTheme.typography.labelLarge,
                                color = Color(0xFFFAF7F0)
                            )
                        }
                    }

                    Surface(
                        onClick = onOpenDateConverter,
                        color = Color(0xFFD4AF37).copy(alpha = 0.22f),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.testTag("date_converter_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = "تحويل التاريخ الهجري والميلادي",
                                tint = Color(0xFFE7C55D),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "محوّل التاريخ",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFFE7C55D)
                            )
                        }
                    }
                }

                // Hijri & Gregorian Date Banner
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = Color(0xFFE7C55D),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = hijriFormatted,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFFFAF7F0)
                        )
                    }
                    Text(
                        text = gregorianFormatted,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFD0DDD6)
                    )
                }

                // Next Prayer Countdown Highlight
                if (nextPrayer != null) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.28f),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, Color(0xFFD4AF37).copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "الصلاة القادمة",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFFE7C55D)
                                )
                                Text(
                                    text = nextPrayer.prayer.arabicName,
                                    style = MaterialTheme.typography.displaySmall,
                                    color = Color.White
                                )
                                Text(
                                    text = "الأذان: ${nextPrayer.formattedAdhanTime}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFD0DDD6)
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "الوقت المتبقي",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFD0DDD6)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = formatCountdown(remainingMillis),
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = Color(0xFFE7C55D),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeatherSummaryCard(
    weather: WeatherData?,
    unit: TemperatureUnit,
    isLoading: Boolean,
    error: String?,
    onRefresh: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.WbSunny,
                        contentDescription = "حالة الطقس",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "حالة الطقس اليوم",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.testTag("refresh_weather_btn")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "تحديث الطقس"
                        )
                    }
                }
            }

            if (weather != null) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = formatTemp(weather.tempC, unit),
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = weather.conditionAr,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "العظمى ${formatTemp(weather.maxTempC, unit)}  •  الصغرى ${formatTemp(weather.minTempC, unit)}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.WaterDrop,
                                    contentDescription = "الرطوبة",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "${weather.humidity}%",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Air,
                                    contentDescription = "سرعة الرياح",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "${weather.windSpeedKmh} كم/س",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
            } else {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = error ?: "جاري تحميل بيانات الطقس المباشرة...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun QiyamNightTimesCard(
    midnightTime: String,
    lastThirdTime: String
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("qiyam_night_times_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.NightsStay,
                    contentDescription = "أوقات قيام الليل",
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "أوقات الليل وقيام الليل",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "منتصف الليل الشرعي",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = midnightTime,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "بداية الثلث الأخير",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = lastThirdTime,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PrayerRowCard(
    item: PrayerItem,
    iqamahEnabled: Boolean
) {
    val containerColor = if (item.isNext) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    val borderStroke = if (item.isNext) {
        BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary)
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    }

    val icon = when (item.prayer) {
        PrayerName.FAJR, PrayerName.ISHA -> Icons.Outlined.NightsStay
        PrayerName.SUNRISE, PrayerName.DHUHR -> Icons.Default.WbSunny
        else -> Icons.Outlined.Cloud
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = borderStroke,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (item.isNext) MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = item.prayer.arabicName,
                        tint = if (item.isNext) MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.prayer.arabicName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (item.isNext) {
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.secondary,
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = "القادمة",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    if (iqamahEnabled && item.formattedIqamahTime != null) {
                        Text(
                            text = "الإقامة: ${item.formattedIqamahTime}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = item.formattedAdhanTime,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (item.isNext) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun HijriConverterDialog(onDismiss: () -> Unit) {
    val todayCal = remember { Calendar.getInstance() }
    val todayHijri = remember { HijriCalendarHelper.todayHijri() }

    var modeToHijri by remember { mutableStateOf(true) }
    var gYear by remember { mutableStateOf(todayCal.get(Calendar.YEAR).toString()) }
    var gMonth by remember { mutableStateOf((todayCal.get(Calendar.MONTH) + 1).toString()) }
    var gDay by remember { mutableStateOf(todayCal.get(Calendar.DAY_OF_MONTH).toString()) }

    var hYear by remember { mutableStateOf(todayHijri.year.toString()) }
    var hMonth by remember { mutableIntStateOf(todayHijri.month) }
    var hDay by remember { mutableStateOf(todayHijri.day.toString()) }

    val resultText = remember(modeToHijri, gYear, gMonth, gDay, hYear, hMonth, hDay) {
        if (modeToHijri) {
            val y = gYear.toIntOrNull()
            val m = gMonth.toIntOrNull()
            val d = gDay.toIntOrNull()
            if (y != null && m in 1..12 && d in 1..31) {
                runCatching {
                    HijriCalendarHelper.gregorianToHijri(y, m!!, d!!).formattedAr
                }.getOrDefault("تاريخ غير صالح")
            } else {
                "أدخل تاريخاً ميلادياً صحيحاً"
            }
        } else {
            val y = hYear.toIntOrNull()
            val d = hDay.toIntOrNull()
            if (y != null && hMonth in 1..12 && d in 1..30) {
                runCatching {
                    val (gy, gm, gd) = HijriCalendarHelper.hijriToGregorian(y, hMonth, d!!)
                    HijriCalendarHelper.formatGregorianTripleAr(gy, gm, gd)
                }.getOrDefault("تاريخ هجري غير صالح")
            } else {
                "أدخل تاريخاً هجرياً صحيحاً"
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "محوّل التاريخ الهجري والميلادي",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = modeToHijri,
                        onClick = { modeToHijri = true },
                        label = { Text("ميلادي ← هجري") }
                    )
                    FilterChip(
                        selected = !modeToHijri,
                        onClick = { modeToHijri = false },
                        label = { Text("هجري ← ميلادي") }
                    )
                }

                if (modeToHijri) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = gDay,
                            onValueChange = { gDay = it.filter { c -> c.isDigit() }.take(2) },
                            label = { Text("اليوم") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = gMonth,
                            onValueChange = { gMonth = it.filter { c -> c.isDigit() }.take(2) },
                            label = { Text("الشهر") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = gYear,
                            onValueChange = { gYear = it.filter { c -> c.isDigit() }.take(4) },
                            label = { Text("السنة") },
                            modifier = Modifier.weight(1.3f),
                            singleLine = true
                        )
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = hDay,
                            onValueChange = { hDay = it.filter { c -> c.isDigit() }.take(2) },
                            label = { Text("اليوم") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = hMonth.toString(),
                            onValueChange = {
                                val parsed = it.filter { c -> c.isDigit() }.take(2).toIntOrNull()
                                if (parsed != null && parsed in 1..12) hMonth = parsed
                            },
                            label = { Text("الشهر (1-12)") },
                            modifier = Modifier.weight(1.1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = hYear,
                            onValueChange = { hYear = it.filter { c -> c.isDigit() }.take(4) },
                            label = { Text("السنة هـ") },
                            modifier = Modifier.weight(1.3f),
                            singleLine = true
                        )
                    }
                    Text(
                        text = "الشهر المختار: ${HijriCalendarHelper.hijriMonthsAr.getOrElse(hMonth - 1) { "" }}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = resultText,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        },
        confirmButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )
}

private fun formatTemp(tempC: Double, unit: TemperatureUnit): String {
    val value = if (unit == TemperatureUnit.FAHRENHEIT) (tempC * 9.0 / 5.0) + 32.0 else tempC
    return "${value.roundToInt()}${unit.symbol}"
}

private fun formatCountdown(millis: Long): String {
    val totalSeconds = (millis / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
}
