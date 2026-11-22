package com.example.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
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
import com.example.data.model.PrayerName

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
    val nextPrayer = snapshot.schedule.nextPrayer
    val weather = snapshot.weather
    val obligatory = snapshot.schedule.prayers.filter { it.prayer != PrayerName.SUNRISE }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(WidgetColors.DeepPanel))
            .cornerRadius(22.dp)
            .padding(14.dp)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Header: City, Hijri + Gregorian, Actual Temp
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = snapshot.schedule.cityName,
                    style = TextStyle(
                        color = ColorProvider(WidgetColors.GoldAccent),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = snapshot.schedule.hijriDate.formattedAr,
                    style = TextStyle(
                        color = ColorProvider(WidgetColors.CreamText),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
                Text(
                    text = snapshot.schedule.gregorianDateAr,
                    style = TextStyle(
                        color = ColorProvider(WidgetColors.MutedText),
                        fontSize = 10.sp
                    )
                )
            }
            Column(
                horizontalAlignment = Alignment.End,
                modifier = GlanceModifier.clickable(actionRunCallback<RefreshWidgetsActionCallback>())
            ) {
                Text(
                    text = snapshot.actualTempOrDash(),
                    style = TextStyle(
                        color = ColorProvider(WidgetColors.GoldAccent),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                if (weather != null) {
                    Text(
                        text = "${weather.conditionAr} (${snapshot.formatTemp(weather.maxTempC)}/${snapshot.formatTemp(weather.minTempC)})",
                        style = TextStyle(
                            color = ColorProvider(WidgetColors.MutedText),
                            fontSize = 10.sp
                        )
                    )
                }
            }
        }

        Spacer(modifier = GlanceModifier.height(10.dp))

        // Next Prayer Banner
        if (nextPrayer != null) {
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(ColorProvider(WidgetColors.EmeraldBg))
                    .cornerRadius(12.dp)
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "الصلاة القادمة: ${nextPrayer.prayer.arabicName}",
                    style = TextStyle(
                        color = ColorProvider(WidgetColors.GoldAccent),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = GlanceModifier.defaultWeight())
                Text(
                    text = "${nextPrayer.formattedAdhanTime} (${snapshot.formatRemainingTime()})",
                    style = TextStyle(
                        color = ColorProvider(WidgetColors.CreamText),
                        fontSize = 12.sp
                    )
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(8.dp))

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
                        .padding(horizontal = 2.dp, vertical = 6.dp)
                        .background(
                            ColorProvider(
                                if (isNext) WidgetColors.GoldAccent.copy(alpha = 0.22f)
                                else WidgetColors.MidnightCard
                            )
                        )
                        .cornerRadius(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = item.prayer.arabicName,
                        style = TextStyle(
                            color = ColorProvider(
                                if (isNext) WidgetColors.GoldAccent else WidgetColors.MutedText
                            ),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(2.dp))
                    Text(
                        text = snapshot.compactTime(item.formattedAdhanTime),
                        style = TextStyle(
                            color = ColorProvider(WidgetColors.CreamText),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }

        Spacer(modifier = GlanceModifier.height(8.dp))

        // Night Vigil Strip: Midnight & Last Third of the Night
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(ColorProvider(WidgetColors.MidnightCard))
                .cornerRadius(12.dp)
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "منتصف الليل: ${snapshot.schedule.formattedMidnightTime}",
                style = TextStyle(
                    color = ColorProvider(WidgetColors.CreamText),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            Text(
                text = "الثلث الأخير: ${snapshot.schedule.formattedLastThirdNight}",
                style = TextStyle(
                    color = ColorProvider(WidgetColors.GoldAccent),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

class Dashboard4x3WidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = Dashboard4x3Widget()
}
