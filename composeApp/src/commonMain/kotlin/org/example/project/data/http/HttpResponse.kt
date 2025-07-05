package org.example.project.data.http

data class HttpResponse(
    val version: String = "HTTP/1.1",
    val statusCode: Int,
    val statusMessage: String,
    val headers: Map<String, String> = emptyMap(),
    val body: String? = null
) {
    companion object {
        fun parse(raw: String): HttpResponse {
            val lines = raw.split("\r\n")

            // Parse status line
            val statusLine = lines[0]
            val statusParts = statusLine.split(" ", limit = 3)
            val version = statusParts[0]
            val statusCode = statusParts[1].toInt()
            val statusMessage = statusParts.getOrElse(2) { "" }

            // Parse headers
            val headers = mutableMapOf<String, String>()
            var i = 1
            while (i < lines.size && lines[i].isNotEmpty()) {
                val (key, value) = lines[i].split(":", limit = 2)
                headers[key.trim()] = value.trim()
                i++
            }

            // The rest is body
            val body = lines.drop(i + 1).joinToString("\r\n").ifBlank { null }

            return HttpResponse(
                version = version,
                statusCode = statusCode,
                statusMessage = statusMessage,
                headers = headers,
                body = body
            )
        }

    }
}

fun HttpResponse.toRawString(): String {
    val builder = StringBuilder()

    // Status line
    builder.append("${this.version} ${this.statusCode} ${this.statusMessage}\r\n")

    // Headers
    this.headers.forEach { (key, value) ->
        builder.append("$key: $value\r\n")
    }

    builder.append("\r\n") // Blank line to separate headers and body

    // Body
    this.body?.let {
        builder.append(it)
    }
    return builder.toString()
}

fun HttpResponse.isSuccess(): Boolean {
    return this.statusCode in 200..209
}

fun HttpResponse.isFail(): Boolean {
    return !this.isSuccess()
}
