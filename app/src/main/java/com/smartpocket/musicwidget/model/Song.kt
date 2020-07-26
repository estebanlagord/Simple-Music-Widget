package com.smartpocket.musicwidget.model

import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import org.apache.commons.lang3.StringUtils
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*

const val ALBUM_ART_PATH = "content://media/external/audio/albumart/"

class Song(private val id: Long,
           val title: String,
           val artist: String,
           val duration: Long,
           val albumId: Long)
    : Serializable {

    val albumArtPath: String = ALBUM_ART_PATH + albumId

    fun getDurationStr(): String {
        val df = SimpleDateFormat("mm:ss", Locale.US)
        return df.format(Date(duration))
    }

    fun getURI(): Uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)

    fun getAlbumArtURI(): Uri = Uri.parse("$ALBUM_ART_PATH/$albumId")

    val isUnknownArtist: Boolean = StringUtils.isBlank(artist)
            || artist.equals("<unknown>", ignoreCase = true)

    override fun toString() = "$title - $artist - ${getDurationStr()}"

}