package com.maouuusama.ai.device.optimizer.monitor

internal enum class ShizukuTelemetryOperation {
    MEMORY_INFO,
    PROCESS_MEMORY_INFO,
    CPU_STAT
}

internal object ShizukuTelemetryCommands {
    fun commandFor(operation: ShizukuTelemetryOperation): String = when (operation) {
        ShizukuTelemetryOperation.MEMORY_INFO ->
            "cat /proc/meminfo | grep -E '^(MemTotal|MemFree|MemAvailable|Cached|SwapTotal|SwapFree|SReclaimable|Shmem):'"
        ShizukuTelemetryOperation.PROCESS_MEMORY_INFO ->
            "dumpsys meminfo | grep -E '^[[:space:]]+[0-9,]+K: '"
        ShizukuTelemetryOperation.CPU_STAT ->
            "cat /proc/stat"
    }
}