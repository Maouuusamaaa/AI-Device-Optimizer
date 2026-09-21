package com.maouuusama.ai.device.optimizer.benchmark

import android.content.Context
import android.os.Debug
import android.os.Process
import android.os.SystemClock
import com.maouuusama.ai.device.optimizer.localai.LocalLlamaRuntime
import com.maouuusama.ai.device.optimizer.localai.QwenLocalModel
import com.maouuusama.ai.device.optimizer.localai.QwenModelDownloader
import com.maouuusama.ai.device.optimizer.monitor.DeviceMonitor
import java.util.UUID
import org.json.JSONObject

data class LocalInferenceBenchmarkSample(
    val observationId: String,
    val threadCount: Int,
    val repetition: Int,
    val startedAtMs: Long,
    val finishedAtMs: Long,
    val wallTimeMs: Long,
    val processCpuTimeMs: Long,
    val processPssBeforeKb: Long,
    val processPssAfterKb: Long,
    val processPssDeltaKb: Long,
    val availableRamBeforeMb: Long,
    val availableRamAfterMb: Long,
    val batteryBeforePercent: Int?,
    val batteryAfterPercent: Int?,
    val temperatureBeforeC: Double?,
    val temperatureAfterC: Double?,
    val thermalStatusBefore: Int?,
    val thermalStatusAfter: Int?,
    val promptTokens: Int,
    val generatedTokens: Int,
    val loadMs: Long,
    val tokenizationMs: Long,
    val contextInitMs: Long,
    val promptDecodeMs: Long,
    val generationMs: Long,
    val generationTokensPerSecond: Double,
    val totalNativeMs: Long,
    val modelOutputContainsThink: Boolean
)

data class LocalInferenceBenchmarkResult(
    val benchmarkId: String,
    val schemaVersion: Int,
    val startedAtMs: Long,
    val finishedAtMs: Long,
    val modelId: String,
    val runtimeVersion: String,
    val contextTokens: Int,
    val maxTokens: Int,
    val threadConfigurations: List<Int>,
    val repetitionsPerConfiguration: Int,
    val samples: List<LocalInferenceBenchmarkSample>
)

class LocalInferenceBenchmark(private val context: Context) {
    companion object {
        const val SCHEMA_VERSION = 1
        val DEFAULT_THREAD_CONFIGURATIONS = listOf(2, 4)
        const val DEFAULT_REPETITIONS = 2
        const val DEFAULT_CONTEXT_TOKENS = 4096
        const val DEFAULT_MAX_TOKENS = 64
        const val DEFAULT_DELAY_BETWEEN_RUNS_MS = 10_000L

        private const val FIXED_BENCHMARK_PROMPT =
            "Analyze a fixed Android telemetry observation for the purpose of a controlled " +
                "local inference benchmark. Battery=50%, Temperature=35C, AvailableRAM=2000MB. " +
                "Return concise advisory analysis only. Do not request or execute device mutations."
    }

    fun run(
        threadConfigurations: List<Int> = DEFAULT_THREAD_CONFIGURATIONS,
        repetitions: Int = DEFAULT_REPETITIONS,
        contextTokens: Int = DEFAULT_CONTEXT_TOKENS,
        maxTokens: Int = DEFAULT_MAX_TOKENS,
        delayBetweenRunsMs: Long = DEFAULT_DELAY_BETWEEN_RUNS_MS,
        onProgress: (threadCount: Int, repetition: Int, totalRuns: Int) -> Unit = { _, _, _ -> }
    ): LocalInferenceBenchmarkResult {
        require(threadConfigurations.size >= 2) { "At least two thread configurations are required" }
        require(threadConfigurations.all { it in LocalLlamaRuntime.MIN_THREADS..LocalLlamaRuntime.MAX_THREADS }) {
            "Thread configurations must be 1..8"
        }
        require(threadConfigurations.distinct().size == threadConfigurations.size) {
            "Thread configurations must be unique"
        }
        require(repetitions >= 1) { "repetitions must be >= 1" }
        require(contextTokens in 256..8192) { "contextTokens must be 256..8192" }
        require(maxTokens in 1..256) { "maxTokens must be 1..256" }
        require(delayBetweenRunsMs >= 0L) { "delayBetweenRunsMs must be >= 0" }

        check(QwenModelDownloader(context).isInstalled()) {
            "Qwen3 model is not installed and SHA-256 verified"
        }

        val modelFile = QwenLocalModel.file(context)
        val runtime = LocalLlamaRuntime()
        val runtimeVersion = runtime.runtimeVersion()
        val benchmarkId = UUID.randomUUID().toString()
        val benchmarkStartedAt = System.currentTimeMillis()
        val totalRuns = threadConfigurations.size * repetitions
        val samples = mutableListOf<LocalInferenceBenchmarkSample>()
        var completedRuns = 0

        repeat(repetitions) { repetitionIndex ->
            threadConfigurations.forEach { threadCount ->
                if (samples.isNotEmpty() && delayBetweenRunsMs > 0L) {
                    SystemClock.sleep(delayBetweenRunsMs)
                }
                onProgress(threadCount, repetitionIndex + 1, totalRuns)

                val before = DeviceMonitor(context).collectSnapshot(includeSystemTelemetry = false)
                val pssBeforeKb = Debug.getPss()
                val cpuBeforeMs = Process.getElapsedCpuTime()
                val startedAtMs = System.currentTimeMillis()
                val startedElapsed = SystemClock.elapsedRealtime()

                val resultText = runtime.generate(
                    modelFile = modelFile,
                    prompt = QwenLocalModel.prompt(FIXED_BENCHMARK_PROMPT),
                    contextTokens = contextTokens,
                    maxTokens = maxTokens,
                    threads = threadCount
                )

                val finishedElapsed = SystemClock.elapsedRealtime()
                val finishedAtMs = System.currentTimeMillis()
                val cpuAfterMs = Process.getElapsedCpuTime()
                val pssAfterKb = Debug.getPss()
                val after = DeviceMonitor(context).collectSnapshot(includeSystemTelemetry = false)
                val native = parseSuccessfulNativeResult(resultText)

                samples += LocalInferenceBenchmarkSample(
                    observationId = UUID.randomUUID().toString(),
                    threadCount = threadCount,
                    repetition = repetitionIndex + 1,
                    startedAtMs = startedAtMs,
                    finishedAtMs = finishedAtMs,
                    wallTimeMs = (finishedElapsed - startedElapsed).coerceAtLeast(0L),
                    processCpuTimeMs = (cpuAfterMs - cpuBeforeMs).coerceAtLeast(0L),
                    processPssBeforeKb = pssBeforeKb,
                    processPssAfterKb = pssAfterKb,
                    processPssDeltaKb = pssAfterKb - pssBeforeKb,
                    availableRamBeforeMb = before.availableRamMb,
                    availableRamAfterMb = after.availableRamMb,
                    batteryBeforePercent = before.batteryPercent,
                    batteryAfterPercent = after.batteryPercent,
                    temperatureBeforeC = before.batteryTemperatureC,
                    temperatureAfterC = after.batteryTemperatureC,
                    thermalStatusBefore = before.thermalStatus,
                    thermalStatusAfter = after.thermalStatus,
                    promptTokens = native.getInt("promptTokens"),
                    generatedTokens = native.getInt("generatedTokens"),
                    loadMs = native.getLong("loadMs"),
                    tokenizationMs = native.getLong("tokenizationMs"),
                    contextInitMs = native.getLong("contextInitMs"),
                    promptDecodeMs = native.getLong("promptDecodeMs"),
                    generationMs = native.getLong("generationMs"),
                    generationTokensPerSecond = native.getDouble("generationTokensPerSecond"),
                    totalNativeMs = native.getLong("totalNativeMs"),
                    modelOutputContainsThink = native.getBoolean("modelOutputContainsThink")
                )
                completedRuns++
                check(completedRuns <= totalRuns)
            }
        }

        return LocalInferenceBenchmarkResult(
            benchmarkId = benchmarkId,
            schemaVersion = SCHEMA_VERSION,
            startedAtMs = benchmarkStartedAt,
            finishedAtMs = System.currentTimeMillis(),
            modelId = QwenLocalModel.MODEL_ID,
            runtimeVersion = runtimeVersion,
            contextTokens = contextTokens,
            maxTokens = maxTokens,
            threadConfigurations = threadConfigurations,
            repetitionsPerConfiguration = repetitions,
            samples = samples.toList()
        )
    }

    private fun parseSuccessfulNativeResult(resultText: String): JSONObject {
        val result = runCatching { JSONObject(resultText) }
            .getOrElse { error("Native inference returned invalid JSON") }
        check(result.optBoolean("ok", false)) {
            "Native inference failed: " + result.optString("error", "unknown_error")
        }
        check(result.optBoolean("advisoryOnly", false))
        check(!result.optBoolean("executionRequested", true))
        check(!result.optBoolean("deviceMutationAllowed", true))
        return result
    }
}
