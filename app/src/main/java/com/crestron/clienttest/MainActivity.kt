package com.crestron.clienttest

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.Html
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.google.gson.internal.LinkedTreeMap
import com.programmerbox.dragswipe.DragSwipeAdapter
import io.ktor.client.HttpClient
import io.ktor.client.features.websocket.DefaultClientWebSocketSession
import io.ktor.client.features.websocket.WebSockets
import io.ktor.client.features.websocket.ws
import io.ktor.http.HttpMethod
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readText
import io.ktor.http.cio.websocket.send
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.message_info.view.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat

@Suppress("UNCHECKED_CAST")
class MainActivity : AppCompatActivity() {

    private val client = HttpClient {
        install(WebSockets)
    }

    private val listOfMessages = arrayListOf<SendMessage>()
    private lateinit var adapter: ChatAdapter
    private lateinit var userAdapter: UserAdapter

    private fun addAndUpdate(sendMessage: SendMessage) {

        when (sendMessage.type) {
            MessageType.MESSAGE -> adapter.addItem(sendMessage)
            MessageType.EPISODE -> adapter.addItem(sendMessage)
            MessageType.SERVER -> adapter.addItem(sendMessage)
            MessageType.INFO -> userAdapter.setListNotify(sendMessage.data as ArrayList<ChatUser>)
            MessageType.TYPING_INDICATOR -> typing_indicator.text = sendMessage.message
            MessageType.DOWNLOADING -> ""
            null -> adapter.addItem(sendMessage)
        }
        rv.smoothScrollToPosition(adapter.itemCount)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Loged.FILTER_BY_CLASS_NAME = "crestron"

        adapter = ChatAdapter(listOfMessages, this)
        rv.adapter = adapter

        userAdapter = UserAdapter(arrayListOf(), this, textToSend)
        user_list.adapter = userAdapter

        GlobalScope.launch {
            setupChatClient()
        }

    }

    class ChatAdapter(
        list: ArrayList<SendMessage>,
        private val context: Context
    ) : DragSwipeAdapter<SendMessage, ViewHolder>(list) {

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            Loged.d("${BBCodeParser().parse(list[position].message)} and before ${list[position].message}")
            holder.tv.text = BBCodeParser().parse(list[position].message)
            //Picasso.get().load(list[position].user.image).error(R.mipmap.ic_launcher).resize(150, 150).into(holder.image)
            Glide.with(context).load(list[position].user.image).into(holder.image)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(
                LayoutInflater.from(context).inflate(
                    R.layout.message_info,
                    parent,
                    false
                )
            )
        }
    }

    class UserAdapter(
        list: ArrayList<ChatUser>,
        private val context: Context,
        private val textField: EditText
    ) : DragSwipeAdapter<ChatUser, ViewHolder>(list) {

        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val s = list[position] as LinkedTreeMap<Any, Any>
            holder.tv.text = s["name"] as String
            Glide.with(context).load(s["image"] as String).into(holder.image)
            holder.itemView.setOnClickListener {
                textField.setText("/pm ${s["name"]} ")
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(
                LayoutInflater.from(context).inflate(
                    R.layout.message_info,
                    parent,
                    false
                )
            )
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tv = view.text_message!!
        val image = view.avatar!!
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

    private suspend fun DefaultClientWebSocketSession.uiSetup() {
        sendButton.setOnClickListener {
            GlobalScope.launch {
                send(textToSend.text.toString())
            }
            runOnUiThread {
                textToSend.setText("")
            }
        }

        textToSend.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {

            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                GlobalScope.launch {
                    val isTyping = !p0.isNullOrBlank() && !p0.startsWith("/pm ")
                    val s = Gson().toJson(
                        Action(
                            "Typing",
                            Gson().toJson(TypingIndicator(isTyping))
                        )
                    )
                    send(s)
                }
            }

        })

        // Receive frame.
        incoming.consumeEach {
            if (it is Frame.Text) {
                runOnUiThread {
                    addAndUpdate(
                        Gson().fromJson(
                            it.readText(),
                            SendMessage::class.java
                        )
                    )
                }
            }
        }
    }

    private suspend fun setupChatClient() {
        client.ws(
            method = HttpMethod.Get,
            host = "192.168.1.128",
            port = 8080, path = "/chat/ws"
        ) {
            // this: DefaultClientWebSocketSession
            uiSetup()
        }
    }

}