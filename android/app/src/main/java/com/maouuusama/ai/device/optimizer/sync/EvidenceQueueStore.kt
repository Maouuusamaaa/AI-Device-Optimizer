package com.maouuusama.ai.device.optimizer.sync

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.UUID

enum class EvidenceSyncStatus { PENDING, RETRY, SYNCED, INVALID, AUTH_FAILED, CONFLICT }

data class EvidenceQueueItem(
    val id: String, val localPath: String, val repositoryPath: String, val sha256: String,
    val sizeBytes: Long, val createdAtMs: Long, val status: EvidenceSyncStatus,
    val attempts: Int, val lastError: String?, val remoteCommitSha: String?, val lastAttemptAtMs: Long?
)

data class EvidenceValidation(val sha256: String, val sizeBytes: Long, val repositoryPath: String)

class EvidenceQueueStore(context: Context) {
    private val appContext = context.applicationContext
    private val directory = File(appContext.filesDir, "evidence-sync").apply { mkdirs() }
    private val queueFile = File(directory, "queue.json")

    @Synchronized
    fun enqueue(file: File): EvidenceQueueItem {
        val validation = validate(file)
        val existing = readItems()
        existing.firstOrNull { it.localPath == file.canonicalPath && it.sha256 == validation.sha256 }?.let { return it }
        val item = EvidenceQueueItem(
            id = UUID.randomUUID().toString(), localPath = file.canonicalPath,
            repositoryPath = validation.repositoryPath, sha256 = validation.sha256,
            sizeBytes = validation.sizeBytes, createdAtMs = System.currentTimeMillis(),
            status = EvidenceSyncStatus.PENDING, attempts = 0, lastError = null,
            remoteCommitSha = null, lastAttemptAtMs = null
        )
        writeItems(existing + item)
        return item
    }

    @Synchronized fun pending(): List<EvidenceQueueItem> =
        readItems().filter { it.status == EvidenceSyncStatus.PENDING || it.status == EvidenceSyncStatus.RETRY }
            .sortedBy { it.createdAtMs }

    @Synchronized fun all(): List<EvidenceQueueItem> = readItems().sortedByDescending { it.createdAtMs }

    @Synchronized fun markRetry(id: String, error: String) = update(id) {
        it.copy(status = EvidenceSyncStatus.RETRY, attempts = it.attempts + 1,
            lastError = sanitizeError(error), lastAttemptAtMs = System.currentTimeMillis())
    }
    @Synchronized fun markSynced(id: String, commitSha: String?) = update(id) {
        it.copy(status = EvidenceSyncStatus.SYNCED, lastError = null, remoteCommitSha = commitSha,
            lastAttemptAtMs = System.currentTimeMillis())
    }
    @Synchronized fun markInvalid(id: String, error: String) = update(id) {
        it.copy(status = EvidenceSyncStatus.INVALID, attempts = it.attempts + 1,
            lastError = sanitizeError(error), lastAttemptAtMs = System.currentTimeMillis())
    }
    @Synchronized fun markAuthFailed(id: String, error: String) = update(id) {
        it.copy(status = EvidenceSyncStatus.AUTH_FAILED, attempts = it.attempts + 1,
            lastError = sanitizeError(error), lastAttemptAtMs = System.currentTimeMillis())
    }
    @Synchronized fun markConflict(id: String, error: String) = update(id) {
        it.copy(status = EvidenceSyncStatus.CONFLICT, attempts = it.attempts + 1,
            lastError = sanitizeError(error), lastAttemptAtMs = System.currentTimeMillis())
    }

    fun validate(file: File): EvidenceValidation {
        require(file.exists() && file.isFile) { "Evidence file does not exist" }
        require(file.length() in 2..10L * 1024L * 1024L) { "Evidence file size is outside 2 bytes..10 MiB" }
        val root = File(appContext.getExternalFilesDir(null), "benchmarks").canonicalFile
        val canonical = file.canonicalFile
        require(canonical.parentFile == root) { "Evidence file is outside the approved benchmark directory" }
        require(APPROVED_FILENAME.matches(canonical.name)) { "Evidence filename is not allowlisted" }
        val json = JSONObject(canonical.readText())
        require(json.optInt("schemaVersion", 0) > 0) { "Missing or invalid schemaVersion" }
        require(json.optJSONArray("samples")?.length() ?: 0 > 0) { "Evidence samples are missing or empty" }
        val protocol = json.optString("protocol", "")
        val workload = json.optString("workload", "")
        require(protocol in APPROVED_PROTOCOLS || workload in APPROVED_WORKLOADS) {
            "Evidence protocol/workload is not allowlisted"
        }
        return EvidenceValidation(sha256(canonical), canonical.length(), "benchmarks/results/${canonical.name}")
    }

    private fun readItems(): List<EvidenceQueueItem> {
        if (!queueFile.exists()) return emptyList()
        val array = runCatching { JSONArray(queueFile.readText()) }.getOrElse { JSONArray() }
        return buildList {
            for (index in 0 until array.length()) {
                val o = array.getJSONObject(index)
                add(EvidenceQueueItem(
                    id = o.getString("id"), localPath = o.getString("localPath"),
                    repositoryPath = o.getString("repositoryPath"), sha256 = o.getString("sha256"),
                    sizeBytes = o.getLong("sizeBytes"), createdAtMs = o.getLong("createdAtMs"),
                    status = EvidenceSyncStatus.valueOf(o.getString("status")), attempts = o.getInt("attempts"),
                    lastError = o.optString("lastError", "").ifBlank { null },
                    remoteCommitSha = o.optString("remoteCommitSha", "").ifBlank { null },
                    lastAttemptAtMs = if (o.isNull("lastAttemptAtMs")) null else o.optLong("lastAttemptAtMs")
                ))
            }
        }
    }

    private fun writeItems(items: List<EvidenceQueueItem>) {
        val array = JSONArray()
        items.forEach { item ->
            array.put(JSONObject()
                .put("id", item.id).put("localPath", item.localPath).put("repositoryPath", item.repositoryPath)
                .put("sha256", item.sha256).put("sizeBytes", item.sizeBytes).put("createdAtMs", item.createdAtMs)
                .put("status", item.status.name).put("attempts", item.attempts)
                .put("lastError", item.lastError ?: JSONObject.NULL)
                .put("remoteCommitSha", item.remoteCommitSha ?: JSONObject.NULL)
                .put("lastAttemptAtMs", item.lastAttemptAtMs ?: JSONObject.NULL))
        }
        val temp = File(directory, "queue.json.tmp")
        temp.writeText(array.toString(2))
        if (!temp.renameTo(queueFile)) { temp.copyTo(queueFile, overwrite = true); temp.delete() }
    }

    private fun update(id: String, transform: (EvidenceQueueItem) -> EvidenceQueueItem) {
        writeItems(readItems().map { if (it.id == id) transform(it) else it })
    }
    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            while (true) { val count = input.read(buffer); if (count <= 0) break; digest.update(buffer, 0, count) }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
    private fun sanitizeError(error: String) = error.replace(Regex("\\s+"), " ").take(300)

    companion object {
        private val APPROVED_FILENAME =
            Regex("""^(baseline|workload|repeated-workload-recovery|long-recovery-memory|runtime-lifecycle-memory|local-inference)-[A-Za-z0-9_-]+[.]json$""")
        private val APPROVED_PROTOCOLS = setOf(
            "repeated_workload_recovery_memory_observation",
            "long_recovery_memory_observation",
            "runtime_lifecycle_memory_observation"
        )
        private val APPROVED_WORKLOADS = setOf(
            "monitor_foreground_idle", "foreground_user_workload", "local_qwen3_0_6b_inference"
        )
    }
}
