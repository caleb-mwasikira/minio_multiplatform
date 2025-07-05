package org.example.project.data

import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.json.Json
import org.example.project.data.http.HttpClient
import org.example.project.data.http.isSuccess
import java.net.NetworkInterface
import java.util.Collections

object Network {
    fun getLocalIPAddress(): String? {
        val interfaces = NetworkInterface.getNetworkInterfaces()

        for (iface in interfaces) {
            if (iface.isUp && !iface.isLoopback) {
                val addresses = Collections.list(iface.inetAddresses)
                for (address in addresses) {
                    val ip = address.hostAddress
                    if (ip.contains('.') && !ip.startsWith("127")) {
                        return ip
                    }
                }
            }
        }
        return null
    }

    fun getOnlineDevices() = channelFlow<Device> {
        println("Searching online devices...")

        val localIPAddress = getLocalIPAddress() ?: run {
            println("Error acquiring local IP address")
            return@channelFlow
        }

        val subnet = localIPAddress.substringBeforeLast('.')
        val semaphore = Semaphore(50)

        for (i in 1..254) {
            val ip = "$subnet.$i"
            if (ip == localIPAddress) continue

            semaphore.withPermit {
                launch {
                    try {
                        val port = 8080
                        HttpClient(ip, port).use { client ->
                            val response = client.get("/")
                            if (response.isSuccess() && response.body != null) {
                                val device = Json.decodeFromString<Device>(response.body)
                                send(device)
                            }
                        }
                    } catch (_: Exception) {
                        // Ignore failed pings
                    }
                }
            }
        }
    }

}