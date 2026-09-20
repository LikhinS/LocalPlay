package com.localplay.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.localplay.app.core.database.entity.TrackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(tracks: List<TrackEntity>)

    @Query("DELETE FROM tracks WHERE id NOT IN (:currentIds)")
    suspend fun deleteStale(currentIds: List<Long>)

    @Query("UPDATE tracks SET playCount = playCount + 1, lastPlayedAt = :nowMillis WHERE id = :trackId")
    suspend fun recordPlay(trackId: Long, nowMillis: Long)

    @Query("SELECT * FROM tracks ORDER BY title ASC")
    fun observeAllByTitle(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks ORDER BY dateAdded DESC")
    fun observeRecentlyAdded(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks ORDER BY lastPlayedAt DESC LIMIT :limit")
    fun observeRecentlyPlayed(limit: Int = 20): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks ORDER BY playCount DESC LIMIT :limit")
    fun observeMostPlayed(limit: Int = 25): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE album = :album ORDER BY discNumber ASC, trackNumber ASC")
    fun observeAlbumTracks(album: String): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE artist = :artist ORDER BY album ASC, trackNumber ASC")
    fun observeArtistTracks(artist: String): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%' OR album LIKE '%' || :query || '%' ORDER BY title ASC LIMIT 100")
    fun search(query: String): Flow<List<TrackEntity>>

    @Query("SELECT COUNT(*) FROM tracks")
    suspend fun count(): Int

    @Query("SELECT id FROM tracks")
    suspend fun getAllIds(): List<Long>

    @Query("SELECT * FROM tracks WHERE id = :id")
    suspend fun getById(id: Long): TrackEntity?
}
