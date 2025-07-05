package org.example.project.data

import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.example.project.data.http.readRequest
import org.example.project.data.http.respond
import org.example.project.data.http.respondJson
import java.net.ServerSocket
import java.net.Socket

object SyncServer {
    private val host: String = Network.getLocalIPAddress() ?: "0.0.0.0"
    private const val PORT: Int = 8080
    private val myDevice = getDevice()
    val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    suspend fun start() = withContext(Dispatchers.IO) {
        try {
            println("Starting server on address $host:$PORT")
            val serverSocket = ServerSocket(PORT)
            while (true) {
                try {
                    val client = serverSocket.accept()
                    scope.launch {
                        handleClient(client)
                    }

                } catch (e: Exception) {
                    println("Error handling client connection; ${e.message}")
                }
            }

        } catch (e: Exception) {
            println("Server socket closed; ${e.message}")
        }
    }

    private suspend fun handleClient(client: Socket): Unit = withContext(Dispatchers.IO) {
        val request = client.readRequest() ?: run {
            client.respond(HttpStatusCode.BadRequest)
            return@withContext
        }

        println("Received ${request.method} ${request.path} request")
        val route = "${request.method.value.uppercase()} ${request.path}"
        when (route) {
            "GET /" -> {
                client.respondJson(body = myDevice.value)
            }

            "POST /track-device" -> {
                try {
                    request.body ?: throw IllegalArgumentException("Missing request body")

                    val device = Json.decodeFromString<Device>(request.body)
                    val success = LocalStore.trackNewDevice(device)
                    if (success) {
                        client.respondJson(HttpStatusCode.OK, myDevice.value)
                    } else {
                        client.respond(HttpStatusCode.InternalServerError)
                    }

                } catch (e: Exception) {
                    val status = when (e) {
                        is SerializationException, is IllegalArgumentException -> {
                            HttpStatusCode.BadRequest
                        }

                        else -> {
                            HttpStatusCode.InternalServerError
                        }
                    }
                    client.respond(status, status.description)
                }
            }

            else -> {
                client.respond(HttpStatusCode.NotFound)
            }
        }
    }
}

