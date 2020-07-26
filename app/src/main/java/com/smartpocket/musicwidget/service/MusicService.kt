package com.smartpocket.musicwidget.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.database.CursorIndexOutOfBoundsException
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver
import com.smartpocket.musicwidget.MusicWidget
import com.smartpocket.musicwidget.R
import com.smartpocket.musicwidget.activities.ConfigurationActivity
import com.smartpocket.musicwidget.activities.needsToRequestPermissions
import com.smartpocket.musicwidget.backend.AlbumArtLoader
import com.smartpocket.musicwidget.backend.MusicLoader
import com.smartpocket.musicwidget.backend.MusicNotificationBuilder
import com.smartpocket.musicwidget.model.Song
import com.smartpocket.musicwidget.musicplayer.MusicPlayer
import com.smartpocket.musicwidget.musicplayer.MusicPlayerCompletionListener
import kotlinx.coroutines.*
import org.koin.core.KoinComponent
import org.koin.core.inject
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

class MusicService : MediaBrowserServiceCompat(), MusicPlayerCompletionListener, KoinComponent {

    private val musicLoader: MusicLoader by inject()
    private val albumArtLoader: AlbumArtLoader by inject()
    private val job = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + job)
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var player: MusicPlayer
    private var currFlipperState = ViewFlipperState.STOPPED
    private val defaultAlbumArt: Bitmap by lazy { BitmapFactory.decodeResource(resources, R.drawable.album_white) }
    private val notificationManager: NotificationManager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
    private val notificationBuilder: MusicNotificationBuilder by lazy { MusicNotificationBuilder(this, channelId) }

    internal enum class ViewFlipperState {
        STOPPED, PLAYING
    }

    override fun onCreate() {
        super.onCreate()
        /* let's wait until the debugger attaches */
        //android.os.Debug.waitForDebugger();
        Log.d(TAG, "onCreate()")
        startForeground(ONGOING_NOTIFICATION_ID, NotificationCompat.Builder(this, channelId)
                .setContentTitle("")
                .setContentText("").build())

        player = MusicPlayer(this).also {
            it.setOnCompletionListener(this)
        }

        // Create a MediaSessionCompat
        mediaSession = MediaSessionCompat(applicationContext, TAG).also {
            // Set an initial PlaybackState with ACTION_PLAY, so media buttons can start the player
            it.setPlaybackState(getPlaybackStateBuilder().build())

            // MySessionCallback() has methods that handle callbacks from a media controller
            it.setCallback(MySessionCallback(applicationContext))
            it.isActive = true

            // Set the session's token so that client activities can communicate with it.
            sessionToken = it.sessionToken
        }
        listenForPhoneCalls()
    }

    private fun getPlaybackStateBuilder() = PlaybackStateCompat.Builder()
            .setActions(PlaybackStateCompat.ACTION_PLAY
                    or PlaybackStateCompat.ACTION_STOP
                    or PlaybackStateCompat.ACTION_PAUSE
                    or PlaybackStateCompat.ACTION_PLAY_PAUSE
                    or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                    or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                    or PlaybackStateCompat.ACTION_SET_SHUFFLE_MODE)

    private fun listenForPhoneCalls() {
        (getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager?)?.listen(object : PhoneStateListener() {
            private val wasPlayingBeforeCall = AtomicBoolean(false)
            override fun onCallStateChanged(state: Int, phoneNumber: String) {
                try {
                    when (state) {
                        TelephonyManager.CALL_STATE_RINGING, TelephonyManager.CALL_STATE_OFFHOOK ->
                            if (player.isPlaying()) {
                                wasPlayingBeforeCall.set(true)
                                pauseMusic()
                            }
                        TelephonyManager.CALL_STATE_IDLE ->
                            if (wasPlayingBeforeCall.getAndSet(false)) {
                                playMusic()
                            }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }, PhoneStateListener.LISTEN_CALL_STATE)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isRunning = true
        if (intent == null || intent.action == null) {
            return Service.START_STICKY
        }
        if (needsToRequestPermissions()) {
            openConfigActivity()
        } else {
            processStartCommand(intent)
            MediaButtonReceiver.handleIntent(mediaSession, intent) //TODO ???
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun openConfigActivity() =
            startActivity(Intent(this, ConfigurationActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))

    private fun processStartCommand(intent: Intent) {
        try {
            when (intent.action) {
                MusicWidget.ACTION_PLAY_PAUSE ->
                    if (player.isPlaying()) pauseMusic()
                    else playMusic()
                MusicWidget.ACTION_STOP -> stopMusic()
                MusicWidget.ACTION_NEXT -> nextSong()
                MusicWidget.ACTION_PREVIOUS -> previousSong()
                MusicWidget.ACTION_SHUFFLE -> toggleShuffle()
                MusicWidget.ACTION_JUMP_TO -> jumpTo(intent.extras!!["song"] as Song)
            }
        } catch (e: CursorIndexOutOfBoundsException) {
            Toast.makeText(this, R.string.toast_no_music_found, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun toggleShuffle() {
        musicLoader.toggleShuffle()

        // update the shuffle icon
        if (player.isStopped()) {
            stopMusic()
        } else {
            val song = musicLoader.getCurrent()
            updateUI(song, player.isPlaying())
        }
        val msg = if (musicLoader.isShuffleOn) {
            R.string.toast_shuffle_on
        } else {
            R.string.toast_shuffle_off
        }
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        Log.d(TAG, "DESTROY SERVICE")
        isRunning = false
        if (!player.isStopped()) stopMusic()
        mediaSession.release()
        super.onDestroy()
    }

    private fun updateUI(song: Song?, isPlaying: Boolean) {
        // Update widget
        coroutineScope.launch {
            var loadedAlbumArt: Bitmap? = null
            withContext(Dispatchers.IO) {
                loadedAlbumArt = song?.let { albumArtLoader.getAlbumArt(song) }
            }
            val albumArt = loadedAlbumArt ?: defaultAlbumArt

            // We need to use the RemoteViews generated by the MusicWidget, class to make sure we preserve the pending intents for the buttons.
            // Otherwise the widget's buttons can stop responding to touch events.
            val remoteViews = MusicWidget.getRemoteViews(this@MusicService)
            currFlipperState = if (song != null) {
                setMetadata(song, albumArt)
                remoteViews.setTextViewText(R.id.textViewTitle, song.title)
                remoteViews.setTextViewText(R.id.textViewArtist, song.artist)
                remoteViews.setTextViewText(R.id.textViewDuration, song.getDurationStr())
                if (loadedAlbumArt != null)
                    remoteViews.setImageViewBitmap(R.id.ivAlbumArt, loadedAlbumArt)
                else
                    remoteViews.setImageViewResource(R.id.ivAlbumArt, R.drawable.ic_launcher)

                if (currFlipperState == ViewFlipperState.STOPPED) {
                    remoteViews.setDisplayedChild(R.id.viewFlipper, ViewFlipperState.PLAYING.ordinal)
                }
                ViewFlipperState.PLAYING
            } else {
                if (currFlipperState == ViewFlipperState.PLAYING) {
                    remoteViews.setDisplayedChild(R.id.viewFlipper, ViewFlipperState.STOPPED.ordinal)
                }
                ViewFlipperState.STOPPED
            }
            val playPauseIconRes = if (isPlaying) R.drawable.ic_pause_white_36dp else R.drawable.ic_play_arrow_white_36dp
            remoteViews.setImageViewResource(R.id.button_play_pause, playPauseIconRes)

            val isShuffleOn = musicLoader.isShuffleOn
            val shuffleIconRes = if (isShuffleOn) R.drawable.shuffle_on else R.drawable.shuffle_off
            remoteViews.setImageViewResource(R.id.button_shuffle, shuffleIconRes)

            val thisWidget = ComponentName(this@MusicService, MusicWidget::class.java)
            val manager = AppWidgetManager.getInstance(this@MusicService)
            manager.updateAppWidget(thisWidget, remoteViews)

            // Create/Update a notification, to run the service in foreground
            if (song != null) {
                val notification = notificationBuilder
                        .build(song, albumArt, isPlaying, isShuffleOn, mediaSession.sessionToken)
                notificationManager.notify(ONGOING_NOTIFICATION_ID, notification)
            }
        }
    }

    // If earlier version channel ID is not used
    // https://developer.android.com/reference/android/support/v4/app/NotificationCompat.Builder.html#NotificationCompat.Builder(android.content.Context)
    private val channelId: String by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel("music_widget_service",
                    "Music playback controls")
        } else {
            // If earlier version channel ID is not used
            // https://developer.android.com/reference/android/support/v4/app/NotificationCompat.Builder.html#NotificationCompat.Builder(android.content.Context)
            "Default Channel"
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String, channelName: String): String {
        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
        channel.lightColor = Color.BLUE
        channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        notificationManager.createNotificationChannel(channel)
        return channelId
    }

    @Throws(IOException::class)
    private fun playMusic() {
        Log.d(TAG, "PLAY")
        val song = musicLoader.getCurrent()
        var position: Long = 0
        if (player.isPaused()) {
            player.play()
            position = player.getPosition()
        } else {
            player.setSong(song, true)
        }
        setPlaybackState(position, PlaybackStateCompat.STATE_PLAYING)
        updateUI(song, true)
        Log.i("Music Service", "Playing: " + song.title)
    }

    private fun setPlaybackState(position: Long, statePlaying: Int) =
            mediaSession.setPlaybackState(getPlaybackStateBuilder()
                    .setState(statePlaying, position, PLAYBACK_SPEED)
                    .build())

    private fun setMetadata(song: Song, albumArt: Bitmap) =
            mediaSession.setMetadata(MediaMetadataCompat.Builder()
                    .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArt)
                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.artist)
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.title)
                    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, song.duration)
                    .build())

    private fun pauseMusic() {
        Log.d(TAG, "PAUSE")
        if (player.isPlaying()) {
            val song = musicLoader.getCurrent()
            updateUI(song, false)
            player.pause()
            Log.d(TAG, "Music paused")
            setPlaybackState(player.getPosition(), PlaybackStateCompat.STATE_PAUSED)
        }
    }

    private fun stopMusic() {
        Log.d(TAG, "STOP MUSIC")
        isRunning = false
        setPlaybackState(player.getPosition(), PlaybackStateCompat.STATE_STOPPED)
        player.stop()
        updateUI(null, false)
        musicLoader.close()
        stopForeground(true)
        stopSelf()
    }

    @Throws(IOException::class)
    private fun nextSong() {
        Log.d(TAG, "NEXT SONG")
        val wasPlaying = player.isPlaying()
        val nextSong = musicLoader.getNext()
        player.setSong(nextSong, false)
        setPlaybackState(0, if (wasPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED)
        updateUI(nextSong, wasPlaying)
    }

    @Throws(IOException::class)
    private fun previousSong() {
        Log.d(TAG, "PREVIOUS SONG")
        val wasPlaying = player.isPlaying()
        val prevSong = musicLoader.getPrevious()
        player.setSong(prevSong, false)
        setPlaybackState(0, if (wasPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED)
        updateUI(prevSong, wasPlaying)
    }

    @Throws(IOException::class)
    private fun jumpTo(song: Song) {
        musicLoader.jumpTo(song)
        playMusic()
    }

    @Throws(IOException::class)
    override fun onMusicCompletion() {
        nextSong()
        player.play()
        val song = musicLoader.getCurrent()
        updateUI(song, true)
    }

    override fun onBind(intent: Intent): IBinder? = null

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot? = null

    override fun onLoadChildren(parentId: String, result: Result<List<MediaBrowserCompat.MediaItem>>) {}

    companion object {
        @JvmField
        var isRunning = false
        private const val TAG = "Music Service"
        private const val ONGOING_NOTIFICATION_ID = 1
        private const val PLAYBACK_SPEED = 1.0f
    }
}