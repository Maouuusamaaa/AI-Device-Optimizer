package com.maouuusama.ai.device.optimizer.benchmark

import android.content.Context
import android.os.Build
import com.maouuusama.ai.device.optimizer.sync.EvidenceSyncManager
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object RuntimeLifecycleMemoryBenchmarkJsonWriter {
    fun write(context: Context, result: RuntimeLifecycleDiagnosticResult): File {
        val samples = result.samples
        require(samples.isNotEmpty())
        val root = JSONObject()
            .put("schemaVersion", 3)
            .put("protocol", "runtime_lifecycle_memory_observation")
            .put("device", JSONObject()
                .put("androidApi", Build.VERSION.SDK_INT)
                .put("manufacturer", Build.MANUFACTURER)
                .put("model", Build.MODEL))
            .put("durationsMs", JSONObject()
                .put("baseline", RuntimeLifecycleMemoryBenchmark.BASELINE_DURATION_MS)
                .put("postCleanup", RuntimeLifecycleMemoryBenchmark.POST_CLEANUP_DURATION_MS)
                .put("postReset", RuntimeLifecycleMemoryBenchmark.POST_CLEANUP_DURATION_MS)
                .put("postReset", RuntimeLifecycleMemoryBenchmark.POST_RESET_DURATION_MS)
                .put("interval", RuntimeLifecycleMemoryBenchmark.INTERVAL_MS)
                .put("inferenceMaxTokens", RuntimeLifecycleMemoryBenchmark.MAX_TOKENS)
                .put("inferenceThreads", RuntimeLifecycleMemoryBenchmark.THREADS))
        val array = JSONArray()
        samples.forEach { wrapped ->
            val s = wrapped.sample
            val processes = JSONArray()
            s.processes.forEach { p ->
                processes.put(JSONObject()
                    .put("pid", p.pid)
                    .put("processName", p.processName)
                    .put("packageNames", JSONArray(p.packageNames))
                    .put("appLabels", JSONArray(p.appLabels))
                    .put("importance", p.importance)
                    .put("importanceLabel", p.importanceLabel)
                    .put("isForeground", p.isForeground)
                    .put("pssKb", p.pssKb)
                    .put("rssKb", p.rssKb ?: JSONObject.NULL)
                    .put("swapPssKb", p.swapPssKb))
            }
            array.put(JSONObject()
                .put("phase", wrapped.phase)
                .put("phaseSampleIndex", wrapped.phaseSampleIndex)
                .put("timestampMs", s.timestampMs)
                .put("availableRamMb", s.availableRamMb)
                .put("totalRamMb", s.totalRamMb)
                .put("batteryPercent", s.batteryPercent ?: JSONObject.NULL)
                .put("isCharging", s.isCharging)
                .put("temperatureC", s.temperatureC ?: JSONObject.NULL)
                .put("storageAvailableMb", s.storageAvailableMb)
                .put("processCpuTimeMs", s.processCpuTimeMs)
                .put("collectionDurationMs", s.collectionDurationMs)
                .put("processes", processes))
        }
        root.put("samples", array)
        val events = JSONArray()
        result.events.forEach { event ->
            events.put(JSONObject()
                .put("name", event.name)
                .put("timestampMs", event.timestampMs))
        }
        root.put("events", events)
        val dir = File(context.getExternalFilesDir(null), "benchmarks").apply { mkdirs() }
        val file = File(dir, "runtime-lifecycle-memory-" + samples.first().sample.timestampMs + ".json")
        file.writeText(root.toString(2))
        EvidenceSyncManager.enqueue(context, file)
        return file
    }

    fun exportToSharedDownloads(context: Context, file: File): String? = runCatching {
        val base = android.os.Environment.getExternalStorageDirectory()
        val targetDir = File(base, "Download/AI-Device-Optimizer/benchmarks").apply { mkdirs() }
        val target = File(targetDir, file.name)
        file.copyTo(target, overwrite = true)
        "Download/AI-Device-Optimizer/benchmarks/" + target.name
    }.getOrNull()
}
