package com.maouuusama.ai.device.optimizer.benchmark

import android.content.Context
import com.maouuusama.ai.device.optimizer.monitor.DeviceMonitor

data class MonitorSample(
    val timestampMs: Long,
    val availableRamMb: Long,
    val batteryPercent: Int?,
    val collectionDurationMs: Long
)

class MonitorBenchmark(context: Context) {
    private val monitor = DeviceMonitor(context.applicationContext)

    fun sample(): MonitorSample {
        val startedAt = System.nanoTime()
        val snapshot = monitor.collectSnapshot()
        return MonitorSample(
            timestampMs = snapshot.timestampMs,
            availableRamMb = snapshot.availableRamMb,
            batteryPercent = snapshot.batteryPercent,
            collectionDurationMs = (System.nanoTime() - startedAt) / 1_000_000
        )
    }
}