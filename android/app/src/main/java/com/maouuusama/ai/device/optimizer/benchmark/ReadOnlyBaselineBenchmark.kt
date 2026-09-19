package com.maouuusama.ai.device.optimizer.benchmark

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Process
import android.os.StatFs
import com.maouuusama.ai.device.optimizer.monitor.DeviceMonitor

class ReadOnlyBaselineBenchmark(private val context: Context) {

    fun run(
        sampleCount: Int = DEFAULT_SAMPLE_COUNT,
        intervalMs: Long = DEFAULT_INTERVAL_MS,
        onSample: ((BenchmarkSample) -> Unit)? = null
    ): List<BenchmarkSample> {
        require(sampleCount > 0) { "sampleCount must be greater than zero" }
        require(intervalMs >= 0) { "intervalMs must not be negative" }

        val samples = ArrayList<BenchmarkSample>(sampleCount)
        val monitor = DeviceMonitor(context)

        repeat(sampleCount) { index ->
            val startedNs = System.nanoTime()
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
                collectionDurationMs = (System.nanoTime() - startedNs) / 1_000_000L,
                processes = snapshot.processes
            )

            samples += sample
            onSample?.invoke(sample)

            if (index < sampleCount - 1 && intervalMs > 0) {
                Thread.sleep(intervalMs)
            }
        }

        return samples
    }

    companion object {
        const val DEFAULT_SAMPLE_COUNT = 30
        const val DEFAULT_INTERVAL_MS = 2_000L
    }
}
