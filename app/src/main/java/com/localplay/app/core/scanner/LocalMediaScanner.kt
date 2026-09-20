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
        MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.ALBUM_ARTIST, MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.TRACK,
        MediaStore.Audio.Media.YEAR, MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.DATE_ADDED,
        MediaStore.Audio.Media.DATE_MODIFIED, MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.MIME_TYPE,
        MediaStore.Audio.Media.SIZE, MediaStore.Audio.Media.BITRATE)
    private const val SELECTION = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
    private val COLLECTION_URI: Uri get() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

    suspend fun scan(context: Context, knownIds: Set<Long> = emptySet()): List<TrackEntity> = withContext(Dispatchers.IO) {
        val results = mutableListOf<TrackEntity>()
        val cursor = context.contentResolver.query(COLLECTION_URI, PROJECTION, SELECTION, null, "${MediaStore.Audio.Media.TITLE} ASC") ?: return@withContext results
        cursor.use { c ->
            val cols = mapOf(
                "id" to c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID),
                "title" to c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE),
                "artist" to c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST),
                "albumArt" to c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ARTIST),
                "album" to c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM),
                "track" to c.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK),
                "year" to c.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR),
                "dur" to c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION),
                "dateAdd" to c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED),
                "dateMod" to c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED),
                "path" to c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA),
                "mime" to c.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE),
                "size" to c.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE),
                "bitrate" to c.getColumnIndexOrThrow(MediaStore.Audio.Media.BITRATE))
            while (c.moveToNext()) {
                val id = c.getLong(cols["id"]!!); val mime = c.getString(cols["mime"]!!) ?: "audio/mpeg"
                val path = c.getString(cols["path"]!!) ?: continue
                val raw = c.getInt(cols["track"]!!)
                val fmt = if (id !in knownIds) probeFormat(context, id, mime) else FormatInfo.UNKNOWN
                results.add(TrackEntity(id=id, title=c.getString(cols["title"]!!) ?: "Unknown",
                    artist=c.getString(cols["artist"]!!) ?: "Unknown", albumArtist=c.getString(cols["albumArt"]!!) ?: "",
                    album=c.getString(cols["album"]!!) ?: "Unknown", trackNumber=raw%1000, discNumber=raw/1000,
                    year=c.getInt(cols["year"]!!), durationMs=c.getLong(cols["dur"]!!),
                    dateAdded=c.getLong(cols["dateAdd"]!!), dateModified=c.getLong(cols["dateMod"]!!),
                    filePath=path, mimeType=mime, fileSizeBytes=c.getLong(cols["size"]!!),
                    sampleRateHz=fmt.sampleRateHz, bitDepth=fmt.bitDepth, channelCount=fmt.channelCount,
                    bitrateKbps=c.getInt(cols["bitrate"]!!)/1000, isLossless=fmt.isLossless, isAtmos=fmt.isAtmos))
            }
        }
        results
    }

    private data class FormatInfo(val sampleRateHz:Int,val bitDepth:Int,val channelCount:Int,val isLossless:Boolean,val isAtmos:Boolean) {
        companion object { val UNKNOWN = FormatInfo(0,0,0,false,false) }
    }
    private fun probeFormat(context: Context, trackId: Long, mime: String): FormatInfo = try {
        val ex = MediaExtractor(); ex.setDataSource(context, ContentUris.withAppendedId(COLLECTION_URI, trackId), null)
        var info = FormatInfo.UNKNOWN
        for (i in 0 until ex.trackCount) {
            val fmt = ex.getTrackFormat(i); val tm = fmt.getString(MediaFormat.KEY_MIME) ?: continue
            if (!tm.startsWith("audio/")) continue
            val sr = if(fmt.containsKey(MediaFormat.KEY_SAMPLE_RATE)) fmt.getInteger(MediaFormat.KEY_SAMPLE_RATE) else 0
            val ch = if(fmt.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) fmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT) else 0
            val bd = if(fmt.containsKey(MediaFormat.KEY_PCM_ENCODING)) when(fmt.getInteger(MediaFormat.KEY_PCM_ENCODING)){2->8;3->16;4->32;0x15->24;else->0} else 0
            val lossless = setOf("audio/flac","audio/x-flac","audio/alac","audio/x-alac","audio/wav","audio/x-wav","audio/aiff","audio/x-aiff","audio/raw","audio/l16").let { tm.lowercase() in it || mime.lowercase() in it }
            info = FormatInfo(sr, bd, ch, lossless, tm.equals("audio/eac3-joc",true) || ch>=6); break
        }
        ex.release(); info
    } catch(e:Exception) { FormatInfo.UNKNOWN }
}
