package com.devson.vedtube.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.devson.vedtube.core.database.dao.AppInfoDao
import com.devson.vedtube.core.database.model.AppInfoEntity

@Database(
    entities = [
        AppInfoEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class VedTubeDatabase : RoomDatabase() {
    abstract fun appInfoDao(): AppInfoDao
}
