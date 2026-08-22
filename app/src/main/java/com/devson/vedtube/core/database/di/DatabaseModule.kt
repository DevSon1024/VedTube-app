package com.devson.vedtube.core.database.di

import android.content.Context
import androidx.room.Room
import com.devson.vedtube.core.database.VedTubeDatabase
import com.devson.vedtube.core.database.dao.AppInfoDao
import com.devson.vedtube.core.database.dao.SubscriptionDao
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
    ): com.devson.vedtube.core.database.dao.DownloadDao {
        return database.downloadDao()
    }
}
