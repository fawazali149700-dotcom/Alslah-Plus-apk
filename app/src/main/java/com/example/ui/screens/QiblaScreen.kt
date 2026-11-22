package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.sensor.CompassReading
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun QiblaScreen(
    qiblaDegrees: Double,
    cityName: String,
    compassReading: CompassReading,
    modifier: Modifier = Modifier
) {
    val azimuth = compassReading.azimuthDegrees
    val relativeQibla = ((qiblaDegrees.toFloat() - azimuth) % 360f + 360f) % 360f
    val angularDiff = minOf(relativeQibla, 360f - relativeQibla)
    val isAligned = compassReading.hasSensor && angularDiff <= 5f

    val primaryColor = MaterialTheme.colorScheme.primary
    val goldColor = MaterialTheme.colorScheme.secondary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurface

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 560.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "بوصلة اتجاه القبلة",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "الموقع: $cityName • زاوية القبلة: ${qiblaDegrees.roundToInt()}° من الشمال الحقيقي",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            if (!compassReading.hasSensor) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "لا يوجد مستشعر بوصلة",
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "هذا الجهاز لا يحتوي على مستشعر مغناطيسي (Magnetometer). زاوية القبلة المحسوبة لموقعك هي ${qiblaDegrees.roundToInt()}° شرق الشمال.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Compass Dial Canvas
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(
                    width = if (isAligned) 2.5.dp else 1.dp,
                    color = if (isAligned) primaryColor else goldColor.copy(alpha = 0.45f)
                ),
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .aspectRatio(1f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val radius = size.minDimension / 2f

                        // Outer bezel
                        drawCircle(
                            color = if (isAligned) primaryColor.copy(alpha = 0.12f) else surfaceVariant.copy(alpha = 0.35f),
                            radius = radius,
                            center = center
                        )
                        drawCircle(
                            color = if (isAligned) primaryColor else goldColor,
                            radius = radius,
                            center = center,
                            style = Stroke(width = 4f)
                        )

                        // Rotating compass rose (rotates opposite to phone azimuth so N points North)
                        rotate(degrees = -azimuth, pivot = center) {
                            for (deg in 0 until 360 step 10) {
                                val isMajor = deg % 90 == 0
                                val isMedium = deg % 30 == 0
                                val tickLen = when {
                                    isMajor -> 22f
                                    isMedium -> 14f
                                    else -> 7f
                                }
                                val rad = Math.toRadians((deg - 90).toDouble())
                                val start = Offset(
                                    center.x + ((radius - tickLen) * cos(rad)).toFloat(),
                                    center.y + ((radius - tickLen) * sin(rad)).toFloat()
                                )
                                val end = Offset(
                                    center.x + ((radius - 4f) * cos(rad)).toFloat(),
                                    center.y + ((radius - 4f) * sin(rad)).toFloat()
                                )
                                drawLine(
                                    color = if (deg == 0) Color(0xFFD32F2F) else onSurface.copy(alpha = 0.45f),
                                    start = start,
                                    end = end,
                                    strokeWidth = if (isMajor) 4f else 2f
                                )
                            }
                        }

                        // Rotating Qibla Needle (points to Kaaba relative to phone top)
                        rotate(degrees = relativeQibla, pivot = center) {
                            val needleTip = Offset(center.x, center.y - radius * 0.76f)
                            val needleLeft = Offset(center.x - 18f, center.y)
                            val needleRight = Offset(center.x + 18f, center.y)
                            val needleTail = Offset(center.x, center.y + radius * 0.34f)

                            val qiblaPath = Path().apply {
                                moveTo(needleTip.x, needleTip.y)
                                lineTo(needleRight.x, needleRight.y)
                                lineTo(center.x, center.y - 10f)
                                lineTo(needleLeft.x, needleLeft.y)
                                close()
                            }
                            drawPath(
                                path = qiblaPath,
                                color = if (isAligned) primaryColor else goldColor
                            )

                            val tailPath = Path().apply {
                                moveTo(needleTail.x, needleTail.y)
                                lineTo(needleRight.x, needleRight.y)
                                lineTo(center.x, center.y + 8f)
                                lineTo(needleLeft.x, needleLeft.y)
                                close()
                            }
                            drawPath(
                                path = tailPath,
                                color = onSurface.copy(alpha = 0.25f)
                            )

                            // Kaaba icon badge near needle tip
                            drawCircle(
                                color = Color(0xFF141414),
                                radius = 16f,
                                center = Offset(center.x, center.y - radius * 0.54f)
                            )
                            drawCircle(
                                color = goldColor,
                                radius = 16f,
                                center = Offset(center.x, center.y - radius * 0.54f),
                                style = Stroke(width = 3f)
                            )
                        }

                        // Center pivot
                        drawCircle(
                            color = goldColor,
                            radius = 10f,
                            center = center
                        )
                    }
                }
            }

            // Alignment Status Pill
            Surface(
                color = if (isAligned) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = when {
                        !compassReading.hasSensor -> "زاوية القبلة: ${qiblaDegrees.roundToInt()}°"
                        isAligned -> "أنت الآن باتجاه القبلة الشريفة"
                        else -> "أدر الهاتف ${abs(relativeQibla.let { if (it > 180f) it - 360f else it }).roundToInt()}° نحو السهم الذهبي"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isAligned) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )
            }

            if (compassReading.accuracyLow && compassReading.hasSensor) {
                Text(
                    text = "تنبيه: دقة البوصلة منخفضة، حرّك الهاتف على شكل رقم (8) لمعايرة المستشعر.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
