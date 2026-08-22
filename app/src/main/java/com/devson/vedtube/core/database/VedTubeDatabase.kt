package com.devson.vedtube.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.devson.vedtube.core.database.dao.AppInfoDao
import com.devson.vedtube.core.database.dao.DownloadDao
import com.devson.vedtube.core.database.dao.SubscriptionDao
import com.devson.vedtube.core.database.dao.WatchHistoryDao
import com.devson.vedtube.core.database.model.AppInfoEntity
import com.devson.vedtube.core.database.model.DownloadEntity
import com.devson.vedtube.core.database.model.SubscriptionEntity
import com.devson.vedtube.core.database.model.WatchHistoryEntity

@Database(
    entities = [
        AppInfoEntity::class,
        WatchHistoryEntity::class,
        SubscriptionEntity::class,
        DownloadEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class VedTubeDatabase : RoomDatabase() {
    abstract fun appInfoDao(): AppInfoDao
    abstract fun watchHistoryDao(): WatchHistoryDao
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun downloadDao(): DownloadDao
}
