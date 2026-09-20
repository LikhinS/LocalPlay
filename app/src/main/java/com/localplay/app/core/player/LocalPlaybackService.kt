package com.localplay.app.core.player
import android.content.ContentUris
import android.content.Intent
import android.os.Build
import android.provider.MediaStore
import androidx.media3.common.*
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.*
class LocalPlaybackService : MediaSessionService() {
    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession
    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this)
            .setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), true)
            .setHandleAudioBecomingNoisy(true).build()
        mediaSession = MediaSession.Builder(this, player).build()
    }
    override fun onGetSession(info: MediaSession.ControllerInfo) = mediaSession
    override fun onTaskRemoved(rootIntent: Intent?) { if (!player.isPlaying) stopSelf() }
    override fun onDestroy() { mediaSession.release(); player.release(); super.onDestroy() }
    companion object {
        private val COLLECTION_URI get() =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        fun mediaItemFrom(track: com.localplay.app.core.database.entity.TrackEntity): MediaItem =
            MediaItem.Builder().setUri(ContentUris.withAppendedId(COLLECTION_URI, track.id))
                .setMediaId(track.id.toString())
                .setMediaMetadata(MediaMetadata.Builder().setTitle(track.title).setArtist(track.artist)
                    .setAlbumTitle(track.album).setTrackNumber(track.trackNumber).setDiscNumber(track.discNumber).build())
                .build()
    }
}
