package com.maouuusama.ai.device.optimizer.policy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosisEngineTest {
    private fun state(
        availableRamMb: Long,
        totalRamMb: Long = 5634L,
        batteryPercent: Int? = 50,
        isCharging: Boolean = true
    ) = DeviceState(
        availableRamMb = availableRamMb,
        totalRamMb = totalRamMb,
        batteryPercent = batteryPercent,
        isCharging = isCharging,
        isGaming = false
    )

    @Test
    fun criticalMemoryProducesHighConfidenceDiagnosis() {
        val diagnosis = DiagnosisEngine().diagnose(state(900)).single()

        assertEquals("memory.critical", diagnosis.conditionId)
        assertEquals(DiagnosisSeverity.HIGH, diagnosis.severity)
        assertEquals(0.95, diagnosis.confidence, 0.0)
        assertEquals("observe.memory_critical", diagnosis.proposedActionId)
        assertTrue(diagnosis.evidence.any { it.contains("availableRamMb=900") })
    }

    @Test
    fun lowBatteryRequiresNotCharging() {
        val diagnosis = DiagnosisEngine().diagnose(
            state(3000, batteryPercent = 15, isCharging = false)
        ).single()

        assertEquals("battery.low", diagnosis.conditionId)
        assertEquals("observe.power_pressure", diagnosis.proposedActionId)
    }

    @Test
    fun normalStateProducesInformationalDiagnosis() {
        val diagnosis = DiagnosisEngine().diagnose(
            state(3000, batteryPercent = 80, isCharging = true)
        ).single()

        assertEquals("device.normal", diagnosis.conditionId)
        assertEquals(DiagnosisSeverity.INFO, diagnosis.severity)
    }

    @Test
    fun invalidTotalRamReducesConfidence() {
        val diagnosis = DiagnosisEngine().diagnose(state(900, totalRamMb = 0)).single()

        assertEquals("memory.critical", diagnosis.conditionId)
        assertEquals(0.50, diagnosis.confidence, 0.0)
    }
}
