package com.devson.vedtube.core.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_info")
data class AppInfoEntity(
    @PrimaryKey
    val key: String,
    val value: String
)
