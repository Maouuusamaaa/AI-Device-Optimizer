package com.maouuusama.ai.device.optimizer.policy

import org.junit.Assert.*
import org.junit.Test

class SafetyGateContractTest {
    private fun proposal(actionId: String? = "observe.memory_pressure") =
        DryRunPolicyProposal(
            DeviceState(1400L, 5634L, 50, true, false),
            listOf(PolicyDecision("memory.pressure", PolicySeverity.ADVISORY, "test", actionId))
        )

    @Test fun validObservationCandidateStillCannotExecute() {
        val result = DryRunSafetyGate().evaluate(proposal())
        assertFalse(result.allowed)
        assertTrue(SafetyBlockReason.EXECUTION_DISABLED in result.blockReasons)
    }

    @Test fun unknownActionIsStructuredAsBlocked() {
        val result = DryRunSafetyGate().evaluate(proposal("not.allowlisted"))
        assertFalse(result.allowed)
        assertTrue(SafetyBlockReason.UNKNOWN_ACTION in result.blockReasons)
        assertTrue(SafetyBlockReason.ACTION_NOT_ALLOWLISTED in result.blockReasons)
    }

    @Test fun catalogObservationActionsHaveRequiredContract() {
        ActionCatalog.all().forEach {
            assertTrue(it.id.startsWith("observe."))
            assertEquals(ActionRisk.LOW, it.risk)
            assertEquals("none", it.requiredPermission)
            assertTrue(it.reversible)
            assertTrue(it.expectedEffect.isNotBlank())
            assertTrue(it.rollback.isNotBlank())
            assertTrue(it.measurement.isNotBlank())
        }
    }
}
