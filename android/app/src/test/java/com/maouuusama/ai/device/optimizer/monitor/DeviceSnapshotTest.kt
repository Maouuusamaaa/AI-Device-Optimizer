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
    fun gameModeEvidenceSeparatesAvailablePackagesFromCheckedPackages() {
        val snapshot = DeviceSnapshot(
            timestampMs = 0L,
            androidApi = 33,
            manufacturer = "test",
            model = "test",
            totalRamMb = 4096L,
            availableRamMb = 2048L,
            batteryPercent = 80,
            isCharging = false,
            gameModeProviderAvailable = true,
            gameModePackages = listOf("com.example.game", "com.example.other"),
            gameModeCheckedPackages = listOf("com.example.game"),
            gameModeProviderError = null
        )

        assertTrue(snapshot.gameModeProviderAvailable)
        assertEquals(listOf("com.example.game", "com.example.other"), snapshot.gameModePackages)
        assertEquals(listOf("com.example.game"), snapshot.gameModeCheckedPackages)
        assertEquals(null, snapshot.gameModeProviderError)
    }

    @Test
    fun gameModeEvidenceDefaultsToUnavailableWithoutProviderData() {
        val snapshot = DeviceSnapshot(
            timestampMs = 0L,
            androidApi = 33,
            manufacturer = "test",
            model = "test",
            totalRamMb = 4096L,
            availableRamMb = 2048L,
            batteryPercent = null,
            isCharging = false
        )

        assertTrue(!snapshot.gameModeProviderAvailable)
        assertEquals(emptyList<String>(), snapshot.gameModePackages)
        assertEquals(emptyList<String>(), snapshot.gameModeCheckedPackages)
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
