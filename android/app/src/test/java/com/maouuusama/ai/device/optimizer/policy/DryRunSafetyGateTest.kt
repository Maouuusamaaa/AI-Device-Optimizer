package com.maouuusama.ai.device.optimizer.policy

import com.maouuusama.ai.device.optimizer.monitor.DeviceSnapshot

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DryRunSafetyGateTest {
    @Test
    fun catalogContainsOnlyKnownObservationCandidates() {
        assertNotNull(ActionCatalog.find("observe.memory_critical"))
        assertNotNull(ActionCatalog.find("observe.memory_pressure"))
        assertNotNull(ActionCatalog.find("observe.power_pressure"))
        assertTrue(ActionCatalog.all().all { it.requiredPermission == "none" })
        assertTrue(ActionCatalog.all().all { it.reversible })
        assertTrue(ActionCatalog.all().all { it.risk == ActionRisk.LOW })
    }

    @Test
    fun normalDryRunProposalIsNeverExecutionAllowed() {
        val snapshot = DeviceSnapshot(1_000L, 33, "itel", "P661N", 8000L, 1500L, 44, false)
        val proposal = DryRunPolicyEvaluator().evaluate(snapshot)
        val result = DryRunSafetyGate().evaluate(proposal)
        assertFalse(result.allowed)
        assertTrue(result.reasons.any { it.contains("Execution remains disabled") })
    }

    @Test
    fun unknownActionIsBlockedAndExplained() {
        val snapshot = DeviceSnapshot(2_000L, 33, "itel", "P661N", 8000L, 900L, 44, false)
        val base = DryRunPolicyEvaluator().evaluate(snapshot)
        val proposal = base.copy(decisions = base.decisions.map { it.copy(proposedActionId = "unknown.action") })
        val result = DryRunSafetyGate().evaluate(proposal)
        assertFalse(result.allowed)
        assertTrue(result.reasons.any { it.contains("not present in the action catalog") })
    }
}
