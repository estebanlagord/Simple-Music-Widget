package com.smartpocket.musicwidget.backend

import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.futuremind.recyclerviewfastscroll.SectionTitleProvider
import com.smartpocket.musicwidget.R
import com.smartpocket.musicwidget.model.Song
import org.apache.commons.lang3.StringUtils

class SongCursorRecyclerAdapter(cursor: Cursor?,
                                private val context: Context,
                                private val albumArtLoader: AlbumArtLoader,
                                private val listener: SongClickListener)
    : CursorRecyclerAdapter<SongViewHolder>(cursor), ItemClickListener, SectionTitleProvider {

    override fun onBindViewHolder(holder: SongViewHolder, cursor: Cursor) {
        val song = getSongFromCurrentCursorPos(cursor)
        if (song.isUnknownArtist) {
            holder.artist.visibility = View.GONE
        } else {
            holder.artist.visibility = View.VISIBLE
        }
        holder.artist.text = song.artist
        holder.title.text = song.title
        holder.duration.text = song.getDurationStr()

        albumArtLoader.addAlbumArtPath(song)
        Glide.with(context)
                .load(song.albumArtPath)
                .fallback(R.drawable.album_white)
                .into(holder.albumArt)
    }

    private fun getSongFromCurrentCursorPos(cursor: Cursor): Song {
        val artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST))
        val title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE))
        val duration = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION))
        val albumId = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID))
        return Song(0, title, artist, duration, albumId)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.song_list_row, parent, false)
        return SongViewHolder(view, this)
    }

    override fun onItemClick(position: Int) {
        check(cursor?.moveToPosition(position) == true) { "couldn't move cursor to position $position" }
        listener.onSongSelected(getSongFromCurrentCursorPos(cursor!!))
    }

    override fun getSectionTitle(position: Int): String {
        var result = ""
        val cursor = cursor
        var artist = cursor?.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST))
        if (artist != null) {
            artist = artist.trim { it <= ' ' }
            if (StringUtils.isNotBlank(artist) && !artist.equals("<unknown>", ignoreCase = true)) {
                result = artist.substring(0, 1)
            }
        }
        return result
    }

}