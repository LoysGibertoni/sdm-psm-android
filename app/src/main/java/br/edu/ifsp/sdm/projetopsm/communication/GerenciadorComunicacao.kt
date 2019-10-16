package br.edu.ifsp.sdm.projetopsm.communication

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Handler
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import br.edu.ifsp.sdm.projetopsm.getBroadcastAddress
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.NetworkInterface

class GerenciadorComunicacao(context: Context) : LifecycleObserver {

    companion object {
        const val PORT = 5000
    }

    private var socket: DatagramSocket? = null
    private var wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager?
    private var multicastLock = wifiManager?.createMulticastLock(javaClass.simpleName)?.apply { setReferenceCounted(true) }
    private val thread = RecebimentoThread(Handler {
        receberAcao(it.obj as Acao)
        return@Handler true
    })
    var callback: ((Acao) -> Unit)? = null

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    private fun iniciar() {
        multicastLock?.takeUnless { it.isHeld }?.acquire()
        socket = DatagramSocket(PORT).apply { broadcast = true }
        thread.socket = socket
        thread.start()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    private fun parar() {
        socket?.close()
        socket = null
        thread.interrupt()
        thread.socket = null
        multicastLock?.takeIf { it.isHeld }?.release()
    }

    private fun receberAcao(acao: Acao) {
        Log.d(javaClass.simpleName, "Recebendo ação: $acao")
        callback?.invoke(acao)
    }

    fun enviarAcao(acao: Acao): Boolean {
        Log.d(javaClass.simpleName, "Enviando ação: $acao")
        val broadcastAddress = NetworkInterface.getByName("wlan0").getBroadcastAddress()
        return if (broadcastAddress != null) {
            Thread {
                val bytes = acao.name.toByteArray(Charsets.US_ASCII)
                val packet = DatagramPacket(bytes, bytes.size, broadcastAddress, PORT)
                socket?.send(packet)
            }.start()
            true
        } else {
            false
        }
    }
}