package com.bankpoker.app.data.remote

import android.content.Context
import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

data class SocketEvent(
    val event: String,
    val payload: JSONObject? = null
)

class SocketManager private constructor(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO)
    private var socket: Socket? = null
    private val tokenManager = TokenManager(context)
    private val activeGroups = ConcurrentHashMap.newKeySet<String>()
    private val activeTables = ConcurrentHashMap.newKeySet<String>()

    private val _events = MutableSharedFlow<SocketEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<SocketEvent> = _events.asSharedFlow()

    companion object {
        private const val TAG = "SocketManager"

        @Volatile
        private var instance: SocketManager? = null

        fun getInstance(context: Context): SocketManager {
            return instance ?: synchronized(this) {
                instance ?: SocketManager(context.applicationContext).also { instance = it }
            }
        }
    }

    @Synchronized
    fun connect() {
        val serverUrl = ApiClient.getCurrentBaseUrl(context)
        val token = tokenManager.getToken() ?: ""

        if (socket != null && socket?.connected() == true) {
            return
        }

        try {
            val options = IO.Options().apply {
                transports = arrayOf("websocket", "polling")
                reconnection = true
                reconnectionAttempts = Int.MAX_VALUE
                reconnectionDelay = 1000L
                reconnectionDelayMax = 5000L
                timeout = 20000L
                auth = Collections.singletonMap("token", token)
                query = "token=$token"
            }

            socket = IO.socket(serverUrl, options)

            socket?.on(Socket.EVENT_CONNECT) {
                Log.d(TAG, "Connected to Socket.IO: ${socket?.id()}")
                // Rejoin any active groups & tables on reconnect
                activeGroups.forEach { gid -> socket?.emit("join_group", gid) }
                activeTables.forEach { tid -> socket?.emit("join_table", tid) }
                scope.launch {
                    _events.emit(SocketEvent("connect"))
                }
            }

            socket?.on(Socket.EVENT_DISCONNECT) {
                Log.d(TAG, "Disconnected from Socket.IO")
                scope.launch {
                    _events.emit(SocketEvent("disconnect"))
                }
            }

            socket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
                Log.w(TAG, "Socket connection error: ${args.firstOrNull()}")
            }

            // Register business real-time event listeners
            val businessEvents = listOf(
                "buyin_recorded",
                "exit_recorded",
                "payment_created",
                "settlement_done",
                "group_updated",
                "table_created",
                "table_closed",
                "table_updated",
                "table_published",
                "request_created",
                "request_resolved",
                "player_added",
                "player_deleted",
                "claim_done",
                "entry_fee_updated"
            )

            businessEvents.forEach { eventName ->
                socket?.on(eventName) { args ->
                    val payload = args.firstOrNull() as? JSONObject
                    Log.d(TAG, "Received socket event: $eventName payload: $payload")
                    scope.launch {
                        _events.emit(SocketEvent(eventName, payload))
                    }
                }
            }

            socket?.connect()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Socket.IO", e)
        }
    }

    @Synchronized
    fun disconnect() {
        try {
            socket?.disconnect()
            socket = null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to disconnect Socket.IO", e)
        }
    }

    @Synchronized
    fun updateAuth() {
        disconnect()
        connect()
    }

    fun joinGroup(groupId: String) {
        if (groupId.isBlank()) return
        activeGroups.add(groupId)
        socket?.emit("join_group", groupId)
    }

    fun leaveGroup(groupId: String) {
        if (groupId.isBlank()) return
        activeGroups.remove(groupId)
        socket?.emit("leave_group", groupId)
    }

    fun joinTable(tableId: String) {
        if (tableId.isBlank()) return
        activeTables.add(tableId)
        socket?.emit("join_table", tableId)
    }

    fun leaveTable(tableId: String) {
        if (tableId.isBlank()) return
        activeTables.remove(tableId)
        socket?.emit("leave_table", tableId)
    }
}
