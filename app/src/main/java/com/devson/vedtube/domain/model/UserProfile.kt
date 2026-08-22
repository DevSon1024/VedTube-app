package com.devson.vedtube.domain.model

data class UserProfile(
    val id: String,
    val name: String,
    val avatarPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isDefault: Boolean = false
) {
    companion object {
        const val DEFAULT_PROFILE_ID = "profile_default"
        const val DEFAULT_PROFILE_NAME = "Default Profile"

        fun createDefault(): UserProfile = UserProfile(
            id = DEFAULT_PROFILE_ID,
            name = DEFAULT_PROFILE_NAME,
            isDefault = true
        )
    }
}
