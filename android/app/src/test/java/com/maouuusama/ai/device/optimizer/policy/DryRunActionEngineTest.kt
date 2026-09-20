package com.maouuusama.ai.device.optimizer.policy

import org.junit.Assert.*
import org.junit.Test

class DryRunActionEngineTest {
    private fun proposal(actionId: String? = "observe.memory_pressure") =
        DryRunPolicyProposal(DeviceState(1400L, 5634L, 50, true, false), listOf(PolicyDecision("memory.pressure", PolicySeverity.ADVISORY, "test", actionId)))

    @Test fun knownActionProducesSimulationWithoutAuthorization() {
        val result = DryRunActionEngine().simulate(proposal())
        assertEquals(ActionSimulationStatus.SIMULATED, result.single().status)
        assertTrue(result.single().message.contains("no device mutation"))
        assertTrue(result.single().preconditionChecks.all { it.endsWith("PASS") })
        assertTrue(result.single().measurementPlan.isNotBlank())
        assertTrue(result.single().rollbackPlan.isNotBlank())
    }

    @Test fun unknownActionIsBlockedWithoutExecution() {
        val result = DryRunActionEngine().simulate(proposal("not.allowlisted"))
        assertEquals(ActionSimulationStatus.BLOCKED, result.single().status)
        assertTrue(result.single().message.contains("allowlist"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun executionAuthorizingGateIsRejected() {
        DryRunActionEngine().simulate(proposal(), SafetyGateResult(true, emptyList(), emptyList()))
    }

    @Test fun multipleDecisionsRemainIndependent() {
        val proposal = DryRunPolicyProposal(DeviceState(900L, 5634L, 50, true, false), listOf(
            PolicyDecision("memory.critical", PolicySeverity.HIGH, "test", "observe.memory_critical"),
            PolicyDecision("memory.pressure", PolicySeverity.ADVISORY, "test", "observe.memory_pressure")
        ))
        val result = DryRunActionEngine().simulate(proposal)
        assertEquals(2, result.size)
        assertEquals(listOf("observe.memory_critical", "observe.memory_pressure"), result.map { it.actionId })
    }
}
