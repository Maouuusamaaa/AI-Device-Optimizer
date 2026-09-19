package com.maouuusama.ai.device.optimizer.benchmark

import com.maouuusama.ai.device.optimizer.monitor.ProcessSnapshot

data class BenchmarkSample(
    val timestampMs: Long,
    val availableRamMb: Long,
    val totalRamMb: Long,
    val batteryPercent: Int?,
    val isCharging: Boolean,
    val temperatureC: Double?,
    val appFilesStorageAvailableMb: Long,
    val optimizerProcessCpuTimeMs: Long,
    val collectionDurationMs: Long,
    val processes: List<ProcessSnapshot> = emptyList()
)
