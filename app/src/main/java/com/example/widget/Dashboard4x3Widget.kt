package com.example.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.MainActivity
import com.example.R
import com.example.data.model.PrayerName
import com.example.data.model.WidgetTypeKey

/**
 * Medium-Large 4x3 Dashboard Widget:
 * Hijri + Gregorian Dates, Actual Temp + High/Low, All 5 Prayers + Sunrise,
 * Midnight & Last Third of the Night.
 */
class Dashboard4x3Widget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = WidgetDataLoader.load(context)
        provideContent {
            Dashboard4x3Content(snapshot)
        }
    }
}

@Composable
private fun Dashboard4x3Content(snapshot: WidgetSnapshot) {
    val key = WidgetTypeKey.DASHBOARD_4X3
    val cfg = snapshot.configFor(key)
    val nextPrayer = snapshot.schedule.nextPrayer
    val weather = snapshot.weather
    val obligatory = snapshot.schedule.prayers.filter { it.prayer != PrayerName.SUNRISE }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(WidgetColors.DeepPanel))
            .background(
                imageProvider = ImageProvider(R.drawable.img_widget_islamic_midnight),
                contentScale = ContentScale.Crop
            )
            .cornerRadius(snapshot.dp(key, 22f))
            .padding(snapshot.dp(key, 14f))
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Header: Optional Clock + City, Hijri + Optional Gregorian, Actual Temp
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = if (cfg.showClock) "${snapshot.currentTimeText} • ${snapshot.schedule.cityName}"
                    else snapshot.schedule.cityName,
                    style = TextStyle(
                        color = ColorProvider(WidgetColors.GoldAccent),
                        fontSize = snapshot.sp(key, 14f),
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = snapshot.schedule.hijriDate.formattedAr,
                    style = TextStyle(
                        color = ColorProvider(WidgetColors.CreamText),
                        fontSize = snapshot.sp(key, 12f),
                        fontWeight = FontWeight.Medium
                    )
                )
                if (cfg.showGregorianDate) {
                    Text(
                        text = snapshot.schedule.gregorianDateAr,
                        style = TextStyle(
                            color = ColorProvider(WidgetColors.MutedText),
                            fontSize = snapshot.sp(key, 10f)
                        )
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.End,
                modifier = GlanceModifier.clickable(actionRunCallback<RefreshWidgetsActionCallback>())
            ) {
                Text(
                    text = snapshot.actualTempOrDash(),
                    style = TextStyle(
                        color = ColorProvider(WidgetColors.GoldAccent),
                        fontSize = snapshot.sp(key, 22f),
                        fontWeight = FontWeight.Bold
                    )
                )
                if (weather != null) {
                    Text(
                        text = "${weather.conditionAr} (${snapshot.formatTemp(weather.maxTempC)}/${snapshot.formatTemp(weather.minTempC)})",
                        style = TextStyle(
                            color = ColorProvider(WidgetColors.MutedText),
                            fontSize = snapshot.sp(key, 10f)
                        )
                    )
                }
            }
        }

        Spacer(modifier = GlanceModifier.height(snapshot.dp(key, 10f)))

        // Next Prayer Banner
        if (nextPrayer != null) {
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(ColorProvider(WidgetColors.EmeraldBg.copy(alpha = 0.88f)))
                    .cornerRadius(snapshot.dp(key, 12f))
                    .padding(horizontal = snapshot.dp(key, 10f), vertical = snapshot.dp(key, 7f)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "الصلاة القادمة: ${nextPrayer.prayer.arabicName}",
                    style = TextStyle(
                        color = ColorProvider(WidgetColors.GoldAccent),
                        fontSize = snapshot.sp(key, 13f),
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = GlanceModifier.defaultWeight())
                Text(
                    text = "${nextPrayer.formattedAdhanTime} (${snapshot.formatRemainingTime()})",
                    style = TextStyle(
                        color = ColorProvider(WidgetColors.CreamText),
                        fontSize = snapshot.sp(key, 12f)
                    )
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(snapshot.dp(key, 8f)))

        // All 5 Prayers Cards
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            obligatory.forEach { item ->
                val isNext = item.prayer == nextPrayer?.prayer
                Column(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .padding(horizontal = snapshot.dp(key, 2f), vertical = snapshot.dp(key, 6f))
                        .background(
                            ColorProvider(
                                if (isNext) WidgetColors.GoldAccent.copy(alpha = 0.25f)
                                else WidgetColors.MidnightCard.copy(alpha = 0.82f)
                            )
                        )
                        .cornerRadius(snapshot.dp(key, 10f)),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = item.prayer.arabicName,
                        style = TextStyle(
                            color = ColorProvider(
                                if (isNext) WidgetColors.GoldAccent else WidgetColors.MutedText
                            ),
                            fontSize = snapshot.sp(key, 11f),
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(snapshot.dp(key, 2f)))
                    Text(
                        text = snapshot.compactTime(item.formattedAdhanTime),
                        style = TextStyle(
                            color = ColorProvider(WidgetColors.CreamText),
                            fontSize = snapshot.sp(key, 11f),
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }

        Spacer(modifier = GlanceModifier.height(snapshot.dp(key, 8f)))

        // Night Vigil Strip: Midnight & Last Third of the Night
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(ColorProvider(WidgetColors.MidnightCard.copy(alpha = 0.85f)))
                .cornerRadius(snapshot.dp(key, 12f))
                .padding(horizontal = snapshot.dp(key, 10f), vertical = snapshot.dp(key, 7f)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "منتصف الليل: ${snapshot.schedule.formattedMidnightTime}",
                style = TextStyle(
                    color = ColorProvider(WidgetColors.CreamText),
                    fontSize = snapshot.sp(key, 11f),
                    fontWeight = FontWeight.Medium
                )
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            Text(
                text = "الثلث الأخير: ${snapshot.schedule.formattedLastThirdNight}",
                style = TextStyle(
                    color = ColorProvider(WidgetColors.GoldAccent),
                    fontSize = snapshot.sp(key, 11f),
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

class Dashboard4x3WidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = Dashboard4x3Widget()
}
