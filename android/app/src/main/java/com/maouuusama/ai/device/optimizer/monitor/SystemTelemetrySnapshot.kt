package com.maouuusama.ai.device.optimizer.monitor

enum class SystemTelemetryStatus {
    AVAILABLE,
    PERMISSION_REQUIRED,
    UNAVAILABLE,
    ERROR
}

data class SystemTelemetrySnapshot(
    val status: SystemTelemetryStatus,
    val provider: String,
    val memory: SystemMemorySnapshot?,
    val processes: List<SystemProcessSnapshot>,
    val cpu: SystemCpuSnapshot?,
    val errorMessage: String? = null
)
