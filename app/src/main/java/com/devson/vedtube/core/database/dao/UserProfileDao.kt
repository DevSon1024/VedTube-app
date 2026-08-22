package com.devson.vedtube.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.devson.vedtube.core.database.model.UserProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: UserProfileEntity)

    @Update
    suspend fun updateProfile(profile: UserProfileEntity)

    @Query("DELETE FROM user_profiles WHERE profileId = :profileId AND isDefault = 0")
    suspend fun deleteProfile(profileId: String)

    @Query("SELECT * FROM user_profiles ORDER BY isDefault DESC, createdAt ASC")
    fun getAllProfiles(): Flow<List<UserProfileEntity>>

    @Query("SELECT * FROM user_profiles ORDER BY isDefault DESC, createdAt ASC")
    suspend fun getAllProfilesSync(): List<UserProfileEntity>

    @Query("SELECT * FROM user_profiles WHERE profileId = :profileId LIMIT 1")
    fun getProfile(profileId: String): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profiles WHERE profileId = :profileId LIMIT 1")
    suspend fun getProfileSync(profileId: String): UserProfileEntity?

    @Query("SELECT * FROM user_profiles WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultProfile(): UserProfileEntity?

    @Query("SELECT COUNT(*) FROM user_profiles")
    suspend fun getProfileCount(): Int
}
