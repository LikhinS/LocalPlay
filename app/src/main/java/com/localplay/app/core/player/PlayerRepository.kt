package com.localplay.app.core.player

import android.content.ComponentName
import android.content.Context
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

    // Crossfade runtime state
    private var crossfadeRampJob: Job? = null
    private var isCrossfading = false
    private var crossfadeTriggered = false   // guard: trigger fade only once per track end

    // ── Lifecycle ─────────────────────────────────────────────────────────

    fun connect() {
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
    }

    // ── Crossfade config ──────────────────────────────────────────────────

    fun setCrossfadeDuration(durationMs: Long) {
        _crossfadeConfig.value = _crossfadeConfig.value.copy(
            crossfadeDurationMs = durationMs.coerceIn(0L, 12_000L)
        )
    }

    // ── Commands ──────────────────────────────────────────────────────────

    fun playQueue(tracks: List<TrackEntity>, startIndex: Int = 0) {
        if (controller == null) {
            pendingCommand = { playQueue(tracks, startIndex) }
            return
        }
        _queue.clear()
        _queue.addAll(tracks)
        _queueIndex = startIndex.coerceIn(0, tracks.lastIndex)
        controller!!.setMediaItems(
            tracks.map { LocalPlaybackService.mediaItemFrom(it) },
            _queueIndex, 0L
        )
        controller!!.prepare()
        controller!!.play()
        resetCrossfadeState()
        pushState()
    }

    fun playOrPause() {
        controller?.let { if (it.isPlaying) it.pause() else it.play() }
    }

    fun seekTo(ms: Long) {
        resetCrossfadeState()
        controller?.seekTo(ms)
    }

    fun skipToNext() {
        resetCrossfadeState()
        controller?.seekToNextMediaItem()
    }

    fun skipToPrevious() {
        resetCrossfadeState()
        controller?.let {
            if (it.currentPosition > 3_000L) it.seekTo(0L)
            else it.seekToPreviousMediaItem()
        }
    }

    fun setShuffleEnabled(on: Boolean) { controller?.shuffleModeEnabled = on; pushState() }

    fun setRepeatMode(mode: RepeatMode) {
        controller?.repeatMode = when (mode) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
        }
        pushState()
    }

    fun playNext(track: TrackEntity) {
        val c = controller ?: return
        val at = (c.currentMediaItemIndex + 1).coerceAtMost(c.mediaItemCount)
        c.addMediaItem(at, LocalPlaybackService.mediaItemFrom(track))
        _queue.add(at.coerceAtMost(_queue.size), track)
        pushState()
    }

    fun addToQueue(track: TrackEntity) {
        controller?.addMediaItem(LocalPlaybackService.mediaItemFrom(track))
        _queue.add(track)
        pushState()
    }

    // ── Crossfade ─────────────────────────────────────────────────────────

    /**
     * Called every 500 ms from the position ticker while a track is playing.
     *
     * Timeline (example: 5s crossfade, 1.5s lead):
     *
     *   T-6.5s  show "Mixing" badge (lead window starts)
     *   T-5.0s  begin volume ramp: fade out current track over 5s,
     *           skip to next track + fade in over 5s
     *   T-0.0s  track ends naturally — ExoPlayer auto-advances
     *
     * The volume ramp runs as a coroutine updating ExoPlayer.volume every
     * 50 ms — 100 steps over the fade duration. This is lighter than an
     * AudioProcessor and doesn't require MediaTransformer setup.
     *
     * After the ramp completes we reset volume to 1.0 so the next track
     * plays at full volume.
     */
    private fun checkCrossfadeTiming(positionMs: Long, durationMs: Long) {
        val config = _crossfadeConfig.value
        if (!config.isEnabled || durationMs <= 0L) return

        val timeRemaining = durationMs - positionMs
        val leadWindowStart = config.crossfadeDurationMs + config.mixingLabelLeadMs

        when {
            // Show badge in the lead window
            timeRemaining in config.crossfadeDurationMs..leadWindowStart -> {
                if (!isCrossfading) {
                    isCrossfading = true
                    pushState()
                }
            }

            // Trigger the volume ramp at the fade point (only once)
            timeRemaining in 0L until config.crossfadeDurationMs -> {
                if (!isCrossfading) { isCrossfading = true; pushState() }
                if (!crossfadeTriggered) {
                    crossfadeTriggered = true
                    triggerVolumeCrossfade(config.crossfadeDurationMs)
                }
            }

            // Outside any crossfade window — clear badge if it was showing
            else -> {
                if (isCrossfading) {
                    isCrossfading = false
                    pushState()
                }
            }
        }
    }

    /**
     * Ramps the current track's volume from 1.0 → 0.0 over [fadeDurationMs],
     * seeks to the next track at the midpoint, then ramps 0.0 → 1.0.
     *
     * Uses 50 ms ticks (20 updates/s) — imperceptible on 60 Hz and very
     * light on Exynos 850 (one float write per tick to ExoPlayer's mixer).
     */
    private fun triggerVolumeCrossfade(fadeDurationMs: Long) {
        crossfadeRampJob?.cancel()
        crossfadeRampJob = scope.launch {
            val c = controller ?: return@launch
            val steps      = (fadeDurationMs / 50L).coerceAtLeast(1L)
            val stepDelay  = fadeDurationMs / steps

            // Fade out
            for (i in 0..steps) {
                val vol = 1f - (i.toFloat() / steps.toFloat())
                c.volume = vol.coerceIn(0f, 1f)
                delay(stepDelay)
            }
            c.volume = 0f

            // Advance to the next track while silent
            if (c.hasNextMediaItem()) {
                c.seekToNextMediaItem()
                c.prepare()
                c.play()
            }

            // Fade in
            for (i in 0..steps) {
                val vol = i.toFloat() / steps.toFloat()
                c.volume = vol.coerceIn(0f, 1f)
                delay(stepDelay)
            }
            c.volume = 1f

            // Clear mixing badge after fade completes
            isCrossfading = false
            crossfadeTriggered = false
            pushState()
        }
    }

    private fun resetCrossfadeState() {
        crossfadeRampJob?.cancel()
        isCrossfading      = false
        crossfadeTriggered = false
        controller?.volume = 1f   // restore full volume if interrupted mid-fade
        pushState()
    }

    // ── Internal ──────────────────────────────────────────────────────────

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) = pushState()
        override fun onMediaItemTransition(
            item: androidx.media3.common.MediaItem?,
            reason: Int
        ) {
            _queueIndex = controller?.currentMediaItemIndex ?: 0
            // Only reset if transition was NOT triggered by our crossfade ramp
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
