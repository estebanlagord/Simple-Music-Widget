package com.smartpocket.musicwidget.backend

import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.provider.MediaStore
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.smartpocket.musicwidget.model.Song
import java.util.concurrent.ConcurrentHashMap

class AlbumArtLoader(val context: Context) {

    private val TAG = javaClass.simpleName
    private val contentResolver = context.contentResolver
    private val albumArtCache = ConcurrentHashMap<Long, String>()

    fun cacheAllAlbumArt() {
//        Log.d(TAG, "Caching album art for all - started")
        val projection = arrayOf(MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ALBUM_ART)
        val cur: Cursor? = contentResolver.query(
                MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                projection, null, null, null)

        if (cur != null) {
            val idColumnIndex = cur.getColumnIndex(MediaStore.Audio.Albums._ID)
            val pathColumnIndex = cur.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART)
            var path: String?
            var id: Long

            while (cur.moveToNext()) {
                id = cur.getLong(idColumnIndex)
                path = cur.getString(pathColumnIndex)
                albumArtCache[id] = path ?: ""
            }
            cur.close()
        }
//        Log.d(TAG, "Caching album art for all - finished")
    }

    private fun getAlbumArtPath(song: Song): String? {
        if (albumArtCache.containsKey(song.albumId)) {
            Log.d(TAG, "Found album art path in cache for: $song")
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
            Log.d(TAG, "Retrieved album art path for ${song.artist} - ${song.title}: $result")
            return result
        }
    }

    fun getAlbumArt(song: Song): Bitmap? =
            try {
                Glide.with(context)
//                    .load(bitmap)
                        .asBitmap()
                        .load(song.albumArtPath)
//                        .fallback(fallbackRes)
//                        .error(fallbackRes)
                        .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                        .submit()
                        .get()
            } catch (e: Exception) {
//                e.printStackTrace()
                null
            }
}