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
 * Compact 4x1 (1x4) Strip Widget containing ALL prayers, Hijri date, actual temp,
 * Midnight time, and Last Third of the Night.
 */
class Compact4x1AllInOneWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = WidgetDataLoader.load(context)
        provideContent {
            Compact4x1Content(snapshot)
        }
    }
}

@Composable
private fun Compact4x1Content(snapshot: WidgetSnapshot) {
    val obligatory = snapshot.schedule.prayers.filter { it.prayer != PrayerName.SUNRISE }
    val nextPrayer = snapshot.schedule.nextPrayer

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(WidgetColors.EmeraldBg))
            .cornerRadius(16.dp)
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Top Compact Strip: Hijri Date + Actual Temp + Midnight + Last Third
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${snapshot.schedule.hijriDate.shortFormattedAr} • ${snapshot.actualTempOrDash()}",
                style = TextStyle(
                    color = ColorProvider(WidgetColors.GoldAccent),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = GlanceModifier.clickable(actionRunCallback<RefreshWidgetsActionCallback>())
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            Text(
                text = "منتصف ${snapshot.compactTime(snapshot.schedule.formattedMidnightTime)} | ثلث ${snapshot.compactTime(snapshot.schedule.formattedLastThirdNight)}",
                style = TextStyle(
                    color = ColorProvider(WidgetColors.CreamText),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }

        Spacer(modifier = GlanceModifier.height(4.dp))

        // Bottom Compact Strip: All 5 Prayer Times
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            obligatory.forEach { item ->
                val isNext = item.prayer == nextPrayer?.prayer
                Column(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .padding(horizontal = 1.dp, vertical = 2.dp)
                        .background(
                            ColorProvider(
                                if (isNext) WidgetColors.GoldAccent.copy(alpha = 0.25f)
                                else WidgetColors.MidnightCard
                            )
                        )
                        .cornerRadius(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = item.prayer.arabicName,
                        style = TextStyle(
                            color = ColorProvider(
                                if (isNext) WidgetColors.GoldAccent else WidgetColors.MutedText
                            ),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = snapshot.compactTime(item.formattedAdhanTime),
                        style = TextStyle(
                            color = ColorProvider(WidgetColors.CreamText),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
}

class Compact4x1AllInOneWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = Compact4x1AllInOneWidget()
}
