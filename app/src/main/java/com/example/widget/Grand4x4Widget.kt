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
import com.example.data.model.WidgetTypeKey

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
    val key = WidgetTypeKey.GRAND_4X4
    val cfg = snapshot.configFor(key)
    val weather = snapshot.weather
    val nextPrayer = snapshot.schedule.nextPrayer

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(WidgetColors.DeepPanel))
            .background(
                imageProvider = ImageProvider(R.drawable.img_widget_islamic_midnight),
                contentScale = ContentScale.Crop
            )
            .cornerRadius(snapshot.dp(key, 24f))
            .padding(snapshot.dp(key, 14f))
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.Top
    ) {
        // Header: Optional Clock, City, Hijri Date, Optional Gregorian, Actual Temp + Humidity
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
                Text(
                    text = weather?.let { "${it.conditionAr} • رطوبة ${it.humidity}%" } ?: "تحديث الطقس",
                    style = TextStyle(
                        color = ColorProvider(WidgetColors.MutedText),
                        fontSize = snapshot.sp(key, 10f)
                    )
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(snapshot.dp(key, 8f)))

        // Vertical Prayer List (All prayers + Iqamah)
        snapshot.schedule.prayers.forEach { item ->
            val isNext = item.prayer == nextPrayer?.prayer
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(vertical = snapshot.dp(key, 2f), horizontal = snapshot.dp(key, 8f))
                    .background(
                        ColorProvider(
                            if (isNext) WidgetColors.EmeraldBg.copy(alpha = 0.88f)
                            else WidgetColors.MidnightCard.copy(alpha = 0.80f)
                        )
                    )
                    .cornerRadius(snapshot.dp(key, 10f)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.prayer.arabicName,
                    style = TextStyle(
                        color = ColorProvider(
                            if (isNext) WidgetColors.GoldAccent else WidgetColors.CreamText
                        ),
                        fontSize = snapshot.sp(key, 12f),
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = GlanceModifier.defaultWeight())
                if (item.formattedIqamahTime != null) {
                    Text(
                        text = "إقامة ${snapshot.compactTime(item.formattedIqamahTime)} • ",
                        style = TextStyle(
                            color = ColorProvider(WidgetColors.MutedText),
                            fontSize = snapshot.sp(key, 10f)
                        )
                    )
                }
                Text(
                    text = item.formattedAdhanTime,
                    style = TextStyle(
                        color = ColorProvider(
                            if (isNext) WidgetColors.GoldAccent else WidgetColors.CreamText
                        ),
                        fontSize = snapshot.sp(key, 12f),
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(snapshot.dp(key, 6f)))

        // Bottom: Midnight & Last Third of Night
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(ColorProvider(WidgetColors.EmeraldBg.copy(alpha = 0.88f)))
                .cornerRadius(snapshot.dp(key, 12f))
                .padding(horizontal = snapshot.dp(key, 10f), vertical = snapshot.dp(key, 6f)),
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

class Grand4x4WidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = Grand4x4Widget()
}
