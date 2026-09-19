package com.localplay.app.core.player

import android.content.ContentUris
import android.content.Intent
import android.os.Build
import android.provider.MediaStore
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Foreground service that owns the ExoPlayer instance.
 *
 * Design notes for Exynos 850:
 * - ExoPlayer is created once and lives for the service lifetime.
 *   Never create/destroy ExoPlayer per-track — that allocates buffers
 *   on every song change and causes perceptible audio gaps.
 * - We use USAGE_MEDIA + CONTENT_TYPE_MUSIC so Android handles audio
 *   focus (phone calls, notifications) automatically.
 * - handleAudioBecomingNoisy = true pauses playback when headphones
 *   are unplugged, matching expected Android audio behaviour.
 * - The MediaSession lets the lock screen, notification, and Bluetooth
 *   buttons all control playback with zero extra code on our side.
 */
class LocalPlaybackService : MediaSessionService() {

    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession

    override fun onCreate() {
        super.onCreate()

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true
            )
            .setHandleAudioBecomingNoisy(true)   // pause on headphone unplug
            .setWakeMode(C.WAKE_MODE_LOCAL)      // keep CPU alive during playback
            .build()

        mediaSession = MediaSession.Builder(this, player)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession =
        mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Stop the service (and the notification) when the user swipes the app away.
        // Change this to pauseAllPlaybackAndStopSelf() if you'd rather keep the
        // notification alive after the task is removed, like Spotify does.
        if (!player.isPlaying) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession.release()
        player.release()
        super.onDestroy()
    }

    companion object {
        /**
         * Build a Media3 MediaItem from a TrackEntity.
         * The URI points at the MediaStore entry so ExoPlayer can open
         * the file through the ContentResolver without needing file-path
         * permissions on Android 10+.
         */
        fun mediaItemFrom(track: com.localplay.app.core.database.entity.TrackEntity): MediaItem {
            val uri = ContentUris.withAppendedId(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
                else
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                track.id
            )
            return MediaItem.Builder()
                .setUri(uri)
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
}
