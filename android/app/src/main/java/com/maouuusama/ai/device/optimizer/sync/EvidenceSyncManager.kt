package com.maouuusama.ai.device.optimizer.sync

import android.content.Context
import java.io.File

object EvidenceSyncManager {
    fun enqueue(context: Context, file: File) {
        runCatching {
            EvidenceQueueStore(context).enqueue(file)
            EvidenceSyncScheduler.enqueue(context)
        }
    }
    fun status(context: Context): String {
        val queue = EvidenceQueueStore(context).all()
        val pending = queue.count { it.status == EvidenceSyncStatus.PENDING || it.status == EvidenceSyncStatus.RETRY }
        val synced = queue.count { it.status == EvidenceSyncStatus.SYNCED }
        val failed = queue.count {
            it.status == EvidenceSyncStatus.INVALID ||
                it.status == EvidenceSyncStatus.AUTH_FAILED ||
                it.status == EvidenceSyncStatus.CONFLICT
        }
        val config = GitHubSyncConfig(context)
        return buildString {
            append("GitHub evidence sync\n")
            append("Enabled: ").append(config.enabled).append("\n")
            append("Token configured: ").append(GitHubTokenStore(context).hasToken()).append("\n")
            append("Repository: ").append(config.repository).append("\n")
            append("Branch: ").append(config.branch).append("\n")
            append("Pending: ").append(pending).append("\n")
            append("Synced: ").append(synced).append("\n")
            append("Failed: ").append(failed).append("\n")
            queue.firstOrNull {
                it.status == EvidenceSyncStatus.RETRY ||
                    it.status == EvidenceSyncStatus.AUTH_FAILED ||
                    it.status == EvidenceSyncStatus.CONFLICT
            }?.let { append("Last issue: ").append(it.lastError ?: "unknown").append("\n") }
        }
    }
}
