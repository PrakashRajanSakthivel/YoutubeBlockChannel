package io.github.degipe.youtubewhitelist.core.export.transfer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Sends the export JSON from the phone to the TV's embedded HTTP server.
 *
 * @param ip    IP address of the TV as decoded from the QR code
 * @param port  Port as decoded from the QR code
 * @param token One-time security token as decoded from the QR code
 * @param json  Full export JSON produced by [ExportImportService]
 * @return      `true` on HTTP 200, `false` on any error
 */
object QrTransferClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    suspend fun send(ip: String, port: Int, token: String, json: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("http://$ip:$port${QrTransferServer.PATH}")
                    .addHeader("x-transfer-token", token)
                    .post(json.toRequestBody(JSON_MEDIA_TYPE))
                    .build()
                client.newCall(request).execute().use { response ->
                    response.isSuccessful
                }
            } catch (e: Exception) {
                false
            }
        }
}
