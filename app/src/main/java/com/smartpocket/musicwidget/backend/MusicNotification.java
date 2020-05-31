package com.smartpocket.musicwidget.backend;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

import com.smartpocket.musicwidget.MusicWidget;
import com.smartpocket.musicwidget.R;
import com.smartpocket.musicwidget.service.MusicService;

public class MusicNotification {
    private Notification notification;
    private final NotificationManager manager;
    private final int notificationID;
    private final Context context;
    private final String channelId;

    public MusicNotification(Context context, int notificationID, String title, String artist, boolean isShuffleOn, String channelId) {
        this.notificationID = notificationID;
        this.context = context;
        this.channelId = channelId;
        manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notification = getNotificationBuilder(title, artist, isShuffleOn, true).build();
    }

    private NotificationCompat.Builder getNotificationBuilder(String title, String artist, boolean isShuffleOn, boolean isPlaying) {
        Intent notificationIntent = new Intent(context, MusicService.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
        int shuffleIcon = isShuffleOn ? R.drawable.shuffle_on : R.drawable.shuffle_off;
        int playPauseIcon = isPlaying ? R.drawable.ic_pause_white_36dp : R.drawable.ic_play_arrow_white_36dp;

        return new NotificationCompat.Builder(context, channelId)
                // Show controls on lock screen even when user hides sensitive content.
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                // Add media control buttons that invoke intents in your media service
                .addAction(shuffleIcon, null, MusicWidget.getPendingIntent(context, MusicWidget.ACTION_SHUFFLE))
                .addAction(R.drawable.ic_skip_previous_white_36dp, null, MusicWidget.getPendingIntent(context, MusicWidget.ACTION_PREVIOUS))
                .addAction(playPauseIcon, null, MusicWidget.getPendingIntent(context, MusicWidget.ACTION_PLAY_PAUSE))
                .addAction(R.drawable.ic_skip_next_white_36dp, null, MusicWidget.getPendingIntent(context, MusicWidget.ACTION_NEXT))
                .addAction(R.drawable.ic_stop_white_36dp, null, MusicWidget.getPendingIntent(context, MusicWidget.ACTION_STOP))
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(1, 2, 3))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.ic_stat_name)
                .setTicker(title)
                .setWhen(System.currentTimeMillis())
                .setContentTitle(title)
                .setOngoing(true)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setContentText(artist);
    }


    public Notification getNotification() {
        return notification;
    }

    public void update(String title, String artist, boolean isPlaying, boolean isShuffleOn) {
        notification = getNotificationBuilder(title, artist, isShuffleOn, isPlaying).build();
        manager.notify(notificationID, notification);
    }
}
