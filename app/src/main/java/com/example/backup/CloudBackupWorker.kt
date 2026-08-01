package com.example.backup

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.AuraDatabase
import com.example.data.AuraRepository
import com.example.data.BackupFrequency
import com.example.data.CloudBackupRepository
import com.example.data.UserPreferencesRepository

/**
 * Periodic (24h) cloud backup when the user is signed in and backup is enabled.
 */
class CloudBackupWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val prefs = UserPreferencesRepository(applicationContext)
            val profile = prefs.currentProfile()
            if (!profile.isSignedIn || profile.backupFrequency == BackupFrequency.NEVER) {
                return Result.success()
            }
            val dao = AuraDatabase.getDatabase(applicationContext).auraDao()
            val repo = AuraRepository(dao)
            val cloud = CloudBackupRepository(applicationContext, repo, prefs)
            cloud.backupNow().fold(
                onSuccess = { Result.success() },
                onFailure = { Result.retry() }
            )
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
