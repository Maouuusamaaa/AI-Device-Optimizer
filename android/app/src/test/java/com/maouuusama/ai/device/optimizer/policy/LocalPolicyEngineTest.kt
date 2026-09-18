package com.maouuusama.ai.device.optimizer.policy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalPolicyEngineTest {
    private val engine = LocalPolicyEngine()

    @Test
    fun normalDeviceProducesNormalDecision() {
        val result = engine.evaluate(DeviceState(4000, 8000, 80, false, false))
        assertEquals("device.normal", result.single().policyId)
    }

    @Test
    fun lowMemoryProducesObservation() {
        val result = engine.evaluate(DeviceState(1000, 8000, 80, false, false))
        assertEquals("memory.low", result.single().policyId)
        assertEquals(PolicySeverity.HIGH, result.single().severity)
        assertEquals("observe.background_pressure", result.single().proposedActionId)
    }

    @Test
    fun lowBatteryWhileNotChargingProducesPowerObservation() {
        val result = engine.evaluate(DeviceState(4000, 8000, 15, false, false))
        assertEquals("battery.low", result.single().policyId)
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
}