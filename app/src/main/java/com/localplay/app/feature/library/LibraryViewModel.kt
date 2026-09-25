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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SortOrder { TITLE, ARTIST, ALBUM, DATE_ADDED }

class LibraryViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = LibraryRepository(app)

    sealed class ScanState {
        object Idle : ScanState()
        object Scanning : ScanState()
        data class Done(val count: Int) : ScanState()
        data class Error(val msg: String) : ScanState()
    }

    private val _scan = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scan.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.TITLE)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    val allTracks: StateFlow<List<TrackEntity>> = _sortOrder
        .flatMapLatest { sort ->
            when (sort) {
                SortOrder.TITLE      -> repo.observeAllTracks()
                SortOrder.ARTIST     -> repo.observeAllByArtist()
                SortOrder.ALBUM      -> repo.observeAllByAlbum()
                SortOrder.DATE_ADDED -> repo.observeRecentlyAdded()
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val recentlyAdded: StateFlow<List<TrackEntity>> = repo.observeRecentlyAdded()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val recentlyPlayed: StateFlow<List<TrackEntity>> = repo.observeRecentlyPlayed()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setSortOrder(order: SortOrder) { _sortOrder.value = order }

    fun scanLibrary() {
        if (_scan.value is ScanState.Scanning) return
        viewModelScope.launch {
            _scan.value = ScanState.Scanning
            try { repo.scanLibrary(getApplication()); _scan.value = ScanState.Done(repo.getTrackCount()) }
            catch (e: Exception) { _scan.value = ScanState.Error(e.message ?: "Scan failed") }
        }
    }
}
