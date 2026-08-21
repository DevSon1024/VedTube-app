package com.devson.vedtube.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.devson.vedtube.core.database.model.AppInfoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppInfoDao {

    @Query("SELECT * FROM app_info WHERE `key` = :key")
    suspend fun getInfo(key: String): AppInfoEntity?

    @Query("SELECT * FROM app_info WHERE `key` = :key")
    fun getInfoFlow(key: String): Flow<AppInfoEntity?>

    @Query("SELECT * FROM app_info")
    fun getAllInfo(): Flow<List<AppInfoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInfo(info: AppInfoEntity)

    @Query("DELETE FROM app_info WHERE `key` = :key")
    suspend fun deleteInfo(key: String)
}
