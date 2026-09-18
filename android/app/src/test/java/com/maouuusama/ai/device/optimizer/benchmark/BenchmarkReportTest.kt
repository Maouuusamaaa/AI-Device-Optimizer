package com.maouuusama.ai.device.optimizer.benchmark

import org.junit.Assert.assertEquals
import org.junit.Test

class BenchmarkReportTest {

    @Test
    fun reportUsesFirstAndLastSamplesForSessionValues() {
        val samples = listOf(
            BenchmarkSample(
                timestampMs = 1_000L,
                availableRamMb = 1000L,
                totalRamMb = 5000L,
                batteryPercent = 50,
                isCharging = false,
                temperatureC = 30.0,
                storageAvailableMb = 10_000L,
                processCpuTimeMs = 10L,
                collectionDurationMs = 2L
            ),
            BenchmarkSample(
                timestampMs = 3_000L,
                availableRamMb = 1500L,
                totalRamMb = 5000L,
                batteryPercent = 49,
                isCharging = false,
                temperatureC = 31.0,
                storageAvailableMb = 9_900L,
                processCpuTimeMs = 15L,
                collectionDurationMs = 4L
            )
        )

        val report = BenchmarkReport.from(samples)

        assertEquals(2, report.sampleCount)
        assertEquals(2_000L, report.durationMs)
        assertEquals(1250L, report.averageAvailableRamMb)
        assertEquals(1000L, report.minimumAvailableRamMb)
        assertEquals(1500L, report.maximumAvailableRamMb)
        assertEquals(50, report.startBatteryPercent)
        assertEquals(49, report.endBatteryPercent)
        assertEquals(30.0, report.startTemperatureC!!, 0.001)
        assertEquals(31.0, report.endTemperatureC!!, 0.001)
        assertEquals(9_900L, report.storageAvailableMb)
    }
}
