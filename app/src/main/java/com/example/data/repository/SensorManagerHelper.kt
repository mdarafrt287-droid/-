package com.example.data.repository

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class GyroscopeOffset(
    val roll: Float = 0f,   // X tilt (-1.0 to 1.0)
    val pitch: Float = 0f   // Y tilt (-1.0 to 1.0)
)

class SensorManagerHelper(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val _offset = MutableStateFlow(GyroscopeOffset())
    val offset: StateFlow<GyroscopeOffset> = _offset.asStateFlow()

    private var isListening = false

    fun startListening() {
        if (isListening || sensorManager == null || rotationSensor == null) return
        sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_UI)
        isListening = true
    }

    fun stopListening() {
        if (!isListening || sensorManager == null) return
        sensorManager.unregisterListener(this)
        isListening = false
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            val rotationMatrix = FloatArray(9)
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            val orientationAngles = FloatArray(3)
            SensorManager.getOrientation(rotationMatrix, orientationAngles)

            // azimuth, pitch, roll in radians
            val pitch = orientationAngles[1].coerceIn(-1.0f, 1.0f)
            val roll = orientationAngles[2].coerceIn(-1.0f, 1.0f)

            // Low-pass smooth interpolation
            val current = _offset.value
            _offset.value = GyroscopeOffset(
                roll = current.roll * 0.85f + roll * 0.15f,
                pitch = current.pitch * 0.85f + pitch * 0.15f
            )
        } else if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val ax = (event.values[0] / 9.8f).coerceIn(-1.0f, 1.0f)
            val ay = (event.values[1] / 9.8f).coerceIn(-1.0f, 1.0f)
            val current = _offset.value
            _offset.value = GyroscopeOffset(
                roll = current.roll * 0.85f + (-ax) * 0.15f,
                pitch = current.pitch * 0.85f + (ay - 0.5f) * 0.15f
            )
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
