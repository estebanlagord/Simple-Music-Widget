package com.smartpocket.musicwidget.backend

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.song_list_row.view.*

class SongViewHolder(view: View, listener: ItemClickListener) : RecyclerView.ViewHolder(view) {

    val title = view.textTitle
    val artist = view.textArtist
    val duration = view.textDuration

    init {
        view.setOnClickListener {
            listener.onItemClick(adapterPosition)
        }
    }
}