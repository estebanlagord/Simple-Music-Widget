package com.smartpocket.musicwidget.backend

import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import com.smartpocket.musicwidget.model.Song
import java.util.concurrent.ConcurrentHashMap

class AlbumArtLoader(val context: Context) {

    private val TAG = javaClass.simpleName
    private val contentResolver = context.contentResolver
    private val albumArtCache = ConcurrentHashMap<Long, String>()

    private fun getAlbumArtPath(song: Song): String? {
        if (albumArtCache.containsKey(song.albumId)) {
//            Log.d(TAG, "Found album art path in cache for: $song")
            return albumArtCache[song.albumId]?.ifEmpty { null }
        } else {
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
                    albumArtCache[song.albumId] = result ?: ""
                }
                cur.close()
            }
//        Log.d(TAG, "Getting album art path for ${song.artist} - ${song.title}: $result")
            return result
        }
    }

    fun addAlbumArtPath(song: Song) {
        if (song.albumArtPath.isNullOrBlank()) {
            song.albumArtPath = getAlbumArtPath(song)
        }
    }
}