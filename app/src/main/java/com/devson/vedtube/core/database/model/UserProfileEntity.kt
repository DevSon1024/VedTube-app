package com.devson.vedtube.core.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey val profileId: String,
    val name: String,
    val avatarPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isDefault: Boolean = false
)
