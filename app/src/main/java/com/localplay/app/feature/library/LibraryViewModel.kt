package com.localplay.app.feature.library
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.localplay.app.core.database.entity.TrackEntity
import com.localplay.app.core.repository.LibraryRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
class LibraryViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = LibraryRepository(app)
    sealed class ScanState { object Idle:ScanState(); object Scanning:ScanState(); data class Done(val count:Int):ScanState(); data class Error(val msg:String):ScanState() }
    private val _scan = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scan.asStateFlow()
    val allTracks:StateFlow<List<TrackEntity>> = repo.observeAllTracks().stateIn(viewModelScope,SharingStarted.WhileSubscribed(5_000), emptyList())
    val recentlyAdded:StateFlow<List<TrackEntity>> = repo.observeRecentlyAdded().stateIn(viewModelScope,SharingStarted.WhileSubscribed(5_000), emptyList())
    val recentlyPlayed:StateFlow<List<TrackEntity>> = repo.observeRecentlyPlayed().stateIn(viewModelScope,SharingStarted.WhileSubscribed(5_000), emptyList())
    fun scanLibrary() {
        if (_scan.value is ScanState.Scanning) return
        viewModelScope.launch {
            _scan.value = ScanState.Scanning
            try { repo.scanLibrary(getApplication()); _scan.value = ScanState.Done(repo.getTrackCount()) }
            catch(e:Exception) { _scan.value = ScanState.Error(e.message ?: "Scan failed") }
        }
    }
}
