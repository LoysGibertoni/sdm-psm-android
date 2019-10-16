package br.edu.ifsp.sdm.projetopsm

import java.net.InetAddress
import java.net.NetworkInterface

fun NetworkInterface.getBroadcastAddress(): InetAddress? {
    return interfaceAddresses.mapNotNull { it.broadcast  }.firstOrNull()
}