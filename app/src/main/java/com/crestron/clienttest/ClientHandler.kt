package com.crestron.clienttest

import android.annotation.SuppressLint
import com.google.gson.Gson
import io.ktor.client.HttpClient
import io.ktor.client.features.websocket.DefaultClientWebSocketSession
import io.ktor.client.features.websocket.WebSockets
import io.ktor.client.features.websocket.ws
import io.ktor.http.HttpMethod
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readText
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat

class ClientHandler(clientUISetup: ClientUISetup) {
    private val client = HttpClient {
        install(WebSockets)
    }

    init {
        GlobalScope.launch {
            client.ws(
                method = HttpMethod.Get,
                host = "192.168.1.128",
                port = 8080, path = "/chat/ws"
            ) {
                // this: DefaultClientWebSocketSession
                clientUISetup.uiSetup(this)
            }
        }
    }
}

interface ClientUISetup {
    suspend fun uiSetup(socket: DefaultClientWebSocketSession)
}

suspend fun DefaultClientWebSocketSession.newMessage(message: (Frame.Text) -> Unit) {
    // Receive frame.
    incoming.consumeEach {
        if (it is Frame.Text) {
            message(it)
        }
    }
}

data class ChatUser(
    var name: String,
    var image: String = "https://www.w3schools.com/w3images/bandmember.jpg"
)

enum class MessageType {
    MESSAGE, EPISODE, SERVER, INFO, TYPING_INDICATOR, DOWNLOADING
}

data class SendMessage(
    val user: ChatUser,
    val message: String,
    val type: MessageType?,
    val data: Any? = null
) {
    @SuppressLint("SimpleDateFormat")
    val time = SimpleDateFormat("MM/dd hh:mm a").format(System.currentTimeMillis())!!

    fun toJson(): String = Gson().toJson(this)
}

data class Action(val type: String, val json: String)
data class TypingIndicator(val isTyping: Boolean)
data class Profile(val username: String?, val image: String?)