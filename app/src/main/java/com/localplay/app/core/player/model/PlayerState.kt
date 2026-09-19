package com.localplay.app.core.player.model

import com.localplay.app.core.database.entity.TrackEntity

/**
 * Immutable snapshot of everything the UI needs to render playback controls.
 * Emitted as a StateFlow from PlayerRepository so Compose can observe it.
 *
 * Keeping this as a plain data class (no LiveData, no MutableState inside)
 * means the entire UI tree only recomposes when the state actually changes —
 * important for smooth 60fps on Exynos 850.
 */
data class PlayerState(
    val currentTrack: TrackEntity?   = null,
    val isPlaying: Boolean           = false,
    val positionMs: Long             = 0L,
    val durationMs: Long             = 0L,
    val shuffleEnabled: Boolean      = false,
    val repeatMode: RepeatMode       = RepeatMode.OFF,
    val queue: List<TrackEntity>     = emptyList(),
    val queueIndex: Int              = 0,
    // Crossfade state — used by the "Mixing" badge in Phase 6
    val isCrossfading: Boolean       = false
) {
    val progress: Float
        get() = if (durationMs > 0) positionMs / durationMs.toFloat() else 0f

    val hasNext: Boolean
        get() = queueIndex < queue.size - 1 || shuffleEnabled
    val hasPrevious: Boolean
        get() = queueIndex > 0
}

enum class RepeatMode { OFF, ALL, ONE }
