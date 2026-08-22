package com.devson.vedtube.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.devson.vedtube.core.database.dao.AppInfoDao
import com.devson.vedtube.core.database.dao.DownloadDao
import com.devson.vedtube.core.database.dao.LocalPlaylistDao
import com.devson.vedtube.core.database.dao.SearchHistoryDao
import com.devson.vedtube.core.database.dao.SubscriptionDao
import com.devson.vedtube.core.database.dao.UserProfileDao
import com.devson.vedtube.core.database.dao.WatchHistoryDao
import com.devson.vedtube.core.database.model.AppInfoEntity
import com.devson.vedtube.core.database.model.DownloadEntity
import com.devson.vedtube.core.database.model.LocalPlaylistEntity
import com.devson.vedtube.core.database.model.PlaylistVideoCrossRef
import com.devson.vedtube.core.database.model.SearchHistoryEntity
import com.devson.vedtube.core.database.model.SubscriptionEntity
import com.devson.vedtube.core.database.model.UserProfileEntity
import com.devson.vedtube.core.database.model.WatchHistoryEntity

@Database(
    entities = [
        AppInfoEntity::class,
        WatchHistoryEntity::class,
        SubscriptionEntity::class,
        DownloadEntity::class,
        SearchHistoryEntity::class,
        LocalPlaylistEntity::class,
        PlaylistVideoCrossRef::class,
        UserProfileEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class VedTubeDatabase : RoomDatabase() {
    abstract fun appInfoDao(): AppInfoDao
    abstract fun watchHistoryDao(): WatchHistoryDao
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun downloadDao(): DownloadDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun localPlaylistDao(): LocalPlaylistDao
    abstract fun userProfileDao(): UserProfileDao
}
