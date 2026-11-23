package com.immichtv.app.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.security.SecureRandom

/**
 * Tiny HTTP server that only runs while the setup screen is visible.
 * The phone scans the QR code, opens the form, pastes server URL + API key
 * and posts it back to the TV. A random one-time token in the URL makes sure
 * only someone who can see the TV screen can use the form.
 */
class SetupWebServer(
    private val port: Int,
    val token: String,
    private val prefillUrl: () -> String,
    private val onSubmit: (serverUrl: String, apiKey: String) -> Unit
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile private var serverSocket: ServerSocket? = null

    fun start() {
        scope.launch {
            try {
                val ss = ServerSocket(port)
                serverSocket = ss
                while (isActive) {
                    val client = ss.accept()
                    launch { handle(client) }
                }
            } catch (_: IOException) {
                // Socket closed in stop(), or port unavailable.
            }
        }
    }

    fun stop() {
        try { serverSocket?.close() } catch (_: IOException) { }
        scope.cancel()
    }

    private fun handle(socket: Socket) {
        try {
            socket.use { s ->
                s.soTimeout = 10_000
                val input = BufferedInputStream(s.getInputStream())
                val out = s.getOutputStream()
                val requestLine = readLine(input) ?: return
                val headers = mutableMapOf<String, String>()
                while (true) {
                    val line = readLine(input) ?: break
                    if (line.isEmpty()) break
                    val i = line.indexOf(':')
                    if (i > 0) headers[line.substring(0, i).trim().lowercase()] = line.substring(i + 1).trim()
                }
                val parts = requestLine.split(" ")
                val method = parts.getOrElse(0) { "" }
                val target = parts.getOrElse(1) { "/" }
                val path = target.substringBefore('?')
                val query = parseForm(target.substringAfter('?', ""))

                when {
                    method == "GET" && path == "/" && query["t"] == token ->
                        respond(out, 200, formPage())

                    method == "POST" && path == "/submit" -> {
                        val len = headers["content-length"]?.toIntOrNull() ?: 0
                        if (len > 8192) {
                            respond(out, 413, page("Too much data.")); return
                        }
                        val body = ByteArray(len)
                        var read = 0
                        while (read < len) {
                            val n = input.read(body, read, len - read)
                            if (n < 0) break
                            read += n
                        }
                        val form = parseForm(String(body, 0, read, Charsets.UTF_8))
                        if (form["t"] != token) {
                            respond(out, 403, page("This link has expired. Scan the QR code on the TV again."))
                            return
                        }
                        val url = form["url"].orEmpty().trim()
                        val key = form["key"].orEmpty().trim()
                        if (url.isEmpty() || key.isEmpty()) {
                            respond(out, 400, page("Server URL and API key are both required. Go back and try again."))
                            return
                        }
                        onSubmit(url, key)
                        respond(out, 200, page("&#10003; Sent to your TV. Look at the TV screen."))
                    }

                    else -> respond(out, 404, page("Not found. Scan the QR code on the TV again."))
                }
            }
        } catch (_: IOException) {
            // Client went away; ignore.
        }
    }

    private fun formPage(): String {
        val url = escape(prefillUrl())
        return page(
            """
            <h1>immich TV</h1>
            <p>Paste your Immich details. They are sent straight to your TV.</p>
            <form method="post" action="/submit">
              <input type="hidden" name="t" value="${escape(token)}">
              <label for="url">Server URL</label>
              <input id="url" name="url" type="url" value="$url" placeholder="http://192.168.1.49:2283" required>
              <small>Address of your Immich server, including http:// and the port.</small>
              <label for="key">API Key</label>
              <input id="key" name="key" type="text" autocomplete="off" autocapitalize="off" spellcheck="false" required>
              <small>Immich: Account Settings &rarr; API Keys &rarr; New API Key.</small>
              <button type="submit">Send to TV</button>
            </form>
            """.trimIndent()
        )
    }

    private fun page(content: String): String = """
        <!DOCTYPE html><html><head><meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>immich TV setup</title>
        <style>
          body{font-family:system-ui,sans-serif;background:#111827;color:#f3f4f6;margin:0;padding:24px;}
          h1{color:#facc15;margin-top:0}
          label{display:block;font-weight:600;margin-top:18px}
          input{width:100%;box-sizing:border-box;font-size:16px;padding:12px;margin-top:6px;border-radius:8px;border:1px solid #6b7280;background:#1f2937;color:#fff}
          small{display:block;color:#9ca3af;margin-top:4px}
          button{margin-top:24px;width:100%;padding:14px;font-size:17px;border:0;border-radius:8px;background:#facc15;color:#111827;font-weight:700}
          p{font-size:17px}
        </style></head><body>$content</body></html>
    """.trimIndent()

    private fun respond(out: OutputStream, code: Int, html: String) {
        val body = html.toByteArray(Charsets.UTF_8)
        val status = when (code) {
            200 -> "OK"; 400 -> "Bad Request"; 403 -> "Forbidden"; 404 -> "Not Found"; else -> "Error"
        }
        val header = "HTTP/1.1 $code $status\r\n" +
            "Content-Type: text/html; charset=utf-8\r\n" +
            "Content-Length: ${body.size}\r\n" +
            "Cache-Control: no-store\r\n" +
            "Connection: close\r\n\r\n"
        out.write(header.toByteArray(Charsets.ISO_8859_1))
        out.write(body)
        out.flush()
    }

    private fun readLine(input: InputStream): String? {
        val sb = StringBuilder()
        while (true) {
            val b = input.read()
            if (b < 0) return if (sb.isEmpty()) null else sb.toString()
            if (b == '\n'.code) return sb.toString().trimEnd('\r')
            if (sb.length > 8192) return null
            sb.append(b.toChar())
        }
    }

    private fun parseForm(s: String): Map<String, String> =
        s.split('&').filter { it.isNotEmpty() }.associate { pair ->
            val k = pair.substringBefore('=')
            val v = pair.substringAfter('=', "")
            decode(k) to decode(v)
        }

    private fun decode(s: String): String =
        try { URLDecoder.decode(s, "UTF-8") } catch (_: Exception) { s }

    private fun escape(s: String): String = s
        .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        .replace("\"", "&quot;").replace("'", "&#39;")

    companion object {
        const val PORT = 8765

        fun newToken(): String {
            val bytes = ByteArray(9)
            SecureRandom().nextBytes(bytes)
            return bytes.joinToString("") { "%02x".format(it) }
        }

        /** First private IPv4 address of this device (Ethernet or Wi-Fi). */
        fun localIpv4(): String? = try {
            NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
                .filter { it.isUp && !it.isLoopback }
                .flatMap { it.inetAddresses.toList() }
                .filterIsInstance<Inet4Address>()
                .firstOrNull { it.isSiteLocalAddress }
                ?.hostAddress
        } catch (_: Exception) {
            null
        }
    }
}
