package com.devson.vedtube.domain.repository

import com.devson.vedtube.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface UserProfileRepository {
    val allProfiles: Flow<List<UserProfile>>
    val activeProfileId: Flow<String>
    val activeProfile: Flow<UserProfile?>

    suspend fun getActiveProfileIdSync(): String
    suspend fun setActiveProfile(profileId: String)
    suspend fun createProfile(name: String, avatarPath: String? = null): String
    suspend fun updateProfile(profile: UserProfile)
    suspend fun deleteProfile(profileId: String)
}
