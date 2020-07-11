package com.smartpocket.musicwidget.model

import android.content.ContentUris
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import org.apache.commons.lang3.StringUtils
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*

class Song(private val id: Long,
           val title: String,
           val artist: String,
           val duration: Long,
           val albumId: Long)
    : Serializable {

    var albumArtPath: String? = null

    var albumArt: Bitmap? = null
        get() {
            if (field == null && albumArtPath != null) {
                field = BitmapFactory.decodeFile(albumArtPath)
            }
            return field
        }
        private set

    fun getDurationStr(): String {
        val df = SimpleDateFormat("mm:ss", Locale.US)
        return df.format(Date(duration))
    }

    fun getURI(): Uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)

    val isUnknownArtist: Boolean = StringUtils.isBlank(artist)
            || artist.equals("<unknown>", ignoreCase = true)

    override fun toString() = "$title - $artist - ${getDurationStr()}"

}