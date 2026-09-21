package com.localplay.app.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val artist: String,
    val albumArtist: String,
    val album: String,
    val albumId: Long,          // used to build the albumart URI directly
    val trackNumber: Int,
    val discNumber: Int,
    val year: Int,
    val durationMs: Long,
    val dateAdded: Long,
    val dateModified: Long,
    val filePath: String,
    val mimeType: String,
    val fileSizeBytes: Long,
    val sampleRateHz: Int,
    val bitDepth: Int,
    val channelCount: Int,
    val bitrateKbps: Int,
    val isLossless: Boolean,
    val isAtmos: Boolean,
    val playCount: Int = 0,
    val lastPlayedAt: Long = 0L
)
