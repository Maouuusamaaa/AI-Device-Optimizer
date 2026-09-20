package com.maouuusama.ai.device.optimizer.policy

import com.maouuusama.ai.device.optimizer.monitor.DeviceSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PolicySimulationEvaluatorTest {
    private fun snapshot(ram: Long, battery: Int, charging: Boolean) = DeviceSnapshot(1L, 33, "ITEL", "itel P661N", 5634L, ram, battery, charging)
    @Test fun evaluatorProducesDiagnosisSimulationAndDeniedSafetyGate() {
        val plan = PolicySimulationEvaluator().evaluate(snapshot(900, 50, true))
        assertEquals("memory.critical", plan.diagnoses.single().conditionId)
        assertEquals("observe.memory_critical", plan.simulation.proposedActionIds.single())
        assertFalse(plan.simulation.executionAllowed)
        assertFalse(plan.safetyGate.allowed)
        assertFalse(plan.safetyGate.reasons.isEmpty())
    }
}