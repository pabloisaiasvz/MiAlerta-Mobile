package com.example.tpfinal_pablovelazquez.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object FCMService {
    private const val FCM_URL = "https://fcm.googleapis.com/fcm/send"
    private const val SERVER_KEY = "AAAA...."

    suspend fun sendPushNotification(tokens: List<String>, message: String) {
        withContext(Dispatchers.IO) {
            val client = OkHttpClient()
            tokens.forEach { token ->
                val json = JSONObject()
                val notification = JSONObject()
                notification.put("title", "Alerta de Usuario")
                notification.put("body", message)
                json.put("to", token)
                json.put("notification", notification)

                val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
                val request = Request.Builder()
                    .url(FCM_URL)
                    .addHeader("Authorization", "key=$SERVER_KEY")
                    .post(body)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        println("Error enviando notificación: ${response.body?.string()}")
                    }
                }
            }
        }
    }
}
