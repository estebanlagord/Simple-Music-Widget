package com.smartpocket.musicwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.RemoteViews
import com.smartpocket.musicwidget.activities.SongListActivity
import com.smartpocket.musicwidget.service.MusicService

class MusicWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        Log.d(TAG, "Widget's onUpdate()")
        associateIntents(context)
    }

    private fun associateIntents(context: Context) {
        try {
            val remoteViews = getRemoteViews(context)

            // Push update for this widget to the home screen
            val thisWidget = ComponentName(context, MusicWidget::class.java)
            AppWidgetManager.getInstance(context).updateAppWidget(thisWidget, remoteViews)
        } catch (ignored: Exception) {
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        Log.d(TAG, "Deleting widget")
        super.onDeleted(context, appWidgetIds)
        val oService = Intent(context, MusicService::class.java)
        context.stopService(oService)
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "Widget received action: $action")
        if (ACTION_PLAY_PAUSE == action
                || ACTION_NEXT == action
                || (ACTION_STOP == action && MusicService.isRunning)
                || ACTION_PREVIOUS == action
                || ACTION_SHUFFLE == action) {

            val serviceIntent = Intent(context, MusicService::class.java)
            serviceIntent.action = action
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } else {
            super.onReceive(context, intent)
        }
    }

    companion object {
        private const val TAG = "Music Widget"
        const val ACTION_PLAY_PAUSE = "com.smartpocket.musicwidget.play_pause"
        const val ACTION_STOP = "com.smartpocket.musicwidget.stop"
        const val ACTION_NEXT = "com.smartpocket.musicwidget.next"
        const val ACTION_PREVIOUS = "com.smartpocket.musicwidget.previous"
        const val ACTION_SHUFFLE = "com.smartpocket.musicwidget.shuffle"
        const val ACTION_JUMP_TO = "com.smartpocket.musicwidget.jump_to"

        fun getRemoteViews(context: Context): RemoteViews {
            val remoteViews = RemoteViews(context.packageName, R.layout.widget)

            // For Play/Pause button
            remoteViews.setOnClickPendingIntent(R.id.button_play_pause, getPendingIntent(context, ACTION_PLAY_PAUSE))

            // For Stop button
            remoteViews.setOnClickPendingIntent(R.id.button_stop, getPendingIntent(context, ACTION_STOP))

            // For Previous button
            remoteViews.setOnClickPendingIntent(R.id.button_prev, getPendingIntent(context, ACTION_PREVIOUS))

            // For Next button
            remoteViews.setOnClickPendingIntent(R.id.button_next, getPendingIntent(context, ACTION_NEXT))

            // For Shuffle button
            remoteViews.setOnClickPendingIntent(R.id.button_shuffle, getPendingIntent(context, ACTION_SHUFFLE))

            // For Song List activity
            val intent = Intent(context, SongListActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            val pendIntentSongList = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
            remoteViews.setOnClickPendingIntent(R.id.viewFlipper, pendIntentSongList)
            return remoteViews
        }

        fun getPendingIntent(context: Context?, action: String?): PendingIntent {
            val intent = Intent(context, MusicWidget::class.java)
            intent.action = action
            return PendingIntent.getBroadcast(context, 0, intent, 0)
        }
    }
}