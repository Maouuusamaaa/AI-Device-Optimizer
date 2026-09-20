package com.maouuusama.ai.device.optimizer.policy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PolicySimulatorTest {
    @Test fun criticalDiagnosisBecomesDryRunHighSeverityDecision() {
        val diagnosis = Diagnosis("memory.critical", DiagnosisSeverity.HIGH, 0.95, listOf("availableRamMb=900"), "observe.memory_critical")
        val result = PolicySimulator().simulate(listOf(diagnosis))
        assertFalse(result.executionAllowed)
        assertEquals(listOf("observe.memory_critical"), result.proposedActionIds)
        assertEquals(PolicyMode.DRY_RUN, result.decisions.single().mode)
        assertEquals(PolicySeverity.HIGH, result.decisions.single().severity)
        assertTrue(result.decisions.single().reason.contains("availableRamMb=900"))
    }
    @Test(expected = IllegalArgumentException::class) fun emptyDiagnosisListIsRejected() { PolicySimulator().simulate(emptyList()) }
    @Test fun multipleDiagnosesRemainSeparateAndDeduplicateActions() {
        val a = Diagnosis("memory.pressure", DiagnosisSeverity.ADVISORY, 0.95, listOf("ram"))
        val b = Diagnosis("battery.low", DiagnosisSeverity.ADVISORY, 0.95, listOf("battery"), "observe.power_pressure")
        val result = PolicySimulator().simulate(listOf(a, b))
        assertEquals(2, result.decisions.size)
        assertEquals(listOf("observe.power_pressure"), result.proposedActionIds)
    }
}