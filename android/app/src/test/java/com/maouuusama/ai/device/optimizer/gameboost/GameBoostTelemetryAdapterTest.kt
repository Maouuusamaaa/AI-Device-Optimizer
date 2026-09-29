package com.maouuusama.ai.device.optimizer.gameboost

import com.maouuusama.ai.device.optimizer.monitor.SystemTelemetrySnapshot
import com.maouuusama.ai.device.optimizer.monitor.SystemTelemetryStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class GameBoostTelemetryAdapterTest {
    @Test
    fun unavailableSystemTelemetryProducesUnavailableCapability() {
        val snapshot = SystemTelemetrySnapshot(
            status = SystemTelemetryStatus.UNAVAILABLE,
            provider = "android-api-fallback",
            memory = null,
            processes = emptyList(),
            cpu = null
        )

        val telemetry = GameBoostTelemetryAdapter.from(snapshot)

        assertEquals(CapabilityStatus.UNAVAILABLE, telemetry.systemTelemetry.status)
        assertEquals("android-api-fallback", telemetry.systemTelemetry.source)
    }
}
