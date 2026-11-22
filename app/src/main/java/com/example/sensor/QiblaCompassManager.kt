package com.example.sensor

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

data class CompassReading(
    val azimuthDegrees: Float,
    val hasSensor: Boolean,
    val accuracyLow: Boolean
)

class QiblaCompassManager(context: Context) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    fun observeAzimuth(latitude: Double, longitude: Double): Flow<CompassReading> = callbackFlow {
        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        if (rotationSensor == null && (accelSensor == null || magnetSensor == null)) {
            trySend(CompassReading(azimuthDegrees = 0f, hasSensor = false, accuracyLow = false))
            awaitClose { }
            return@callbackFlow
        }

        val geomagneticField = GeomagneticField(
            latitude.toFloat(),
            longitude.toFloat(),
            0f,
            System.currentTimeMillis()
        )
        val declination = geomagneticField.declination

        val gravity = FloatArray(3)
        val geomagnetic = FloatArray(3)
        var hasGravity = false
        var hasMagnetic = false
        var lowAccuracy = false

        var smoothedSin = 0.0
        var smoothedCos = 1.0
        var initialized = false

        fun emitSmoothed(rawDegrees: Float) {
            val trueNorth = ((rawDegrees + declination) % 360f + 360f) % 360f
            val rad = Math.toRadians(trueNorth.toDouble())
            val alpha = 0.18
            if (!initialized) {
                smoothedSin = sin(rad)
                smoothedCos = cos(rad)
                initialized = true
            } else {
                smoothedSin = smoothedSin * (1 - alpha) + sin(rad) * alpha
                smoothedCos = smoothedCos * (1 - alpha) + cos(rad) * alpha
            }
            val smoothedDeg = ((Math.toDegrees(atan2(smoothedSin, smoothedCos)) + 360.0) % 360.0).toFloat()
            trySend(
                CompassReading(
                    azimuthDegrees = smoothedDeg,
                    hasSensor = true,
                    accuracyLow = lowAccuracy
                )
            )
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ROTATION_VECTOR -> {
                        val rotMatrix = FloatArray(9)
                        val orientation = FloatArray(3)
                        SensorManager.getRotationMatrixFromVector(rotMatrix, event.values)
                        SensorManager.getOrientation(rotMatrix, orientation)
                        val azimuthDeg = Math.toDegrees(orientation[0].toDouble()).toFloat()
                        emitSmoothed(azimuthDeg)
                    }
                    Sensor.TYPE_ACCELEROMETER -> {
                        System.arraycopy(event.values, 0, gravity, 0, 3)
                        hasGravity = true
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        System.arraycopy(event.values, 0, geomagnetic, 0, 3)
                        hasMagnetic = true
                    }
                }

                if (rotationSensor == null && hasGravity && hasMagnetic) {
                    val r = FloatArray(9)
                    val i = FloatArray(9)
                    if (SensorManager.getRotationMatrix(r, i, gravity, geomagnetic)) {
                        val orientation = FloatArray(3)
                        SensorManager.getOrientation(r, orientation)
                        val azimuthDeg = Math.toDegrees(orientation[0].toDouble()).toFloat()
                        emitSmoothed(azimuthDeg)
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                if (sensor?.type == Sensor.TYPE_MAGNETIC_FIELD || sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
                    lowAccuracy = accuracy <= SensorManager.SENSOR_STATUS_ACCURACY_LOW
                }
            }
        }

        if (rotationSensor != null) {
            sensorManager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            sensorManager.registerListener(listener, accelSensor, SensorManager.SENSOR_DELAY_UI)
            sensorManager.registerListener(listener, magnetSensor, SensorManager.SENSOR_DELAY_UI)
        }

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }
}
