package com.maouuusama.ai.device.optimizer.benchmark

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MonitorSampleTest {
    @Test
    fun sampleKeepsTelemetryValues() {
        val sample = MonitorSample(100L, 2048L, 75, 2L)
        assertEquals(2048L, sample.availableRamMb)
        assertEquals(75, sample.batteryPercent)
        assertEquals(2L, sample.collectionDurationMs)
    }

    @Test
    fun unsupportedBatteryIsRepresentedAsNull() {
        val sample = MonitorSample(100L, 2048L, null, 2L)
        assertNull(sample.batteryPercent)
    }
}