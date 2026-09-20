package com.maouuusama.ai.device.optimizer.policy

import com.maouuusama.ai.device.optimizer.monitor.DeviceSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OfflineEvaluationMetadataFactoryTest {
    @Test
    fun factoryPreservesSnapshotTelemetryWithoutInference() {
        val snapshot = DeviceSnapshot(
            timestampMs = 100L,
            androidApi = 33,
            manufacturer = "ITEL",
            model = "itel P661N",
            totalRamMb = 5634L,
            availableRamMb = 2000L,
            batteryPercent = 50,
            isCharging = true,
            batteryTemperatureC = 36.5,
            thermalStatus = 0,
            storageTotalBytes = 1000L,
            storageFreeBytes = 500L,
            networkTransport = "wifi",
            networkValidated = true,
            isInteractive = true,
            uptimeMs = 1000L,
            processes = emptyList(),
            systemTelemetry = null
        )
        val metadata = OfflineEvaluationMetadataFactory.fromSnapshot(snapshot)
        assertEquals(true, metadata.charging)
        assertEquals(36.5, metadata.batteryTemperatureC, 0.0)
        assertEquals(0, metadata.thermalStatus)
        assertEquals("wifi", metadata.networkTransport)
        assertEquals(true, metadata.networkValidated)
        assertEquals(true, metadata.interactive)
        assertNull(metadata.workload)
    }
}
