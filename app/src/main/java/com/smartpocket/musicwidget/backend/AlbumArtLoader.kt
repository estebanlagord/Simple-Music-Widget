package com.smartpocket.musicwidget.backend

import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import com.smartpocket.musicwidget.model.Song

class AlbumArtLoader(val context: Context) {

    private val contentResolver = context.contentResolver

    fun getAlbumArtPath(song: Song): String? {
        var result: String? = null
        val projection = arrayOf(MediaStore.Audio.Albums.ALBUM_ART)
        val selection = MediaStore.Audio.Albums._ID + " = ?"
        val selectionArgs = arrayOf(song.albumId.toString())
        val cur: Cursor? = contentResolver.query(
                MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                projection, selection, selectionArgs, null)

        if (cur != null) {
            if (cur.moveToFirst()) {
                result = cur.getString(cur.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART))
            }
            cur.close()
        }
        return result
    }

    fun addAlbumArtPath(song: Song) {
        song.albumArtPath = getAlbumArtPath(song)
    }
}