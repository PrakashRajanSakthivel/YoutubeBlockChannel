package io.github.degipe.youtubewhitelist.core.export.transfer

import fi.iki.elonen.NanoHTTPD
import java.util.UUID

/**
 * Tiny embedded HTTP server that waits for a POST from the phone.
 * Runs on a fixed port, protected by a one-time token.
 *
 * Flow:
 *   TV starts server → encodes URL in QR → phone POSTs JSON to URL → callback fires
 */
class QrTransferServer(
    private val token: String,
    private val onDataReceived: (json: String) -> Unit
) : NanoHTTPD(PORT) {

    companion object {
        const val PORT = 8787
        const val PATH = "/receive"

        fun generateToken(): String = UUID.randomUUID().toString().replace("-", "").take(16)
    }

    /** Start listening. Prefer this over [start] to avoid exposing NanoHTTPD on the call-site classpath. */
    fun startServer() = start()

    /** Stop listening. Prefer this over [stop] to avoid exposing NanoHTTPD on the call-site classpath. */
    fun stopServer() = stop()

    override fun serve(session: IHTTPSession): Response {
        if (session.method == Method.POST && session.uri == PATH) {
            val receivedToken = session.headers["x-transfer-token"] ?: ""
            if (receivedToken != token) {
                return newFixedLengthResponse(
                    Response.Status.FORBIDDEN, MIME_PLAINTEXT, "Invalid token"
                )
            }
            val body = mutableMapOf<String, String>()
            try {
                session.parseBody(body)
            } catch (e: Exception) {
                return newFixedLengthResponse(
                    Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Parse error"
                )
            }
            val json = body["postData"] ?: ""
            if (json.isNotBlank()) {
                onDataReceived(json)
                return newFixedLengthResponse(
                    Response.Status.OK, "application/json", """{"status":"ok"}"""
                )
            }
            return newFixedLengthResponse(
                Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Empty body"
            )
        }
        return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not found")
    }
}
