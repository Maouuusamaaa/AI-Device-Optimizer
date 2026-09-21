package com.maouuusama.ai.device.optimizer.benchmark

import android.content.Context
import android.os.Build
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

object LocalInferenceBenchmarkJsonWriter {
    fun write(context: Context, result: LocalInferenceBenchmarkResult): File {
        val root = JSONObject()
            .put("schemaVersion", result.schemaVersion)
            .put("benchmarkId", result.benchmarkId)
            .put("startedAtMs", result.startedAtMs)
            .put("finishedAtMs", result.finishedAtMs)
            .put("workload", "local_qwen3_0_6b_inference")
            .put(
                "device",
                JSONObject()
                    .put("androidApi", Build.VERSION.SDK_INT)
                    .put("manufacturer", Build.MANUFACTURER)
                    .put("model", Build.MODEL)
                    .put("abi", Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown")
            )
            .put(
                "model",
                JSONObject()
                    .put("modelId", result.modelId)
                    .put("runtimeVersion", result.runtimeVersion)
                    .put("contextTokens", result.contextTokens)
                    .put("maxTokens", result.maxTokens)
                    .put("threadConfigurations", JSONArray(result.threadConfigurations))
                    .put("repetitionsPerConfiguration", result.repetitionsPerConfiguration)
            )
            .put(
                "measurementSemantics",
                JSONObject()
                    .put("processCpuTime", "android.os.Process.getElapsedCpuTime delta; process CPU time")
                    .put("processPss", "android.os.Debug.getPss; sampled process PSS in KB")
                    .put("wallTime", "elapsedRealtime delta around the native inference call")
                    .put("batteryTemperature", "Android battery telemetry sampled immediately before and after inference")
            )

        val samples = JSONArray()
        result.samples.forEach { sample ->
            samples.put(
                JSONObject()
                    .put("observationId", sample.observationId)
                    .put("threadCount", sample.threadCount)
                    .put("repetition", sample.repetition)
                    .put("startedAtMs", sample.startedAtMs)
                    .put("finishedAtMs", sample.finishedAtMs)
                    .put("wallTimeMs", sample.wallTimeMs)
                    .put("processCpuTimeMs", sample.processCpuTimeMs)
                    .put("processPssBeforeKb", sample.processPssBeforeKb)
                    .put("processPssAfterKb", sample.processPssAfterKb)
                    .put("processPssDeltaKb", sample.processPssDeltaKb)
                    .put("availableRamBeforeMb", sample.availableRamBeforeMb)
                    .put("availableRamAfterMb", sample.availableRamAfterMb)
                    .put("batteryBeforePercent", sample.batteryBeforePercent ?: JSONObject.NULL)
                    .put("batteryAfterPercent", sample.batteryAfterPercent ?: JSONObject.NULL)
                    .put("temperatureBeforeC", sample.temperatureBeforeC ?: JSONObject.NULL)
                    .put("temperatureAfterC", sample.temperatureAfterC ?: JSONObject.NULL)
                    .put("thermalStatusBefore", sample.thermalStatusBefore ?: JSONObject.NULL)
                    .put("thermalStatusAfter", sample.thermalStatusAfter ?: JSONObject.NULL)
                    .put("promptTokens", sample.promptTokens)
                    .put("generatedTokens", sample.generatedTokens)
                    .put("loadMs", sample.loadMs)
                    .put("tokenizationMs", sample.tokenizationMs)
                    .put("contextInitMs", sample.contextInitMs)
                    .put("promptDecodeMs", sample.promptDecodeMs)
                    .put("generationMs", sample.generationMs)
                    .put("generationTokensPerSecond", sample.generationTokensPerSecond)
                    .put("totalNativeMs", sample.totalNativeMs)
                    .put("modelOutputContainsThink", sample.modelOutputContainsThink)
                    .put("advisoryOnly", true)
                    .put("executionRequested", false)
                    .put("deviceMutationAllowed", false)
            )
        }
        root.put("samples", samples)

        val directory = File(context.getExternalFilesDir(null), "benchmarks").apply { mkdirs() }
        val file = File(directory, "local-inference-${result.benchmarkId}-${result.startedAtMs}.json")
        file.writeText(root.toString(2))
        return file
    }
}
