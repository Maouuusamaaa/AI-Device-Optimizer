package com.maouuusama.ai.device.optimizer.gameboost

import com.maouuusama.ai.device.optimizer.monitor.SystemCpuSnapshot
import com.maouuusama.ai.device.optimizer.monitor.SystemMemorySnapshot
import com.maouuusama.ai.device.optimizer.monitor.SystemProcessSnapshot
import com.maouuusama.ai.device.optimizer.monitor.SystemTelemetrySnapshot
import com.maouuusama.ai.device.optimizer.monitor.SystemTelemetryStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class GameBoostTelemetryCapabilitiesTest {
    @Test
    fun availableSnapshotExposesCpuAndMemoryCapabilities() {
        val snapshot = SystemTelemetrySnapshot(
            status = SystemTelemetryStatus.AVAILABLE,
            provider = "android-api-fallback",
            memory = SystemMemorySnapshot(
                memTotalKb = 1024L,
                memFreeKb = 256L,
                memAvailableKb = 512L,
                cachedKb = 128L,
                swapTotalKb = 512L,
                swapFreeKb = 256L,
                shmemKb = null,
                sreclaimableKb = null
            ),
            processes = emptyList<SystemProcessSnapshot>(),
            cpu = SystemCpuSnapshot(
                userJiffies = 10L,
                niceJiffies = 0L,
                systemJiffies = 5L,
                idleJiffies = 20L,
                ioWaitJiffies = 0L,
                irqJiffies = 0L,
                softIrqJiffies = 0L,
                totalJiffies = 35L,
                utilizationPercent = 42.5
            )
        )

        val telemetry = GameBoostTelemetryAdapter.from(snapshot)

        assertEquals(CapabilityStatus.AVAILABLE, telemetry.cpu.status)
        assertEquals(CapabilityStatus.AVAILABLE, telemetry.memory.status)
        assertEquals("android-api-fallback", telemetry.cpu.source)
        assertEquals("android-api-fallback", telemetry.memory.source)
    }
}
