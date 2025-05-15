package de.quartett.mobile.roadgallery

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.sqrt


class CameraViewModel(sensorManager: SensorManager) : ViewModel() {
    val lastValues = mutableListOf(0.0, 0.0, 0.0, 0.0)
    val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            event?.let {
               // Log.i("sensor", "${it.values.joinToString()}")
                val x = it.values[0]
                val y = it.values[1]
                val z = it.values[2] - 9.81

                val result = sqrt(x * x + y * y + z * z)


                lastValues.add(0, result)
                lastValues.removeAt(lastValues.size-1)
                if (lastValues.sum() > 15.0 * lastValues.size) {
                    Log.i("sensor", "Exceeded $result, ${it.values.joinToString()}")
                    deboucedAccel.value = true
                } else {
                    deboucedAccel.value = false
                }
            }
        }



        override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
            // ignore
        }
    }

    val deboucedAccel = MutableStateFlow<Boolean>(false)

    val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    var camera: Int = CameraSelector.LENS_FACING_FRONT
    val state = MutableStateFlow<Pair<String, CameraSelector>>("initial" to CameraSelector.Builder().requireLensFacing(camera).build())

    fun changeLens(counter: Int){
        camera = if (camera == CameraSelector.LENS_FACING_FRONT){
            Log.i("a", "BACK")
            CameraSelector.LENS_FACING_BACK
        }else{
            Log.i("a", "FRONT")
            CameraSelector.LENS_FACING_FRONT
        }
        state.value = "$counter" to CameraSelector.Builder().requireLensFacing(camera).build()
    }

    init {
        sensorManager.registerListener(sensorListener, sensor, SensorManager.SENSOR_DELAY_UI)
    }
}
