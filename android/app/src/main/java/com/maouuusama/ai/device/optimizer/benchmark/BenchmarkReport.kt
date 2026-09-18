package com.maouuusama.ai.device.optimizer.benchmark

import kotlin.math.roundToLong

data class BenchmarkReport(
    val sampleCount: Int,
    val durationMs: Long,
    val averageAvailableRamMb: Long,
    val minimumAvailableRamMb: Long,
    val maximumAvailableRamMb: Long,
    val averageCollectionDurationMs: Double,
    val startBatteryPercent: Int?,
    val endBatteryPercent: Int?,
    val startTemperatureC: Double?,
    val endTemperatureC: Double?,
    val storageAvailableMb: Long
) {
    companion object {
        fun from(samples: List<BenchmarkSample>): BenchmarkReport {
            require(samples.isNotEmpty()) { "samples must not be empty" }

            val first = samples.first()
            val last = samples.last()
            val ramValues = samples.map { it.availableRamMb }

            return BenchmarkReport(
                sampleCount = samples.size,
                durationMs = (last.timestampMs - first.timestampMs).coerceAtLeast(0L),
                averageAvailableRamMb = ramValues.average().roundToLong(),
                minimumAvailableRamMb = ramValues.minOrNull() ?: first.availableRamMb,
                maximumAvailableRamMb = ramValues.maxOrNull() ?: first.availableRamMb,
                averageCollectionDurationMs =
                    samples.map { it.collectionDurationMs }.average(),
                startBatteryPercent = first.batteryPercent,
                endBatteryPercent = last.batteryPercent,
                startTemperatureC = first.temperatureC,
                endTemperatureC = last.temperatureC,
                storageAvailableMb = last.storageAvailableMb
            )
        }
    }
}
