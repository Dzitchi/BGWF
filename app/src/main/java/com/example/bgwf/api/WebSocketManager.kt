package com.example.bgwf.api

import android.util.Log
import okhttp3.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit


class WebSocketManager(
    private val userId: Int,
    private val token: String,
    private val listener: Listener
) {
    interface Listener {
        /** тип = "friend_request_received" или "friend_request_response" */
        fun onEvent(type: String, payload: JSONObject)
    }

    private var webSocket: WebSocket? = null

    fun connect() {
        val client = OkHttpClient.Builder()
            .pingInterval(30, TimeUnit.SECONDS) // поддерживаем соединение
            .build()

        val request = Request.Builder()
            // если сервер слушает по ws://192.168.1.100:8000/ws/{user_id}
            .url("ws://192.168.1.100:8000/ws/$userId?token=$token")
            .addHeader("Authorization", "Bearer $token")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                Log.d("WS", "Open")
            }

            override fun onMessage(ws: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
                    val type = json.getString("type")
                    listener.onEvent(type, json)
                } catch (t: Throwable) {
                    Log.e("WS", "Parse error", t)
                }
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.e("WS", "Failure", t)
                // Попытка переподключения через 5 секунд
                Thread.sleep(5000)
                connect()
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                Log.d("WS", "Closed: $code / $reason")
            }
        })
    }

    fun disconnect() {
        webSocket?.close(1000, "Client closed")
        webSocket = null
    }
}
