package br.edu.ifsp.sdm.projetopsm.communication

import android.os.Handler
import java.net.DatagramPacket
import java.net.DatagramSocket

class RecebimentoThread(private val handler: Handler) : Thread() {

    companion object {
        const val BUFFER_SIZE = 1024
    }

    var socket: DatagramSocket? = null

    override fun run() {
        super.run()

        val buffer = ByteArray(BUFFER_SIZE)
        val packet = DatagramPacket(buffer, buffer.size)

        while (!isInterrupted) {
            socket?.receive(packet)
            val dados = String(buffer, 0, packet.length, Charsets.US_ASCII)
            receberAcao(Acao.valueOf(dados))
        }
    }

    private fun receberAcao(acao: Acao) {
        val message = handler.obtainMessage()
        message.obj = acao
        handler.sendMessage(message)
    }
}