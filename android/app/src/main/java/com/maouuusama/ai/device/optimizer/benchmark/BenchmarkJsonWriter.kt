package com.maouuusama.ai.device.optimizer.benchmark

import android.content.Context
import android.os.Build
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object BenchmarkJsonWriter {

    fun write(
        context: Context,
        workload: String,
        samples: List<BenchmarkSample>
    ): File {
        require(samples.isNotEmpty()) { "samples must not be empty" }

        val root = JSONObject()
            .put("schemaVersion", 1)
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
            )
        }

        root.put("samples", array)

        val directory = File(
            context.getExternalFilesDir(null),
            "benchmarks"
        ).apply { mkdirs() }

        val file = File(
            directory,
            "baseline-" + samples.first().timestampMs + ".json"
        )
        file.writeText(root.toString(2))
        return file
    }
}
