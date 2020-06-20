package com.smartpocket.musicwidget.service

import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.v4.media.session.MediaSessionCompat
import com.smartpocket.musicwidget.MusicWidget.*

class MySessionCallback(val context: Context) : MediaSessionCompat.Callback() {

    override fun onPlay() = callService(ACTION_PLAY_PAUSE)

    override fun onPause() = callService(ACTION_PLAY_PAUSE)

    override fun onSetShuffleMode(shuffleMode: Int) = callService(ACTION_SHUFFLE)

    override fun onStop() = callService(ACTION_STOP)

    override fun onSkipToNext() = callService(ACTION_NEXT)

    override fun onSkipToPrevious() = callService(ACTION_PREVIOUS)

    private fun callService(action: String) {
        val serviceIntent = Intent(context, MusicService::class.java)
        serviceIntent.action = action

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}