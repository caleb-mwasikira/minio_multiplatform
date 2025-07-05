package org.example.project.data

import kotlinx.serialization.Serializable

@Serializable
data class Device(
    val id: String,
    val name: String,
    var ip: String = ""
)

fun getDevice() = lazy {
    val file = FileOperations.getOrCreateInternalFile("device_id") ?: return@lazy null
    val deviceId = file.readText()
    val deviceName = getDeviceName()
    return@lazy Device(
        id = deviceId,
        name = deviceName,
        ip = Network.getLocalIPAddress() ?: "0.0.0.0"
    )
}

expect fun getDeviceName(): String

