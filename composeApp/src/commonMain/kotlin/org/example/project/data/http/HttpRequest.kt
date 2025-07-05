package org.example.project.data.http

import io.ktor.http.HttpMethod

data class HttpRequest(
    val method: HttpMethod,
    val path: String,
    val version: String = "HTTP/1.1",
    val headers: Map<String, String> = emptyMap(),
    val body: String? = null
) {
    companion object {
        fun parse(raw: String): HttpRequest? {
            val lines = raw.split("\r\n")
            val requestLineSections = lines[0].split(" ")
            if (requestLineSections.size != 3) {
                return null
            }

            val method = requestLineSections[0]
            val path = requestLineSections[1]
            val version = requestLineSections[2]

            val headers = mutableMapOf<String, String>()
            var i = 1
            while (i < lines.size && lines[i].isNotEmpty()) {
                val (key, value) = lines[i].split(":", limit = 2)
                headers[key.trim()] = value.trim()
                i++
            }

            val body = lines.drop(i + 1).joinToString("\r\n").ifBlank { null }
            return HttpRequest(
                method = HttpMethod.parse(method),
                path = path,
                version = version,
                headers = headers,
                body = body
            )
        }
    }
}

fun HttpRequest.toRawString(): String {
    val builder = StringBuilder()

    // Request line
    builder.append("${this.method} ${this.path} ${this.version}\r\n")

    // Headers
    this.headers.forEach { (key, value) ->
        builder.append("$key: $value\r\n")
    }

    builder.append("\r\n") // Blank line to separate headers and body

    // Body (optional)
    this.body?.let {
        builder.append(it)
    }
    return builder.toString()
}