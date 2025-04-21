package com.example.bgwf.api

import android.util.Log
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit


class WebSocketManager(
    private val userId: Int,
    private val token: String
) {
    private var webSocket: WebSocket? = null
    private var listener: Listener? = null

    interface Listener {
        fun onEvent(type: String, payload: JSONObject)
    }

    fun setListener(listener: Listener?) {
        this.listener = listener
    }

    fun connect() {
        val client = OkHttpClient.Builder()
            .pingInterval(30, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            // если сервер слушает по ws://192.168.1.100:8000/ws/{user_id}
            .url("ws://192.168.1.100:8000/ws/$userId?token=$token")
            .addHeader("Authorization", "Bearer $token")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("WS", "Open")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
                    val type = json.getString("type")
                    listener?.onEvent(type, json)
                } catch (t: Throwable) {
                    Log.e("WS", "Parse error", t)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("WS", "Failure", t)
                // Попытка переподключения через 5 секунд
                Thread.sleep(5000)
                connect()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("WS", "Closed: $code / $reason")
            }
        })
    }

    fun disconnect() {
        webSocket?.close(1000, "Client closed")
        webSocket = null
    }

    fun sendFiltersUpdate(
        groupId: Int,
        genres: List<String>,
        minPlayers: String,
        maxPlayers: String,
        minPlayTime: String,
        maxPlayTime: String
    ) {
        val json = JSONObject().apply {
            put("type", "group_filters_updated")
            put("group_id", groupId)
            put("genres", JSONArray(genres))
            put("min_players", minPlayers)
            put("max_players", maxPlayers)
            put("min_play_time", minPlayTime)
            put("max_play_time", maxPlayTime)
        }
        webSocket?.send(json.toString())
    }
}
