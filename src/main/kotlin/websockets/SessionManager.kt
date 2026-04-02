package com.websockets

import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.send
import kotlinx.serialization.Serializable
import java.util.concurrent.ConcurrentHashMap

object SessionManager {
    private val sessions =
        ConcurrentHashMap<String, DefaultWebSocketServerSession>()
     fun connect(uid: String,session: DefaultWebSocketServerSession){
        sessions[uid]=session
    }
    fun getSession(uid: String): DefaultWebSocketServerSession? {
        return sessions[uid]
    }
     fun disConnect(uid: String){
        sessions.remove(uid)
    }
    fun isOnline(uid: String): Boolean {
        return sessions.containsKey(uid)
    }
    suspend fun sendToUser(uid: String, message: String) {
        try {
            sessions[uid]?.send(message)
        } catch (e: Exception) {
            println("Send error: ${e.message}")
        }
    }

    }




data class WebSkUserSession(
    val userId: String,
    val chat_Id: String,
    val session: DefaultWebSocketServerSession
)