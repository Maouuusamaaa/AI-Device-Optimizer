package com.maouuusama.ai.device.optimizer.sync

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.io.File
import java.io.IOException

class EvidenceSyncWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {
    override fun doWork(): Result {
        val context = applicationContext
        val config = GitHubSyncConfig(context)
        val token = GitHubTokenStore(context).read()
        if (!config.enabled || token.isNullOrBlank()) return Result.success()

        val queue = EvidenceQueueStore(context)
        while (true) {
            val item = queue.pending().firstOrNull() ?: return Result.success()
            try {
                val file = File(item.localPath)
                val validation = queue.validate(file)
                check(validation.sha256 == item.sha256) { "Evidence file changed after it entered the queue" }
                val upload = GitHubEvidenceClient(token, config.repository, config.branch)
                    .uploadIfMissingOrIdentical(item, file)
                queue.markSynced(item.id, upload.commitSha)
            } catch (error: GitHubHttpException) {
                when {
                    error.statusCode == 401 || error.statusCode == 403 ->
                        queue.markAuthFailed(item.id, error.message ?: "GitHub authentication/permission failed")
                    error.statusCode == 409 || error.statusCode == 422 ->
                        queue.markConflict(item.id, error.message ?: "GitHub rejected evidence")
                    error.statusCode in 500..599 ->
                        queue.markRetry(item.id, error.message ?: "GitHub server error")
                    else -> queue.markInvalid(item.id, error.message ?: "GitHub request failed")
                }
                return if (error.statusCode in 500..599) Result.retry() else Result.success()
            } catch (error: IOException) {
                queue.markRetry(item.id, error.message ?: "Network error")
                return Result.retry()
            } catch (error: Exception) {
                queue.markInvalid(item.id, error.message ?: error.javaClass.simpleName)
            }
        }
    }
}
