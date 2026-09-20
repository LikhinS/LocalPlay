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

object LocalMediaScanner {

    private val PROJECTION = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.ALBUM_ARTIST,
        MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Media.TRACK,
        MediaStore.Audio.Media.YEAR,
        MediaStore.Audio.Media.DURATION,
        MediaStore.Audio.Media.DATE_ADDED,
        MediaStore.Audio.Media.DATE_MODIFIED,
        MediaStore.Audio.Media.DATA,
        MediaStore.Audio.Media.MIME_TYPE,
        MediaStore.Audio.Media.SIZE,
        MediaStore.Audio.Media.BITRATE,
    )

    private const val SELECTION = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

    private val COLLECTION_URI: Uri get() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        else
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

    suspend fun scan(context: Context, knownIds: Set<Long> = emptySet()): List<TrackEntity> =
        withContext(Dispatchers.IO) {
            val results = mutableListOf<TrackEntity>()
            val cursor = context.contentResolver.query(
                COLLECTION_URI, PROJECTION, SELECTION, null,
                "${MediaStore.Audio.Media.TITLE} ASC"
            ) ?: return@withContext results

            cursor.use { c ->
                val idCol       = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol    = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol   = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumArtCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ARTIST)
                val albumCol    = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val trackCol    = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
                val yearCol     = c.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
                val durCol      = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dateAddCol  = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val dateModCol  = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
                val pathCol     = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val mimeCol     = c.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
                val sizeCol     = c.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val bitrateCol  = c.getColumnIndexOrThrow(MediaStore.Audio.Media.BITRATE)

                while (c.moveToNext()) {
                    val id       = c.getLong(idCol)
                    val rawTrack = c.getInt(trackCol)
                    val mime     = c.getString(mimeCol) ?: "audio/mpeg"
                    val path     = c.getString(pathCol) ?: continue

                    val fmt = if (id !in knownIds) probeFormat(context, id, mime)
                              else FormatInfo.UNKNOWN

                    results.add(TrackEntity(
                        id            = id,
                        title         = c.getString(titleCol) ?: "Unknown Title",
                        artist        = c.getString(artistCol) ?: "Unknown Artist",
                        albumArtist   = c.getString(albumArtCol) ?: "",
                        album         = c.getString(albumCol) ?: "Unknown Album",
                        trackNumber   = rawTrack % 1000,
                        discNumber    = rawTrack / 1000,
                        year          = c.getInt(yearCol),
                        durationMs    = c.getLong(durCol),
                        dateAdded     = c.getLong(dateAddCol),
                        dateModified  = c.getLong(dateModCol),
                        filePath      = path,
                        mimeType      = mime,
                        fileSizeBytes = c.getLong(sizeCol),
                        sampleRateHz  = fmt.sampleRateHz,
                        bitDepth      = fmt.bitDepth,
                        channelCount  = fmt.channelCount,
                        bitrateKbps   = c.getInt(bitrateCol) / 1000,
                        isLossless    = fmt.isLossless,
                        isAtmos       = fmt.isAtmos
                    ))
                }
            }
            results
        }

    private data class FormatInfo(
        val sampleRateHz: Int, val bitDepth: Int, val channelCount: Int,
        val isLossless: Boolean, val isAtmos: Boolean
    ) { companion object { val UNKNOWN = FormatInfo(0,0,0,false,false) } }

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
                val sr  = fmt.getIntOrZero(MediaFormat.KEY_SAMPLE_RATE)
                val ch  = fmt.getIntOrZero(MediaFormat.KEY_CHANNEL_COUNT)
                val bd  = pcmEncodingToBitDepth(fmt.getIntOrZero(MediaFormat.KEY_PCM_ENCODING))
                val lossless = isLosslessCodec(trackMime, mime)
                val atmos = trackMime.equals("audio/eac3-joc", ignoreCase = true) || ch >= 6
                info = FormatInfo(sr, bd, ch, lossless, atmos)
                break
            }
            extractor.release()
            info
        } catch (e: Exception) { FormatInfo.UNKNOWN }
    }

    private fun isLosslessCodec(trackMime: String, fileMime: String): Boolean {
        val set = setOf("audio/flac","audio/x-flac","audio/alac","audio/x-alac",
            "audio/wav","audio/x-wav","audio/aiff","audio/x-aiff","audio/raw","audio/l16")
        return trackMime.lowercase() in set || fileMime.lowercase() in set
    }

    private fun pcmEncodingToBitDepth(enc: Int) = when(enc) {
        2 -> 8; 3 -> 16; 4 -> 32; 0x15 -> 24; else -> 0
    }

    private fun MediaFormat.getIntOrZero(key: String) =
        if (containsKey(key)) getInteger(key) else 0
}
