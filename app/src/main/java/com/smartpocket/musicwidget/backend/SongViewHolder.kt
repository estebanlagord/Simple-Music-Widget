package com.smartpocket.musicwidget.backend

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.song_list_row.view.*

class SongViewHolder(view: View, listener: ItemClickListener) : RecyclerView.ViewHolder(view) {

    val title: TextView = view.textTitle
    val artist: TextView = view.textArtist
    val duration: TextView = view.textDuration
    val albumArt: ImageView = view.albumArt

    init {
        view.setOnClickListener {
            listener.onItemClick(adapterPosition)
        }
    }
}