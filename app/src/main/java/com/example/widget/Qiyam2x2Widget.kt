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
    val nextPrayer = snapshot.schedule.nextPrayer
    val obligatory = snapshot.schedule.prayers.filter { it.prayer != PrayerName.SUNRISE }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(WidgetColors.MidnightCard))
            .cornerRadius(18.dp)
            .padding(10.dp)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = snapshot.schedule.hijriDate.shortFormattedAr,
                style = TextStyle(
                    color = ColorProvider(WidgetColors.GoldAccent),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            Text(
                text = snapshot.actualTempOrDash(),
                style = TextStyle(
                    color = ColorProvider(WidgetColors.CreamText),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        Spacer(modifier = GlanceModifier.height(4.dp))

        if (nextPrayer != null) {
            Text(
                text = "القادمة: ${nextPrayer.prayer.arabicName} ${snapshot.compactTime(nextPrayer.formattedAdhanTime)}",
                style = TextStyle(
                    color = ColorProvider(WidgetColors.CreamText),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        Spacer(modifier = GlanceModifier.height(4.dp))

        // Compact prayer times list (2 rows)
        val firstRow = obligatory.take(3)
        val secondRow = obligatory.drop(3)
        Text(
            text = firstRow.joinToString(" • ") { "${it.prayer.arabicName} ${snapshot.compactTime(it.formattedAdhanTime)}" },
            style = TextStyle(color = ColorProvider(WidgetColors.MutedText), fontSize = 9.sp)
        )
        Text(
            text = secondRow.joinToString(" • ") { "${it.prayer.arabicName} ${snapshot.compactTime(it.formattedAdhanTime)}" },
            style = TextStyle(color = ColorProvider(WidgetColors.MutedText), fontSize = 9.sp)
        )

        Spacer(modifier = GlanceModifier.height(4.dp))

        Text(
            text = "منتصف الليل: ${snapshot.schedule.formattedMidnightTime}",
            style = TextStyle(color = ColorProvider(WidgetColors.CreamText), fontSize = 10.sp)
        )
        Text(
            text = "الثلث الأخير: ${snapshot.schedule.formattedLastThirdNight}",
            style = TextStyle(
                color = ColorProvider(WidgetColors.GoldAccent),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

class Qiyam2x2WidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = Qiyam2x2Widget()
}
