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

class ComprehensiveWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = WidgetDataLoader.load(context)
        provideContent {
            ComprehensiveWidgetContent(snapshot)
        }
    }
}

@Composable
private fun ComprehensiveWidgetContent(snapshot: WidgetSnapshot) {
    val nextPrayer = snapshot.schedule.nextPrayer
    val weather = snapshot.weather
    val obligatory = snapshot.schedule.prayers.filter { it.prayer != PrayerName.SUNRISE }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(WidgetColors.MidnightCard))
            .cornerRadius(20.dp)
            .padding(12.dp)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Row 1: Time + City + Hijri Date + Actual Temp
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = "${snapshot.currentTimeText} • ${snapshot.schedule.cityName}",
                    style = TextStyle(
                        color = ColorProvider(WidgetColors.CreamText),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "${snapshot.schedule.hijriDate.shortFormattedAr} • ${nextPrayer?.prayer?.arabicName ?: ""} ${snapshot.formatRemainingTime()}",
                    style = TextStyle(
                        color = ColorProvider(WidgetColors.GoldAccent),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
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
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = weather?.conditionAr ?: "تحديث",
                    style = TextStyle(
                        color = ColorProvider(WidgetColors.MutedText),
                        fontSize = 10.sp
                    )
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(6.dp))

        // Row 2: All 5 Prayers Strip
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(ColorProvider(WidgetColors.EmeraldBg))
                .cornerRadius(12.dp)
                .padding(horizontal = 4.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            obligatory.forEach { item ->
                val isNext = item.prayer == nextPrayer?.prayer
                Column(
                    modifier = GlanceModifier.defaultWeight(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = item.prayer.arabicName,
                        style = TextStyle(
                            color = ColorProvider(
                                if (isNext) WidgetColors.GoldAccent else WidgetColors.MutedText
                            ),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = snapshot.compactTime(item.formattedAdhanTime),
                        style = TextStyle(
                            color = ColorProvider(WidgetColors.CreamText),
                            fontSize = 10.sp
                        )
                    )
                }
            }
        }

        Spacer(modifier = GlanceModifier.height(6.dp))

        // Row 3: Midnight & Last Third of Night
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

class ComprehensiveWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ComprehensiveWidget()
}
