package com.crestron.clienttest

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.programmerbox.dragswipe.DragSwipeAdapter
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.host
import io.ktor.client.request.port
import io.ktor.http.HttpMethod
import kotlinx.android.synthetic.main.activity_show.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class ShowActivity : AppCompatActivity() {

    private val client = HttpClient()
    private lateinit var adapter: ShowAdapter
    private lateinit var showList: List<EpisodeInfo>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show)

        adapter = ShowAdapter(arrayListOf(), this)
        rv.adapter = adapter

        val dividerItemDecoration =
            DividerItemDecoration(rv.context, (rv.layoutManager as LinearLayoutManager).orientation)
        rv.addItemDecoration(dividerItemDecoration)

        GlobalScope.launch {
            getShows()
        }

    }

    data class ShowList(val shows: List<EpisodeInfo>)

    private suspend fun getShows() {
        val s = client.get<String>("/api/user/allEpisodes.json") {
            method = HttpMethod.Get
            host = ClientHandler.host
            port = 8080
        }
        val q = Gson().fromJson<ShowList>(s, ShowList::class.java)
        showList = q.shows
        runOnUiThread {
            adapter.setListNotify(showList as ArrayList<EpisodeInfo>)
            search_text.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(p0: Editable?) {}
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    adapter.setListNotify(showList.filter {
                        it.name.contains(
                            "$p0",
                            ignoreCase = true
                        )
                    } as ArrayList<EpisodeInfo>)
                }
            })
        }
    }

    class ShowAdapter(
        list: ArrayList<EpisodeInfo>,
        private val context: Context
    ) : DragSwipeAdapter<EpisodeInfo, MainActivity.ViewHolder>(list) {

        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: MainActivity.ViewHolder, position: Int) {
            holder.tv.text = list[position].name
            Glide.with(context).load(list[position].image).into(holder.image)
            holder.itemView.setOnClickListener {
                context.startActivity(Intent(context, EpisodeActivity::class.java).apply {
                    putExtra("episode_info", list[position].url)
                })
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainActivity.ViewHolder {
            return MainActivity.ViewHolder(
                LayoutInflater.from(context).inflate(
                    R.layout.message_info,
                    parent,
                    false
                )
            )
        }
    }

}

data class EpisodeInfo(
    val name: String = "",
    val image: String = "",
    val url: String = "",
    val description: String = ""
)
