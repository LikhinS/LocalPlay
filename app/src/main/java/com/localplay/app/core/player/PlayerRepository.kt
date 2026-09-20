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
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
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
    private var pendingCommand: (() -> Unit)? = null

    fun connect() {
        val token = SessionToken(appContext, ComponentName(appContext, LocalPlaybackService::class.java))
        controllerFuture = MediaController.Builder(appContext, token).buildAsync()
        controllerFuture?.addListener({
            try {
                controller = controllerFuture?.get()
                controller?.addListener(playerListener)
                startPositionTicker()
                pendingCommand?.invoke(); pendingCommand = null
            } catch(e:Exception) { _state.value = _state.value.copy(currentTrack=null,isPlaying=false) }
        }, MoreExecutors.directExecutor())
    }
    fun disconnect() {
        positionTickJob?.cancel()
        controller?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controller = null
    }
    fun playQueue(tracks: List<TrackEntity>, startIndex: Int = 0) {
        if (controller == null) { pendingCommand = { playQueue(tracks, startIndex) }; return }
        _queue.clear(); _queue.addAll(tracks)
        _queueIndex = startIndex.coerceIn(0, tracks.lastIndex)
        controller!!.setMediaItems(tracks.map { LocalPlaybackService.mediaItemFrom(it) }, _queueIndex, 0L)
        controller!!.prepare(); controller!!.play(); pushState()
    }
    fun playOrPause() { controller?.let { if(it.isPlaying) it.pause() else it.play() } }
    fun seekTo(ms: Long) { controller?.seekTo(ms) }
    fun skipToNext() { controller?.seekToNextMediaItem() }
    fun skipToPrevious() { controller?.let { if(it.currentPosition>3_000L) it.seekTo(0L) else it.seekToPreviousMediaItem() } }
    fun setShuffleEnabled(on: Boolean) { controller?.shuffleModeEnabled = on; pushState() }
    fun setRepeatMode(mode: RepeatMode) {
        controller?.repeatMode = when(mode) { RepeatMode.OFF->Player.REPEAT_MODE_OFF; RepeatMode.ALL->Player.REPEAT_MODE_ALL; RepeatMode.ONE->Player.REPEAT_MODE_ONE }
        pushState()
    }
    fun playNext(track: TrackEntity) {
        val c = controller ?: return
        val at = (c.currentMediaItemIndex+1).coerceAtMost(c.mediaItemCount)
        c.addMediaItem(at, LocalPlaybackService.mediaItemFrom(track))
        _queue.add(at.coerceAtMost(_queue.size), track); pushState()
    }
    fun addToQueue(track: TrackEntity) { controller?.addMediaItem(LocalPlaybackService.mediaItemFrom(track)); _queue.add(track); pushState() }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) = pushState()
        override fun onMediaItemTransition(item: androidx.media3.common.MediaItem?, reason: Int) { _queueIndex = controller?.currentMediaItemIndex ?: 0; pushState() }
        override fun onShuffleModeEnabledChanged(enabled: Boolean) = pushState()
        override fun onRepeatModeChanged(repeatMode: Int) = pushState()
        override fun onPlaybackStateChanged(state: Int) = pushState()
    }
    private fun startPositionTicker() {
        positionTickJob?.cancel()
        positionTickJob = scope.launch { while(true) { if(controller?.isPlaying==true) pushState(); delay(500L) } }
    }
    private fun pushState() {
        val c = controller
        val track = if(_queue.isNotEmpty() && _queueIndex in _queue.indices) _queue[_queueIndex] else null
        _state.value = PlayerState(currentTrack=track, isPlaying=c?.isPlaying?:false,
            positionMs=c?.currentPosition?:0L,
            durationMs=track?.durationMs?:(c?.duration?.takeIf{it>0}?:0L),
            shuffleEnabled=c?.shuffleModeEnabled?:false,
            repeatMode=when(c?.repeatMode){Player.REPEAT_MODE_ONE->RepeatMode.ONE;Player.REPEAT_MODE_ALL->RepeatMode.ALL;else->RepeatMode.OFF},
            queue=_queue.toList(), queueIndex=_queueIndex, isCrossfading=false)
    }
}
