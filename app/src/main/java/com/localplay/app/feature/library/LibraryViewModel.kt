package com.localplay.app.feature.library

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.localplay.app.core.database.entity.TrackEntity
import com.localplay.app.core.repository.LibraryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for the library/home screens.
 *
 * Uses AndroidViewModel so it can hold an Application context safely
 * (needed to construct LibraryRepository). Never holds a reference to
 * an Activity or View context — those leak.
 *
 * The scan is kicked off from the UI once permission is confirmed,
 * not automatically in init{}, so we don't scan before the user has
 * granted READ_MEDIA_AUDIO.
 */
class LibraryViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = LibraryRepository(app)

    // ── Scan state ────────────────────────────────────────────────────────

    sealed class ScanState {
        object Idle        : ScanState()
        object Scanning    : ScanState()
        data class Done(val trackCount: Int) : ScanState()
        data class Error(val message: String) : ScanState()
    }

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    // ── Library data (Room Flows → StateFlow for Compose) ────────────────

    val allTracks: StateFlow<List<TrackEntity>> = repository
        .observeAllTracks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val recentlyAdded: StateFlow<List<TrackEntity>> = repository
        .observeRecentlyAdded()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val recentlyPlayed: StateFlow<List<TrackEntity>> = repository
        .observeRecentlyPlayed()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── Actions ───────────────────────────────────────────────────────────

    /**
     * Trigger a library scan. Call this once, after the audio permission
     * has been granted. Subsequent app launches will call it again to
     * pick up any new/deleted files — it's idempotent and fast for
     * unchanged libraries because the scanner skips re-probing known IDs.
     */
    fun scanLibrary() {
        if (_scanState.value is ScanState.Scanning) return   // already running

        viewModelScope.launch {
            _scanState.value = ScanState.Scanning
            try {
                repository.scanLibrary(getApplication())
                val count = repository.getTrackCount()
                _scanState.value = ScanState.Done(count)
            } catch (e: Exception) {
                _scanState.value = ScanState.Error(e.message ?: "Scan failed")
            }
        }
    }
}
