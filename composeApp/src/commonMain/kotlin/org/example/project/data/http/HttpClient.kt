package org.example.project.data.http

import io.ktor.http.HttpMethod
import kotlinx.serialization.json.Json
import java.net.Socket

class HttpClient(
    private val host: String,
    private val port: Int
) : AutoCloseable {
    var socket: Socket? = Socket(host, port)

    fun readResponse(): HttpResponse {
        socket ?: throw IllegalStateException("Socket closed")

        val reader = socket!!.getInputStream().bufferedReader()

        // Read status line and headers
        val lines = mutableListOf<String>()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            if (line!!.isEmpty()) break // empty line = end of headers
            lines += line!!
        }

        val statusLine = lines[0]
        val headers = lines.drop(1).associate {
            val (k, v) = it.split(":", limit = 2)
            k.trim() to v.trim()
        }

        val contentLength = headers["Content-Length"]?.toIntOrNull() ?: 0
        val body = CharArray(contentLength)
        reader.read(body, 0, contentLength)

        return HttpResponse(
            version = statusLine.split(" ")[0],
            statusCode = statusLine.split(" ")[1].toInt(),
            statusMessage = statusLine.split(" ", limit = 3).getOrElse(2) { "" },
            headers = headers,
            body = String(body)
        )
    }

    fun get(path: String): HttpResponse {
        socket ?: throw IllegalStateException("Socket closed")

        println("Sending GET $path request")
        val request = HttpRequest(
            method = HttpMethod.Get,
            path = path
        )
        val requestData = request.toRawString()

        // Send request
        val outputStream = socket!!.getOutputStream()
        outputStream.write(requestData.toByteArray())
        outputStream.flush()

        return readResponse()
    }

    inline fun <reified T> post(path: String, body: T): HttpResponse {
        socket ?: throw IllegalStateException("Socket closed")

        println("Sending POST $path request")
        val jsonBody = Json.encodeToString(body)
        val request = HttpRequest(
            method = HttpMethod.Post,
            path = path,
            headers = mapOf(
                "Content-Type" to "application/json",
                "Content-Length" to jsonBody.toByteArray().size.toString()
            ),
            body = jsonBody
        )
        val requestData = request.toRawString()

        // Send request
        val outputStream = socket!!.getOutputStream()
        outputStream.write(requestData.toByteArray())
        outputStream.flush()

        return readResponse()
    }

    override fun close() {
        socket?.close()
        socket = null
    }
}