package com.smartpocket.musicwidget.service;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.CursorIndexOutOfBoundsException;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.smartpocket.musicwidget.MusicWidget;
import com.smartpocket.musicwidget.R;
import com.smartpocket.musicwidget.activities.ConfigurationActivity;
import com.smartpocket.musicwidget.backend.MusicLoader;
import com.smartpocket.musicwidget.backend.MusicNotification;
import com.smartpocket.musicwidget.model.Song;
import com.smartpocket.musicwidget.musicplayer.MusicPlayer;
import com.smartpocket.musicwidget.musicplayer.MusicPlayerCompletionListener;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.smartpocket.musicwidget.activities.ConfigurationActivityKt.needsToRequestPermissions;


public class MusicService extends Service implements MusicPlayerCompletionListener {
    public static boolean isRunning = false;
    private static final String TAG = "Music Service";
    private static final int ONGOING_NOTIFICATION_ID = 1;
    private MusicPlayer player;
    private MusicNotification mNotification;

    @Override
    public void onCreate() {
        /* let's wait until the debugger attaches */
        //android.os.Debug.waitForDebugger();

        Log.d(TAG, "onCreate()");

        player = new MusicPlayer(this);
        player.setOnCompletionListener(this);
        listenForPhoneCalls();
    }

    private void listenForPhoneCalls() {
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        if (telephonyManager != null) {
            telephonyManager.listen(new PhoneStateListener() {
                private AtomicBoolean wasPlayingBeforeCall = new AtomicBoolean(false);

                @Override
                public void onCallStateChanged(int state, String phoneNumber) {
                    try {
                        switch (state) {
                            case TelephonyManager.CALL_STATE_RINGING:
                            case TelephonyManager.CALL_STATE_OFFHOOK:
                                if (player != null && player.isPlaying()) {
                                    wasPlayingBeforeCall.set(true);
                                    pauseMusic();
                                }
                                break;
                            case TelephonyManager.CALL_STATE_IDLE:
                                if (wasPlayingBeforeCall.getAndSet(false)) {
                                    playMusic();
                                }
                                break;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }, PhoneStateListener.LISTEN_CALL_STATE);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        isRunning = true;
        if (intent == null || intent.getAction() == null) {
            return START_STICKY;
        }

        if (needsToRequestPermissions(this)) {
            openConfigActivity();
        } else {
            processStartCommand(intent);
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void openConfigActivity() {
        startActivity(new Intent(this, ConfigurationActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    private void processStartCommand(@NonNull Intent intent) {
        try {
            switch (intent.getAction()) {
                case MusicWidget.ACTION_PLAY_PAUSE:
                    if (player.isPlaying())
                        pauseMusic();
                    else {
                        playMusic();
                    }
                    break;
                case MusicWidget.ACTION_STOP:
                    stopMusic();
                    break;
                case MusicWidget.ACTION_NEXT:
                    nextSong();
                    break;
                case MusicWidget.ACTION_PREVIOUS:
                    previousSong();
                    break;
                case MusicWidget.ACTION_SHUFFLE:
                    toggleShuffle();
                    break;
                case MusicWidget.ACTION_JUMP_TO:
                    Song song = (Song) intent.getExtras().get("song");
                    jumpTo(song);
                    break;
            }
        } catch (CursorIndexOutOfBoundsException e) {
            Toast.makeText(this, R.string.toast_no_music_found, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void toggleShuffle() {
        MusicLoader.getInstance(this).toggleShuffle();

        // update the shuffle icon
        if (player.isStopped()) {
            updateUI(null, null, null, null);
        } else {
            Song song = MusicLoader.getInstance(this).getCurrent();
            updateUI(song.getTitle(), song.getArtist(), song.getDurationStr(), player.isPlaying());
        }

        if (MusicLoader.getInstance(this).isShuffleOn())
            Toast.makeText(this, R.string.toast_shuffle_on, Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(this, R.string.toast_shuffle_off, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "DESTROY SERVICE");
        isRunning = false;
        if (player != null && !player.isStopped())
            stopMusic();

        super.onDestroy();
    }

    private void updateUI(String title, String artist, String duration, Boolean isPlaying) {
        // Update widget

        // We need to use the RemoteViews generated by the MusicWidget, class to make sure we preserve the pending intents for the buttons.
        // Otherwise the widget's buttons can stop responding to touch events.
        RemoteViews remoteViews = MusicWidget.getRemoteViews(this);

        if (title != null && artist != null && duration != null) {
            remoteViews.setViewVisibility(R.id.layoutTextViews, View.VISIBLE);
            remoteViews.setTextViewText(R.id.textViewTitle, title);
            remoteViews.setTextViewText(R.id.textViewArtist, artist);
            remoteViews.setTextViewText(R.id.textViewDuration, duration);
        } else {
            remoteViews.setViewVisibility(R.id.layoutTextViews, View.GONE);
        }


        if (isPlaying != null) {
            if (isPlaying)
                remoteViews.setImageViewResource(R.id.button_play_pause, R.drawable.ic_pause_white_36dp);
            else
                remoteViews.setImageViewResource(R.id.button_play_pause, R.drawable.ic_play_arrow_white_36dp);
        }

        boolean isShuffleOn = MusicLoader.getInstance(this).isShuffleOn();
        if (isShuffleOn)
            remoteViews.setImageViewResource(R.id.button_shuffle, R.drawable.shuffle_on);
        else
            remoteViews.setImageViewResource(R.id.button_shuffle, R.drawable.shuffle_off);

        ComponentName thisWidget = new ComponentName(this, MusicWidget.class);
        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        manager.updateAppWidget(thisWidget, remoteViews);

        // Create/Update a notification, to run the service in foreground
        if (artist != null && title != null) {
            if (mNotification == null) {
                mNotification = new MusicNotification(this, ONGOING_NOTIFICATION_ID, title, artist, isShuffleOn, getChannelId());
                startForeground(ONGOING_NOTIFICATION_ID, mNotification.getNotification());
            } else {
                boolean isPlayingUnboxed = isPlaying != null ? isPlaying : false;
                mNotification.update(title, artist, isPlayingUnboxed, isShuffleOn);
            }
        } else {
            stopForeground(true);
        }
    }

    private String getChannelId() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return createNotificationChannel("music_widget_service",
                    "Music playback controls");
        } else {
            // If earlier version channel ID is not used
            // https://developer.android.com/reference/android/support/v4/app/NotificationCompat.Builder.html#NotificationCompat.Builder(android.content.Context)
            return "Default Channel";
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private String createNotificationChannel(String channelId, String channelName) {
        NotificationChannel channel = new NotificationChannel(channelId,
                channelName, NotificationManager.IMPORTANCE_LOW);
        channel.setLightColor(Color.BLUE);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        NotificationManager service = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        service.createNotificationChannel(channel);
        return channelId;
    }


    private void playMusic() throws IOException {
        Log.d(TAG, "PLAY");
        Song song = MusicLoader.getInstance(this).getCurrent();

        if (player.isPaused()) {
            player.play();
        } else {
            player.setSong(song, true);
        }

        updateUI(song.getTitle(), song.getArtist(), song.getDurationStr(), true);
        Log.i("Music Service", "Playing: " + song.getTitle());
    }


    private void pauseMusic() {
        Log.d(TAG, "PAUSE");
        if (player.isPlaying()) {
            Song song = MusicLoader.getInstance(this).getCurrent();
            updateUI(song.getTitle(), song.getArtist(), song.getDurationStr(), false);

            player.pause();
            Log.d(TAG, "Music paused");
        }
    }


    private void stopMusic() {
        Log.d(TAG, "STOP MUSIC");
        isRunning = false;

        player.stop();
        updateUI(null, null, null, false);
        MusicLoader.getInstance(this).close();

        stopForeground(true);
        stopSelf();
    }

    private void nextSong() throws IOException {
        Log.d(TAG, "NEXT SONG");

        if (player != null) {
            boolean wasPlaying = player.isPlaying();
            Song nextSong = MusicLoader.getInstance(this).getNext();
            player.setSong(nextSong, false);

            updateUI(nextSong.getTitle(), nextSong.getArtist(), nextSong.getDurationStr(), wasPlaying);
        }
    }

    private void previousSong() throws IOException {
        Log.d(TAG, "PREVIOUS SONG");

        if (player != null) {
            boolean wasPlaying = player.isPlaying();
            Song prevSong = MusicLoader.getInstance(this).getPrevious();
            player.setSong(prevSong, false);

            updateUI(prevSong.getTitle(), prevSong.getArtist(), prevSong.getDurationStr(), wasPlaying);
        }
    }

    private void jumpTo(Song song) throws IOException {
        MusicLoader.getInstance(this).jumpTo(song);
        playMusic();
    }

    @Override
    public void onMusicCompletion() throws IOException {
        nextSong();
        player.play();
        Song song = MusicLoader.getInstance(this).getCurrent();
        updateUI(song.getTitle(), song.getArtist(), song.getDurationStr(), true);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
