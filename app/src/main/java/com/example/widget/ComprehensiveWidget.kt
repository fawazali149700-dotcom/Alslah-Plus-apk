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
    val key = WidgetTypeKey.COMPREHENSIVE_4X2
    val cfg = snapshot.configFor(key)
    val nextPrayer = snapshot.schedule.nextPrayer
    val weather = snapshot.weather
    val obligatory = snapshot.schedule.prayers.filter { it.prayer != PrayerName.SUNRISE }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(WidgetColors.MidnightCard))
            .background(
                imageProvider = ImageProvider(R.drawable.img_widget_islamic_midnight),
                contentScale = ContentScale.Crop
            )
            .cornerRadius(snapshot.dp(key, 20f))
            .padding(snapshot.dp(key, 12f))
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Row 1: Optional Time + City + Hijri Date + Optional Gregorian + Actual Temp
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = if (cfg.showClock) "${snapshot.currentTimeText} • ${snapshot.schedule.cityName}"
                    else snapshot.schedule.cityName,
                    style = TextStyle(
                        color = ColorProvider(WidgetColors.CreamText),
                        fontSize = snapshot.sp(key, 13f),
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "${snapshot.schedule.hijriDate.shortFormattedAr} • ${nextPrayer?.prayer?.arabicName ?: ""} ${snapshot.formatRemainingTime()}",
                    style = TextStyle(
                        color = ColorProvider(WidgetColors.GoldAccent),
                        fontSize = snapshot.sp(key, 11f),
                        fontWeight = FontWeight.Medium
                    )
                )
                if (cfg.showGregorianDate) {
                    Text(
                        text = snapshot.schedule.gregorianDateAr,
                        style = TextStyle(
                            color = ColorProvider(WidgetColors.MutedText),
                            fontSize = snapshot.sp(key, 9.5f)
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
                        fontSize = snapshot.sp(key, 17f),
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = weather?.conditionAr ?: "تحديث",
                    style = TextStyle(
                        color = ColorProvider(WidgetColors.MutedText),
                        fontSize = snapshot.sp(key, 10f)
                    )
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(snapshot.dp(key, 6f)))

        // Row 2: All 5 Prayers Strip
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(ColorProvider(WidgetColors.EmeraldBg.copy(alpha = 0.88f)))
                .cornerRadius(snapshot.dp(key, 12f))
                .padding(horizontal = snapshot.dp(key, 4f), vertical = snapshot.dp(key, 5f)),
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
                            fontSize = snapshot.sp(key, 10f),
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = snapshot.compactTime(item.formattedAdhanTime),
                        style = TextStyle(
                            color = ColorProvider(WidgetColors.CreamText),
                            fontSize = snapshot.sp(key, 10f)
                        )
                    )
                }
            }
        }

        Spacer(modifier = GlanceModifier.height(snapshot.dp(key, 6f)))

        // Row 3: Midnight & Last Third of Night
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "منتصف الليل: ${snapshot.schedule.formattedMidnightTime}",
                style = TextStyle(
                    color = ColorProvider(WidgetColors.MutedText),
                    fontSize = snapshot.sp(key, 10f)
                )
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
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
}

class ComprehensiveWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ComprehensiveWidget()
}
