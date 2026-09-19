package com.localplay.app.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One row per audio file found by the MediaStore scanner.
 *
 * All format-detection fields (isLossless, isAtmos, channelCount, etc.)
 * are populated once at scan time and cached here — we never re-probe
 * a file at playback time, keeping the playback path free of disk I/O.
 *
 * Exynos 850 note: Room on SQLite is fast even on eMMC because we write
 * in a single batch transaction per scan (see LocalMediaScanner). Never
 * insert rows one at a time inside a loop — that causes one fsync per row
 * on eMMC-class storage and makes the scan feel slow.
 */
@Entity(tableName = "tracks")
data class TrackEntity(
    // MediaStore's stable ID for this audio file — use as the primary key
    // so re-scans can upsert (INSERT OR REPLACE) rather than wipe-and-reinsert.
    @PrimaryKey
    val id: Long,

    // ── Basic metadata ──────────────────────────────────────────────────
    val title: String,
    val artist: String,
    val albumArtist: String,
    val album: String,
    val trackNumber: Int,          // 0 if unknown
    val discNumber: Int,           // 0 if unknown
    val year: Int,                 // 0 if unknown
    val durationMs: Long,
    val dateAdded: Long,           // Unix epoch seconds (from MediaStore)
    val dateModified: Long,        // Unix epoch seconds (from MediaStore)

    // ── File info ───────────────────────────────────────────────────────
    val filePath: String,
    val mimeType: String,          // e.g. "audio/flac", "audio/mpeg"
    val fileSizeBytes: Long,

    // ── Audio format (probed by MediaExtractor at scan time) ────────────
    val sampleRateHz: Int,         // e.g. 44100, 48000, 96000
    val bitDepth: Int,             // e.g. 16, 24, 32 — 0 if compressed/unknown
    val channelCount: Int,         // 1=mono 2=stereo 6=5.1 8=7.1
    val bitrateKbps: Int,          // 0 if lossless/unknown

    // ── Derived badges (computed once, stored here) ─────────────────────
    // Lossless: FLAC, ALAC, WAV, AIFF, or any PCM-based codec
    val isLossless: Boolean,
    // Atmos: true if file has E-AC3-JOC/object-audio flag OR channelCount >= 6
    val isAtmos: Boolean,

    // ── Playback stats ──────────────────────────────────────────────────
    val playCount: Int = 0,
    val lastPlayedAt: Long = 0L    // Unix epoch millis; 0 = never played
)
