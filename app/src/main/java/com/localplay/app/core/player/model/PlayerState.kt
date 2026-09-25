package com.localplay.app.core.player.model
import com.localplay.app.core.database.entity.TrackEntity
data class PlayerState(
    val currentTrack: TrackEntity? = null, val isPlaying: Boolean = false,
    val positionMs: Long = 0L, val durationMs: Long = 0L,
    val shuffleEnabled: Boolean = false, val repeatMode: RepeatMode = RepeatMode.OFF,
    val queue: List<TrackEntity> = emptyList(), val queueIndex: Int = 0,
    val isCrossfading: Boolean = false
) {
    val progress: Float get() = if (durationMs > 0) positionMs / durationMs.toFloat() else 0f
}
enum class RepeatMode { OFF, ALL, ONE }
