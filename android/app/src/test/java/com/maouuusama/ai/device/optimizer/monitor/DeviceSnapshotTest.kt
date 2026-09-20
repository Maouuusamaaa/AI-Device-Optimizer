package com.maouuusama.ai.device.optimizer.monitor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceSnapshotTest {
    @Test
    fun batteryPercentAndHealthFieldsAreRepresentedCorrectly() {
        val snapshot = DeviceSnapshot(
            timestampMs = 0L,
            androidApi = 33,
            manufacturer = "test",
            model = "test",
            totalRamMb = 4096L,
            availableRamMb = 2048L,
            batteryPercent = 75,
            isCharging = false,
            batteryTemperatureC = 36.5,
            thermalStatus = 0,
            storageTotalBytes = 1000L,
            storageFreeBytes = 250L,
            networkTransport = "WIFI",
            networkValidated = true,
            isInteractive = true,
            uptimeMs = 12345L
        )

        assertEquals(75, snapshot.batteryPercent)
        assertEquals(36.5, snapshot.batteryTemperatureC ?: Double.NaN, 0.001)
        assertEquals(0, snapshot.thermalStatus)
        assertEquals(250L, snapshot.storageFreeBytes)
        assertEquals("WIFI", snapshot.networkTransport)
        assertTrue(snapshot.networkValidated == true)
        assertTrue(snapshot.isInteractive == true)
        assertEquals(12345L, snapshot.uptimeMs)
    }

    @Test
    fun healthFieldsRemainOptionalWhenUnavailable() {
        val snapshot = DeviceSnapshot(
            timestampMs = 0L,
            androidApi = 29,
            manufacturer = "test",
            model = "test",
            totalRamMb = 4096L,
            availableRamMb = 2048L,
            batteryPercent = null,
            isCharging = false
        )

        assertEquals(null, snapshot.batteryTemperatureC)
        assertEquals(null, snapshot.thermalStatus)
        assertEquals(null, snapshot.storageFreeBytes)
        assertEquals(null, snapshot.networkTransport)
        assertEquals(null, snapshot.networkValidated)
    }
}
