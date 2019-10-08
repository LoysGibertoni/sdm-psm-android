package br.edu.ifsp.sdm.projetopsm

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.collection.LongSparseArray

class MainActivity : AppCompatActivity() {

    companion object {
        const val DELAY = SensorManager.SENSOR_DELAY_NORMAL
    }

    private var sensorManager: SensorManager? = null
    private var sensorAcelerometro: Sensor? = null
    private var sensorGiroscopio: Sensor? = null
    private var sensorAcelerometroListener: OnSensorChangedListener? = null
    private var sensorGiroscopioListener: OnSensorChangedListener? = null
    private val dadosAcelerometro = LongSparseArray<FloatArray>()
    private val dadosGiroscopio = LongSparseArray<FloatArray>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        criarSensores()
        criarListeners()
        criarEventosBotoes()
    }

    private fun iniciarLeitura() {
        sensorManager?.registerListener(sensorAcelerometroListener, sensorAcelerometro, DELAY)
        sensorManager?.registerListener(sensorGiroscopioListener, sensorGiroscopio, DELAY)
    }

    private fun pararLeitura() {
        sensorManager?.unregisterListener(sensorAcelerometroListener)
        sensorManager?.unregisterListener(sensorGiroscopioListener)
    }

    private fun criarSensores() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorAcelerometro = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorGiroscopio = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    }

    private fun criarListeners() {
        sensorAcelerometroListener = object : OnSensorChangedListener() {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.run {
                    dadosAcelerometro.put(timestamp, values)
                    Log.d("Dados do acelerômetro", "x: ${event.values[0]}; y: ${event.values[1]}; z: ${event.values[2]}")
                }
            }
        }
        sensorGiroscopioListener = object : OnSensorChangedListener() {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.run {
                    dadosGiroscopio.put(timestamp, values)
                    Log.d("Dados do giroscópio", "x: ${event.values[0]}; y: ${event.values[1]}; z: ${event.values[2]}")
                }
            }
        }
    }

    private fun criarEventosBotoes() {
        val btIniciar = findViewById<Button>(R.id.btIniciar)
        val btParar = findViewById<Button>(R.id.btParar)
        btIniciar.setOnClickListener {
            iniciarLeitura()
            it.isEnabled = false
            btParar.isEnabled = true
        }
        btParar.setOnClickListener {
            pararLeitura()
            it.isEnabled = false
            btIniciar.isEnabled = true
        }
    }
}