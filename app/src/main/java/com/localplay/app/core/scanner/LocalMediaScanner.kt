package com.localplay.app.core.scanner

import android.content.ContentUris
import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.localplay.app.core.database.entity.TrackEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Scans MediaStore.Audio for every audio file on the device and maps
 * each result into a [TrackEntity] ready for Room insertion.
 *
 * Design decisions for Exynos 850 / eMMC storage:
 *
 * 1. The entire scan runs on Dispatchers.IO (never the main thread).
 * 2. We use a single ContentResolver cursor query with an explicit
 *    projection — no SELECT * — so the OS returns only the columns we need.
 * 3. Format probing (MediaExtractor) is done only for files we haven't
 *    seen before (new IDs since the last scan). Re-scanning known files
 *    just updates their metadata from the cursor without re-probing.
 * 4. The caller (LibraryRepository) writes the results to Room in a single
 *    batch — one SQLite transaction / one fsync — not one insert per row.
 */
object LocalMediaScanner {

    // Columns we actually need — explicit projection is faster than SELECT *
    private val PROJECTION = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.ALBUM_ARTIST,
        MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Media.TRACK,          // encoded as disc*1000 + track on some devices
        MediaStore.Audio.Media.YEAR,
        MediaStore.Audio.Media.DURATION,
        MediaStore.Audio.Media.DATE_ADDED,
        MediaStore.Audio.Media.DATE_MODIFIED,
        MediaStore.Audio.Media.DATA,           // absolute file path
        MediaStore.Audio.Media.MIME_TYPE,
        MediaStore.Audio.Media.SIZE,
        MediaStore.Audio.Media.BITRATE,
    )

    // Only return music (not ringtones, notifications, etc.)
    private const val SELECTION = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

    private val COLLECTION_URI: Uri =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

    /**
     * Returns a list of [TrackEntity] objects for every music file found.
     * Pass [knownIds] (the IDs already in Room) so we can skip re-probing
     * files we've already analysed — only new files get the MediaExtractor pass.
     */
    suspend fun scan(
        context: Context,
        knownIds: Set<Long> = emptySet()
    ): List<TrackEntity> = withContext(Dispatchers.IO) {

        val results = mutableListOf<TrackEntity>()

        val cursor = context.contentResolver.query(
            COLLECTION_URI,
            PROJECTION,
            SELECTION,
            null,
            "${MediaStore.Audio.Media.TITLE} ASC"
        ) ?: return@withContext results

        cursor.use { c ->
            // Cache column indices outside the loop — getColumnIndexOrThrow
            // does a string search every call, so hoisting it saves N lookups.
            val idCol        = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol     = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol    = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumArtCol  = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ARTIST)
            val albumCol     = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val trackCol     = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val yearCol      = c.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
            val durationCol  = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dateAddCol   = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val dateModCol   = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
            val pathCol      = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val mimeCol      = c.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
            val sizeCol      = c.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val bitrateCol   = c.getColumnIndexOrThrow(MediaStore.Audio.Media.BITRATE)

            while (c.moveToNext()) {
                val id       = c.getLong(idCol)
                val rawTrack = c.getInt(trackCol)
                val mime     = c.getString(mimeCol) ?: "audio/mpeg"
                val path     = c.getString(pathCol) ?: continue   // skip if no path

                // Decode packed track/disc number:
                // MediaStore stores as discNumber*1000 + trackNumber
                val trackNumber = rawTrack % 1000
                val discNumber  = rawTrack / 1000

                // Format probing via MediaExtractor — only for files we
                // haven't seen before, to avoid re-probing on every launch.
                val formatInfo: FormatInfo = if (id !in knownIds) {
                    probeFormat(context, id, mime)
                } else {
                    // Already in DB — use a sentinel that signals "skip probe,
                    // the existing DB row has the real values".
                    // LibraryRepository will keep the existing row's format fields.
                    FormatInfo.UNKNOWN
                }

                results.add(
                    TrackEntity(
                        id            = id,
                        title         = c.getString(titleCol) ?: "Unknown Title",
                        artist        = c.getString(artistCol) ?: "Unknown Artist",
                        albumArtist   = c.getString(albumArtCol) ?: "",
                        album         = c.getString(albumCol) ?: "Unknown Album",
                        trackNumber   = trackNumber,
                        discNumber    = discNumber,
                        year          = c.getInt(yearCol),
                        durationMs    = c.getLong(durationCol),
                        dateAdded     = c.getLong(dateAddCol),
                        dateModified  = c.getLong(dateModCol),
                        filePath      = path,
                        mimeType      = mime,
                        fileSizeBytes = c.getLong(sizeCol),
                        sampleRateHz  = formatInfo.sampleRateHz,
                        bitDepth      = formatInfo.bitDepth,
                        channelCount  = formatInfo.channelCount,
                        bitrateKbps   = c.getInt(bitrateCol) / 1000,
                        isLossless    = formatInfo.isLossless,
                        isAtmos       = formatInfo.isAtmos
                    )
                )
            }
        }

        results
    }

    // ────────────────────────────────────────────────────────────────────
    // MediaExtractor format probe
    // ────────────────────────────────────────────────────────────────────

    private data class FormatInfo(
        val sampleRateHz: Int,
        val bitDepth: Int,
        val channelCount: Int,
        val isLossless: Boolean,
        val isAtmos: Boolean
    ) {
        companion object {
            val UNKNOWN = FormatInfo(0, 0, 0, isLossless = false, isAtmos = false)
        }
    }

    /**
     * Uses [MediaExtractor] to read the audio track's format without
     * decoding any frames. This is cheap but still does a file open,
     * so we call it only for genuinely new files.
     */
    private fun probeFormat(context: Context, trackId: Long, mime: String): FormatInfo {
        return try {
            val extractor = MediaExtractor()
            val uri = ContentUris.withAppendedId(COLLECTION_URI, trackId)
            extractor.setDataSource(context, uri, null)

            var info = FormatInfo.UNKNOWN
            for (i in 0 until extractor.trackCount) {
                val fmt = extractor.getTrackFormat(i)
                val trackMime = fmt.getString(MediaFormat.KEY_MIME) ?: continue
                if (!trackMime.startsWith("audio/")) continue

                val sampleRate   = fmt.getIntOrZero(MediaFormat.KEY_SAMPLE_RATE)
                val channels     = fmt.getIntOrZero(MediaFormat.KEY_CHANNEL_COUNT)
                val bitDepth     = fmt.getIntOrZero(MediaFormat.KEY_PCM_ENCODING)
                    .let { pcmEncoding -> pcmEncodingToBitDepth(pcmEncoding) }

                val lossless = isLosslessCodec(trackMime, mime)

                // Atmos: E-AC3-JOC object audio OR multichannel surround (5.1/7.1+)
                val atmos = trackMime.equals("audio/eac3-joc", ignoreCase = true)
                    || channels >= 6

                info = FormatInfo(
                    sampleRateHz = sampleRate,
                    bitDepth     = bitDepth,
                    channelCount = channels,
                    isLossless   = lossless,
                    isAtmos      = atmos
                )
                break   // we only need the first audio track
            }

            extractor.release()
            info
        } catch (e: Exception) {
            // File unreadable, DRM-protected, or format unsupported —
            // store UNKNOWN so we don't crash and don't retry on the next launch.
            FormatInfo.UNKNOWN
        }
    }

    /**
     * Returns true for lossless codecs.
     * We check both the probed MIME (from MediaExtractor) and the
     * file-level MIME from MediaStore to catch ALAC-in-M4A correctly.
     */
    private fun isLosslessCodec(trackMime: String, fileMime: String): Boolean {
        val losslessMimes = setOf(
            "audio/flac",
            "audio/x-flac",
            "audio/alac",
            "audio/x-alac",
            "audio/wav",
            "audio/x-wav",
            "audio/aiff",
            "audio/x-aiff",
            "audio/raw",        // raw PCM
            "audio/l16",        // linear PCM
        )
        // ALAC is often stored in an M4A container — MediaStore reports
        // "audio/mp4" but the extracted track MIME is "audio/alac".
        return trackMime.lowercase() in losslessMimes ||
               fileMime.lowercase() in losslessMimes
    }

    private fun pcmEncodingToBitDepth(pcmEncoding: Int): Int = when (pcmEncoding) {
        // android.media.AudioFormat constants
        2    -> 8    // ENCODING_PCM_8BIT
        3    -> 16   // ENCODING_PCM_16BIT
        4    -> 32   // ENCODING_PCM_32BIT (float)
        0x15 -> 24   // ENCODING_PCM_24BIT_PACKED
        else -> 0    // compressed or unknown
    }

    private fun MediaFormat.getIntOrZero(key: String): Int =
        if (containsKey(key)) getInteger(key) else 0
}
