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
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.collection.LongSparseArray
import androidx.core.app.ActivityCompat
import br.edu.ifsp.sdm.projetopsm.communication.Acao
import br.edu.ifsp.sdm.projetopsm.communication.GerenciadorComunicacao
import com.opencsv.CSVWriter
import kotlinx.android.synthetic.main.activity_main.*
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
    private var timestampInicio = 0L
    private val dadosAcelerometro = LongSparseArray<FloatArray>()
    private val dadosGiroscopio = LongSparseArray<FloatArray>()
    private var gerenciadorComunicacao: GerenciadorComunicacao? = null

    private val REQUEST_EXTERNAL_STORAGE = 1
    private val PERMISSIONS_STORAGE = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        criarSensores()
        criarListeners()
        criarEventosBotoes()
        criarGerenciadorComunicacao()
    }

    private fun iniciarLeitura() {
        btIniciar.isEnabled = false
        btParar.isEnabled = true
        btExportar.isEnabled = false

        timestampInicio = System.currentTimeMillis()
        dadosAcelerometro.clear()
        dadosGiroscopio.clear()

        sensorManager?.registerListener(sensorAcelerometroListener, sensorAcelerometro, DELAY)
        sensorManager?.registerListener(sensorGiroscopioListener, sensorGiroscopio, DELAY)
    }

    private fun pararLeitura() {
        btIniciar.isEnabled = true
        btParar.isEnabled = false
        btExportar.isEnabled = true

        sensorManager?.unregisterListener(sensorAcelerometroListener)
        sensorManager?.unregisterListener(sensorGiroscopioListener)
    }

    private fun processarAcao(acao: Acao) {
        if (acao == Acao.INICIAR) {
            iniciarLeitura()
        } else if (acao == Acao.PARAR) {
            pararLeitura()
        }
    }

    private fun criarSensores() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorAcelerometro = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorGiroscopio = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    }

    private fun criarListeners() {
        sensorAcelerometroListener = object : OnSensorChangedListener() {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.values?.copyOf()?.let {
                    dadosAcelerometro.put(System.currentTimeMillis() - timestampInicio, it)
                    Log.d(
                        "Dados do acelerômetro",
                        "x: ${it[0]}; y: ${it[1]}; z: ${it[2]}"
                    )
                }
            }
        }
        sensorGiroscopioListener = object : OnSensorChangedListener() {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.values?.copyOf()?.let {
                    dadosGiroscopio.put(System.currentTimeMillis() - timestampInicio, it)
                    Log.d(
                        "Dados do giroscópio",
                        "x: ${it[0]}; y: ${it[1]}; z: ${it[2]}"
                    )
                }
            }
        }
    }

    private fun criarEventosBotoes() {
        btIniciar.setOnClickListener {
            if (gerenciadorComunicacao?.enviarAcao(Acao.INICIAR) == false) {
                iniciarLeitura()
            }
        }

        btParar.setOnClickListener {
            if (gerenciadorComunicacao?.enviarAcao(Acao.PARAR) == false) {
                pararLeitura()
            }
        }

        btExportar.setOnClickListener {
            exportData("/acelerometro.csv", dadosAcelerometro)
            exportData("/giroscopio.csv", dadosGiroscopio)
            it.isEnabled = false
        }
    }

    private fun criarGerenciadorComunicacao() {
        if (gerenciadorComunicacao == null) {
            gerenciadorComunicacao = GerenciadorComunicacao(this)
                .apply { callback = this@MainActivity::processarAcao }
        }
        gerenciadorComunicacao?.let { lifecycle.addObserver(it) }
    }

    private fun exportData(fileName: String, data: LongSparseArray<FloatArray>) {
        try {
            val directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)

            verifyStoragePermissions(this)

            val file = File(directory.toString() + fileName)

            directory.mkdirs()
            file.createNewFile()

            val fileWriter = FileWriter(directory.toString() + fileName)
            val csvWriter = CSVWriter(fileWriter,
                CSVWriter.DEFAULT_SEPARATOR,
                CSVWriter.NO_QUOTE_CHARACTER,
                CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                CSVWriter.DEFAULT_LINE_END)

            for (i in 0 until data.size()) {
                val key = data.keyAt(i)
                val obj = data.get(key)
                val line = arrayOf(key.toString(), obj?.get(0).toString(), obj?.get(1).toString(), obj?.get(2).toString())
                csvWriter.writeNext(line)
            }

            csvWriter.close()

            Toast.makeText(applicationContext, R.string.sucesso_exportacao_mensagem, Toast.LENGTH_LONG).show()

        }catch (e : Exception){
            Log.d("ERRO EXPORTACAO", e.message)
            Toast.makeText(applicationContext, R.string.erro_exportacao_mensagem, Toast.LENGTH_LONG).show()
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