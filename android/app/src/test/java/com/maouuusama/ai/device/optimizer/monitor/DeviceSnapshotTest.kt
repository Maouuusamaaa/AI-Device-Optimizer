package com.maouuusama.ai.device.optimizer.monitor

import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceSnapshotTest {
    @Test
    fun batteryPercentIsRepresentedCorrectly() {
        val snapshot = DeviceSnapshot(
            timestampMs = 0L,
            androidApi = 33,
            manufacturer = "test",
            model = "test",
            totalRamMb = 4096L,
            availableRamMb = 2048L,
            batteryPercent = 75,
            isCharging = false
        )

        assertEquals(75, snapshot.batteryPercent)
    }
}
