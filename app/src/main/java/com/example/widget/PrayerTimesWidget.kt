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

class PrayerTimesWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = WidgetDataLoader.load(context)
        provideContent {
            PrayerTimesWidgetContent(snapshot)
        }
    }
}

@Composable
private fun PrayerTimesWidgetContent(snapshot: WidgetSnapshot) {
    val obligatoryPrayers = snapshot.schedule.prayers.filter { it.prayer != PrayerName.SUNRISE }
    val nextPrayer = snapshot.schedule.nextPrayer

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(WidgetColors.EmeraldBg))
            .cornerRadius(20.dp)
            .padding(12.dp)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${snapshot.schedule.cityName} • ${snapshot.actualTempOrDash()}",
                style = TextStyle(
                    color = ColorProvider(WidgetColors.GoldAccent),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = GlanceModifier.clickable(actionRunCallback<RefreshWidgetsActionCallback>())
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            Text(
                text = snapshot.schedule.hijriDate.shortFormattedAr,
                style = TextStyle(
                    color = ColorProvider(WidgetColors.CreamText),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }

        Spacer(modifier = GlanceModifier.height(8.dp))

        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            obligatoryPrayers.forEach { item ->
                val isNext = item.prayer == nextPrayer?.prayer
                Column(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .padding(horizontal = 2.dp, vertical = 4.dp)
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
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }

        Spacer(modifier = GlanceModifier.height(6.dp))

        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "منتصف الليل: ${snapshot.schedule.formattedMidnightTime}",
                style = TextStyle(
                    color = ColorProvider(WidgetColors.MutedText),
                    fontSize = 10.sp
                )
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
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
}

class PrayerTimesWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PrayerTimesWidget()
}
