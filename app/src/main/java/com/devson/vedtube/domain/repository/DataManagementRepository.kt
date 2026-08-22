package com.devson.vedtube.domain.repository

import com.devson.vedtube.domain.model.BackupSummary
import java.io.InputStream
import java.io.OutputStream

interface DataManagementRepository {
    suspend fun exportData(outputStream: OutputStream): Result<BackupSummary>
    suspend fun importData(inputStream: InputStream): Result<BackupSummary>
    suspend fun clearAllData(): Result<Unit>
}
