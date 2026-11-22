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

/**
 * Full 4x4 Grand Widget displaying complete Adhan & Iqamah schedule, Sunrise,
 * Hijri & Gregorian dates, detailed actual weather, Midnight, and Last Third of Night.
 */
class Grand4x4Widget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = WidgetDataLoader.load(context)
        provideContent {
            Grand4x4Content(snapshot)
        }
    }
}

@Composable
private fun Grand4x4Content(snapshot: WidgetSnapshot) {
    val weather = snapshot.weather
    val nextPrayer = snapshot.schedule.nextPrayer

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(WidgetColors.DeepPanel))
            .cornerRadius(24.dp)
            .padding(14.dp)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.Top
    ) {
        // Header: Time, City, Hijri Date, Actual Temp + Humidity
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = "${snapshot.currentTimeText} • ${snapshot.schedule.cityName}",
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
                Text(
                    text = weather?.let { "${it.conditionAr} • رطوبة ${it.humidity}%" } ?: "تحديث الطقس",
                    style = TextStyle(
                        color = ColorProvider(WidgetColors.MutedText),
                        fontSize = 10.sp
                    )
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(8.dp))

        // Vertical Prayer List (All prayers + Iqamah)
        snapshot.schedule.prayers.forEach { item ->
            val isNext = item.prayer == nextPrayer?.prayer
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp, horizontal = 8.dp)
                    .background(
                        ColorProvider(
                            if (isNext) WidgetColors.EmeraldBg else WidgetColors.MidnightCard
                        )
                    )
                    .cornerRadius(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.prayer.arabicName,
                    style = TextStyle(
                        color = ColorProvider(
                            if (isNext) WidgetColors.GoldAccent else WidgetColors.CreamText
                        ),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = GlanceModifier.defaultWeight())
                if (item.formattedIqamahTime != null) {
                    Text(
                        text = "إقامة ${snapshot.compactTime(item.formattedIqamahTime)} • ",
                        style = TextStyle(
                            color = ColorProvider(WidgetColors.MutedText),
                            fontSize = 10.sp
                        )
                    )
                }
                Text(
                    text = item.formattedAdhanTime,
                    style = TextStyle(
                        color = ColorProvider(
                            if (isNext) WidgetColors.GoldAccent else WidgetColors.CreamText
                        ),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(6.dp))

        // Bottom: Midnight & Last Third of Night
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(ColorProvider(WidgetColors.EmeraldBg))
                .cornerRadius(12.dp)
                .padding(horizontal = 10.dp, vertical = 6.dp),
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

class Grand4x4WidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = Grand4x4Widget()
}
