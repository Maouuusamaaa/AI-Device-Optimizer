package com.maouuusama.ai.device.optimizer.benchmark

data class BenchmarkSample(
    val timestampMs: Long,
    val availableRamMb: Long,
    val totalRamMb: Long,
    val batteryPercent: Int?,
    val isCharging: Boolean,
    val temperatureC: Double?,
    val storageAvailableMb: Long,
    val processCpuTimeMs: Long,
    val collectionDurationMs: Long
)
