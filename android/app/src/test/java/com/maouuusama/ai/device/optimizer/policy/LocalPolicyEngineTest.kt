package com.maouuusama.ai.device.optimizer.policy

import com.maouuusama.ai.device.optimizer.monitor.DeviceSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalPolicyEngineTest {
    private val engine = LocalPolicyEngine()

    @Test
    fun normalDeviceProducesNormalDecision() {
        val result = engine.evaluate(DeviceState(4000, 8000, 80, false, false))
        assertEquals("device.normal", result.single().policyId)
        assertEquals(PolicyMode.DRY_RUN, result.single().mode)
    }

    @Test
    fun lowMemoryProducesObservation() {
        val result = engine.evaluate(DeviceState(1000, 8000, 80, false, false))
        assertEquals("memory.low", result.single().policyId)
        assertEquals(PolicySeverity.HIGH, result.single().severity)
        assertEquals("observe.background_pressure", result.single().proposedActionId)
        assertEquals(PolicyMode.DRY_RUN, result.single().mode)
    }

    @Test
    fun lowBatteryWhileNotChargingProducesPowerObservation() {
        val result = engine.evaluate(DeviceState(4000, 8000, 15, false, false))
        assertEquals("battery.low", result.single().policyId)
        assertEquals(PolicyMode.DRY_RUN, result.single().mode)
    }

    @Test
    fun lowBatteryWhileChargingDoesNotTriggerPolicy() {
        val result = engine.evaluate(DeviceState(4000, 8000, 15, true, false))
        assertEquals("device.normal", result.single().policyId)
    }

    @Test
    fun gamingProducesProtectiveDecision() {
        val result = engine.evaluate(DeviceState(4000, 8000, 80, false, true))
        assertTrue(result.any { it.policyId == "workload.gaming" })
        assertTrue(result.none { it.proposedActionId == "observe.background_pressure" })
    }

    @Test
    fun missingBatteryDoesNotTriggerLowBatteryPolicy() {
        val result = engine.evaluate(DeviceState(4000, 8000, null, false, false))
        assertEquals("device.normal", result.single().policyId)
    }

    @Test
    fun snapshotCanBeConvertedToPolicyState() {
        val snapshot = DeviceSnapshot(
            timestampMs = 1L,
            androidApi = 33,
            manufacturer = "ITEL",
            model = "itel P661N",
            totalRamMb = 5634L,
            availableRamMb = 1800L,
            batteryPercent = 44,
            isCharging = false
        )
        val state = DeviceState.fromSnapshot(snapshot)
        assertEquals(1800L, state.availableRamMb)
        assertEquals(5634L, state.totalRamMb)
        assertEquals(44, state.batteryPercent)
        assertEquals(false, state.isCharging)
    }
}
