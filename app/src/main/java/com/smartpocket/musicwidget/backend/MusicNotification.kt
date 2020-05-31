package com.smartpocket.musicwidget.backend

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.smartpocket.musicwidget.MusicWidget
import com.smartpocket.musicwidget.R
import com.smartpocket.musicwidget.service.MusicService

class MusicNotification(private val context: Context,
                        private val notificationID: Int,
                        title: String,
                        artist: String,
                        isShuffleOn: Boolean,
                        private val channelId: String) {

    var notification: Notification = getNotificationBuilder(title, artist, isShuffleOn, true).build()
    private val manager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private fun getNotificationBuilder(title: String, artist: String, isShuffleOn: Boolean, isPlaying: Boolean): NotificationCompat.Builder {
        val notificationIntent = Intent(context, MusicService::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0)
        val shuffleIcon = if (isShuffleOn) R.drawable.shuffle_on else R.drawable.shuffle_off
        val playPauseIcon = if (isPlaying) R.drawable.ic_pause_white_36dp else R.drawable.ic_play_arrow_white_36dp

        return NotificationCompat.Builder(context, channelId) // Show controls on lock screen even when user hides sensitive content.
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Add media control buttons that invoke intents in your media service
                .addAction(shuffleIcon, null, MusicWidget.getPendingIntent(context, MusicWidget.ACTION_SHUFFLE))
                .addAction(R.drawable.ic_skip_previous_white_36dp, null, MusicWidget.getPendingIntent(context, MusicWidget.ACTION_PREVIOUS))
                .addAction(playPauseIcon, null, MusicWidget.getPendingIntent(context, MusicWidget.ACTION_PLAY_PAUSE))
                .addAction(R.drawable.ic_skip_next_white_36dp, null, MusicWidget.getPendingIntent(context, MusicWidget.ACTION_NEXT))
                .addAction(R.drawable.ic_stop_white_36dp, null, MusicWidget.getPendingIntent(context, MusicWidget.ACTION_STOP))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.ic_stat_name)
                .setTicker(title)
                .setWhen(System.currentTimeMillis())
                .setContentTitle(title)
                .setContentText(artist)
                .setOngoing(true)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(1, 2, 3))
    }

    fun update(title: String, artist: String, isPlaying: Boolean, isShuffleOn: Boolean) {
        notification = getNotificationBuilder(title, artist, isShuffleOn, isPlaying).build()
        manager.notify(notificationID, notification)
    }
}