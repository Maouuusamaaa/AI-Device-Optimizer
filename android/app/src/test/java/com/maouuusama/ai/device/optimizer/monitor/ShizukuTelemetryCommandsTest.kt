package com.maouuusama.ai.device.optimizer.monitor

import org.junit.Assert.assertEquals
import org.junit.Test

class ShizukuTelemetryCommandsTest {
    @Test
    fun mapsOnlySupportedTelemetryOperations() {
        assertEquals(
            "awk '/^cpu / { print; exit }' /proc/stat",
            ShizukuTelemetryCommands.commandFor(ShizukuTelemetryOperation.CPU_STAT)
        )
        assertEquals(
            "cat /proc/meminfo | grep -E '^(MemTotal|MemFree|MemAvailable|Cached|SwapTotal|SwapFree|SReclaimable|Shmem):'",
            ShizukuTelemetryCommands.commandFor(ShizukuTelemetryOperation.MEMORY_INFO)
        )
    }
}