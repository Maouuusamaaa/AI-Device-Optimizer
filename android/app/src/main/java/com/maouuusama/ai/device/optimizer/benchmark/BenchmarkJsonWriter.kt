package com.maouuusama.ai.device.optimizer.benchmark

import com.maouuusama.ai.device.optimizer.sync.EvidenceSyncManager

import android.content.Context
import android.os.Build
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object BenchmarkJsonWriter {

    fun write(
        context: Context,
        workload: String,
        samples: List<BenchmarkSample>,
        filePrefix: String = "benchmark"
    ): File {
        require(samples.isNotEmpty()) { "samples must not be empty" }
        require(filePrefix.matches(Regex("[A-Za-z0-9_-]+"))) {
            "filePrefix must contain only letters, numbers, '_' or '-'"
        }

        val root = JSONObject()
            .put("schemaVersion", 2)
            .put("timestampMs", samples.first().timestampMs)
            .put("workload", workload)
            .put(
                "device",
                JSONObject()
                    .put("androidApi", Build.VERSION.SDK_INT)
                    .put("manufacturer", Build.MANUFACTURER)
                    .put("model", Build.MODEL)
            )

        val array = JSONArray()
        samples.forEach { sample ->
            val processArray = JSONArray()
            sample.processes.forEach { process ->
                processArray.put(
                    JSONObject()
                        .put("pid", process.pid)
                        .put("processName", process.processName)
                        .put("packageNames", JSONArray(process.packageNames))
                        .put("appLabels", JSONArray(process.appLabels))
                        .put("importance", process.importance)
                        .put("importanceLabel", process.importanceLabel)
                        .put("isForeground", process.isForeground)
                        .put("pssKb", process.pssKb)
                        .put("rssKb", process.rssKb ?: JSONObject.NULL)
                        .put("swapPssKb", process.swapPssKb)
                )
            }

            array.put(
                JSONObject()
                    .put("timestampMs", sample.timestampMs)
                    .put("availableRamMb", sample.availableRamMb)
                    .put("totalRamMb", sample.totalRamMb)
                    .put("batteryPercent", sample.batteryPercent ?: JSONObject.NULL)
                    .put("isCharging", sample.isCharging)
                    .put("temperatureC", sample.temperatureC ?: JSONObject.NULL)
                    .put("storageAvailableMb", sample.storageAvailableMb)
                    .put("processCpuTimeMs", sample.processCpuTimeMs)
                    .put("collectionDurationMs", sample.collectionDurationMs)
                    .put("processes", processArray)
            )
        }

        root.put("samples", array)

        val directory = File(
            context.getExternalFilesDir(null),
            "benchmarks"
        ).apply { mkdirs() }

        val file = File(
            directory,
            filePrefix + "-" + workload + "-" + samples.first().timestampMs + ".json"
        )
        file.writeText(root.toString(2))
        EvidenceSyncManager.enqueue(context, file)
        return file
    }
}
