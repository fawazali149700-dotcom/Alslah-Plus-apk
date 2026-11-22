package com.example.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.DailyPrayerSchedule
import com.example.data.model.HijriDateModel
import com.example.data.model.PrayerItem
import com.example.data.model.PrayerName
import com.example.data.model.TemperatureUnit
import com.example.data.model.WeatherData
import com.example.data.model.WidgetTypeKey
import com.example.widget.WidgetColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun WidgetLivePreview(
    widgetKey: WidgetTypeKey,
    sizeScale: Float,
    fontScale: Float,
    showClock: Boolean,
    showGregorianDate: Boolean,
    schedule: DailyPrayerSchedule?,
    weather: WeatherData?,
    tempUnit: TemperatureUnit,
    modifier: Modifier = Modifier
) {
    val activeSchedule = remember(schedule) { schedule ?: samplePreviewSchedule() }
    val currentTimeText = remember {
        SimpleDateFormat("hh:mm a", Locale("ar")).format(Date())
    }

    fun fSp(base: Float): TextUnit = (base * fontScale).sp
    fun fDp(base: Float): Dp = (base * sizeScale).dp

    fun formatTemp(tempC: Double): String {
        val value = if (tempUnit == TemperatureUnit.FAHRENHEIT) (tempC * 9.0 / 5.0) + 32.0 else tempC
        return "${value.roundToInt()}${tempUnit.symbol}"
    }

    val actualTempOrDash = weather?.let { formatTemp(it.tempC) } ?: "26${tempUnit.symbol}"

    fun compactTime(formatted: String): String =
        formatted.replace(" ص", "").replace(" م", "").trim()

    fun remainingText(): String {
        val totalMin = (activeSchedule.remainingMillis / 60_000L).coerceAtLeast(1L)
        val hours = totalMin / 60L
        val mins = totalMin % 60L
        return if (hours > 0) "بعد $hours س $mins د" else "بعد $mins د"
    }

    val obligatory = remember(activeSchedule) {
        activeSchedule.prayers.filter { it.prayer != PrayerName.SUNRISE }
    }
    val nextPrayer = activeSchedule.nextPrayer
    val widthFraction = (0.66f + ((sizeScale - 0.65f) / 0.80f) * 0.34f).coerceIn(0.66f, 1.0f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF131D24))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.28f), RoundedCornerShape(18.dp))
            .padding(12.dp)
            .testTag("widget_live_preview_container"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "معاينة مباشرة: ${widgetKey.titleAr}",
                style = MaterialTheme.typography.labelLarge,
                color = WidgetColors.GoldAccent
            )
            Text(
                text = "خط ${(fontScale * 100f).roundToInt()}% • حجم ${(sizeScale * 100f).roundToInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = WidgetColors.MutedText
            )
        }

        when (widgetKey) {
            WidgetTypeKey.COMPACT_4X1 -> {
                PreviewWidgetShell(
                    backgroundColor = WidgetColors.EmeraldBg,
                    cornerRadius = fDp(16f),
                    paddingH = fDp(6f),
                    paddingV = fDp(5f),
                    widthFraction = widthFraction
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (showClock) {
                            PreviewMiniBox(corner = fDp(7f), padH = fDp(5f), padV = fDp(3f)) {
                                Text(
                                    text = compactTime(currentTimeText),
                                    color = WidgetColors.CreamText,
                                    fontSize = fSp(9f),
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                            Spacer(Modifier.width(fDp(3f)))
                        }

                        PreviewMiniBox(
                            corner = fDp(7f),
                            padH = fDp(5f),
                            padV = fDp(3f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = activeSchedule.hijriDate.shortFormattedAr,
                                color = WidgetColors.GoldAccent,
                                fontSize = fSp(8.5f),
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (showGregorianDate) {
                                Text(
                                    text = activeSchedule.gregorianDateAr,
                                    color = WidgetColors.MutedText,
                                    fontSize = fSp(7.5f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(Modifier.width(fDp(3f)))

                        PreviewMiniBox(corner = fDp(7f), padH = fDp(5f), padV = fDp(3f)) {
                            Text(
                                text = actualTempOrDash,
                                color = WidgetColors.GoldAccent,
                                fontSize = fSp(9f),
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }

                        Spacer(Modifier.width(fDp(3f)))

                        PreviewMiniBox(corner = fDp(7f), padH = fDp(5f), padV = fDp(3f)) {
                            Text(
                                text = "منتصف ${compactTime(activeSchedule.formattedMidnightTime)}",
                                color = WidgetColors.CreamText,
                                fontSize = fSp(8.5f),
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                        }

                        Spacer(Modifier.width(fDp(3f)))

                        PreviewMiniBox(corner = fDp(7f), padH = fDp(5f), padV = fDp(3f)) {
                            Text(
                                text = "ثلث ${compactTime(activeSchedule.formattedLastThirdNight)}",
                                color = WidgetColors.GoldAccent,
                                fontSize = fSp(8.5f),
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(Modifier.height(fDp(4f)))
                    PreviewPrayerRow(
                        prayers = obligatory,
                        nextPrayer = nextPrayer,
                        nameFontSize = fSp(9f),
                        timeFontSize = fSp(9f),
                        cellCorner = fDp(8f),
                        cellPadV = fDp(2f),
                        compactTime = ::compactTime
                    )
                }
            }

            WidgetTypeKey.PRAYER_4X2 -> {
                PreviewWidgetShell(
                    backgroundColor = WidgetColors.EmeraldBg,
                    backgroundDrawableRes = R.drawable.img_widget_islamic_emerald,
                    cornerRadius = fDp(20f),
                    paddingH = fDp(12f),
                    paddingV = fDp(12f),
                    widthFraction = widthFraction
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (showClock) "$currentTimeText • ${activeSchedule.cityName} • $actualTempOrDash"
                            else "${activeSchedule.cityName} • $actualTempOrDash",
                            color = WidgetColors.GoldAccent,
                            fontSize = fSp(12f),
                            fontWeight = FontWeight.Bold
                        )
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = activeSchedule.hijriDate.shortFormattedAr,
                                color = WidgetColors.CreamText,
                                fontSize = fSp(11f),
                                fontWeight = FontWeight.Medium
                            )
                            if (showGregorianDate) {
                                Text(
                                    text = activeSchedule.gregorianDateAr,
                                    color = WidgetColors.MutedText,
                                    fontSize = fSp(9.5f)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(fDp(8f)))
                    PreviewPrayerRow(
                        prayers = obligatory,
                        nextPrayer = nextPrayer,
                        nameFontSize = fSp(11f),
                        timeFontSize = fSp(10f),
                        cellCorner = fDp(10f),
                        cellPadV = fDp(4f),
                        compactTime = ::compactTime
                    )
                    Spacer(Modifier.height(fDp(6f)))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "منتصف الليل: ${activeSchedule.formattedMidnightTime}",
                            color = WidgetColors.MutedText,
                            fontSize = fSp(10f)
                        )
                        Text(
                            text = "الثلث الأخير: ${activeSchedule.formattedLastThirdNight}",
                            color = WidgetColors.GoldAccent,
                            fontSize = fSp(10f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            WidgetTypeKey.COMPREHENSIVE_4X2 -> {
                PreviewWidgetShell(
                    backgroundColor = WidgetColors.MidnightCard,
                    backgroundDrawableRes = R.drawable.img_widget_islamic_midnight,
                    cornerRadius = fDp(20f),
                    paddingH = fDp(12f),
                    paddingV = fDp(12f),
                    widthFraction = widthFraction
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (showClock) "$currentTimeText • ${activeSchedule.cityName}"
                                else activeSchedule.cityName,
                                color = WidgetColors.CreamText,
                                fontSize = fSp(13f),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${activeSchedule.hijriDate.shortFormattedAr} • ${nextPrayer?.prayer?.arabicName ?: ""} ${remainingText()}",
                                color = WidgetColors.GoldAccent,
                                fontSize = fSp(11f),
                                fontWeight = FontWeight.Medium
                            )
                            if (showGregorianDate) {
                                Text(
                                    text = activeSchedule.gregorianDateAr,
                                    color = WidgetColors.MutedText,
                                    fontSize = fSp(9.5f)
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = actualTempOrDash,
                                color = WidgetColors.GoldAccent,
                                fontSize = fSp(17f),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = weather?.conditionAr ?: "صحو",
                                color = WidgetColors.MutedText,
                                fontSize = fSp(10f)
                            )
                        }
                    }
                    Spacer(Modifier.height(fDp(6f)))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(fDp(12f)))
                            .background(WidgetColors.EmeraldBg.copy(alpha = 0.88f))
                            .padding(horizontal = fDp(4f), vertical = fDp(5f)),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        obligatory.forEach { item ->
                            val isNext = item.prayer == nextPrayer?.prayer
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = item.prayer.arabicName,
                                    color = if (isNext) WidgetColors.GoldAccent else WidgetColors.MutedText,
                                    fontSize = fSp(10f),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = compactTime(item.formattedAdhanTime),
                                    color = WidgetColors.CreamText,
                                    fontSize = fSp(10f)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(fDp(6f)))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "منتصف الليل: ${activeSchedule.formattedMidnightTime}",
                            color = WidgetColors.MutedText,
                            fontSize = fSp(10f)
                        )
                        Text(
                            text = "الثلث الأخير: ${activeSchedule.formattedLastThirdNight}",
                            color = WidgetColors.GoldAccent,
                            fontSize = fSp(10f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            WidgetTypeKey.DASHBOARD_4X3 -> {
                PreviewWidgetShell(
                    backgroundColor = WidgetColors.DeepPanel,
                    backgroundDrawableRes = R.drawable.img_widget_islamic_midnight,
                    cornerRadius = fDp(22f),
                    paddingH = fDp(14f),
                    paddingV = fDp(14f),
                    widthFraction = widthFraction
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (showClock) "$currentTimeText • ${activeSchedule.cityName}"
                                else activeSchedule.cityName,
                                color = WidgetColors.GoldAccent,
                                fontSize = fSp(14f),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = activeSchedule.hijriDate.formattedAr,
                                color = WidgetColors.CreamText,
                                fontSize = fSp(12f),
                                fontWeight = FontWeight.Medium
                            )
                            if (showGregorianDate) {
                                Text(
                                    text = activeSchedule.gregorianDateAr,
                                    color = WidgetColors.MutedText,
                                    fontSize = fSp(10f)
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = actualTempOrDash,
                                color = WidgetColors.GoldAccent,
                                fontSize = fSp(22f),
                                fontWeight = FontWeight.Bold
                            )
                            if (weather != null) {
                                Text(
                                    text = "${weather.conditionAr} (${formatTemp(weather.maxTempC)}/${formatTemp(weather.minTempC)})",
                                    color = WidgetColors.MutedText,
                                    fontSize = fSp(10f)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(fDp(10f)))
                    if (nextPrayer != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(fDp(12f)))
                                .background(WidgetColors.EmeraldBg.copy(alpha = 0.90f))
                                .padding(horizontal = fDp(10f), vertical = fDp(7f)),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "الصلاة القادمة: ${nextPrayer.prayer.arabicName}",
                                color = WidgetColors.GoldAccent,
                                fontSize = fSp(13f),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${nextPrayer.formattedAdhanTime} (${remainingText()})",
                                color = WidgetColors.CreamText,
                                fontSize = fSp(12f)
                            )
                        }
                    }
                    Spacer(Modifier.height(fDp(8f)))
                    PreviewPrayerRow(
                        prayers = obligatory,
                        nextPrayer = nextPrayer,
                        nameFontSize = fSp(11f),
                        timeFontSize = fSp(11f),
                        cellCorner = fDp(10f),
                        cellPadV = fDp(6f),
                        compactTime = ::compactTime
                    )
                    Spacer(Modifier.height(fDp(8f)))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(fDp(12f)))
                            .background(WidgetColors.MidnightCard.copy(alpha = 0.88f))
                            .padding(horizontal = fDp(10f), vertical = fDp(7f)),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "منتصف الليل: ${activeSchedule.formattedMidnightTime}",
                            color = WidgetColors.CreamText,
                            fontSize = fSp(11f),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "الثلث الأخير: ${activeSchedule.formattedLastThirdNight}",
                            color = WidgetColors.GoldAccent,
                            fontSize = fSp(11f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            WidgetTypeKey.GRAND_4X4 -> {
                PreviewWidgetShell(
                    backgroundColor = WidgetColors.DeepPanel,
                    backgroundDrawableRes = R.drawable.img_widget_islamic_midnight,
                    cornerRadius = fDp(24f),
                    paddingH = fDp(14f),
                    paddingV = fDp(14f),
                    widthFraction = widthFraction
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (showClock) "$currentTimeText • ${activeSchedule.cityName}"
                                else activeSchedule.cityName,
                                color = WidgetColors.GoldAccent,
                                fontSize = fSp(14f),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = activeSchedule.hijriDate.formattedAr,
                                color = WidgetColors.CreamText,
                                fontSize = fSp(12f),
                                fontWeight = FontWeight.Medium
                            )
                            if (showGregorianDate) {
                                Text(
                                    text = activeSchedule.gregorianDateAr,
                                    color = WidgetColors.MutedText,
                                    fontSize = fSp(10f)
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = actualTempOrDash,
                                color = WidgetColors.GoldAccent,
                                fontSize = fSp(22f),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = weather?.let { "${it.conditionAr} • رطوبة ${it.humidity}%" } ?: "صحو • رطوبة 35%",
                                color = WidgetColors.MutedText,
                                fontSize = fSp(10f)
                            )
                        }
                    }
                    Spacer(Modifier.height(fDp(8f)))
                    Column(verticalArrangement = Arrangement.spacedBy(fDp(3f))) {
                        activeSchedule.prayers.forEach { item ->
                            val isNext = item.prayer == nextPrayer?.prayer
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(fDp(10f)))
                                    .background(
                                        if (isNext) WidgetColors.EmeraldBg.copy(alpha = 0.90f)
                                        else WidgetColors.MidnightCard.copy(alpha = 0.85f)
                                    )
                                    .padding(vertical = fDp(3f), horizontal = fDp(8f)),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.prayer.arabicName,
                                    color = if (isNext) WidgetColors.GoldAccent else WidgetColors.CreamText,
                                    fontSize = fSp(12f),
                                    fontWeight = FontWeight.Bold
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (item.formattedIqamahTime != null) {
                                        Text(
                                            text = "إقامة ${compactTime(item.formattedIqamahTime)} • ",
                                            color = WidgetColors.MutedText,
                                            fontSize = fSp(10f)
                                        )
                                    }
                                    Text(
                                        text = item.formattedAdhanTime,
                                        color = if (isNext) WidgetColors.GoldAccent else WidgetColors.CreamText,
                                        fontSize = fSp(12f),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(fDp(6f)))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(fDp(12f)))
                            .background(WidgetColors.EmeraldBg.copy(alpha = 0.90f))
                            .padding(horizontal = fDp(10f), vertical = fDp(6f)),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "منتصف الليل: ${activeSchedule.formattedMidnightTime}",
                            color = WidgetColors.CreamText,
                            fontSize = fSp(11f),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "الثلث الأخير: ${activeSchedule.formattedLastThirdNight}",
                            color = WidgetColors.GoldAccent,
                            fontSize = fSp(11f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            WidgetTypeKey.QIYAM_2X2 -> {
                PreviewWidgetShell(
                    backgroundColor = WidgetColors.MidnightCard,
                    cornerRadius = fDp(18f),
                    paddingH = fDp(10f),
                    paddingV = fDp(10f),
                    widthFraction = widthFraction,
                    modifier = Modifier.widthIn(max = (220f * sizeScale).dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (showClock) "${compactTime(currentTimeText)} • ${activeSchedule.hijriDate.shortFormattedAr}"
                            else activeSchedule.hijriDate.shortFormattedAr,
                            color = WidgetColors.GoldAccent,
                            fontSize = fSp(10f),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = actualTempOrDash,
                            color = WidgetColors.CreamText,
                            fontSize = fSp(11f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (showGregorianDate) {
                        Text(
                            text = activeSchedule.gregorianDateAr,
                            color = WidgetColors.MutedText,
                            fontSize = fSp(9f)
                        )
                    }
                    Spacer(Modifier.height(fDp(4f)))
                    if (nextPrayer != null) {
                        Text(
                            text = "القادمة: ${nextPrayer.prayer.arabicName} ${compactTime(nextPrayer.formattedAdhanTime)}",
                            color = WidgetColors.CreamText,
                            fontSize = fSp(12f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(fDp(4f)))
                    val firstRow = obligatory.take(3)
                    val secondRow = obligatory.drop(3)
                    Text(
                        text = firstRow.joinToString(" • ") { "${it.prayer.arabicName} ${compactTime(it.formattedAdhanTime)}" },
                        color = WidgetColors.MutedText,
                        fontSize = fSp(9f)
                    )
                    Text(
                        text = secondRow.joinToString(" • ") { "${it.prayer.arabicName} ${compactTime(it.formattedAdhanTime)}" },
                        color = WidgetColors.MutedText,
                        fontSize = fSp(9f)
                    )
                    Spacer(Modifier.height(fDp(4f)))
                    Text(
                        text = "منتصف الليل: ${activeSchedule.formattedMidnightTime}",
                        color = WidgetColors.CreamText,
                        fontSize = fSp(10f)
                    )
                    Text(
                        text = "الثلث الأخير: ${activeSchedule.formattedLastThirdNight}",
                        color = WidgetColors.GoldAccent,
                        fontSize = fSp(10f),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            WidgetTypeKey.HIJRI_3X1 -> {
                PreviewWidgetShell(
                    backgroundColor = WidgetColors.MidnightCard,
                    cornerRadius = fDp(20f),
                    paddingH = fDp(14f),
                    paddingV = fDp(14f),
                    widthFraction = widthFraction,
                    modifier = Modifier.widthIn(max = (300f * sizeScale).dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (showClock) "$currentTimeText • التقويم الهجري"
                            else "التقويم الهجري (أم القرى)",
                            color = WidgetColors.GoldAccent,
                            fontSize = fSp(11f),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(fDp(4f)))
                        Text(
                            text = activeSchedule.hijriDate.formattedAr,
                            color = WidgetColors.CreamText,
                            fontSize = fSp(15f),
                            fontWeight = FontWeight.Bold
                        )
                        if (showGregorianDate) {
                            Spacer(Modifier.height(fDp(4f)))
                            Text(
                                text = activeSchedule.gregorianDateAr,
                                color = WidgetColors.MutedText,
                                fontSize = fSp(12f)
                            )
                        }
                    }
                }
            }

            WidgetTypeKey.WEATHER_3X2 -> {
                PreviewWidgetShell(
                    backgroundColor = WidgetColors.EmeraldBg,
                    cornerRadius = fDp(20f),
                    paddingH = fDp(14f),
                    paddingV = fDp(14f),
                    widthFraction = widthFraction,
                    modifier = Modifier.widthIn(max = (300f * sizeScale).dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (showClock) "$currentTimeText • ${activeSchedule.cityName}"
                            else activeSchedule.cityName,
                            color = WidgetColors.GoldAccent,
                            fontSize = fSp(13f),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "تحديث",
                            color = WidgetColors.MutedText,
                            fontSize = fSp(11f)
                        )
                    }
                    if (showGregorianDate) {
                        Text(
                            text = activeSchedule.gregorianDateAr,
                            color = WidgetColors.MutedText,
                            fontSize = fSp(10f)
                        )
                    }
                    Spacer(Modifier.height(fDp(6f)))
                    Text(
                        text = actualTempOrDash,
                        color = WidgetColors.CreamText,
                        fontSize = fSp(26f),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (weather != null) {
                            "${weather.conditionAr} • العظمى ${formatTemp(weather.maxTempC)} / الصغرى ${formatTemp(weather.minTempC)}"
                        } else {
                            "صحو • العظمى 30${tempUnit.symbol} / الصغرى 20${tempUnit.symbol}"
                        },
                        color = WidgetColors.MutedText,
                        fontSize = fSp(11f)
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewMiniBox(
    corner: Dp,
    padH: Dp,
    padV: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(corner))
            .background(WidgetColors.MidnightCard)
            .padding(horizontal = padH, vertical = padV),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        content()
    }
}

@Composable
private fun PreviewWidgetShell(
    backgroundColor: Color,
    @DrawableRes backgroundDrawableRes: Int? = null,
    cornerRadius: Dp,
    paddingH: Dp,
    paddingV: Dp,
    widthFraction: Float = 1f,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .fillMaxWidth(widthFraction)
            .clip(shape)
            .background(backgroundColor)
    ) {
        if (backgroundDrawableRes != null) {
            Image(
                painter = painterResource(id = backgroundDrawableRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
                alpha = 0.72f
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = paddingH, vertical = paddingV)
        ) {
            content()
        }
    }
}

@Composable
private fun PreviewPrayerRow(
    prayers: List<PrayerItem>,
    nextPrayer: PrayerItem?,
    nameFontSize: TextUnit,
    timeFontSize: TextUnit,
    cellCorner: Dp,
    cellPadV: Dp,
    compactTime: (String) -> String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        prayers.forEach { item ->
            val isNext = item.prayer == nextPrayer?.prayer
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(cellCorner))
                    .background(
                        if (isNext) WidgetColors.GoldAccent.copy(alpha = 0.24f)
                        else WidgetColors.MidnightCard.copy(alpha = 0.85f)
                    )
                    .padding(horizontal = 2.dp, vertical = cellPadV),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = item.prayer.arabicName,
                    color = if (isNext) WidgetColors.GoldAccent else WidgetColors.MutedText,
                    fontSize = nameFontSize,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = compactTime(item.formattedAdhanTime),
                    color = WidgetColors.CreamText,
                    fontSize = timeFontSize,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun samplePreviewSchedule(): DailyPrayerSchedule {
    val now = System.currentTimeMillis()
    val prayers = listOf(
        PrayerItem(PrayerName.FAJR, now, now, 20, "04:45 ص", "05:05 ص", false, true),
        PrayerItem(PrayerName.SUNRISE, now, null, 0, "06:10 ص", null, false, true),
        PrayerItem(PrayerName.DHUHR, now, now, 10, "12:15 م", "12:25 م", true, false),
        PrayerItem(PrayerName.ASR, now, now, 10, "03:35 م", "03:45 م", false, false),
        PrayerItem(PrayerName.MAGHRIB, now, now, 5, "06:02 م", "06:07 م", false, false),
        PrayerItem(PrayerName.ISHA, now, now, 10, "07:32 م", "07:42 م", false, false)
    )
    return DailyPrayerSchedule(
        prayers = prayers,
        nextPrayer = prayers[2],
        remainingMillis = 45 * 60_000L,
        qiblaDegrees = 152.0,
        hijriDate = HijriDateModel(12, 6, 1448, "جمادى الآخرة", "الأحد"),
        gregorianDateAr = "22 نوفمبر 2026",
        cityName = "مكة المكرمة",
        countryName = "المملكة العربية السعودية",
        midnightTimeMillis = now,
        formattedMidnightTime = "11:25 م",
        lastThirdNightMillis = now,
        formattedLastThirdNight = "01:12 ص"
    )
}
