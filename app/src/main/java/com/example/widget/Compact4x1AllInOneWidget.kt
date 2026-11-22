package com.example.widget

import android.content.Context
import androidx.compose.runtime.Composable
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
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.MainActivity
import com.example.data.model.PrayerName
import com.example.data.model.WidgetTypeKey

/**
 * Compact 4x1 (1x4) Strip Widget containing ALL prayers and individual rounded boxes for
 * Hijri (+ optional Gregorian) date, actual temp, Midnight, Last Third, and optional Clock.
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
    val key = WidgetTypeKey.COMPACT_4X1
    val cfg = snapshot.configFor(key)
    val obligatory = snapshot.schedule.prayers.filter { it.prayer != PrayerName.SUNRISE }
    val nextPrayer = snapshot.schedule.nextPrayer

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(WidgetColors.EmeraldBg))
            .cornerRadius(snapshot.dp(key, 16f))
            .padding(horizontal = snapshot.dp(key, 6f), vertical = snapshot.dp(key, 5f))
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Top Strip: Each item inside its own small rounded rectangle
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (cfg.showClock) {
                Column(
                    modifier = GlanceModifier
                        .background(ColorProvider(WidgetColors.MidnightCard))
                        .cornerRadius(snapshot.dp(key, 7f))
                        .padding(horizontal = snapshot.dp(key, 5f), vertical = snapshot.dp(key, 3f)),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = snapshot.compactTime(snapshot.currentTimeText),
                        style = TextStyle(
                            color = ColorProvider(WidgetColors.CreamText),
                            fontSize = snapshot.sp(key, 9f),
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                Spacer(modifier = GlanceModifier.width(snapshot.dp(key, 3f)))
            }

            // Date mini-rectangle (Hijri + optional Gregorian)
            Column(
                modifier = GlanceModifier
                    .defaultWeight()
                    .background(ColorProvider(WidgetColors.MidnightCard))
                    .cornerRadius(snapshot.dp(key, 7f))
                    .padding(horizontal = snapshot.dp(key, 5f), vertical = snapshot.dp(key, 3f)),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = snapshot.schedule.hijriDate.shortFormattedAr,
                    style = TextStyle(
                        color = ColorProvider(WidgetColors.GoldAccent),
                        fontSize = snapshot.sp(key, 8.5f),
                        fontWeight = FontWeight.Bold
                    )
                )
                if (cfg.showGregorianDate) {
                    Text(
                        text = snapshot.schedule.gregorianDateAr,
                        style = TextStyle(
                            color = ColorProvider(WidgetColors.MutedText),
                            fontSize = snapshot.sp(key, 7.5f)
                        )
                    )
                }
            }

            Spacer(modifier = GlanceModifier.width(snapshot.dp(key, 3f)))

            // Temperature mini-rectangle
            Column(
                modifier = GlanceModifier
                    .background(ColorProvider(WidgetColors.MidnightCard))
                    .cornerRadius(snapshot.dp(key, 7f))
                    .padding(horizontal = snapshot.dp(key, 5f), vertical = snapshot.dp(key, 3f))
                    .clickable(actionRunCallback<RefreshWidgetsActionCallback>()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = snapshot.actualTempOrDash(),
                    style = TextStyle(
                        color = ColorProvider(WidgetColors.GoldAccent),
                        fontSize = snapshot.sp(key, 9f),
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = GlanceModifier.width(snapshot.dp(key, 3f)))

            // Midnight mini-rectangle
            Column(
                modifier = GlanceModifier
                    .background(ColorProvider(WidgetColors.MidnightCard))
                    .cornerRadius(snapshot.dp(key, 7f))
                    .padding(horizontal = snapshot.dp(key, 5f), vertical = snapshot.dp(key, 3f)),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "منتصف ${snapshot.compactTime(snapshot.schedule.formattedMidnightTime)}",
                    style = TextStyle(
                        color = ColorProvider(WidgetColors.CreamText),
                        fontSize = snapshot.sp(key, 8.5f),
                        fontWeight = FontWeight.Medium
                    )
                )
            }

            Spacer(modifier = GlanceModifier.width(snapshot.dp(key, 3f)))

            // Last Third of Night mini-rectangle
            Column(
                modifier = GlanceModifier
                    .background(ColorProvider(WidgetColors.MidnightCard))
                    .cornerRadius(snapshot.dp(key, 7f))
                    .padding(horizontal = snapshot.dp(key, 5f), vertical = snapshot.dp(key, 3f)),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "ثلث ${snapshot.compactTime(snapshot.schedule.formattedLastThirdNight)}",
                    style = TextStyle(
                        color = ColorProvider(WidgetColors.GoldAccent),
                        fontSize = snapshot.sp(key, 8.5f),
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(snapshot.dp(key, 4f)))

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
                        .padding(horizontal = snapshot.dp(key, 1f), vertical = snapshot.dp(key, 2f))
                        .background(
                            ColorProvider(
                                if (isNext) WidgetColors.GoldAccent.copy(alpha = 0.25f)
                                else WidgetColors.MidnightCard
                            )
                        )
                        .cornerRadius(snapshot.dp(key, 8f)),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = item.prayer.arabicName,
                        style = TextStyle(
                            color = ColorProvider(
                                if (isNext) WidgetColors.GoldAccent else WidgetColors.MutedText
                            ),
                            fontSize = snapshot.sp(key, 9f),
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = snapshot.compactTime(item.formattedAdhanTime),
                        style = TextStyle(
                            color = ColorProvider(WidgetColors.CreamText),
                            fontSize = snapshot.sp(key, 9f),
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
