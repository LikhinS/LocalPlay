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

/**
 * Foreground service owning the ExoPlayer instance.
 *
 * Crossfade is implemented in PlayerRepository via volume ramp on this
 * player's volume property. ExoPlayer.volume is a simple float [0,1]
 * that the audio mixer applies per-stream — cheap on Exynos 850.
 */
class LocalPlaybackService : MediaSessionService() {

    lateinit var player: ExoPlayer   // internal — accessed by PlayerRepository
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

        fun mediaItemFrom(
            track: com.localplay.app.core.database.entity.TrackEntity
        ): MediaItem =
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
