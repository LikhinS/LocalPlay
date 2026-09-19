package com.localplay.app.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.localplay.app.core.database.dao.TrackDao
import com.localplay.app.core.database.entity.TrackEntity

@Database(
    entities = [TrackEntity::class],
    version = 1,
    exportSchema = false   // set true and add schemaLocation when adding migrations
)
abstract class LocalPlayDatabase : RoomDatabase() {

    abstract fun trackDao(): TrackDao

    companion object {
        // Volatile so every thread always reads the real instance, not a cached one.
        @Volatile
        private var INSTANCE: LocalPlayDatabase? = null

        fun getInstance(context: Context): LocalPlayDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    LocalPlayDatabase::class.java,
                    "localplay.db"
                )
                    // On a schema version bump, wipe and re-scan rather than
                    // writing a migration — the library can always be re-indexed
                    // from MediaStore, so there's no user data worth migrating.
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
