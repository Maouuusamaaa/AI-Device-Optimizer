package com.maouuusama.ai.device.optimizer.benchmark

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Process
import android.os.StatFs
import com.maouuusama.ai.device.optimizer.monitor.DeviceMonitor

data class RecoveryBenchmarkSample(
    val phase: String,
    val cycle: Int,
    val phaseSampleIndex: Int,
    val sample: BenchmarkSample
)

class WorkloadRecoveryBenchmark(private val context: Context) {
    companion object {
        const val DEFAULT_BASELINE_DURATION_MS = 60_000L
        const val DEFAULT_WORKLOAD_DURATION_MS = 5 * 60_000L
        const val DEFAULT_RECOVERY_DURATION_MS = 2 * 60_000L
        const val DEFAULT_INTERVAL_MS = 2_000L
        const val DEFAULT_CYCLE_COUNT = 2
        const val WORKLOAD_SWITCH_DELAY_MS = 5_000L
    }

    fun run(
        baselineDurationMs: Long = DEFAULT_BASELINE_DURATION_MS,
        workloadDurationMs: Long = DEFAULT_WORKLOAD_DURATION_MS,
        recoveryDurationMs: Long = DEFAULT_RECOVERY_DURATION_MS,
        intervalMs: Long = DEFAULT_INTERVAL_MS,
        cycleCount: Int = DEFAULT_CYCLE_COUNT,
        onPhase: (phase: String) -> Unit = {},
        onSample: (RecoveryBenchmarkSample) -> Unit = {}
    ): List<RecoveryBenchmarkSample> {
        require(baselineDurationMs > 0L)
        require(workloadDurationMs > 0L)
        require(recoveryDurationMs > 0L)
        require(intervalMs > 0L)
        require(cycleCount > 0)
        val all = mutableListOf<RecoveryBenchmarkSample>()

        onPhase("baseline")
        collectPhase("baseline", 0, baselineDurationMs, intervalMs, all, onSample)

        repeat(cycleCount) {
            onPhase("workload")
            Thread.sleep(WORKLOAD_SWITCH_DELAY_MS)
            collectPhase("workload", it + 1, workloadDurationMs, intervalMs, all, onSample)

            onPhase("recovery")
            collectPhase("recovery", it + 1, recoveryDurationMs, intervalMs, all, onSample)
        }
        return all
    }

    private fun collectPhase(
        phase: String,
        cycle: Int,
        durationMs: Long,
        intervalMs: Long,
        output: MutableList<RecoveryBenchmarkSample>,
        onSample: (RecoveryBenchmarkSample) -> Unit
    ) {
        val monitor = DeviceMonitor(context.applicationContext)
        val phaseStartedNs = System.nanoTime()
        val phaseDeadlineNs = phaseStartedNs + durationMs * 1_000_000L
        var nextSampleNs = phaseStartedNs
        var index = 0
        while (true) {
            val waitNs = nextSampleNs - System.nanoTime()
            if (waitNs > 0L) {
                val sleepMs = waitNs / 1_000_000L
                val sleepNs = (waitNs % 1_000_000L).toInt()
                Thread.sleep(sleepMs, sleepNs)
            }
            val nowNs = System.nanoTime()
            if (index > 0 && nowNs >= phaseDeadlineNs) break
            val sampleStartedNs = System.nanoTime()
            val snapshot = monitor.collectSnapshot()
            val battery = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val temperatureC = battery
                ?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
                ?.takeIf { it != Int.MIN_VALUE }
                ?.div(10.0)
            val storage = StatFs(context.filesDir.absolutePath)
            val storageAvailableMb = (storage.availableBytes / 1024L / 1024L).coerceAtLeast(0L)
            val sample = BenchmarkSample(
                timestampMs = snapshot.timestampMs,
                availableRamMb = snapshot.availableRamMb,
                totalRamMb = snapshot.totalRamMb,
                batteryPercent = snapshot.batteryPercent,
                isCharging = snapshot.isCharging,
                temperatureC = temperatureC,
                storageAvailableMb = storageAvailableMb,
                processCpuTimeMs = Process.getElapsedCpuTime(),
                collectionDurationMs = (System.nanoTime() - sampleStartedNs) / 1_000_000L,
                processes = snapshot.processes
            )
            index += 1
            val wrapped = RecoveryBenchmarkSample(phase, cycle, index, sample)
            output += wrapped
            onSample(wrapped)
            nextSampleNs = sampleStartedNs + intervalMs * 1_000_000L
            if (System.nanoTime() >= phaseDeadlineNs) break
        }
    }
}