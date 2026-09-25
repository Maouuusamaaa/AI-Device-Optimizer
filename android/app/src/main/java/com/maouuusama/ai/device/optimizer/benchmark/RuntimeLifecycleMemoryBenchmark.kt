package com.maouuusama.ai.device.optimizer.benchmark

import android.content.Context
import com.maouuusama.ai.device.optimizer.localai.LocalLlamaRuntime
import com.maouuusama.ai.device.optimizer.localai.QwenLocalModel
import com.maouuusama.ai.device.optimizer.monitor.DeviceMonitor

data class RuntimeLifecycleSample(
    val phase: String,
    val phaseSampleIndex: Int,
    val sample: BenchmarkSample
)

data class RuntimeLifecycleEvent(
    val name: String,
    val timestampMs: Long
)

data class RuntimeLifecycleDiagnosticResult(
    val samples: List<RuntimeLifecycleSample>,
    val events: List<RuntimeLifecycleEvent>
)

class RuntimeLifecycleMemoryBenchmark(private val context: Context) {
    companion object {
        const val BASELINE_DURATION_MS = 60_000L
        const val POST_CLEANUP_DURATION_MS = 60_000L
        const val POST_RESET_DURATION_MS = 300_000L
        const val INTERVAL_MS = 2_000L
        const val MAX_TOKENS = 64
        const val THREADS = 4
    }

    fun run(onPhase: (String) -> Unit = {}): RuntimeLifecycleDiagnosticResult {
        val all = mutableListOf<RuntimeLifecycleSample>()
        val events = mutableListOf<RuntimeLifecycleEvent>()
        fun event(name: String) { events += RuntimeLifecycleEvent(name, System.currentTimeMillis()) }
        onPhase("baseline")
        event("baseline_start")
        collect("baseline", BASELINE_DURATION_MS, all)
        event("baseline_end")
        onPhase("inference")
        event("inference_start")
        val runtime = LocalLlamaRuntime()
        check(runtime.isAvailable()) { "Local AI runtime unavailable" }
        val model = QwenLocalModel.file(context)
        check(model.isFile && model.length() > 0L) { "Qwen3 model is not installed" }
        val prompt = QwenLocalModel.prompt(
            "Perform one advisory-only diagnostic inference. " +
                "Do not request or execute device mutations."
        )
        runtime.generateForLifecycleDiagnostic(model, prompt, maxTokens = MAX_TOKENS, threads = THREADS)
        event("inference_end")
        event("post_cleanup_start")
        collect("post_cleanup", POST_CLEANUP_DURATION_MS, all)
        event("post_cleanup_end")
        onPhase("runtime_reset")
        event("reset_before")
        val resetCompletedAtMs = runtime.resetRuntime()
        events += RuntimeLifecycleEvent("reset_native_completed", resetCompletedAtMs)
        event("post_reset_start")
        collect("post_reset", POST_RESET_DURATION_MS, all)
        event("post_reset_end")
        return RuntimeLifecycleDiagnosticResult(all, events)
    }

    private fun collect(
        phase: String,
        durationMs: Long,
        output: MutableList<RuntimeLifecycleSample>
    ) {
        val monitor = DeviceMonitor(context.applicationContext)
        val startedNs = System.nanoTime()
        val deadlineNs = startedNs + durationMs * 1_000_000L
        var nextSampleNs = startedNs
        var index = 0
        while (true) {
            val waitNs = nextSampleNs - System.nanoTime()
            if (waitNs > 0L) {
                Thread.sleep(waitNs / 1_000_000L, (waitNs % 1_000_000L).toInt())
            }
            val nowNs = System.nanoTime()
            if (index > 0 && nowNs >= deadlineNs) break
            val sampleStartedNs = System.nanoTime()
            val snapshot = monitor.collectSnapshot(includeSystemTelemetry = false)
            val storage = android.os.StatFs(context.filesDir.absolutePath)
            val sample = BenchmarkSample(
                timestampMs = snapshot.timestampMs,
                availableRamMb = snapshot.availableRamMb,
                totalRamMb = snapshot.totalRamMb,
                batteryPercent = snapshot.batteryPercent,
                isCharging = snapshot.isCharging,
                temperatureC = snapshot.batteryTemperatureC,
                storageAvailableMb = (storage.availableBytes / 1024L / 1024L).coerceAtLeast(0L),
                processCpuTimeMs = android.os.Process.getElapsedCpuTime(),
                collectionDurationMs = (System.nanoTime() - sampleStartedNs) / 1_000_000L,
                processes = snapshot.processes
            )
            index += 1
            output += RuntimeLifecycleSample(phase, index, sample)
            nextSampleNs = sampleStartedNs + INTERVAL_MS * 1_000_000L
            if (System.nanoTime() >= deadlineNs) break
        }
    }
}
