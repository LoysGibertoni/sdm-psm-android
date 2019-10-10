package br.edu.ifsp.sdm.projetopsm

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.collection.LongSparseArray
import androidx.core.app.ActivityCompat
import com.opencsv.CSVWriter
import java.io.File
import java.io.FileWriter


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

    private val REQUEST_EXTERNAL_STORAGE = 1
    private val PERMISSIONS_STORAGE = arrayOf<String>(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

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
                    Log.d(
                        "Dados do acelerômetro",
                        "x: ${event.values[0]}; y: ${event.values[1]}; z: ${event.values[2]}"
                    )
                }
            }
        }
        sensorGiroscopioListener = object : OnSensorChangedListener() {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.run {
                    dadosGiroscopio.put(timestamp, values)
                    Log.d(
                        "Dados do giroscópio",
                        "x: ${event.values[0]}; y: ${event.values[1]}; z: ${event.values[2]}"
                    )
                }
            }
        }
    }

    private fun criarEventosBotoes() {
        val btIniciar = findViewById<Button>(R.id.btIniciar)
        val btParar = findViewById<Button>(R.id.btParar)
        val btnExportar = findViewById<Button>(R.id.btnExportar)

        btnExportar.visibility = View.INVISIBLE

        btIniciar.setOnClickListener {
            iniciarLeitura()
            it.isEnabled = false
            btParar.isEnabled = true
        }

        btParar.setOnClickListener {
            pararLeitura()
            it.isEnabled = false
            btIniciar.isEnabled = true
            btnExportar.visibility = View.VISIBLE
        }

        btnExportar.setOnClickListener {
            exportData("/acelerometro.csv", dadosAcelerometro)
            exportData("/giroscopio.csv", dadosGiroscopio)
            it.visibility = View.INVISIBLE
        }
    }

    private fun exportData(fileName: String, data : LongSparseArray<FloatArray>) {
        try {
            val directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)

            verifyStoragePermissions(this)

            val file = File(directory.toString() + fileName)

            file.createNewFile()

            var fileWriter = FileWriter(directory.toString() + fileName)
            var csvWriter = CSVWriter(fileWriter,
                CSVWriter.DEFAULT_SEPARATOR,
                CSVWriter.NO_QUOTE_CHARACTER,
                CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                CSVWriter.DEFAULT_LINE_END)

            for (i in 0 until data.size()) {
                val key = data.keyAt(i)

                val obj = data.get(key)

                val data = arrayOf<String>(obj?.get(0).toString(), obj?.get(1).toString(), obj?.get(2).toString() )
                csvWriter.writeNext(data)
            }

            csvWriter.close();

            Toast.makeText(getApplicationContext(), R.string.sucesso_exportacao_mensagem, Toast.LENGTH_LONG).show()

        }catch (e : Exception){
            Log.d("ERRO EXPORTACAO", e.message)
            Toast.makeText(getApplicationContext(), R.string.erro_exportacao_mensagem, Toast.LENGTH_LONG).show()
        }
    }

    private fun verifyStoragePermissions(activity: Activity) {

        val permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)

        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                activity,
                PERMISSIONS_STORAGE,
                REQUEST_EXTERNAL_STORAGE
            )
        }
    }
}