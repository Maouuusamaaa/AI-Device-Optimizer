package com.maouuusama.ai.device.optimizer.benchmark

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

object LocalInferenceBenchmarkJsonWriter {
    const val SHARED_EXPORT_RELATIVE_PATH =
        Environment.DIRECTORY_DOWNLOADS + "/AI-Device-Optimizer/benchmarks/"

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

    /**
     * Copies a benchmark JSON into shared Downloads so Termux and other user tools can read it.
     * The app owns the newly-created MediaStore.Downloads item, so no broad storage permission is needed.
     */
    fun exportToSharedDownloads(context: Context, sourceFile: File): String {
        check(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            "Shared benchmark export requires Android 10/API 29 or newer"
        }

        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, sourceFile.name)
            put(MediaStore.Downloads.MIME_TYPE, "application/json")
            put(MediaStore.Downloads.RELATIVE_PATH, SHARED_EXPORT_RELATIVE_PATH)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: error("MediaStore.Downloads insert returned null")

        try {
            resolver.openOutputStream(uri, "w").use { output ->
                checkNotNull(output) { "Unable to open shared benchmark export" }
                sourceFile.inputStream().use { input ->
                    input.copyTo(output)
                }
            }

            val publishedValues = ContentValues().apply {
                put(MediaStore.Downloads.IS_PENDING, 0)
            }
            resolver.update(uri, publishedValues, null, null)

            return SHARED_EXPORT_RELATIVE_PATH + sourceFile.name
        } catch (error: Exception) {
            resolver.delete(uri, null, null)
            throw error
        }
    }
}
