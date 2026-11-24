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
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.MainActivity
import com.example.data.model.WidgetTypeKey

class HijriDateWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = WidgetDataLoader.load(context)
        provideContent {
            HijriDateWidgetContent(snapshot)
        }
    }
}

@Composable
private fun HijriDateWidgetContent(snapshot: WidgetSnapshot) {
    val key = WidgetTypeKey.HIJRI_3X1
    val cfg = snapshot.configFor(key)
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(WidgetColors.MidnightCard))
            .cornerRadius(snapshot.dp(key, 20f))
            .padding(snapshot.dp(key, 14f))
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (cfg.showClock) "${snapshot.currentTimeText} • تحديث"
            else "التقويم الهجري (تحديث)",
            style = TextStyle(
                color = ColorProvider(WidgetColors.GoldAccent),
                fontSize = snapshot.sp(key, 11f),
                fontWeight = FontWeight.Medium
            ),
            modifier = GlanceModifier.clickable(actionRunCallback<RefreshWidgetsActionCallback>())
        )
        Spacer(modifier = GlanceModifier.height(snapshot.dp(key, 4f)))
        Text(
            text = snapshot.schedule.hijriDate.formattedAr,
            style = TextStyle(
                color = ColorProvider(WidgetColors.CreamText),
                fontSize = snapshot.sp(key, 15f),
                fontWeight = FontWeight.Bold
            )
        )
        if (cfg.showGregorianDate) {
            Spacer(modifier = GlanceModifier.height(snapshot.dp(key, 4f)))
            Text(
                text = snapshot.schedule.gregorianDateAr,
                style = TextStyle(
                    color = ColorProvider(WidgetColors.MutedText),
                    fontSize = snapshot.sp(key, 12f)
                )
            )
        }
    }
}

class HijriDateWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HijriDateWidget()
}
