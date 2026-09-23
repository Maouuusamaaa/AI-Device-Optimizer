package com.maouuusama.ai.device.optimizer.benchmark

import com.maouuusama.ai.device.optimizer.sync.EvidenceSyncManager

import android.content.Context
import android.os.Build
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object WorkloadRecoveryBenchmarkJsonWriter {
    fun write(
        context: Context,
        samples: List<RecoveryBenchmarkSample>,
        protocol: String = "repeated_workload_recovery_memory_observation",
        baselineDurationMs: Long = WorkloadRecoveryBenchmark.DEFAULT_BASELINE_DURATION_MS,
        workloadDurationMs: Long = WorkloadRecoveryBenchmark.DEFAULT_WORKLOAD_DURATION_MS,
        recoveryDurationMs: Long = WorkloadRecoveryBenchmark.DEFAULT_RECOVERY_DURATION_MS,
        intervalMs: Long = WorkloadRecoveryBenchmark.DEFAULT_INTERVAL_MS,
        cycleCount: Int = WorkloadRecoveryBenchmark.DEFAULT_CYCLE_COUNT,
        filenamePrefix: String = "repeated-workload-recovery"
    ): File {
        require(samples.isNotEmpty())
        val root = JSONObject()
            .put("schemaVersion", 2)
            .put("protocol", protocol)
            .put("device", JSONObject()
                .put("androidApi", Build.VERSION.SDK_INT)
                .put("manufacturer", Build.MANUFACTURER)
                .put("model", Build.MODEL))
            .put("durationsMs", JSONObject()
                .put("baseline", baselineDurationMs)
                .put("workload", workloadDurationMs)
                .put("recovery", recoveryDurationMs)
                .put("interval", intervalMs)
                .put("cycleCount", cycleCount))
        val array = JSONArray()
        samples.forEach { wrapped ->
            val sample = wrapped.sample
            val processes = JSONArray()
            sample.processes.forEach { process ->
                processes.put(JSONObject()
                    .put("pid", process.pid)
                    .put("processName", process.processName)
                    .put("packageNames", JSONArray(process.packageNames))
                    .put("appLabels", JSONArray(process.appLabels))
                    .put("importance", process.importance)
                    .put("importanceLabel", process.importanceLabel)
                    .put("isForeground", process.isForeground)
                    .put("pssKb", process.pssKb)
                    .put("rssKb", process.rssKb ?: JSONObject.NULL)
                    .put("swapPssKb", process.swapPssKb))
            }
            array.put(JSONObject()
                .put("phase", wrapped.phase)
                .put("cycle", wrapped.cycle)
                .put("phaseSampleIndex", wrapped.phaseSampleIndex)
                .put("timestampMs", sample.timestampMs)
                .put("availableRamMb", sample.availableRamMb)
                .put("totalRamMb", sample.totalRamMb)
                .put("batteryPercent", sample.batteryPercent ?: JSONObject.NULL)
                .put("isCharging", sample.isCharging)
                .put("temperatureC", sample.temperatureC ?: JSONObject.NULL)
                .put("storageAvailableMb", sample.storageAvailableMb)
                .put("processCpuTimeMs", sample.processCpuTimeMs)
                .put("collectionDurationMs", sample.collectionDurationMs)
                .put("processes", processes))
        }
        root.put("samples", array)
        val directory = File(context.getExternalFilesDir(null), "benchmarks").apply { mkdirs() }
        val file = File(directory, filenamePrefix + "-" + samples.first().sample.timestampMs + ".json")
        file.writeText(root.toString(2))
        EvidenceSyncManager.enqueue(context, file)
        return file
    }

    fun exportToSharedDownloads(context: Context, file: File): String? = runCatching {
        val base = android.os.Environment.getExternalStorageDirectory()
        val targetDirectory = File(base, "Download/AI-Device-Optimizer/benchmarks").apply { mkdirs() }
        val target = File(targetDirectory, file.name)
        file.copyTo(target, overwrite = true)
        "Download/AI-Device-Optimizer/benchmarks/" + target.name
    }.getOrNull()
}