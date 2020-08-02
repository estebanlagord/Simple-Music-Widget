package com.smartpocket.musicwidget.musicplayer

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.MediaPlayer.OnCompletionListener
import android.os.Build
import android.util.Log
import com.smartpocket.musicwidget.model.Song
import java.io.IOException

class MusicPlayer(private val context: Context) : OnCompletionListener {

    private val TAG = "Music Player"
    private var player: MediaPlayer? = null
    private var onMusicCompletionListener: MusicPlayerCompletionListener? = null

    fun isPlaying() = player?.isPlaying == true
    fun isPaused() = player?.isPlaying == false
    fun isStopped() = player == null

    @Throws(IllegalArgumentException::class,
            SecurityException::class,
            IllegalStateException::class,
            IOException::class)
    fun setSong(song: Song, forcePlay: Boolean) {
        val wasPlaying = isPlaying()
        var localPlayer = player
        if (localPlayer == null) {
            localPlayer = MediaPlayer()
            player = localPlayer
        } else {
            localPlayer.reset()
        }
        with(localPlayer) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                setAudioAttributes(AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build())
            } else {
                setAudioStreamType(AudioManager.STREAM_MUSIC)
            }
            setDataSource(context, song.getURI())
            setOnPreparedListener { mp ->
                if (forcePlay || wasPlaying)
                        mp.start()
            }
            setOnCompletionListener(this@MusicPlayer)
            setOnErrorListener { mp, what, extra ->
                Log.e(TAG, "Playback error received - what: $what - extra: $extra")
                // reset the player, then nullify player to trigger the creation of a new object
                mp.reset()
                mp.release()
                player = null
                onCompletion(mp) // skip to the next song
                true
            }
            prepareAsync()
        }
        Log.d(TAG, "Changed song to: " + song.title)
    }

    fun play() {
        checkNotNull(player) { "Must call setSong() before calling play()" }
        player?.start()
    }

    fun pause() {
        if (isPlaying()) {
            player?.pause()
            Log.d(TAG, "Music paused")
        }
    }

    fun stop() {
        val localPlayer = player
        if (localPlayer != null) {
            if (localPlayer.isPlaying) {
                localPlayer.stop()
            }
            localPlayer.reset()
            localPlayer.release()
            player = null
        }
        Log.d(TAG, "Music Stopped")
    }

    /**
     * Register a callback to be invoked when the end of a song has been reached during playback
     */
    fun setOnCompletionListener(listener: MusicPlayerCompletionListener?) {
        onMusicCompletionListener = listener
    }

    override fun onCompletion(mp: MediaPlayer) {
        Log.d(TAG, "Song finished playing")
        try {
            onMusicCompletionListener?.onMusicCompletion()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun getPosition(): Long = player?.currentPosition?.toLong() ?: 0

}