package com.maouuusama.ai.device.optimizer.policy

import com.maouuusama.ai.device.optimizer.monitor.DeviceSnapshot
import com.maouuusama.ai.device.optimizer.monitor.ProcessSnapshot
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
    fun memoryPressureThresholdProducesPressureDecision() {
        val result = engine.evaluate(DeviceState(1500, 5634, 80, false, false))
        assertEquals("memory.pressure", result.single().policyId)
        assertEquals(PolicySeverity.ADVISORY, result.single().severity)
        assertEquals("observe.memory_pressure", result.single().proposedActionId)
        assertEquals(PolicyMode.DRY_RUN, result.single().mode)
    }

    @Test
    fun memoryCriticalThresholdProducesCriticalDecision() {
        val result = engine.evaluate(DeviceState(999, 5634, 80, false, false))
        assertEquals("memory.critical", result.single().policyId)
        assertEquals(PolicySeverity.HIGH, result.single().severity)
        assertEquals("observe.memory_critical", result.single().proposedActionId)
        assertEquals(PolicyMode.DRY_RUN, result.single().mode)
    }

    @Test
    fun abovePressureThresholdRemainsNormal() {
        val result = engine.evaluate(DeviceState(1501, 5634, 80, false, false))
        assertEquals("device.normal", result.single().policyId)
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
        assertTrue(result.none { it.proposedActionId == "observe.memory_pressure" })
    }

    @Test
    fun missingBatteryDoesNotTriggerLowBatteryPolicy() {
        val result = engine.evaluate(DeviceState(4000, 8000, null, false, false))
        assertEquals("device.normal", result.single().policyId)
    }

    @Test
    fun snapshotCarriesProcessTelemetryIntoPolicyState() {
        val process = ProcessSnapshot(
            pid = 1234,
            processName = "example",
            packageNames = listOf("com.example"),
            appLabels = listOf("Example"),
            importance = 100,
            importanceLabel = "FOREGROUND",
            isForeground = true,
            pssKb = 128000L,
            rssKb = 180000L,
            swapPssKb = 32000L
        )
        val snapshot = DeviceSnapshot(
            timestampMs = 1L,
            androidApi = 33,
            manufacturer = "ITEL",
            model = "itel P661N",
            totalRamMb = 5634L,
            availableRamMb = 1800L,
            batteryPercent = 44,
            isCharging = false,
            processes = listOf(process)
        )

        val state = DeviceState.fromSnapshot(snapshot)

        assertEquals(1, state.processes.size)
        assertEquals("com.example", state.processes.single().packageNames.single())
        assertEquals(128000L, state.processes.single().pssKb)
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