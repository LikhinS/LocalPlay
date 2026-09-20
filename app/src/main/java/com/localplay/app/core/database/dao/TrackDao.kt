package com.localplay.app.core.database.dao
import androidx.room.*
import com.localplay.app.core.database.entity.TrackEntity
import kotlinx.coroutines.flow.Flow
@Dao interface TrackDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertAll(tracks: List<TrackEntity>)
    @Query("DELETE FROM tracks WHERE id NOT IN (:currentIds)") suspend fun deleteStale(currentIds: List<Long>)
    @Query("UPDATE tracks SET playCount=playCount+1,lastPlayedAt=:nowMillis WHERE id=:trackId") suspend fun recordPlay(trackId: Long, nowMillis: Long)
    @Query("SELECT * FROM tracks ORDER BY title ASC") fun observeAllByTitle(): Flow<List<TrackEntity>>
    @Query("SELECT * FROM tracks ORDER BY dateAdded DESC") fun observeRecentlyAdded(): Flow<List<TrackEntity>>
    @Query("SELECT * FROM tracks ORDER BY lastPlayedAt DESC LIMIT :limit") fun observeRecentlyPlayed(limit: Int = 20): Flow<List<TrackEntity>>
    @Query("SELECT COUNT(*) FROM tracks") suspend fun count(): Int
    @Query("SELECT id FROM tracks") suspend fun getAllIds(): List<Long>
    @Query("SELECT * FROM tracks WHERE id=:id") suspend fun getById(id: Long): TrackEntity?
}
