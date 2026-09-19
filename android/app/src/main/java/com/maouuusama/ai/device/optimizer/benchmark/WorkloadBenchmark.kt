package com.maouuusama.ai.device.optimizer.benchmark

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Process
import android.os.StatFs
import com.maouuusama.ai.device.optimizer.monitor.DeviceMonitor

/**
 * Collects read-only device telemetry for a fixed wall-clock workload window.
 *
 * The caller can move another app/game to the foreground while this benchmark
 * continues. The optimizer foreground service keeps the application process
 * eligible for background execution.
 */
class WorkloadBenchmark(private val context: Context) {

    fun run(
        durationMs: Long = DEFAULT_DURATION_MS,
        intervalMs: Long = DEFAULT_INTERVAL_MS,
        onSample: ((sample: BenchmarkSample, index: Int, elapsedMs: Long) -> Unit)? = null
    ): List<BenchmarkSample> {
        require(durationMs > 0) { "durationMs must be greater than zero" }
        require(intervalMs > 0) { "intervalMs must be greater than zero" }

        val samples = ArrayList<BenchmarkSample>()
        val monitor = DeviceMonitor(context.applicationContext)
        val startedAt = System.currentTimeMillis()
        var nextSampleAt = startedAt

        while (true) {
            val now = System.currentTimeMillis()
            if (samples.isNotEmpty() && now >= startedAt + durationMs) break

            val sampleStartedNs = System.nanoTime()
            val snapshot = monitor.collectSnapshot()
            val battery = context.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )
            val temperatureC = battery
                ?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
                ?.takeIf { it != Int.MIN_VALUE }
                ?.div(10.0)

            val storage = StatFs(context.filesDir.absolutePath)
            val storageAvailableMb =
                (storage.availableBytes / 1024L / 1024L).coerceAtLeast(0L)

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

            samples += sample
            onSample?.invoke(sample, samples.lastIndex + 1, snapshot.timestampMs - startedAt)

            nextSampleAt += intervalMs
            val sleepMs = nextSampleAt - System.currentTimeMillis()
            if (sleepMs > 0) Thread.sleep(sleepMs)
        }

        return samples
    }

    companion object {
        const val DEFAULT_DURATION_MS = 5 * 60 * 1_000L
        const val DEFAULT_INTERVAL_MS = 2_000L
    }
}
