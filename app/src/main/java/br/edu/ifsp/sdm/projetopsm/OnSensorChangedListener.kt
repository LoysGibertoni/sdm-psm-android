package br.edu.ifsp.sdm.projetopsm

import android.hardware.Sensor
import android.hardware.SensorEventListener

abstract class OnSensorChangedListener : SensorEventListener {

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Ignorado
    }
}