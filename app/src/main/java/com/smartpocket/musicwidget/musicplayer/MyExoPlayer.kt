package com.smartpocket.musicwidget.musicplayer

import android.content.Context
import android.os.Handler
import android.util.Log
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.RenderersFactory
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.audio.AudioRendererEventListener
import com.google.android.exoplayer2.audio.MediaCodecAudioRenderer
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector
import com.google.android.exoplayer2.metadata.MetadataOutput
import com.google.android.exoplayer2.text.TextOutput
import com.google.android.exoplayer2.video.VideoRendererEventListener
import com.smartpocket.musicwidget.model.Song

private const val TAG = "My Exo Player"

class MyExoPlayer(val context: Context) : IMusicPlayer, Player.EventListener {

    private var listener: MusicPlayerCompletionListener? = null
    private var player: SimpleExoPlayer? = null
    private val audioOnlyRenderersFactory =
            RenderersFactory { handler: Handler?,
                               _: VideoRendererEventListener?,
                               audioListener: AudioRendererEventListener?,
                               _: TextOutput?,
                               _: MetadataOutput? ->
                arrayOf(MediaCodecAudioRenderer(context, MediaCodecSelector.DEFAULT, handler, audioListener))
            }

    override fun isPlaying(): Boolean = player?.isPlaying ?: false

    override fun isPaused(): Boolean =
            player?.playbackState == Player.STATE_READY
                    && player?.playWhenReady?.not() ?: false

    override fun isStopped() = player?.playbackState == Player.STATE_ENDED
            || player == null

    override fun setSong(song: Song, forcePlay: Boolean) {
        val wasPlaying = isPlaying()
        var localPlayer: SimpleExoPlayer? = player
        if (localPlayer == null) {
            localPlayer = SimpleExoPlayer.Builder(context, audioOnlyRenderersFactory).build()
            localPlayer.addListener(this)
            player = localPlayer
        }

        with(localPlayer) {
            setMediaItem(MediaItem.fromUri(song.getURI()))
            prepare()
            playWhenReady = forcePlay || wasPlaying
        }
        Log.d(TAG, "Changed song to: " + song.title)
    }

    override fun play() {
        checkNotNull(player) { "Must call setSong() before calling play()" }
        player?.play()
        Log.d(TAG, "Playing music")
    }

    override fun pause() {
        player?.pause()
        Log.d(TAG, "Music paused")
    }

    override fun stop() {
        player?.let {
            it.stop()
            it.release()
            player = null
            Log.d(TAG, "Music Stopped")
        }
    }

    override fun getPosition(): Long = player?.currentPosition ?: 0

    override fun setOnCompletionListener(listener: MusicPlayerCompletionListener) {
        this.listener = listener
    }

    override fun onPlaybackStateChanged(@Player.State state: Int) {
        if (state == Player.STATE_ENDED) {
            Log.d(TAG, "Song finished playing")
            listener?.onMusicCompletion()
        }
    }
}