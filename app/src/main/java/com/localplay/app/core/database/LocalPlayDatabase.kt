package com.localplay.app.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.localplay.app.core.database.dao.TrackDao
import com.localplay.app.core.database.entity.TrackEntity

@Database(
    entities      = [TrackEntity::class],
    version       = 2,           // bumped from 1 — albumId column added
    exportSchema  = false
)
abstract class LocalPlayDatabase : RoomDatabase() {

    abstract fun trackDao(): TrackDao

    companion object {
        @Volatile private var INSTANCE: LocalPlayDatabase? = null

        fun getInstance(context: Context): LocalPlayDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    LocalPlayDatabase::class.java,
                    "localplay.db"
                )
                    .fallbackToDestructiveMigration()  // wipe + rescan on schema change
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
