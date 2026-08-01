package com.example.backup

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.data.BackupFrequency
import java.util.concurrent.TimeUnit

object BackupScheduler {
    const val UNIQUE_WORK = "pixido_cloud_backup_24h"

    fun apply(context: Context, frequency: BackupFrequency, signedIn: Boolean) {
        val wm = WorkManager.getInstance(context.applicationContext)
        if (!signedIn || frequency == BackupFrequency.NEVER) {
            wm.cancelUniqueWork(UNIQUE_WORK)
            return
        }
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<CloudBackupWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()
        wm.enqueueUniquePeriodicWork(
            UNIQUE_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}
