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
        MediaStore.Audio.Media.ALBUM_ARTIST, MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.ALBUM_ID,
        MediaStore.Audio.Media.TRACK, MediaStore.Audio.Media.YEAR, MediaStore.Audio.Media.DURATION,
        MediaStore.Audio.Media.DATE_ADDED, MediaStore.Audio.Media.DATE_MODIFIED,
        MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.MIME_TYPE,
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
            val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumArtCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ARTIST)
            val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val trackCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val yearCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
            val durCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dateAddCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val dateModCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
            val pathCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val mimeCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
            val sizeCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val bitrateCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.BITRATE)
            while (c.moveToNext()) {
                val id = c.getLong(idCol); val mime = c.getString(mimeCol) ?: "audio/mpeg"
                val path = c.getString(pathCol) ?: continue; val raw = c.getInt(trackCol)
                val fmt = if (id !in knownIds) probeFormat(context, id, mime) else FormatInfo.UNKNOWN
                results.add(TrackEntity(id=id, title=c.getString(titleCol)?:"Unknown", artist=c.getString(artistCol)?:"Unknown",
                    albumArtist=c.getString(albumArtCol)?:"", album=c.getString(albumCol)?:"Unknown",
                    albumId=c.getLong(albumIdCol), trackNumber=raw%1000, discNumber=raw/1000,
                    year=c.getInt(yearCol), durationMs=c.getLong(durCol), dateAdded=c.getLong(dateAddCol),
                    dateModified=c.getLong(dateModCol), filePath=path, mimeType=mime,
                    fileSizeBytes=c.getLong(sizeCol), sampleRateHz=fmt.sampleRateHz, bitDepth=fmt.bitDepth,
                    channelCount=fmt.channelCount, bitrateKbps=c.getInt(bitrateCol)/1000,
                    isLossless=fmt.isLossless, isAtmos=fmt.isAtmos))
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
            val losslessSet = setOf("audio/flac","audio/x-flac","audio/alac","audio/x-alac","audio/wav","audio/x-wav","audio/aiff","audio/x-aiff","audio/raw","audio/l16")
            info = FormatInfo(sr,bd,ch,tm.lowercase() in losslessSet||mime.lowercase() in losslessSet,tm.equals("audio/eac3-joc",true)||ch>=6); break
        }
        ex.release(); info
    } catch(e:Exception) { FormatInfo.UNKNOWN }
}
