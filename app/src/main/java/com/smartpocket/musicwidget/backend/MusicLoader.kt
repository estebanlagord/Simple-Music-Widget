package com.smartpocket.musicwidget.backend

import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import android.util.Log
import androidx.preference.PreferenceManager
import com.smartpocket.musicwidget.model.Song
import java.util.concurrent.atomic.AtomicBoolean

class MusicLoader(private val context: Context, private val albumArtLoader: AlbumArtLoader) {

    var isShuffleOn = false
    private var cursor: Cursor? = null
    private val isPrepared = AtomicBoolean(false)

    init {
        prepare()
    }

    @Synchronized
    private fun checkPrepared() {
        if (!isPrepared.getAndSet(true)) {
            prepare()
        }
    }

    private fun prepare() {
        // Check if shuffle mode is on
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        isShuffleOn = prefs.getBoolean(IS_SHUFFLE_ON, false)
        val sortOrder = if (isShuffleOn) "RANDOM()" else DEFAULT_SORT_ORDER
        Log.d(TAG, "Querying media...")

        //Some audio may be explicitly marked as not being music
        val selection = DEFAULT_SELECTION
        val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.ALBUM_ID)
        val cur = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder)
        cursor = cur
        Log.d(TAG, "Query finished. " + if (cur == null) "Returned NULL." else "Returned a cursor.")
        if (cur == null) {
            // Query failed...
            Log.e(TAG, "Failed to retrieve music: cursor is null")
            return
        }

        // Move the cursor to the position of the song which was playing the last time the application was running
        val lastSongTitle = prefs.getString(LAST_SONG_TITLE, null)
        val lastSongArtist = prefs.getString(LAST_SONG_ARTIST, null)
        if (lastSongTitle != null && lastSongArtist != null) {
            // Attempt to restore the cursor to its previous position
            Log.d(TAG, "Searching cursor for: $lastSongTitle - $lastSongArtist")
            while (cur.moveToNext()) {
                val title = cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.TITLE))
                val artist = cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.ARTIST))
                if (title == lastSongTitle && artist == lastSongArtist) {
                    Log.d(TAG, "Song found!")
                    return
                }
            }
            Log.d(TAG, "Song not found")
        }

        // If we are here, either there was no lastSong preference or we can no longer find it. Move to the first position
        if (!cur.moveToFirst()) {
            Log.e(TAG, "Failed to move cursor to first row (no music found).")
            return
        }
        Log.d(TAG, "Done querying media. MusicLoader is ready.")
    }

    fun getCurrent(): Song {
        checkPrepared()
        val cur = cursor!!
        val title = cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.TITLE))
        val artist = cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.ARTIST))
        val duration = cur.getLong(cur.getColumnIndex(MediaStore.Audio.Media.DURATION))
        val id = cur.getLong(cur.getColumnIndex(MediaStore.Audio.Media._ID))
        val albumId = cur.getLong(cur.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID))
        val song = Song(id, title, artist, duration, albumId)
        return song
    }

    fun getNext(): Song {
        checkPrepared()
        val cur = cursor!!
        if (!cur.moveToNext()) cur.moveToFirst()
        return getCurrent()
    }

    fun getPrevious(): Song {
        checkPrepared()
        val cur = cursor!!
        if (!cur.moveToPrevious()) cur.moveToLast()
        return getCurrent()
    }

    fun toggleShuffle() {
        isShuffleOn = isShuffleOn.not()
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(IS_SHUFFLE_ON, isShuffleOn)
                .apply()
        close() // to trigger a new query
    }

    fun jumpTo(song: Song) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit()
                .putString(LAST_SONG_TITLE, song.title)
                .putString(LAST_SONG_ARTIST, song.artist)
                .apply()
        cursor?.close()
        cursor = null
        isPrepared.set(false)
    }

    fun close() {
        val cur = cursor
        if (cur != null) {
            if (cur.count > 0) {
                // Save current song in a Shared Preference
                val title = cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.TITLE))
                val artist = cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.ARTIST))
                val prefs = PreferenceManager.getDefaultSharedPreferences(context)
                prefs.edit()
                        .putString(LAST_SONG_TITLE, title)
                        .putString(LAST_SONG_ARTIST, artist)
                        .apply()
            }
            cur.close()
            cursor = null
        }
        isPrepared.set(false)
    }

    companion object {
        const val DEFAULT_SORT_ORDER = MediaStore.Audio.Media.ARTIST + ", " + MediaStore.Audio.Media.TITLE
        const val DEFAULT_SELECTION = MediaStore.Audio.Media.IS_MUSIC + " != 0 and " +
                MediaStore.Audio.Media.DATA + " NOT LIKE '%/WhatsApp/%'"
        private const val TAG = "Music Loader"
        private const val LAST_SONG_TITLE = "Last Song Title"
        private const val LAST_SONG_ARTIST = "Last Song Artist"
        private const val IS_SHUFFLE_ON = "Is Shuffle On"
    }
}