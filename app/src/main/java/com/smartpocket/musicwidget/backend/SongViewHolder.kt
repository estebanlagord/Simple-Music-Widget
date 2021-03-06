package com.smartpocket.musicwidget.backend

import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.smartpocket.musicwidget.databinding.SongListRowBinding

class SongViewHolder(binding: SongListRowBinding, listener: ItemClickListener)
    : RecyclerView.ViewHolder(binding.root) {

    val title: TextView = binding.textTitle
    val artist: TextView = binding.textArtist
    val duration: TextView = binding.textDuration
    val albumArt: ImageView = binding.albumArt

    init {
        binding.root.setOnClickListener {
            listener.onItemClick(adapterPosition)
        }
    }
}