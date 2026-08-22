package com.devson.vedtube.data.repository

import com.devson.vedtube.core.common.dispatcher.Dispatcher
import com.devson.vedtube.core.common.dispatcher.VedTubeDispatchers
import com.devson.vedtube.core.database.dao.UserProfileDao
import com.devson.vedtube.core.database.model.UserProfileEntity
import com.devson.vedtube.core.datastore.UserPreferencesDataStore
import com.devson.vedtube.domain.model.UserProfile
import com.devson.vedtube.domain.repository.UserProfileRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserProfileRepositoryImpl @Inject constructor(
    private val userProfileDao: UserProfileDao,
    private val userPreferencesDataStore: UserPreferencesDataStore,
    @Dispatcher(VedTubeDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) : UserProfileRepository {

    override val allProfiles: Flow<List<UserProfile>>
        get() = userProfileDao.getAllProfiles()
            .map { entities ->
                if (entities.isEmpty()) {
                    listOf(UserProfile.createDefault())
                } else {
                    entities.map { it.toDomain() }
                }
            }
            .flowOn(ioDispatcher)

    override val activeProfileId: Flow<String>
        get() = userPreferencesDataStore.activeProfileId.flowOn(ioDispatcher)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override val activeProfile: Flow<UserProfile?>
        get() = userPreferencesDataStore.activeProfileId
            .flatMapLatest { profileId ->
                userProfileDao.getProfile(profileId).map { it?.toDomain() ?: UserProfile.createDefault() }
            }
            .flowOn(ioDispatcher)

    override suspend fun getActiveProfileIdSync(): String = withContext(ioDispatcher) {
        userPreferencesDataStore.activeProfileId.first()
    }

    override suspend fun setActiveProfile(profileId: String) {
        withContext(ioDispatcher) {
            userPreferencesDataStore.setActiveProfileId(profileId)
        }
    }

    override suspend fun createProfile(name: String, avatarPath: String?): String = withContext(ioDispatcher) {
        // Ensure default profile exists
        ensureDefaultProfileExists()

        val newId = UUID.randomUUID().toString()
        val entity = UserProfileEntity(
            profileId = newId,
            name = name.trim().ifBlank { "Profile" },
            avatarPath = avatarPath,
            createdAt = System.currentTimeMillis(),
            isDefault = false
        )
        userProfileDao.insertProfile(entity)
        newId
    }

    override suspend fun updateProfile(profile: UserProfile) = withContext(ioDispatcher) {
        userProfileDao.updateProfile(
            UserProfileEntity(
                profileId = profile.id,
                name = profile.name,
                avatarPath = profile.avatarPath,
                createdAt = profile.createdAt,
                isDefault = profile.isDefault
            )
        )
    }

    override suspend fun deleteProfile(profileId: String) = withContext(ioDispatcher) {
        if (profileId == UserProfile.DEFAULT_PROFILE_ID) return@withContext

        // If deleting active profile, revert active to default
        val currentActive = getActiveProfileIdSync()
        if (currentActive == profileId) {
            setActiveProfile(UserProfile.DEFAULT_PROFILE_ID)
        }
        userProfileDao.deleteProfile(profileId)
    }

    private suspend fun ensureDefaultProfileExists() {
        val count = userProfileDao.getProfileCount()
        if (count == 0) {
            userProfileDao.insertProfile(
                UserProfileEntity(
                    profileId = UserProfile.DEFAULT_PROFILE_ID,
                    name = UserProfile.DEFAULT_PROFILE_NAME,
                    avatarPath = null,
                    createdAt = System.currentTimeMillis(),
                    isDefault = true
                )
            )
        }
    }

    private fun UserProfileEntity.toDomain(): UserProfile {
        return UserProfile(
            id = profileId,
            name = name,
            avatarPath = avatarPath,
            createdAt = createdAt,
            isDefault = isDefault
        )
    }
}
