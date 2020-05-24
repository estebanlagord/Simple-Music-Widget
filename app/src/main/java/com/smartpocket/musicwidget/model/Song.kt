package com.smartpocket.musicwidget.model

import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import org.apache.commons.lang3.time.FastDateFormat
import java.io.Serializable
import java.util.*

class Song(val id: Long,
           val title: String?,
           val artist: String?,
           val duration: Long)
    : Serializable {

    companion object {
        @JvmField
        val dateFormat: FastDateFormat = FastDateFormat.getInstance("mm:ss")
    }

    val durationStr: String by lazy {
        dateFormat.format(Date(duration))
    }

    val isUnknownArtist by lazy {
        artist.isNullOrBlank() || artist.equals("<unknown>", ignoreCase = true)
    }

    fun getURI(): Uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)

    override fun toString(): String {
        return "$title - $artist - $durationStr"
    }

}