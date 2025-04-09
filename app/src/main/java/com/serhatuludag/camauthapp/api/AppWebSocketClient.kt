package com.serhatuludag.camauthapp.api

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.serhatuludag.camauthapp.data.model.OverlayMessage
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit
import java.util.Timer
import java.util.TimerTask

class AppWebSocketClient(
    private val request: Request,
    private val listener: OverlayWebSocketListener
) {
    private val TAG = "AppWebSocketClient"
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()
    private var webSocket: WebSocket? = null
    private var isConnected = false
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 5
    private val reconnectDelayMs = 5000L  // 5 seconds delay between reconnect attempts
    private val heartbeatIntervalMs = 30000L  // 30 seconds heartbeat interval
    private val gson = Gson()
    private var heartbeatTimer: Timer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    fun connect() {
        Log.d(TAG, "Connecting to WebSocket...")
        webSocket = client.newWebSocket(request, OverlayWebSocketListenerImpl(listener, gson))
    }

    fun disconnect() {
        stopHeartbeat()
        webSocket?.close(1000, "Client disconnecting")
        webSocket = null
    }

    private fun startHeartbeat() {
        stopHeartbeat()

        heartbeatTimer = Timer().apply {
            schedule(object : TimerTask() {
                override fun run() {
                    if (isConnected) {
                        sendHeartbeat()
                    }
                }
            }, heartbeatIntervalMs, heartbeatIntervalMs)
        }
    }

    private fun stopHeartbeat() {
        heartbeatTimer?.cancel()
        heartbeatTimer = null
    }

    private fun sendHeartbeat() {
        try {
            val heartbeatMessage = "{\"type\":\"heartbeat\"}"
            webSocket?.send(heartbeatMessage)
            Log.d(TAG, "Heartbeat sent")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending heartbeat: ${e.message}")
        }
    }

    interface OverlayWebSocketListener {
        fun onOpen(webSocket: WebSocket, response: okhttp3.Response)
        fun onOverlayMessage(webSocket: WebSocket, overlay: OverlayMessage)
        fun onClosing(webSocket: WebSocket, code: Int, reason: String)
        fun onClosed(webSocket: WebSocket, code: Int, reason: String)
        fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?)
    }

    private class OverlayWebSocketListenerImpl(
        private val listener: OverlayWebSocketListener,
        private val gson: Gson
    ) : WebSocketListener() {
        private val TAG = "OverlayWebSocketListenerImpl"

        override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
            Log.d(TAG, "WebSocket connection opened")
            listener.onOpen(webSocket, response)
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            Log.d(TAG, "Message received: $text")
            try {
                val overlay = gson.fromJson(text, OverlayMessage::class.java)
                listener.onOverlayMessage(webSocket, overlay)
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing message: ${e.message}")
            }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "WebSocket closing: $code - $reason")
            listener.onClosing(webSocket, code, reason)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "WebSocket closed: $code - $reason")
            listener.onClosed(webSocket, code, reason)
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
            Log.e(TAG, "WebSocket error: ${t.message}")
            if (response != null) {
                Log.d(TAG, "Response code: ${response.code}")
                Log.d(TAG, "Response message: ${response.message}")
            }
            listener.onFailure(webSocket, t, response)
        }
    }
}