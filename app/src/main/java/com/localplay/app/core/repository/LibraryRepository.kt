package com.localplay.app.core.repository

import android.content.Context
import com.localplay.app.core.database.LocalPlayDatabase
import com.localplay.app.core.database.entity.TrackEntity
import com.localplay.app.core.scanner.LocalMediaScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class LibraryRepository(context: Context) {
    private val db  = LocalPlayDatabase.getInstance(context)
    private val dao = db.trackDao()

    suspend fun scanLibrary(context: Context) = withContext(Dispatchers.IO) {
        val knownIds = dao.getAllIds().toSet()
        val scanned  = LocalMediaScanner.scan(context, knownIds)
        if (scanned.isEmpty()) return@withContext

        val existingMap = scanned.filter { it.id in knownIds }
            .mapNotNull { dao.getById(it.id) }.associateBy { it.id }

        val toInsert = scanned.map { t ->
            val ex = existingMap[t.id]
            if (ex != null) t.copy(
                sampleRateHz = ex.sampleRateHz, bitDepth = ex.bitDepth,
                channelCount = ex.channelCount, isLossless = ex.isLossless,
                isAtmos = ex.isAtmos, playCount = ex.playCount, lastPlayedAt = ex.lastPlayedAt
            ) else t
        }
        dao.upsertAll(toInsert)
        dao.deleteStale(scanned.map { it.id })
    }

    fun observeAllTracks(): Flow<List<TrackEntity>>      = dao.observeAllByTitle()
    fun observeRecentlyAdded(): Flow<List<TrackEntity>>  = dao.observeRecentlyAdded()
    fun observeRecentlyPlayed(): Flow<List<TrackEntity>> = dao.observeRecentlyPlayed()
    fun observeMostPlayed(): Flow<List<TrackEntity>>     = dao.observeMostPlayed()
    suspend fun getTrackCount(): Int                     = dao.count()
    suspend fun recordPlay(trackId: Long)                = dao.recordPlay(trackId, System.currentTimeMillis())
}
