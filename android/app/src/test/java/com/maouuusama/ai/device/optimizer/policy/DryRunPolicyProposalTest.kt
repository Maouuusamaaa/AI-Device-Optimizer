package com.maouuusama.ai.device.optimizer.policy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DryRunPolicyProposalTest {
    @Test
    fun normalStateProducesObservationOnlyProposal() {
        val state = DeviceState(4000L, 8000L, 80, false, false)
        val proposal = DryRunPolicyProposal.from(state)

        assertEquals(state, proposal.state)
        assertEquals(listOf("device.normal"), proposal.decisions.map { it.policyId })
        assertFalse(proposal.hasActionProposal)
        assertFalse(proposal.actionExecutionAllowed)
        assertTrue(proposal.decisions.all { it.mode == PolicyMode.DRY_RUN })
    }

    @Test
    fun memoryPressureProducesExplicitObservationProposal() {
        val state = DeviceState(1500L, 8000L, 44, false, false)
        val proposal = DryRunPolicyProposal.from(state)

        assertTrue(proposal.hasActionProposal)
        assertEquals(
            listOf("observe.memory_pressure"),
            proposal.decisions.mapNotNull { it.proposedActionId }
        )
        assertFalse(proposal.actionExecutionAllowed)
        assertEquals(1500L, proposal.state.availableRamMb)
        assertEquals(44, proposal.state.batteryPercent)
    }

    @Test
    fun criticalMemoryAndLowBatteryCanCoexistWithoutEnablingActions() {
        val state = DeviceState(900L, 8000L, 15, false, false)
        val proposal = DryRunPolicyProposal.from(state)

        assertEquals(
            setOf("memory.critical", "battery.low"),
            proposal.decisions.map { it.policyId }.toSet()
        )
        assertEquals(
            setOf("observe.memory_critical", "observe.power_pressure"),
            proposal.decisions.mapNotNull { it.proposedActionId }.toSet()
        )
        assertFalse(proposal.actionExecutionAllowed)
    }
}
