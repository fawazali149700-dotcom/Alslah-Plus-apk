package com.example.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
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
import com.example.data.model.WidgetTypeKey

/**
 * Compact 2x2 Square Widget showing Next Prayer, Hijri Date, Actual Temperature,
 * Midnight, and Last Third of Night.
 */
class Qiyam2x2Widget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = WidgetDataLoader.load(context)
        provideContent {
            Qiyam2x2Content(snapshot)
        }
    }
}

@Composable
private fun Qiyam2x2Content(snapshot: WidgetSnapshot) {
    val key = WidgetTypeKey.QIYAM_2X2
    val cfg = snapshot.configFor(key)
    val nextPrayer = snapshot.schedule.nextPrayer
    val obligatory = snapshot.schedule.prayers.filter { it.prayer != PrayerName.SUNRISE }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(WidgetColors.MidnightCard))
            .cornerRadius(snapshot.dp(key, 18f))
            .padding(snapshot.dp(key, 10f))
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (cfg.showClock) "${snapshot.compactTime(snapshot.currentTimeText)} • ${snapshot.schedule.hijriDate.shortFormattedAr}"
                else snapshot.schedule.hijriDate.shortFormattedAr,
                style = TextStyle(
                    color = ColorProvider(WidgetColors.GoldAccent),
                    fontSize = snapshot.sp(key, 10f),
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            Text(
                text = snapshot.actualTempOrDash(),
                style = TextStyle(
                    color = ColorProvider(WidgetColors.CreamText),
                    fontSize = snapshot.sp(key, 11f),
                    fontWeight = FontWeight.Bold
                )
            )
        }

        if (cfg.showGregorianDate) {
            Text(
                text = snapshot.schedule.gregorianDateAr,
                style = TextStyle(
                    color = ColorProvider(WidgetColors.MutedText),
                    fontSize = snapshot.sp(key, 9f)
                )
            )
        }

        Spacer(modifier = GlanceModifier.height(snapshot.dp(key, 4f)))

        if (nextPrayer != null) {
            Text(
                text = "القادمة: ${nextPrayer.prayer.arabicName} ${snapshot.compactTime(nextPrayer.formattedAdhanTime)}",
                style = TextStyle(
                    color = ColorProvider(WidgetColors.CreamText),
                    fontSize = snapshot.sp(key, 12f),
                    fontWeight = FontWeight.Bold
                )
            )
        }

        Spacer(modifier = GlanceModifier.height(snapshot.dp(key, 4f)))

        // Compact prayer times list (2 rows)
        val firstRow = obligatory.take(3)
        val secondRow = obligatory.drop(3)
        Text(
            text = firstRow.joinToString(" • ") { "${it.prayer.arabicName} ${snapshot.compactTime(it.formattedAdhanTime)}" },
            style = TextStyle(color = ColorProvider(WidgetColors.MutedText), fontSize = snapshot.sp(key, 9f))
        )
        Text(
            text = secondRow.joinToString(" • ") { "${it.prayer.arabicName} ${snapshot.compactTime(it.formattedAdhanTime)}" },
            style = TextStyle(color = ColorProvider(WidgetColors.MutedText), fontSize = snapshot.sp(key, 9f))
        )

        Spacer(modifier = GlanceModifier.height(snapshot.dp(key, 4f)))

        Text(
            text = "منتصف الليل: ${snapshot.schedule.formattedMidnightTime}",
            style = TextStyle(color = ColorProvider(WidgetColors.CreamText), fontSize = snapshot.sp(key, 10f))
        )
        Text(
            text = "الثلث الأخير: ${snapshot.schedule.formattedLastThirdNight}",
            style = TextStyle(
                color = ColorProvider(WidgetColors.GoldAccent),
                fontSize = snapshot.sp(key, 10f),
                fontWeight = FontWeight.Bold
            )
        )
    }
}

class Qiyam2x2WidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = Qiyam2x2Widget()
}
