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
        enqueueInternal(context, ExistingWorkPolicy.KEEP)
    }

    fun retryNow(context: Context) {
        // Manual retry must replace a stale/pending unique work instance.
        // KEEP can otherwise preserve an already-enqueued request indefinitely
        // and make the UI "Retry" button a no-op.
        enqueueInternal(context, ExistingWorkPolicy.REPLACE)
    }

    private fun enqueueInternal(context: Context, policy: ExistingWorkPolicy) {
        val request = OneTimeWorkRequestBuilder<EvidenceSyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30L, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            UNIQUE_WORK_NAME, policy, request
        )
    }
}
