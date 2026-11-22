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

class WeatherWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = WidgetDataLoader.load(context)
        provideContent {
            WeatherWidgetContent(snapshot)
        }
    }
}

@Composable
private fun WeatherWidgetContent(snapshot: WidgetSnapshot) {
    val weather = snapshot.weather
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(WidgetColors.EmeraldBg))
            .cornerRadius(20.dp)
            .padding(14.dp)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = snapshot.schedule.cityName,
                style = TextStyle(
                    color = ColorProvider(WidgetColors.GoldAccent),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            Text(
                text = "تحديث",
                style = TextStyle(
                    color = ColorProvider(WidgetColors.MutedText),
                    fontSize = 11.sp
                ),
                modifier = GlanceModifier.clickable(actionRunCallback<RefreshWidgetsActionCallback>())
            )
        }
        Spacer(modifier = GlanceModifier.height(6.dp))
        if (weather != null) {
            Text(
                text = snapshot.formatTemp(weather.tempC),
                style = TextStyle(
                    color = ColorProvider(WidgetColors.CreamText),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = "${weather.conditionAr} • العظمى ${snapshot.formatTemp(weather.maxTempC)} / الصغرى ${snapshot.formatTemp(weather.minTempC)}",
                style = TextStyle(
                    color = ColorProvider(WidgetColors.MutedText),
                    fontSize = 11.sp
                )
            )
        } else {
            Text(
                text = "اضغط لتحديث حالة الطقس",
                style = TextStyle(
                    color = ColorProvider(WidgetColors.CreamText),
                    fontSize = 13.sp
                ),
                modifier = GlanceModifier.clickable(actionRunCallback<RefreshWidgetsActionCallback>())
            )
        }
    }
}

class WeatherWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WeatherWidget()
}
