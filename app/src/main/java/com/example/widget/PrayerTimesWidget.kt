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
    val key = WidgetTypeKey.PRAYER_4X2
    val cfg = snapshot.configFor(key)
    val obligatoryPrayers = snapshot.schedule.prayers.filter { it.prayer != PrayerName.SUNRISE }
    val nextPrayer = snapshot.schedule.nextPrayer

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(WidgetColors.EmeraldBg))
            .background(
                imageProvider = ImageProvider(R.drawable.img_widget_islamic_emerald),
                contentScale = ContentScale.Crop
            )
            .cornerRadius(snapshot.dp(key, 20f))
            .padding(snapshot.dp(key, 12f))
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val leftTitle = if (cfg.showClock) {
                "${snapshot.currentTimeText} • ${snapshot.schedule.cityName} • ${snapshot.actualTempOrDash()}"
            } else {
                "${snapshot.schedule.cityName} • ${snapshot.actualTempOrDash()}"
            }
            Text(
                text = leftTitle,
                style = TextStyle(
                    color = ColorProvider(WidgetColors.GoldAccent),
                    fontSize = snapshot.sp(key, 12f),
                    fontWeight = FontWeight.Bold
                ),
                modifier = GlanceModifier.clickable(actionRunCallback<RefreshWidgetsActionCallback>())
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = snapshot.schedule.hijriDate.shortFormattedAr,
                    style = TextStyle(
                        color = ColorProvider(WidgetColors.CreamText),
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
        }

        Spacer(modifier = GlanceModifier.height(snapshot.dp(key, 8f)))

        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            obligatoryPrayers.forEach { item ->
                val isNext = item.prayer == nextPrayer?.prayer
                Column(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .padding(horizontal = snapshot.dp(key, 2f), vertical = snapshot.dp(key, 4f))
                        .background(
                            ColorProvider(
                                if (isNext) WidgetColors.GoldAccent.copy(alpha = 0.25f)
                                else WidgetColors.MidnightCard.copy(alpha = 0.84f)
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
                            fontSize = snapshot.sp(key, 10f),
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }

        Spacer(modifier = GlanceModifier.height(snapshot.dp(key, 6f)))

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

class PrayerTimesWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PrayerTimesWidget()
}
