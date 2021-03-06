package com.smartpocket.musicwidget.backend

import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.futuremind.recyclerviewfastscroll.SectionTitleProvider
import com.smartpocket.musicwidget.R
import com.smartpocket.musicwidget.databinding.SongListRowBinding
import com.smartpocket.musicwidget.model.Song
import org.apache.commons.lang3.StringUtils

class SongCursorRecyclerAdapter(cursor: Cursor?,
                                private val context: Context,
                                private val listener: SongClickListener)
    : CursorRecyclerAdapter<SongViewHolder>(cursor), ItemClickListener, SectionTitleProvider {

    override fun onBindViewHolder(holder: SongViewHolder, cursor: Cursor) {
        holder.apply {
            val song = getSongFromCurrentCursorPos(cursor)
            if (song.isUnknownArtist) {
                artist.visibility = View.GONE
            } else {
                artist.visibility = View.VISIBLE
            }
            artist.text = song.artist
            title.text = song.title
            duration.text = song.getDurationStr()

            Glide.with(context)
                    .load(song.albumArtPath)
                    .override(ALBUM_ART_SIZE)
                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                    .placeholder(R.drawable.album_white)
                    .into(albumArt)
        }
    }

    private fun getSongFromCurrentCursorPos(cursor: Cursor): Song =
            with(cursor) {
                val id = getLong(getColumnIndex(MediaStore.Audio.Media._ID))
                val artist = getString(getColumnIndex(MediaStore.Audio.Media.ARTIST))
                val title = getString(getColumnIndex(MediaStore.Audio.Media.TITLE))
                val duration = getLong(getColumnIndex(MediaStore.Audio.Media.DURATION))
                val albumId = getLong(getColumnIndex(MediaStore.Audio.Media.ALBUM_ID))
                return Song(id, title, artist, duration, albumId)
            }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val binding = SongListRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SongViewHolder(binding, this)
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