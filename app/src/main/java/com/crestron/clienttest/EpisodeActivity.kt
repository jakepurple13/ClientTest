package com.crestron.clienttest

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
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
import kotlinx.android.synthetic.main.activity_episode.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class EpisodeActivity : AppCompatActivity() {

    private val client = HttpClient()
    private lateinit var adapter: EpisodeAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_episode)

        adapter = EpisodeAdapter(arrayListOf(), this, this)
        episode_list.adapter = adapter

        val dividerItemDecoration =
            DividerItemDecoration(
                episode_list.context,
                (episode_list.layoutManager as LinearLayoutManager).orientation
            )
        episode_list.addItemDecoration(dividerItemDecoration)

        GlobalScope.launch {
            getEpisodeInformation(intent.getStringExtra("episode_info"))
        }
    }

    private suspend fun getEpisodeInformation(url: String?) {
        if (url != null) {
            val p = url.split("/").last { !it.isBlank() }

            val finalText = when {
                url.contains("putlocker") -> "p"
                url.contains("gogoanime") -> "g"
                url.contains("animetoon") -> "a"
                else -> ""
            } + p
            val s = client.get<String>("/api/user/nsi/$finalText.json") {
                method = HttpMethod.Get
                host = ClientHandler.host
                port = 8080
            }
            val q = Gson().fromJson<Episode>(s, Episode::class.java)
            Loged.i(q.EpisodeInfo)
            runOnUiThread {
                adapter.setListNotify(q.EpisodeInfo.episodeList.toMutableList() as ArrayList<EpListInfo>)
                Glide.with(this@EpisodeActivity).load(q.EpisodeInfo.image).into(cover_image)
                title_text.text = q.EpisodeInfo.name
                description_text.text = "${q.EpisodeInfo.url}\n\n${q.EpisodeInfo.description}"
            }
        } else {
            Loged.e("Sorry")
        }
    }

    class EpisodeAdapter(
        list: ArrayList<EpListInfo>,
        private val context: Context,
        private val activity: EpisodeActivity
    ) : DragSwipeAdapter<EpListInfo, MainActivity.ViewHolder>(list) {

        data class VideoLink(val videoLink: String)

        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: MainActivity.ViewHolder, position: Int) {
            holder.tv.text = list[position].name
            holder.itemView.setOnClickListener {
                holder.tv.text = "${list[position].name}\nRetrieving"
                GlobalScope.launch {
                    val s = HttpClient().get<String>(
                        "/api/video/${list[position].url.replace(
                            "/",
                            "_"
                        )}.json"
                    ) {
                        method = HttpMethod.Get
                        host = ClientHandler.host
                        port = 8080
                    }
                    Loged.i(s)
                    activity.runOnUiThread {
                        val v = Gson().fromJson<VideoLink>(s, VideoLink::class.java)
                        holder.tv.text = "${list[position].name}\n${v.videoLink}"
                    }

                }
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

    data class Episode(val EpisodeInfo: EpisodeInfo)
    data class EpListInfo(val name: String, val url: String)
    data class EpisodeInfo(
        val name: String,
        val image: String,
        val url: String,
        val description: String,
        val episodeList: List<EpListInfo>
    )
}
