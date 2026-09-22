package com.localplay.app.core.player

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
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

class PlayerRepository(context: Context) {

    private val appContext = context.applicationContext
    private val scope      = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private val _crossfadeConfig = MutableStateFlow(CrossfadeConfig())
    val crossfadeConfig: StateFlow<CrossfadeConfig> = _crossfadeConfig.asStateFlow()

    private val _queue = mutableListOf<TrackEntity>()
    private var _queueIndex = 0

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var positionTickJob: Job? = null
    private var pendingCommand: (() -> Unit)? = null

    // Direct service reference for volume control
    private var exoPlayer: androidx.media3.exoplayer.ExoPlayer? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            if (binder is LocalPlaybackService.LocalBinder) {
                exoPlayer = binder.getPlayer()
            }
        }
        override fun onServiceDisconnected(name: ComponentName) {
            exoPlayer = null
        }
    }

    // Crossfade runtime
    private var crossfadeRampJob: Job? = null
    private var isCrossfading      = false
    private var crossfadeTriggered = false

    // ── Lifecycle ─────────────────────────────────────────────────────────

    fun connect() {
        // 1. Bind for direct ExoPlayer volume access
        val serviceIntent = Intent(appContext, LocalPlaybackService::class.java)
        appContext.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)

        // 2. Connect MediaController for transport commands
        val token = SessionToken(
            appContext,
            ComponentName(appContext, LocalPlaybackService::class.java)
        )
        controllerFuture = MediaController.Builder(appContext, token).buildAsync()
        controllerFuture?.addListener({
            try {
                controller = controllerFuture?.get()
                controller?.addListener(playerListener)
                startPositionTicker()
                pendingCommand?.invoke()
                pendingCommand = null
            } catch (e: Exception) {
                _state.value = _state.value.copy(currentTrack = null, isPlaying = false)
            }
        }, MoreExecutors.directExecutor())
    }

    fun disconnect() {
        positionTickJob?.cancel()
        crossfadeRampJob?.cancel()
        controller?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controller = null
        try { appContext.unbindService(serviceConnection) } catch (_: Exception) {}
        exoPlayer = null
    }

    // ── Crossfade config ──────────────────────────────────────────────────

    fun setCrossfadeDuration(ms: Long) {
        _crossfadeConfig.value = _crossfadeConfig.value.copy(
            crossfadeDurationMs = ms.coerceIn(0L, 12_000L)
        )
    }

    // ── Commands ──────────────────────────────────────────────────────────

    fun playQueue(tracks: List<TrackEntity>, startIndex: Int = 0) {
        if (controller == null) { pendingCommand = { playQueue(tracks, startIndex) }; return }
        _queue.clear(); _queue.addAll(tracks)
        _queueIndex = startIndex.coerceIn(0, tracks.lastIndex)
        controller!!.setMediaItems(
            tracks.map { LocalPlaybackService.mediaItemFrom(it) }, _queueIndex, 0L
        )
        controller!!.prepare(); controller!!.play()
        resetCrossfadeState(); pushState()
    }

    fun playOrPause() { controller?.let { if (it.isPlaying) it.pause() else it.play() } }
    fun seekTo(ms: Long)   { resetCrossfadeState(); controller?.seekTo(ms) }
    fun skipToNext()       { resetCrossfadeState(); controller?.seekToNextMediaItem() }
    fun skipToPrevious()   {
        resetCrossfadeState()
        controller?.let { if (it.currentPosition > 3_000L) it.seekTo(0L) else it.seekToPreviousMediaItem() }
    }
    fun setShuffleEnabled(on: Boolean) { controller?.shuffleModeEnabled = on; pushState() }
    fun setRepeatMode(mode: RepeatMode) {
        controller?.repeatMode = when (mode) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
        }; pushState()
    }
    fun playNext(track: TrackEntity) {
        val c = controller ?: return
        val at = (c.currentMediaItemIndex + 1).coerceAtMost(c.mediaItemCount)
        c.addMediaItem(at, LocalPlaybackService.mediaItemFrom(track))
        _queue.add(at.coerceAtMost(_queue.size), track); pushState()
    }
    fun addToQueue(track: TrackEntity) {
        controller?.addMediaItem(LocalPlaybackService.mediaItemFrom(track))
        _queue.add(track); pushState()
    }

    // ── Crossfade ─────────────────────────────────────────────────────────

    private fun checkCrossfadeTiming(positionMs: Long, durationMs: Long) {
        val config = _crossfadeConfig.value
        if (!config.isEnabled || durationMs <= 0L) return

        val timeRemaining   = durationMs - positionMs
        val leadWindowStart = config.crossfadeDurationMs + config.mixingLabelLeadMs

        when {
            // Lead window: show Mixing badge only
            timeRemaining in config.crossfadeDurationMs..leadWindowStart -> {
                if (!isCrossfading) { isCrossfading = true; pushState() }
            }
            // Fade window: trigger volume ramp once
            timeRemaining in 1L until config.crossfadeDurationMs -> {
                if (!isCrossfading) { isCrossfading = true; pushState() }
                if (!crossfadeTriggered) {
                    crossfadeTriggered = true
                    triggerVolumeCrossfade(config.crossfadeDurationMs)
                }
            }
            // Outside window
            else -> {
                if (isCrossfading) { isCrossfading = false; pushState() }
            }
        }
    }

    /**
     * Volume ramp crossfade using the real ExoPlayer instance (obtained
     * via service binding — MediaController doesn't expose volume).
     *
     * Ramp out current track: 1.0 → 0.0 over [fadeDurationMs]
     * Skip to next track while silent, then ramp in: 0.0 → 1.0
     * Steps every 50 ms = smooth 20 fps volume curve, lightweight on Exynos 850.
     */
    private fun triggerVolumeCrossfade(fadeDurationMs: Long) {
        crossfadeRampJob?.cancel()
        crossfadeRampJob = scope.launch {
            val player = exoPlayer ?: run {
                // Service not yet bound — fall back to instant skip
                controller?.seekToNextMediaItem()
                resetCrossfadeState()
                return@launch
            }
            val steps     = (fadeDurationMs / 50L).coerceAtLeast(2L)
            val stepDelay = fadeDurationMs / steps

            // Fade out
            for (i in 0..steps) {
                player.volume = 1f - (i.toFloat() / steps)
                delay(stepDelay)
            }
            player.volume = 0f

            // Advance to next track
            if (controller?.hasNextMediaItem() == true) {
                controller?.seekToNextMediaItem()
                controller?.prepare()
                controller?.play()
                _queueIndex = (controller?.currentMediaItemIndex ?: _queueIndex)
                    .coerceIn(0, _queue.lastIndex)
            }

            // Small gap so next track buffers before we ramp up
            delay(120L)

            // Fade in
            for (i in 0..steps) {
                player.volume = i.toFloat() / steps
                delay(stepDelay)
            }
            player.volume = 1f

            // Done
            isCrossfading      = false
            crossfadeTriggered = false
            pushState()
        }
    }

    private fun resetCrossfadeState() {
        crossfadeRampJob?.cancel()
        isCrossfading      = false
        crossfadeTriggered = false
        exoPlayer?.volume  = 1f
        pushState()
    }

    // ── Internal ──────────────────────────────────────────────────────────

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) = pushState()
        override fun onMediaItemTransition(item: androidx.media3.common.MediaItem?, reason: Int) {
            _queueIndex = controller?.currentMediaItemIndex ?: 0
            if (!crossfadeTriggered) resetCrossfadeState()
            pushState()
        }
        override fun onShuffleModeEnabledChanged(enabled: Boolean) = pushState()
        override fun onRepeatModeChanged(repeatMode: Int)           = pushState()
        override fun onPlaybackStateChanged(state: Int)             = pushState()
    }

    private fun startPositionTicker() {
        positionTickJob?.cancel()
        positionTickJob = scope.launch {
            while (true) {
                val c = controller
                if (c?.isPlaying == true) {
                    val pos = c.currentPosition
                    val dur = _queue.getOrNull(_queueIndex)?.durationMs
                        ?: c.duration.takeIf { it > 0 } ?: 0L
                    checkCrossfadeTiming(pos, dur)
                    pushState()
                }
                delay(500L)
            }
        }
    }

    private fun pushState() {
        val c     = controller
        val track = if (_queue.isNotEmpty() && _queueIndex in _queue.indices)
            _queue[_queueIndex] else null
        _state.value = PlayerState(
            currentTrack   = track,
            isPlaying      = c?.isPlaying ?: false,
            positionMs     = c?.currentPosition ?: 0L,
            durationMs     = track?.durationMs ?: (c?.duration?.takeIf { it > 0 } ?: 0L),
            shuffleEnabled = c?.shuffleModeEnabled ?: false,
            repeatMode     = when (c?.repeatMode) {
                Player.REPEAT_MODE_ONE -> RepeatMode.ONE
                Player.REPEAT_MODE_ALL -> RepeatMode.ALL
                else                   -> RepeatMode.OFF
            },
            queue         = _queue.toList(),
            queueIndex    = _queueIndex,
            isCrossfading = isCrossfading
        )
    }
}
