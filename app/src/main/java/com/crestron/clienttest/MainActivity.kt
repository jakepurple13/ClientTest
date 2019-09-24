package com.crestron.clienttest

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.text.Editable
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
import io.ktor.client.features.websocket.DefaultClientWebSocketSession
import io.ktor.http.cio.websocket.readText
import io.ktor.http.cio.websocket.send
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.message_info.view.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@Suppress("UNCHECKED_CAST")
class MainActivity : AppCompatActivity() {

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
            ClientHandler(object : ClientUISetup {
                override suspend fun uiSetup(socket: DefaultClientWebSocketSession) {
                    socket.uiSetup()
                }
            })
        }
    }

    class ChatAdapter(
        list: ArrayList<SendMessage>,
        private val context: Context
    ) : DragSwipeAdapter<SendMessage, ViewHolder>(list) {

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.tv.text = BBCodeParser().parse(list[position].message)
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

    private suspend fun DefaultClientWebSocketSession.uiSetup() {
        sendButton.setOnClickListener {
            if(!textToSend.text.isNullOrBlank()) {
                GlobalScope.launch {
                    send(textToSend.text.toString())
                }
                runOnUiThread {
                    textToSend.setText("")
                }
            }
        }

        textToSend.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {}
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                GlobalScope.launch {
                    val isTyping = !p0.isNullOrBlank() && !p0.startsWith("/pm ")
                    sendAction(TypingIndicator(isTyping).toAction())
                }
            }
        })
        newMessage {
            runOnUiThread {
                addAndUpdate(Gson().fromJson(it.readText(), SendMessage::class.java))
            }
        }
    }
}