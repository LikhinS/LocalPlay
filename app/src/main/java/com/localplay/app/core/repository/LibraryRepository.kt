package com.localplay.app.core.repository

import android.content.Context
import com.localplay.app.core.database.LocalPlayDatabase
import com.localplay.app.core.database.entity.TrackEntity
import com.localplay.app.core.scanner.LocalMediaScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Single source of truth for the local music library.
 *
 * The UI only talks to this class — never to the DAO or scanner directly.
 * Flows returned here come from Room so the UI updates automatically
 * when a scan completes, without any manual "refresh" trigger.
 */
class LibraryRepository(context: Context) {

    private val db  = LocalPlayDatabase.getInstance(context)
    private val dao = db.trackDao()

    // ── Scan ─────────────────────────────────────────────────────────────

    /**
     * Run a full library scan and persist results.
     *
     * Strategy:
     *  1. Load IDs already in Room (fast: indexed primary key query).
     *  2. Pass those IDs to the scanner so it skips re-probing known files.
     *  3. Upsert all found tracks in one Room transaction.
     *  4. Delete stale tracks (files deleted from storage since last scan).
     *
     * This entire function runs on IO dispatcher — safe to call from a
     * ViewModel coroutine launched on viewModelScope.
     */
    suspend fun scanLibrary(context: Context) = withContext(Dispatchers.IO) {
        val knownIds = dao.getAllIds().toSet()

        val scanned = LocalMediaScanner.scan(context, knownIds)

        if (scanned.isEmpty()) return@withContext

        // For tracks we already know (id in knownIds), the scanner returned
        // FormatInfo.UNKNOWN (sentinel). Merge to keep the stored format fields.
        val existingMap: Map<Long, TrackEntity> = if (knownIds.isNotEmpty()) {
            // Load only the rows we need to merge — not the whole DB.
            scanned
                .filter { it.id in knownIds }
                .mapNotNull { dao.getById(it.id) }
                .associateBy { it.id }
        } else emptyMap()

        val toInsert = scanned.map { scannedTrack ->
            val existing = existingMap[scannedTrack.id]
            if (existing != null) {
                // Keep format fields + play stats from DB; update everything else
                scannedTrack.copy(
                    sampleRateHz  = existing.sampleRateHz,
                    bitDepth      = existing.bitDepth,
                    channelCount  = existing.channelCount,
                    isLossless    = existing.isLossless,
                    isAtmos       = existing.isAtmos,
                    playCount     = existing.playCount,
                    lastPlayedAt  = existing.lastPlayedAt
                )
            } else {
                scannedTrack
            }
        }

        // Single batch write — one SQLite transaction / one fsync on eMMC
        dao.upsertAll(toInsert)

        // Remove tracks whose files no longer exist on storage
        val scannedIds = scanned.map { it.id }
        dao.deleteStale(scannedIds)
    }

    // ── Observe (for UI) ─────────────────────────────────────────────────

    fun observeAllTracks(): Flow<List<TrackEntity>>      = dao.observeAllByTitle()
    fun observeRecentlyAdded(): Flow<List<TrackEntity>>  = dao.observeRecentlyAdded()
    fun observeRecentlyPlayed(): Flow<List<TrackEntity>> = dao.observeRecentlyPlayed()
    fun observeMostPlayed(): Flow<List<TrackEntity>>     = dao.observeMostPlayed()
    fun observeAlbums(): Flow<List<String>>              = dao.observeAlbums()
    fun observeArtists(): Flow<List<String>>             = dao.observeArtists()

    fun observeAlbumTracks(album: String): Flow<List<TrackEntity>>   = dao.observeAlbumTracks(album)
    fun observeArtistTracks(artist: String): Flow<List<TrackEntity>> = dao.observeArtistTracks(artist)
    fun search(query: String): Flow<List<TrackEntity>>               = dao.search(query)

    // ── Stats ─────────────────────────────────────────────────────────────

    suspend fun getTrackCount(): Int = dao.count()

    suspend fun recordPlay(trackId: Long) {
        dao.recordPlay(trackId, System.currentTimeMillis())
    }
}
