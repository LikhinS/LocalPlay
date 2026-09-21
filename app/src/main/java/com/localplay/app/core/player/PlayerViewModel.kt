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

class PlayerViewModel(app: Application) : AndroidViewModel(app) {

    val repository = PlayerRepository(app)

    val playerState: StateFlow<PlayerState> = repository.state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlayerState())

    val crossfadeConfig: StateFlow<CrossfadeConfig> = repository.crossfadeConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CrossfadeConfig())

    init { repository.connect() }

    override fun onCleared() {
        repository.disconnect()
        super.onCleared()
    }

    fun playQueue(tracks: List<TrackEntity>, startIndex: Int = 0) =
        repository.playQueue(tracks, startIndex)

    fun playOrPause()                   = repository.playOrPause()
    fun seekTo(ms: Long)                = repository.seekTo(ms)
    fun skipToNext()                    = repository.skipToNext()
    fun skipToPrevious()                = repository.skipToPrevious()
    fun setShuffleEnabled(on: Boolean)  = repository.setShuffleEnabled(on)
    fun setRepeatMode(mode: RepeatMode) = repository.setRepeatMode(mode)
    fun playNext(track: TrackEntity)    = repository.playNext(track)
    fun addToQueue(track: TrackEntity)  = repository.addToQueue(track)
    fun setCrossfadeDuration(ms: Long)  = repository.setCrossfadeDuration(ms)
}
