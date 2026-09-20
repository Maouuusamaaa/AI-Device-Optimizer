package com.maouuusama.ai.device.optimizer.policy

import com.maouuusama.ai.device.optimizer.monitor.DeviceSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DryRunPolicyEvaluatorTest {
    @Test
    fun snapshotFlowsThroughStatePolicyAndDryRunProposal() {
        val snapshot = DeviceSnapshot(
            timestampMs = 1_000L,
            androidApi = 33,
            manufacturer = "itel",
            model = "P661N",
            totalRamMb = 8000L,
            availableRamMb = 1500L,
            batteryPercent = 44,
            isCharging = false
        )

        val proposal = DryRunPolicyEvaluator().evaluate(snapshot)

        assertEquals(snapshot.availableRamMb, proposal.state.availableRamMb)
        assertEquals(snapshot.totalRamMb, proposal.state.totalRamMb)
        assertEquals(snapshot.batteryPercent, proposal.state.batteryPercent)
        assertEquals(listOf("memory.pressure"), proposal.decisions.map { it.policyId })
        assertEquals(
            listOf("observe.memory_pressure"),
            proposal.decisions.mapNotNull { it.proposedActionId }
        )
        assertTrue(proposal.hasActionProposal)
        assertFalse(proposal.actionExecutionAllowed)
        assertTrue(proposal.decisions.all { it.mode == PolicyMode.DRY_RUN })
    }

    @Test
    fun gamingFlagReachesPolicyWithoutEnablingActions() {
        val snapshot = DeviceSnapshot(
            timestampMs = 2_000L,
            androidApi = 33,
            manufacturer = "itel",
            model = "P661N",
            totalRamMb = 8000L,
            availableRamMb = 4000L,
            batteryPercent = 80,
            isCharging = false
        )

        val proposal = DryRunPolicyEvaluator().evaluate(snapshot, isGaming = true)

        assertTrue(proposal.state.isGaming)
        assertEquals(listOf("workload.gaming"), proposal.decisions.map { it.policyId })
        assertFalse(proposal.actionExecutionAllowed)
    }
}
