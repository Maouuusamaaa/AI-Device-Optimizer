package com.maouuusama.ai.device.optimizer.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object EvidenceSyncScheduler {
    private const val UNIQUE_WORK_NAME = "github-evidence-sync"
    fun enqueue(context: Context) {
        val request = OneTimeWorkRequestBuilder<EvidenceSyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30L, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            UNIQUE_WORK_NAME, ExistingWorkPolicy.KEEP, request
        )
    }
}
