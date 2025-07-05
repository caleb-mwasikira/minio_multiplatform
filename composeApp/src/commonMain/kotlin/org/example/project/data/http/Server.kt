package org.example.project.data.http

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.Json
import java.net.Socket

/**
 * Reads request data sent over by client application
 */
fun Socket.readRequest(): HttpRequest? {
    val reader = this.getInputStream().bufferedReader()

    // Read request line
    val requestLine = reader.readLine() ?: return null
    val requestParts = requestLine.split(" ")
    if (requestParts.size < 3) return null
    val method = requestParts[0]
    val path = requestParts[1]
    val version = requestParts[2]

    // Read headers
    val headers = mutableMapOf<String, String>()
    var line: String?
    while (reader.readLine().also { line = it } != null) {
        if (line!!.isEmpty()) break // end of headers
        val (key, value) = line!!.split(":", limit = 2)
        headers[key.trim()] = value.trim()
    }

    // Read body if Content-Length is present
    val contentLength = headers["Content-Length"]?.toIntOrNull() ?: 0
    val body = if (contentLength > 0) {
        val bodyChars = CharArray(contentLength)
        reader.read(bodyChars)
        String(bodyChars)
    } else null

    return HttpRequest(
        method = HttpMethod.parse(method),
        path = path,
        version = version,
        headers = headers,
        body = body
    )
}

/**
 * Sends response back to client application
 */
fun Socket.respond(
    status: HttpStatusCode = HttpStatusCode.OK,
    body: String = ""
) {
    val headers = mapOf(
        "Content-Type" to "text/plain",
        "Content-Length" to body.toByteArray().size.toString()
    )
    val response = HttpResponse(
        statusCode = status.value,
        statusMessage = status.description,
        headers = headers,
        body = body
    )
    val responseData = response.toRawString()

    val outputStream = this.getOutputStream()
    outputStream.write(responseData.toByteArray())
    outputStream.flush()
}

/**
 * Sends json response back to client application
 */
inline fun <reified T> Socket.respondJson(
    status: HttpStatusCode = HttpStatusCode.OK,
    body: T
) {
    val jsonBody = Json.encodeToString(body)
    val headers = mapOf(
        "Content-Type" to "application/json",
        "Content-Length" to jsonBody.toByteArray().size.toString()
    )
    val response = HttpResponse(
        statusCode = status.value,
        statusMessage = status.description,
        headers = headers,
        body = jsonBody
    )

    val responseData = response.toRawString()
    val outputStream = this.getOutputStream()
    outputStream.write(responseData.toByteArray())
    outputStream.flush()
}