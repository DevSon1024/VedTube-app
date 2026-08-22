package com.devson.vedtube.core.database.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.devson.vedtube.core.database.VedTubeDatabase
import com.devson.vedtube.core.database.dao.AppInfoDao
import com.devson.vedtube.core.database.dao.DownloadDao
import com.devson.vedtube.core.database.dao.LocalPlaylistDao
import com.devson.vedtube.core.database.dao.SearchHistoryDao
import com.devson.vedtube.core.database.dao.SubscriptionDao
import com.devson.vedtube.core.database.dao.UserProfileDao
import com.devson.vedtube.core.database.dao.WatchHistoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `search_history` (
                    `query` TEXT NOT NULL,
                    `timestamp` INTEGER NOT NULL,
                    PRIMARY KEY(`query`)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `local_playlists` (
                    `playlistId` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    PRIMARY KEY(`playlistId`)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `playlist_videos` (
                    `playlistId` TEXT NOT NULL,
                    `videoId` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `channelName` TEXT NOT NULL,
                    `thumbnailUrl` TEXT NOT NULL,
                    `durationSeconds` INTEGER NOT NULL,
                    `addedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`playlistId`, `videoId`),
                    FOREIGN KEY(`playlistId`) REFERENCES `local_playlists`(`playlistId`) ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_playlist_videos_playlistId` ON `playlist_videos` (`playlistId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_playlist_videos_videoId` ON `playlist_videos` (`videoId`)")
        }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 1. Create user profiles table
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `user_profiles` (
                    `profileId` TEXT NOT NULL PRIMARY KEY,
                    `name` TEXT NOT NULL,
                    `avatarPath` TEXT,
                    `createdAt` INTEGER NOT NULL,
                    `isDefault` INTEGER NOT NULL
                )
                """.trimIndent()
            )
            // Insert default profile
            db.execSQL(
                """
                INSERT OR IGNORE INTO `user_profiles` (`profileId`, `name`, `avatarPath`, `createdAt`, `isDefault`)
                VALUES ('profile_default', 'Default Profile', NULL, 1000, 1)
                """.trimIndent()
            )

            // 2. Migrate watch_history to include profileId and composite PK
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `watch_history_new` (
                    `videoId` TEXT NOT NULL,
                    `profileId` TEXT NOT NULL DEFAULT 'profile_default',
                    `title` TEXT NOT NULL,
                    `channelName` TEXT NOT NULL,
                    `thumbnailUrl` TEXT NOT NULL,
                    `durationMs` INTEGER NOT NULL,
                    `progressMs` INTEGER NOT NULL,
                    `lastWatchedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`profileId`, `videoId`)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO `watch_history_new` (`videoId`, `profileId`, `title`, `channelName`, `thumbnailUrl`, `durationMs`, `progressMs`, `lastWatchedAt`)
                SELECT `videoId`, 'profile_default', `title`, `channelName`, `thumbnailUrl`, `durationMs`, `progressMs`, `lastWatchedAt` FROM `watch_history`
                """.trimIndent()
            )
            db.execSQL("DROP TABLE `watch_history`")
            db.execSQL("ALTER TABLE `watch_history_new` RENAME TO `watch_history`")

            // 3. Migrate subscriptions to include profileId and composite PK
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `subscriptions_new` (
                    `channelId` TEXT NOT NULL,
                    `profileId` TEXT NOT NULL DEFAULT 'profile_default',
                    `channelName` TEXT NOT NULL,
                    `avatarUrl` TEXT NOT NULL,
                    `subscribedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`profileId`, `channelId`)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO `subscriptions_new` (`channelId`, `profileId`, `channelName`, `avatarUrl`, `subscribedAt`)
                SELECT `channelId`, 'profile_default', `channelName`, `avatarUrl`, `subscribedAt` FROM `subscriptions`
                """.trimIndent()
            )
            db.execSQL("DROP TABLE `subscriptions`")
            db.execSQL("ALTER TABLE `subscriptions_new` RENAME TO `subscriptions`")

            // 4. Alter local_playlists to add profileId
            db.execSQL("ALTER TABLE `local_playlists` ADD COLUMN `profileId` TEXT NOT NULL DEFAULT 'profile_default'")

            // 5. Migrate search_history to include profileId and composite PK
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `search_history_new` (
                    `query` TEXT NOT NULL,
                    `profileId` TEXT NOT NULL DEFAULT 'profile_default',
                    `timestamp` INTEGER NOT NULL,
                    PRIMARY KEY(`profileId`, `query`)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO `search_history_new` (`query`, `profileId`, `timestamp`)
                SELECT `query`, 'profile_default', `timestamp` FROM `search_history`
                """.trimIndent()
            )
            db.execSQL("DROP TABLE `search_history`")
            db.execSQL("ALTER TABLE `search_history_new` RENAME TO `search_history`")
        }
    }

    @Provides
    @Singleton
    fun providesVedTubeDatabase(
        @ApplicationContext context: Context
    ): VedTubeDatabase {
        return Room.databaseBuilder(
            context,
            VedTubeDatabase::class.java,
            "vedtube.db"
        )
            .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    db.execSQL(
                        """
                        INSERT OR IGNORE INTO `user_profiles` (`profileId`, `name`, `avatarPath`, `createdAt`, `isDefault`)
                        VALUES ('profile_default', 'Default Profile', NULL, 1000, 1)
                        """.trimIndent()
                    )
                }
            })
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun providesAppInfoDao(
        database: VedTubeDatabase
    ): AppInfoDao {
        return database.appInfoDao()
    }

    @Provides
    @Singleton
    fun providesWatchHistoryDao(
        database: VedTubeDatabase
    ): WatchHistoryDao {
        return database.watchHistoryDao()
    }

    @Provides
    @Singleton
    fun providesSubscriptionDao(
        database: VedTubeDatabase
    ): SubscriptionDao {
        return database.subscriptionDao()
    }

    @Provides
    @Singleton
    fun providesDownloadDao(
        database: VedTubeDatabase
    ): DownloadDao {
        return database.downloadDao()
    }

    @Provides
    @Singleton
    fun providesSearchHistoryDao(
        database: VedTubeDatabase
    ): SearchHistoryDao {
        return database.searchHistoryDao()
    }

    @Provides
    @Singleton
    fun providesLocalPlaylistDao(
        database: VedTubeDatabase
    ): LocalPlaylistDao {
        return database.localPlaylistDao()
    }

    @Provides
    @Singleton
    fun providesUserProfileDao(
        database: VedTubeDatabase
    ): UserProfileDao {
        return database.userProfileDao()
    }
}
