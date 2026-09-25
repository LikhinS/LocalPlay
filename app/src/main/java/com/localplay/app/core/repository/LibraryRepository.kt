package com.localplay.app.core.repository
import android.content.Context
import com.localplay.app.core.database.LocalPlayDatabase
import com.localplay.app.core.database.entity.TrackEntity
import com.localplay.app.core.scanner.LocalMediaScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
class LibraryRepository(context: Context) {
    private val dao = LocalPlayDatabase.getInstance(context).trackDao()
    suspend fun scanLibrary(context: Context) = withContext(Dispatchers.IO) {
        val knownIds = dao.getAllIds().toSet()
        val scanned = LocalMediaScanner.scan(context, knownIds)
        if (scanned.isEmpty()) return@withContext
        val existingMap = scanned.filter { it.id in knownIds }.mapNotNull { dao.getById(it.id) }.associateBy { it.id }
        dao.upsertAll(scanned.map { t -> existingMap[t.id]?.let { ex ->
            t.copy(sampleRateHz=ex.sampleRateHz, bitDepth=ex.bitDepth, channelCount=ex.channelCount,
                isLossless=ex.isLossless, isAtmos=ex.isAtmos, playCount=ex.playCount, lastPlayedAt=ex.lastPlayedAt)
        } ?: t })
        dao.deleteStale(scanned.map { it.id })
    }
    fun observeAllTracks(): Flow<List<TrackEntity>>      = dao.observeAllByTitle()
    fun observeAllByArtist(): Flow<List<TrackEntity>>    = dao.observeAllByArtist()
    fun observeAllByAlbum(): Flow<List<TrackEntity>>     = dao.observeAllByAlbum()
    fun observeRecentlyAdded(): Flow<List<TrackEntity>>  = dao.observeRecentlyAdded()
    fun observeRecentlyPlayed(): Flow<List<TrackEntity>> = dao.observeRecentlyPlayed()
    suspend fun getTrackCount(): Int = dao.count()
    suspend fun recordPlay(trackId: Long) = dao.recordPlay(trackId, System.currentTimeMillis())
}
