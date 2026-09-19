package com.localplay.app.core.player

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.localplay.app.core.database.entity.TrackEntity
import com.localplay.app.core.player.model.PlayerState
import com.localplay.app.core.player.model.RepeatMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI-facing wrapper around the MediaController that connects to
 * [LocalPlaybackService].
 *
 * Connection race fix: commands that arrive before the MediaController
 * finishes its async build are stored as a [pendingCommand] lambda and
 * replayed immediately once the controller is ready. This prevents the
 * crash that occurred when the user tapped a track before buildAsync()
 * completed (controller was null → NPE in playQueue).
 */
class PlayerRepository(context: Context) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private val _queue = mutableListOf<TrackEntity>()
    private var _queueIndex = 0

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var positionTickJob: Job? = null

    // Stores the last command that arrived before the controller was ready
    private var pendingCommand: (() -> Unit)? = null

    // ── Lifecycle ─────────────────────────────────────────────────────────

    fun connect() {
        val sessionToken = SessionToken(
            appContext,
            ComponentName(appContext, LocalPlaybackService::class.java)
        )
        controllerFuture = MediaController.Builder(appContext, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                controller = controllerFuture?.get()
                controller?.addListener(playerListener)
                startPositionTicker()
                // Replay any command that arrived during the async build
                pendingCommand?.invoke()
                pendingCommand = null
            } catch (e: Exception) {
                // Controller build failed — surface in state so UI can react
                _state.value = _state.value.copy(currentTrack = null, isPlaying = false)
            }
        }, MoreExecutors.directExecutor())
    }

    fun disconnect() {
        positionTickJob?.cancel()
        controller?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controller = null
    }

    // ── Commands ──────────────────────────────────────────────────────────

    fun playQueue(tracks: List<TrackEntity>, startIndex: Int = 0) {
        if (controller == null) {
            // Controller not ready yet — store the command and replay on connect
            pendingCommand = { playQueue(tracks, startIndex) }
            return
        }
        val ctrl = controller!!
        _queue.clear()
        _queue.addAll(tracks)
        _queueIndex = startIndex.coerceIn(0, tracks.lastIndex)

        val mediaItems = tracks.map { LocalPlaybackService.mediaItemFrom(it) }
        ctrl.setMediaItems(mediaItems, _queueIndex, 0L)
        ctrl.prepare()
        ctrl.play()
        pushState()
    }

    fun playOrPause() {
        val ctrl = controller ?: return
        if (ctrl.isPlaying) ctrl.pause() else ctrl.play()
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
    }

    fun skipToNext() {
        controller?.seekToNextMediaItem()
    }

    fun skipToPrevious() {
        val ctrl = controller ?: return
        if (ctrl.currentPosition > 3_000L) ctrl.seekTo(0L)
        else ctrl.seekToPreviousMediaItem()
    }

    fun setShuffleEnabled(enabled: Boolean) {
        controller?.shuffleModeEnabled = enabled
        pushState()
    }

    fun setRepeatMode(mode: RepeatMode) {
        controller?.repeatMode = when (mode) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
        }
        pushState()
    }

    fun playNext(track: TrackEntity) {
        val ctrl = controller ?: return
        val insertAt = (ctrl.currentMediaItemIndex + 1).coerceAtMost(ctrl.mediaItemCount)
        ctrl.addMediaItem(insertAt, LocalPlaybackService.mediaItemFrom(track))
        _queue.add(insertAt.coerceAtMost(_queue.size), track)
        pushState()
    }

    fun addToQueue(track: TrackEntity) {
        controller?.addMediaItem(LocalPlaybackService.mediaItemFrom(track))
        _queue.add(track)
        pushState()
    }

    // ── Internal ──────────────────────────────────────────────────────────

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) = pushState()
        override fun onMediaItemTransition(
            mediaItem: androidx.media3.common.MediaItem?,
            reason: Int
        ) {
            _queueIndex = controller?.currentMediaItemIndex ?: 0
            pushState()
        }
        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) = pushState()
        override fun onRepeatModeChanged(repeatMode: Int) = pushState()
        override fun onPlaybackStateChanged(playbackState: Int) = pushState()
    }

    private fun startPositionTicker() {
        positionTickJob?.cancel()
        positionTickJob = scope.launch {
            while (true) {
                if (controller?.isPlaying == true) pushState()
                delay(500L)
            }
        }
    }

    private fun pushState() {
        val ctrl = controller
        val currentTrack = if (_queue.isNotEmpty() && _queueIndex in _queue.indices)
            _queue[_queueIndex] else null

        _state.value = PlayerState(
            currentTrack   = currentTrack,
            isPlaying      = ctrl?.isPlaying ?: false,
            positionMs     = ctrl?.currentPosition ?: 0L,
            durationMs     = currentTrack?.durationMs
                ?: (ctrl?.duration?.takeIf { it > 0 } ?: 0L),
            shuffleEnabled = ctrl?.shuffleModeEnabled ?: false,
            repeatMode     = when (ctrl?.repeatMode) {
                Player.REPEAT_MODE_ONE -> RepeatMode.ONE
                Player.REPEAT_MODE_ALL -> RepeatMode.ALL
                else                   -> RepeatMode.OFF
            },
            queue          = _queue.toList(),
            queueIndex     = _queueIndex,
            isCrossfading  = false
        )
    }
}
