package com.smartpocket.musicwidget.backend

import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import android.util.Log

class SongListLoader(private val context: Context) {
    private val TAG = "SongListLoader"
    private var cur: Cursor? = null// Query failed...

    //Some audio may be explicitly marked as not being music
    fun getCursor(): Cursor? {
        Log.d(TAG, "Querying media...")

        //Some audio may be explicitly marked as not being music
        val selection = MusicLoader.DEFAULT_SELECTION
        val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.ALBUM_ID
        )
        var localCur = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                MusicLoader.DEFAULT_SORT_ORDER)

        Log.d(TAG, "Query finished. " + if (cur == null) "Returned NULL." else "Returned a cursor.")
        if (localCur == null) {
            // Query failed...
            Log.e(TAG, "Failed to retrieve music: cursor is null")
        } else if (localCur.count == 0) {
            Log.e(TAG, "Failed to retrieve music: no music found")
            localCur.close()
            localCur = null
        } else {
            Log.d(TAG, "Done querying media. SongListLoader is ready.")
        }
        cur = localCur
        return cur
    }

    fun getFilteredCursor(constraint: CharSequence): Cursor? {
        Log.d(TAG, "Querying media for filter...")

        //Some audio may be explicitly marked as not being music
        val selection = (MusicLoader.DEFAULT_SELECTION + " and "
                + "( " + MediaStore.Audio.Media.ARTIST + " LIKE ? or "
                + MediaStore.Audio.Media.TITLE + " LIKE ? )")
        val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.ALBUM_ID
        )
        cur = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection, arrayOf("%$constraint%", "%$constraint%"),
                MusicLoader.DEFAULT_SORT_ORDER)

        Log.d(TAG, "Query for filter finished. " + if (cur == null) "Returned NULL." else "Returned a cursor.")
        if (cur == null) {
            // Query failed...
            Log.e(TAG, "Failed to retrieve music: cursor is null")
        } else {
            Log.d(TAG, "Done querying media. SongListLoader is ready.")
        }
        return cur
    }
}