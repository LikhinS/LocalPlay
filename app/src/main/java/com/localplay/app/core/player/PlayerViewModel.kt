package com.localplay.app.core.player

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.localplay.app.core.database.entity.TrackEntity
import com.localplay.app.core.player.model.PlayerState
import com.localplay.app.core.player.model.RepeatMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * Thin ViewModel over PlayerRepository.
 * Survives configuration changes (rotation, dark-mode switch) so the
 * MediaController connection isn't torn down and re-created on every
 * recomposition — which would cause audio glitches.
 */
class PlayerViewModel(app: Application) : AndroidViewModel(app) {

    val repository = PlayerRepository(app)

    val playerState: StateFlow<PlayerState> = repository.state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlayerState())

    init {
        repository.connect()
    }

    override fun onCleared() {
        repository.disconnect()
        super.onCleared()
    }

    // ── Delegate commands ─────────────────────────────────────────────────

    fun playQueue(tracks: List<TrackEntity>, startIndex: Int = 0) =
        repository.playQueue(tracks, startIndex)

    fun playOrPause()                     = repository.playOrPause()
    fun seekTo(positionMs: Long)          = repository.seekTo(positionMs)
    fun skipToNext()                      = repository.skipToNext()
    fun skipToPrevious()                  = repository.skipToPrevious()
    fun setShuffleEnabled(on: Boolean)    = repository.setShuffleEnabled(on)
    fun setRepeatMode(mode: RepeatMode)   = repository.setRepeatMode(mode)
    fun playNext(track: TrackEntity)      = repository.playNext(track)
    fun addToQueue(track: TrackEntity)    = repository.addToQueue(track)
}
