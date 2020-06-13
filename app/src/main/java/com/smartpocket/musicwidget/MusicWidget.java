package com.smartpocket.musicwidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.widget.RemoteViews;

import com.smartpocket.musicwidget.activities.SongListActivity;
import com.smartpocket.musicwidget.service.MusicService;

public class MusicWidget extends AppWidgetProvider {

    private static final String TAG = "Music Widget";
    public static final String ACTION_PLAY_PAUSE = "com.smartpocket.musicwidget.play_pause";
    public static final String ACTION_STOP = "com.smartpocket.musicwidget.stop";
    public static final String ACTION_NEXT = "com.smartpocket.musicwidget.next";
    public static final String ACTION_PREVIOUS = "com.smartpocket.musicwidget.previous";
    public static final String ACTION_SHUFFLE = "com.smartpocket.musicwidget.shuffle";
    public static final String ACTION_JUMP_TO = "com.smartpocket.musicwidget.jump_to";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        associateIntents(context);
        Log.d(TAG, "Widget's onUpdate()");
    }

    public static RemoteViews getRemoteViews(Context context) {
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget);

        // For Play/Pause button
        PendingIntent pendingIntentStart = getPendingIntent(context, MusicWidget.ACTION_PLAY_PAUSE);
        remoteViews.setOnClickPendingIntent(R.id.button_play_pause, pendingIntentStart);

        // For Stop button
        PendingIntent pendingIntentStop = getPendingIntent(context, MusicWidget.ACTION_STOP);
        remoteViews.setOnClickPendingIntent(R.id.button_stop, pendingIntentStop);

        // For Previous button
        PendingIntent pendingIntentPrevious = getPendingIntent(context, MusicWidget.ACTION_PREVIOUS);
        remoteViews.setOnClickPendingIntent(R.id.button_prev, pendingIntentPrevious);

        // For Next button
        PendingIntent pendingIntentNext = getPendingIntent(context, MusicWidget.ACTION_NEXT);
        remoteViews.setOnClickPendingIntent(R.id.button_next, pendingIntentNext);

        // For Shuffle button
        PendingIntent pendingIntentShuffle = getPendingIntent(context, MusicWidget.ACTION_SHUFFLE);
        remoteViews.setOnClickPendingIntent(R.id.button_shuffle, pendingIntentShuffle);

        // For Song List activity
        Intent intent = new Intent(context, SongListActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendIntentSongList = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.viewFlipper, pendIntentSongList);

        return remoteViews;
    }

    public static PendingIntent getPendingIntent(Context context, String action) {
        Intent intent = new Intent(context, MusicWidget.class);
        intent.setAction(action);
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }

    private void associateIntents(Context context) {

        try {
            RemoteViews remoteViews = getRemoteViews(context);

            // Push update for this widget to the home screen
            ComponentName thisWidget = new ComponentName(context, MusicWidget.class);
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            manager.updateAppWidget(thisWidget, remoteViews);
        } catch (Exception ignored) {
        }
    }


    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
        Intent oService = new Intent(context, MusicService.class);
        context.stopService(oService);
        Log.d(TAG, "Deleting widget");
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        Log.d(TAG, "Widget received action: " + action);

        if (ACTION_PLAY_PAUSE.equals(action)
                || ACTION_NEXT.equals(action)
                || (ACTION_STOP.equals(action) && MusicService.isRunning)
                || ACTION_PREVIOUS.equals(action)
                || ACTION_SHUFFLE.equals(action)) {

            Intent serviceIntent = new Intent(context, MusicService.class);
            serviceIntent.setAction(action);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
        } else {
            super.onReceive(context, intent);
        }
    }
}
