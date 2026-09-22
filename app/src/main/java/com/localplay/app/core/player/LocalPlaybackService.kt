package com.localplay.app.core.player

import android.app.Service
import android.content.ContentUris
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.provider.MediaStore
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.localplay.app.core.database.entity.TrackEntity

/**
 * Foreground service owning the ExoPlayer instance.
 *
 * Exposes a [LocalBinder] so PlayerRepository can bind directly and
 * get the real ExoPlayer for volume control (MediaController is a
 * client-side proxy and doesn't expose volume — binding is required).
 */
class LocalPlaybackService : MediaSessionService() {

    // Direct binder so PlayerRepository can access player.volume
    inner class LocalBinder : Binder() {
        fun getPlayer(): ExoPlayer = player
    }

    private val binder = LocalBinder()
    lateinit var player: ExoPlayer
        private set
    private lateinit var mediaSession: MediaSession

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
        mediaSession = MediaSession.Builder(this, player).build()
    }

    // Return our LocalBinder for direct service binding
    override fun onBind(intent: Intent): IBinder {
        // MediaSessionService.onBind handles the MediaSession intent;
        // for any other intent (our direct bind) return the LocalBinder.
        val sessionBinder = super.onBind(intent)
        return if (sessionBinder != null) sessionBinder else binder
    }

    // Called by MediaSessionService for media session connections
    override fun onGetSession(info: MediaSession.ControllerInfo) = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!player.isPlaying) stopSelf()
    }

    override fun onDestroy() {
        mediaSession.release()
        player.release()
        super.onDestroy()
    }

    companion object {
        private val COLLECTION_URI
            get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            else
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        fun mediaItemFrom(track: TrackEntity): MediaItem =
            MediaItem.Builder()
                .setUri(ContentUris.withAppendedId(COLLECTION_URI, track.id))
                .setMediaId(track.id.toString())
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(track.title)
                        .setArtist(track.artist)
                        .setAlbumTitle(track.album)
                        .setTrackNumber(track.trackNumber)
                        .setDiscNumber(track.discNumber)
                        .build()
                )
                .build()
    }
}
