package com.crestron.clienttest

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.google.gson.internal.LinkedTreeMap
import com.kennyc.bottomsheet.BottomSheetListener
import com.kennyc.bottomsheet.BottomSheetMenuDialogFragment
import com.programmerbox.dragswipe.DragSwipeAdapter
import com.yarolegovich.lovelydialog.LovelyTextInputDialog
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
        if (sendMessage.type != MessageType.TYPING_INDICATOR)
            Loged.r(sendMessage)

        when (sendMessage.type) {
            MessageType.MESSAGE -> adapter.addItem(sendMessage)
            MessageType.EPISODE -> adapter.addItem(sendMessage)
            MessageType.SERVER -> adapter.addItem(sendMessage)
            MessageType.INFO -> userAdapter.setListNotify(sendMessage.data as ArrayList<ChatUser>)
            MessageType.TYPING_INDICATOR -> typing_indicator.text = sendMessage.message
            MessageType.DOWNLOADING -> Loged.i("Download")
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

        val dividerItemDecoration =
            DividerItemDecoration(rv.context, (rv.layoutManager as LinearLayoutManager).orientation)
        rv.addItemDecoration(dividerItemDecoration)

        userAdapter = UserAdapter(arrayListOf(), this, textToSend)
        user_list.adapter = userAdapter

        LovelyTextInputDialog(this)
            .setTitle("Enter host address")
            .setMessage("Enter host address")
            .configureEditText {
                it.hint = "Or leave blank for default"
            }
            .setInputFilter("Nope") { true }
            .setConfirmButton(
                "Ok"
            ) { text ->
                if (!text.isNullOrBlank()) {
                    ClientHandler.host = text
                }
                GlobalScope.launch {
                    ClientHandler(object : ClientUISetup {
                        override suspend fun uiSetup(socket: DefaultClientWebSocketSession) {
                            socket.uiSetup()
                        }
                    })
                }
            }
            .show()
    }

    class ChatAdapter(
        list: ArrayList<SendMessage>,
        private val context: Context
    ) : DragSwipeAdapter<SendMessage, ViewHolder>(list) {

        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            if (list[position].type == MessageType.EPISODE) {
                val q = Gson().fromJson(
                    list[position].data.toString(),
                    Array<EpisodeInfo>::class.java
                )[0]
                holder.tv.text = "${q.name}\n\n${q.description}"
                Glide.with(context).load(q.image).into(holder.image)
                holder.itemView.setOnLongClickListener {
                    context.startActivity(Intent(context, EpisodeActivity::class.java).apply {
                        putExtra("episode_info", q.url)
                    })
                    true
                }
            } else {
                //val processor = BBProcessorFactory.getInstance().create();
                //Loged.r("Before: ${list[position].message}\n\nAfter: ${processor.process(list[position].message)}")
                holder.tv.text = BBCodeParser().parse(list[position].message)
                //holder.tv.text = Html.fromHtml(BBCodeParser().parse(processor.process(list[position].message)).toString())
                Glide.with(context).load(list[position].user.image).into(holder.image)
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
                textField.setSelection(textField.text.toString().length)
                textField.requestAndShowKeyboard()
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

    @SuppressLint("SetTextI18n")
    fun EditText.addText(textToAdd: String, middle: Int = 3) {
        val cursorPosition = selectionStart
        val enteredText = text.toString()
        setText(
            "${enteredText.substring(0, cursorPosition)}$textToAdd${enteredText.substring(
                cursorPosition
            )}"
        )
        setSelection(text.toString().indexOf(textToAdd) + middle)
    }

    fun profileChange(socketSession: DefaultClientWebSocketSession) {
        val linearLayout = LinearLayout(this@MainActivity)
        linearLayout.orientation = LinearLayout.VERTICAL
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        val nameInput = EditText(this@MainActivity)
        nameInput.layoutParams = lp
        nameInput.hint = "Name"
        nameInput.imeOptions = EditorInfo.IME_ACTION_NEXT

        val imageInput = EditText(this@MainActivity)
        imageInput.layoutParams = lp
        imageInput.hint = "Image Url"
        imageInput.imeOptions = EditorInfo.IME_ACTION_GO

        linearLayout.addView(nameInput)
        linearLayout.addView(imageInput)

        val builder = AlertDialog.Builder(this@MainActivity)
        builder.setView(linearLayout)
        builder.setTitle("Change your profile info")
        builder.setMessage("Change your profile info")
        builder.setCancelable(false)
        // Add the buttons
        builder.setPositiveButton("Okay!") { _, _ ->
            GlobalScope.launch {
                val name =
                    if (nameInput.text.toString().isBlank()) null else nameInput.text.toString()
                val image =
                    if (imageInput.text.toString().isBlank()) null else imageInput.text.toString()
                socketSession.sendAction(Profile(name, image).toAction())
            }
        }
        builder.setNegativeButton("Never Mind") { _, _ ->

        }
        val dialog = builder.create()
        dialog.show()
    }

    private suspend fun DefaultClientWebSocketSession.uiSetup() {
        sendButton.setOnClickListener {
            if (!textToSend.text.isNullOrBlank()) {
                GlobalScope.launch {
                    send(textToSend.text.toString())
                }
                runOnUiThread {
                    textToSend.setText("")
                }
            }
        }

        val sheet = BottomSheetMenuDialogFragment.Builder(this@MainActivity)
            .setSheet(R.menu.chat_menu)
            .dark()
            .grid()
            .setTitle("Help")
            .setListener(object : BottomSheetListener {
                override fun onSheetItemSelected(
                    bottomSheet: BottomSheetMenuDialogFragment,
                    item: MenuItem?,
                    `object`: Any?
                ) {
                    when (item?.itemId) {
                        R.id.help -> {
                            GlobalScope.launch {
                                send("/help")
                            }
                        }
                        R.id.musicGame -> {
                            startActivity(Intent(this@MainActivity, MusicGameActivity::class.java))
                        }
                        R.id.shows -> {
                            startActivity(Intent(this@MainActivity, ShowActivity::class.java))
                        }
                        R.id.profileChange -> {
                            profileChange(this@uiSetup)
                        }
                        R.id.boldText -> {
                            textToSend.addText("[b][/b]")
                        }
                        R.id.underlineText -> {
                            textToSend.addText("[u][/u]")
                        }
                        R.id.italicText -> {
                            textToSend.addText("[i][/i]")
                        }
                        R.id.colorText -> {
                            textToSend.addText("[color=][/color]", 8)
                        }
                        R.id.bigText -> {
                            textToSend.addText("[big][/big]", 5)
                        }
                        R.id.smallText -> {
                            textToSend.addText("[small][/small]", 7)
                        }
                        R.id.supText -> {
                            textToSend.addText("[sup][/sup]", 5)
                        }
                        R.id.subText -> {
                            textToSend.addText("[sub][/sub]", 5)
                        }
                        R.id.strikethroughText -> {
                            textToSend.addText("[s][/s]")
                        }
                    }
                }

                override fun onSheetDismissed(
                    bottomSheet: BottomSheetMenuDialogFragment,
                    `object`: Any?,
                    dismissEvent: Int
                ) {
                    textToSend.requestAndShowKeyboard()
                }

                override fun onSheetShown(
                    bottomSheet: BottomSheetMenuDialogFragment,
                    `object`: Any?
                ) {

                }
            })

        helpButton.setOnClickListener {
            sheet.show(this@MainActivity.supportFragmentManager)
        }

        textToSend.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {}
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                GlobalScope.launch {
                    //val isTyping = !p0.isNullOrBlank() && !p0.startsWith("/pm ")
                    //sendAction(TypingIndicator(isTyping).toAction())
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