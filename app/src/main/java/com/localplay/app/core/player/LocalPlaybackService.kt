package com.localplay.app.core.player

import android.content.ContentUris
import android.content.Intent
import android.os.Build
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
 * We expose the live ExoPlayer via a companion-object singleton so
 * PlayerRepository can read/write player.volume for the volume-ramp
 * crossfade without needing a service binding.
 *
 * MediaSessionService does not expose onBind() for override — that
 * is why the previous LocalBinder approach caused a compile error.
 * The companion singleton is the correct pattern for this use case.
 */
class LocalPlaybackService : MediaSessionService() {

    private lateinit var mediaSession: MediaSession

    override fun onCreate() {
        super.onCreate()

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()

        // Publish the instance so PlayerRepository can access it
        instance = player

        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(info: MediaSession.ControllerInfo) = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (instance?.isPlaying != true) stopSelf()
    }

    override fun onDestroy() {
        mediaSession.release()
        instance?.release()
        instance = null
        super.onDestroy()
    }

    companion object {
        /**
         * Live ExoPlayer instance. Non-null while the service is running,
         * null before onCreate and after onDestroy.
         * PlayerRepository checks for null before every volume write.
         */
        var instance: ExoPlayer? = null
            private set

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
