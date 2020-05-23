package com.smartpocket.musicwidget.backend

import com.smartpocket.musicwidget.model.Song

interface SongClickListener {

    fun onSongSelected(song: Song)
}